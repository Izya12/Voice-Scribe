package com.example.core.domain.usecase

import com.example.core.domain.engine.DiarizationEngine
import com.example.core.domain.engine.LanguageDetector
import com.example.core.domain.engine.SpeechEngine
import com.example.core.domain.engine.SpeechSegment
import com.example.core.domain.engine.SpeakerSegment
import com.example.core.domain.engine.VadEngine
import com.example.core.domain.error.DecodingException
import com.example.core.domain.error.ModelManagerException
import com.example.core.domain.error.TranscriptionException
import com.example.core.domain.error.VadException
import com.example.core.domain.repository.ModelRepository
import com.example.core.domain.repository.TranscriptionRepository
import com.example.core.model.DiarizationMode
import com.example.core.model.JobState
import com.example.core.model.LanguageMode
import com.example.core.model.ModelDescriptor
import com.example.core.model.TranscriptionJob
import com.example.core.model.TranscriptionSegment
import com.example.core.model.Word
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList

/**
 * Orchestrates the offline transcription pipeline (В§2, В§3).
 *
 * Stages are strictly sequential and mirror the ratified job state machine
 * (В§22): SUBMITTED в†’ DECODING в†’ PREPROCESSING в†’ DIARIZING в†’ TRANSCRIBING в†’
 * COMPLETED. Skipping a stage or transitioning out of a terminal state is
 * forbidden; failures map to [TranscriptionException] and persist FAILED.
 */
interface RunTranscriptionUseCase {

    /**
     * Runs transcription for [jobId], emitting [JobProgress] reactively.
     *
     * The returned flow completes when the job reaches a terminal state
     * (COMPLETED / FAILED / CANCELLED). Cancellation of the flow cancels the
     * pipeline and persists CANCELLED (В§6.2).
     */
    fun invoke(jobId: String): Flow<JobProgress>
}

/**
 * Default [RunTranscriptionUseCase] wiring the concrete stages together.
 *
 * Kept in `:core:domain` because it depends only on interfaces вЂ” no Android or
 * JNI types. Stages are functor parameters so unit tests can inject fakes.
 */
class DefaultRunTranscriptionUseCase(
    private val jobs: TranscriptionRepository,
    private val models: ModelRepository,
    private val vad: VadEngine,
    private val diarization: DiarizationEngine,
    private val language: LanguageDetector,
    private val speech: SpeechEngine,
) : RunTranscriptionUseCase {

    override fun invoke(jobId: String): Flow<JobProgress> = channelFlow {
        try {
            println("[RunTranscription] start job=$jobId")
            val job = jobs.getJob(jobId) ?: throw DecodingException("Job not found: $jobId")

            // Validate the incoming job is in a startable, non-terminal state (В§22).
            if (job.status.isTerminal) {
                throw IllegalStateException("Job $jobId is already in terminal state ${job.status}")
            }

            send(JobProgress(jobId, JobState.SUBMITTED, 0f))

            val model = resolveModel(job)
            val config = job.config

            // --- DECODING: stream PCM chunks (В§3, step 1-2). ---
            updateState(jobId, JobState.DECODING)
            send(JobProgress(jobId, JobState.DECODING, 0.1f))
            println("[RunTranscription] DECODING useVad=${config.useVad} diar=${config.diarizationMode} model=${model.id}")
            val pcm = jobs.streamPcm(jobId).toList()
            println("[RunTranscription] decoded chunks=${pcm.size} totalSamples=${pcm.sumOf { it.size }}")
            if (pcm.isEmpty()) throw DecodingException("No audio decoded for job $jobId")
            val fullPcm = concat(pcm)
            if (fullPcm.isEmpty()) throw DecodingException("Decoded audio is empty for job $jobId")

            // --- PREPROCESSING: optional VAD segmentation (В§3, step 3). ---
            updateState(jobId, JobState.PREPROCESSING)
            send(JobProgress(jobId, JobState.PREPROCESSING, 0.2f))
            val speechSegments =
                if (config.useVad) {
                    try {
                        val segments = vad.detectSpeech(fullPcm)
                        if (segments.isEmpty()) {
                            // A VAD that finds nothing (missing/broken model, no
                            // speech detected) must not silently yield an empty
                            // transcript — fall back to fixed 30 s chunks.
                            println("[RunTranscription] VAD found no speech, falling back to fixed chunks")
                            fixedDurationSegments(fullPcm.size, WHISPER_CHUNK_SAMPLES)
                        } else {
                            // VAD emits short speech snippets (silero: 0.25 s+).
                            // Whisper transcribes far worse when fed isolated
                            // fragments (no context, words cut at edges), so
                            // merge consecutive snippets (gaps included) into
                            // chunks of up to ~29 s — speech boundaries are
                            // preserved, silence is skipped, quality kept.
                            val merged = mergeSegments(segments, WHISPER_CHUNK_SAMPLES_US)
                            println("[RunTranscription] VAD segments=${segments.size} merged=$merged")
                            merged
                        }
                    } catch (e: VadException) {
                        println("[RunTranscription] VAD failed (${e.message}), falling back to fixed chunks")
                        fixedDurationSegments(fullPcm.size, WHISPER_CHUNK_SAMPLES)
                    }
                } else {
                    fixedDurationSegments(fullPcm.size, WHISPER_CHUNK_SAMPLES)
                }
            println("[RunTranscription] PREPROCESSING segments=${speechSegments.size}")

            // --- DIARIZING (В§3, step 4). The state machine forbids skipping this
            // step (§22), so it is always visited; actual diarization runs only
            // when enabled. ---
            updateState(jobId, JobState.DIARIZING)
            send(JobProgress(jobId, JobState.DIARIZING, 0.3f))
            val speakerSegments =
                if (config.diarizationMode == DiarizationMode.DISABLED) {
                    println("[RunTranscription] DIARIZING disabled")
                    emptyList()
                } else {
                    println("[RunTranscription] DIARIZING start numSpeakers=${config.numSpeakers}")
                    val segments = diarization.diarize(
                        pcm = fullPcm,
                        numSpeakers = config.numSpeakers,
                    )
                    println("[RunTranscription] DIARIZING done count=${segments.size}")
                    segments
                }

            // --- Language: AUTO uses native detection, MANUAL passes config. ---
            // An invalid/blank detection result is treated as "unknown" and
            // passed through as null so Whisper falls back to its own internal
            // per-chunk language detection instead of being forced to "en".
            val languageCode = when (config.languageMode) {
                LanguageMode.MANUAL -> config.language
                LanguageMode.AUTO -> {
                    val detected = language.detectLanguage(fullPcm, model).languageCode
                    println("[RunTranscription] AUTO detected language=\"$detected\"")
                    detected.takeIf { it.length == 2 && it[0].isLowerCase() && it[1].isLowerCase() }
                }
            }

            // --- TRANSCRIBING (В§3, step 6). ---
            updateState(jobId, JobState.TRANSCRIBING)
            send(JobProgress(jobId, JobState.TRANSCRIBING, 0.4f))
            val segments = mutableListOf<TranscriptionSegment>()
            val words = mutableListOf<Word>()
            val speakerIdByCluster = mutableMapOf<Int, Long>()

            speechSegments.forEachIndexed { index, seg ->
                val segmentPcm = slice(fullPcm, seg.startUs, seg.endUs)
                println("[RunTranscription] TRANSCRIBING ${index + 1}/${speechSegments.size} seg=${seg.startUs}-${seg.endUs} samples=${segmentPcm.size}")
                if (segmentPcm.isEmpty()) {
                    // Degenerate slice (e.g. VAD edge at EOF); feeding it to
                    // sherpa crashes natively (SIGSEGV), so skip it.
                    println("[RunTranscription]   empty slice, skipping")
                    return@forEachIndexed
                }
                val result = speech.transcribe(segmentPcm, model, languageCode)
                println("[RunTranscription]   done text=\"${result.text.take(60)}\"")

val speakerId = assignSpeaker(seg, speakerSegments, speakerIdByCluster)

                segments += TranscriptionSegment(
                    id = 0L,
                    jobId = jobId,
                    startUs = seg.startUs,
                    endUs = seg.endUs,
                    text = result.text,
                    speakerId = speakerId,
                )
                words += result.words.map { word ->
                    Word(
                        id = 0L,
                        segmentId = index.toLong(),
                        word = word.word,
                        startUs = seg.startUs + word.startUs,
                        endUs = seg.startUs + word.endUs,
                        confidence = word.confidence,
                    )
                }
                send(JobProgress(jobId, JobState.TRANSCRIBING, (index + 1f) / speechSegments.size))
            }

            // --- COMPLETED: persist final canonical transcript (В§2, step 7). ---
            jobs.saveTranscript(jobId, segments, words)
            updateState(jobId, JobState.COMPLETED)
            send(JobProgress(jobId, JobState.COMPLETED, 1f))
            println("[RunTranscription] COMPLETED segments=${segments.size} words=${words.size}")
        } catch (e: CancellationException) {
            println("[RunTranscription] CANCELLED job=$jobId")
            persistTerminal(jobId, JobState.CANCELLED)
            throw e
        } catch (e: TranscriptionException) {
            println("[RunTranscription] FAILED: ${e.message}")
            persistTerminal(jobId, JobState.FAILED)
            send(JobProgress(jobId, JobState.FAILED, 0f))
        } catch (e: Exception) {
            println("[RunTranscription] ERROR: ${e}")
            persistTerminal(jobId, JobState.FAILED)
            send(JobProgress(jobId, JobState.FAILED, 0f))
        }
    }

private fun assignSpeaker(
        seg: SpeechSegment,
        speakerSegments: List<SpeakerSegment>,
        speakerIdByCluster: MutableMap<Int, Long>,
    ): Long? {
        if (speakerSegments.isEmpty()) return null
        val midpoint = (seg.startUs + seg.endUs) / 2
        val cluster = speakerSegments
            .firstOrNull { midpoint in it.startUs until it.endUs }
            ?.speakerId
            ?: return null
        return speakerIdByCluster.getOrPut(cluster) { speakerIdByCluster.size + 1L }
    }

    private suspend fun resolveModel(job: TranscriptionJob): ModelDescriptor {
        val installed = models.observeInstalledModels().first()
        val asrModels = installed.filter { it.id.startsWith(ASR_MODEL_PREFIX) }
        if (asrModels.isEmpty()) {
            throw ModelManagerException("No ASR model installed")
        }
        val id = job.config.modelId
            ?: asrModels.minByOrNull { it.tier.ordinal }?.id
            ?: throw ModelManagerException("No ASR model installed")
        return asrModels.firstOrNull { it.id == id }
            ?: throw ModelManagerException("ASR model not installed: $id")
    }

private suspend fun updateState(jobId: String, state: JobState) {
        val current = jobs.getJob(jobId) ?: return
        if (!current.status.allowedTransitions().contains(state)) return
        jobs.saveJob(current.copy(status = state, updatedAtUs = nowUs()))
    }

    private suspend fun persistTerminal(jobId: String, state: JobState) {
        val current = jobs.getJob(jobId) ?: return
        // Never overwrite an already-terminal state (§22).
        if (current.status.isTerminal) return
        jobs.saveJob(current.copy(status = state, updatedAtUs = nowUs()))
    }

    private fun nowUs(): Long = System.currentTimeMillis() * 1_000L

    private fun concat(chunks: List<FloatArray>): FloatArray {
        val total = chunks.sumOf { it.size }
        val out = FloatArray(total)
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(out, offset)
            offset += chunk.size
        }
        return out
    }

private fun slice(pcm: FloatArray, startUs: Long, endUs: Long): FloatArray {
        val start = usToSamples(startUs).coerceIn(0, pcm.size)
        val end = usToSamples(endUs).coerceIn(start, pcm.size)
        return pcm.copyOfRange(start, end)
    }

    /**
     * Splits a full PCM buffer into fixed-duration segments without VAD.
     * Whisper's context window is ~30 s, so anything longer must not be fed
     * to the recognizer as a single slice (it would be truncated/dropped).
     */
    /**
     * Merges consecutive VAD speech snippets (gaps included) into chunks no
     * longer than [maxDurationUs]. Whisper degrades on isolated short
     * fragments, so snippets are packed until the cap is exceeded; the chunk
     * boundary then falls on a real speech gap instead of mid-word.
     */
    private fun mergeSegments(segments: List<SpeechSegment>, maxDurationUs: Long): List<SpeechSegment> {
        if (segments.size <= 1) return segments
        val merged = mutableListOf<SpeechSegment>()
        var start = segments.first().startUs
        var end = segments.first().endUs
        for (seg in segments.drop(1)) {
            if (seg.endUs - start <= maxDurationUs) {
                end = seg.endUs
            } else {
                merged += SpeechSegment(start, end)
                start = seg.startUs
                end = seg.endUs
            }
        }
        merged += SpeechSegment(start, end)
        return merged
    }

    private fun fixedDurationSegments(totalSamples: Int, chunkSamples: Int): List<SpeechSegment> {
        if (totalSamples <= 0) return emptyList()
        val nChunks = (totalSamples + chunkSamples - 1) / chunkSamples
        val totalUs = samplesToUs(totalSamples.toLong())
        return (0 until nChunks).map { i ->
            val start = samplesToUs(i.toLong() * chunkSamples)
            val end = samplesToUs((i + 1).toLong() * chunkSamples).coerceAtMost(totalUs)
            SpeechSegment(start, end)
        }
    }

private companion object {
        const val SAMPLE_RATE_HZ = 16_000
        /** 29 s of 16 kHz PCM — just inside Whisper's 30 s context window;
         *  sherpa-onnx rejects waves of exactly 30 s ("less than 30 seconds
         *  are supported") and discards the remainder. */
        const val WHISPER_CHUNK_SAMPLES = 29 * SAMPLE_RATE_HZ
        /** Same window expressed in µs for the VAD segment merger. */
        const val WHISPER_CHUNK_SAMPLES_US = 29_000_000L
        /** ASR models are catalogued with a `whisper-*` id; VAD/speaker models are excluded. */
        const val ASR_MODEL_PREFIX = "whisper-"

        /** Exact µs → sample conversion; avoids 1_000_000/16_000 integer truncation. */
        private fun usToSamples(us: Long): Int = (us * SAMPLE_RATE_HZ / 1_000_000L).toInt()

        /** Exact sample → µs conversion. */
        private fun samplesToUs(samples: Long): Long = samples * 1_000_000L / SAMPLE_RATE_HZ
    }
}
