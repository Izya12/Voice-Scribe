package com.example.core.domain.usecase

import com.example.core.domain.engine.DiarizationEngine
import com.example.core.domain.engine.LanguageDetector
import com.example.core.domain.engine.LanguageDetectionResult
import com.example.core.domain.engine.RecognizedWord
import com.example.core.domain.engine.RecognitionResult
import com.example.core.domain.engine.SpeechEngine
import com.example.core.domain.engine.SpeechSegment
import com.example.core.domain.engine.SpeakerSegment
import com.example.core.domain.engine.VadEngine
import com.example.core.domain.error.DecodingException
import com.example.core.domain.error.VadException
import com.example.core.domain.logging.AppLogger
import com.example.core.model.LogLevel
import com.example.core.domain.repository.ModelRepository
import com.example.core.domain.repository.Transcript
import com.example.core.domain.repository.TranscriptionRepository
import com.example.core.model.DiarizationMode
import com.example.core.model.JobState
import com.example.core.model.LanguageMode
import com.example.core.model.ModelDescriptor
import com.example.core.model.ModelTier
import com.example.core.model.TranscriptionConfig
import com.example.core.model.TranscriptionJob
import com.example.core.model.TranscriptionSegment
import com.example.core.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunTranscriptionUseCaseTest {

    private val fakeLogger = object : AppLogger {
        override fun isEnabled(level: LogLevel) = true
        override fun debug(tag: String, message: String, throwable: Throwable?) {}
        override fun info(tag: String, message: String, throwable: Throwable?) {}
        override fun warn(tag: String, message: String, throwable: Throwable?) {}
        override fun error(tag: String, message: String, throwable: Throwable?) {}
    }

    private val model = ModelDescriptor(
        id = "whisper-tiny",
        displayName = "Whisper Tiny",
        fileName = "tiny.onnx",
        fileSizeBytes = 100,
        sourceUrl = "https://example.com/tiny.onnx",
        sha256 = "0".repeat(64),
        license = "MIT",
        tier = ModelTier.ENTRY,
    )

    private val fakeVad = object : VadEngine {
        override suspend fun detectSpeech(pcm: FloatArray): List<SpeechSegment> =
            listOf(SpeechSegment(0L, 1000L), SpeechSegment(2000L, 3000L))
    }

    private val fakeDiarization = object : DiarizationEngine {
        override suspend fun diarize(
            pcm: FloatArray,
            numSpeakers: Int?,
        ): List<SpeakerSegment> = listOf(
            SpeakerSegment(0L, 1500L, 0),
            SpeakerSegment(1500L, 3000L, 1),
        )
    }

    private val fakeLanguage = object : LanguageDetector {
        override suspend fun detectLanguage(
            pcm: FloatArray,
            model: ModelDescriptor,
        ): LanguageDetectionResult = LanguageDetectionResult("en", 0.9f)
    }

    private val fakeSpeech = object : SpeechEngine {
        override suspend fun transcribe(
            pcm: FloatArray,
            model: ModelDescriptor,
            language: String?,
        ): RecognitionResult = RecognitionResult(
            text = "hello world",
            language = language,
            words = listOf(
                RecognizedWord("hello", 0L, 500L, 0.9f),
                RecognizedWord("world", 500L, 1000L, 0.8f),
            ),
        )
    }

    @Test
    fun `full pipeline reaches COMPLETED with persisted transcript`() = runTest {
        val repo = FakeTranscriptionRepository(job(jobId = "j1"))
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        val progress = useCase.invoke("j1").toList()

        assertEquals(JobState.COMPLETED, progress.last().state)
        val states = progress.map { it.state }
        assertTrue(states.contains(JobState.SUBMITTED))
        assertTrue(states.contains(JobState.DECODING))
        assertTrue(states.contains(JobState.PREPROCESSING))
        assertTrue(states.contains(JobState.DIARIZING))
        assertTrue(states.contains(JobState.TRANSCRIBING))
        assertEquals(JobState.COMPLETED, repo.savedJob?.status)

        val transcript = repo.savedTranscript
        // The two 1 ms snippets merge into a single 0-3000 µs chunk.
        assertEquals(1, transcript?.segments?.size)
        assertEquals("hello world", transcript?.segments?.first()?.text)
        assertEquals(2, transcript?.words?.size)
        // speakers mapped by first appearance (midpoint 1500 µs -> cluster 0)
        assertEquals(1L, transcript?.segments?.first()?.speakerId)
    }

    @Test
    fun `language is detected in AUTO mode`() = runTest {
        val repo = FakeTranscriptionRepository(job(jobId = "j1"))
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        useCase.invoke("j1").toList()

        assertTrue(repo.savedTranscript!!.segments.all { it.speakerId != null })
    }

    @Test
    fun `MANUAL language is passed through without detection`() = runTest {
        var detected = false
        val lang = object : LanguageDetector {
            override suspend fun detectLanguage(
                pcm: FloatArray,
                model: ModelDescriptor,
            ): LanguageDetectionResult {
                detected = true
                return LanguageDetectionResult("ru", 1f)
            }
        }
        val repo = FakeTranscriptionRepository(
            job(jobId = "j1", config = TranscriptionConfig(languageMode = LanguageMode.MANUAL, language = "ru")),
        )
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = fakeDiarization,
            language = lang,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        useCase.invoke("j1").toList()

        assertTrue("LanguageDetector must be bypassed in MANUAL mode", !detected)
    }

    @Test
    fun `AUTO with blank detection falls back to Whisper native detection`() = runTest {
        val repo = FakeTranscriptionRepository(job(jobId = "j1"))
        var passedLanguage: String? = "unset"
        val speech = object : SpeechEngine {
            override suspend fun transcribe(
                pcm: FloatArray,
                model: ModelDescriptor,
                language: String?,
            ): RecognitionResult {
                passedLanguage = language
                return RecognitionResult(text = "hello", language = language, words = emptyList())
            }
        }
        val blankLang = object : LanguageDetector {
            override suspend fun detectLanguage(
                pcm: FloatArray,
                model: ModelDescriptor,
            ): LanguageDetectionResult = LanguageDetectionResult("", 0f)
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = fakeDiarization,
            language = blankLang,
            speech = speech,
            logger = fakeLogger,
        )

        useCase.invoke("j1").toList()

        // A blank/non-ISO detection must degrade to Whisper's internal
        // detection (null), never to a hardcoded "en".
        assertEquals(null, passedLanguage)
        assertEquals(JobState.COMPLETED, repo.savedJob?.status)
    }

    @Test
    fun `VAD failure falls back to fixed chunks instead of empty transcript`() = runTest {
        val repo = FakeTranscriptionRepository(job(jobId = "j1"))
        val failingVad = object : VadEngine {
            override suspend fun detectSpeech(pcm: FloatArray): List<SpeechSegment> =
                throw VadException("Silero VAD model not installed")
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = failingVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        val progress = useCase.invoke("j1").toList()

        assertEquals(JobState.COMPLETED, progress.last().state)
        assertEquals(1, repo.savedTranscript?.segments?.size)
    }

    @Test
    fun `VAD empty result falls back to fixed chunks`() = runTest {
        val repo = FakeTranscriptionRepository(job(jobId = "j1"))
        val emptyVad = object : VadEngine {
            override suspend fun detectSpeech(pcm: FloatArray): List<SpeechSegment> = emptyList()
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = emptyVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        val progress = useCase.invoke("j1").toList()

        assertEquals(JobState.COMPLETED, progress.last().state)
        assertEquals(1, repo.savedTranscript?.segments?.size)
    }

    @Test
    fun `VAD snippets are merged into near-window chunks so Whisper gets context`() = runTest {
        // 41 s of PCM so every merged chunk has a non-empty slice.
        val repo = object : FakeTranscriptionRepository(job(jobId = "j1")) {
            override fun streamPcm(jobId: String): Flow<FloatArray> =
                flowOf(FloatArray(41 * 16_000) { 0.1f })
        }
        // Four 10 s snippets: 0-10, 10-20, 20-30, 30-40 s.
        val snippetVad = object : VadEngine {
            override suspend fun detectSpeech(pcm: FloatArray): List<SpeechSegment> =
                (0L until 40_000_000L step 10_000_000L).map { s ->
                    SpeechSegment(s, s + 10_000_000L)
                }
        }
        val sliceSizes = mutableListOf<Int>()
        val speech = object : SpeechEngine {
            override suspend fun transcribe(
                pcm: FloatArray,
                model: ModelDescriptor,
                language: String?,
            ): RecognitionResult {
                sliceSizes += pcm.size
                return fakeSpeech.transcribe(pcm, model, language)
            }
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = snippetVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = speech,
            logger = fakeLogger,
        )

        useCase.invoke("j1").toList()

        // Merged into 2 chunks of 20 s each (cap = 29 s), not 4 snippets.
        assertEquals(listOf(320_000, 320_000), sliceSizes)
        assertEquals(2, repo.savedTranscript?.segments?.size)
        assertEquals(4, repo.savedTranscript?.words?.size)
    }

    @Test
    fun `words are split into per-speaker turns when diarization is enabled`() = runTest {
        val repo = FakeTranscriptionRepository(job(jobId = "j1"))
        val speech = object : SpeechEngine {
            override suspend fun transcribe(
                pcm: FloatArray,
                model: ModelDescriptor,
                language: String?,
            ): RecognitionResult = RecognitionResult(
                text = "one two three",
                language = language,
                // turn (0-1500) covers one/two, turn (1500-3000) covers three
                words = listOf(
                    RecognizedWord("one", 0L, 500L, 0.9f),
                    RecognizedWord("two", 600L, 1100L, 0.9f),
                    RecognizedWord("three", 1600L, 2100L, 0.9f),
                ),
            )
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = speech,
            logger = fakeLogger,
        )

        useCase.invoke("j1").toList()

        val segments = repo.savedTranscript?.segments.orEmpty()
        assertEquals(2, segments.size)
        assertEquals(0L, segments[0].startUs)
        assertEquals(1100L, segments[0].endUs)
        assertEquals("one two", segments[0].text)
        assertEquals(1L, segments[0].speakerId)
        assertEquals(1600L, segments[1].startUs)
        assertEquals(2100L, segments[1].endUs)
        assertEquals("three", segments[1].text)
        assertEquals(2L, segments[1].speakerId)
        assertEquals(3, repo.savedTranscript?.words?.size)
    }

    @Test
    fun `word in a diarization gap yields an unlabeled segment`() = runTest {
        val repo = FakeTranscriptionRepository(job(jobId = "j1"))
        val gappyDiarization = object : DiarizationEngine {
            override suspend fun diarize(
                pcm: FloatArray,
                numSpeakers: Int?,
            ): List<SpeakerSegment> = listOf(
                SpeakerSegment(0L, 1000L, 0),
                SpeakerSegment(2000L, 3000L, 1),
            )
        }
        val speech = object : SpeechEngine {
            override suspend fun transcribe(
                pcm: FloatArray,
                model: ModelDescriptor,
                language: String?,
            ): RecognitionResult = RecognitionResult(
                text = "one two three",
                language = language,
                // "two" (mid 1050) falls between the two turns
                words = listOf(
                    RecognizedWord("one", 0L, 500L, 0.9f),
                    RecognizedWord("two", 1000L, 1100L, 0.9f),
                    RecognizedWord("three", 2000L, 2500L, 0.9f),
                ),
            )
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = gappyDiarization,
            language = fakeLanguage,
            speech = speech,
            logger = fakeLogger,
        )

        useCase.invoke("j1").toList()

        val segments = repo.savedTranscript?.segments.orEmpty()
        assertEquals(3, segments.size)
        assertEquals(1L, segments[0].speakerId)
        assertEquals(null, segments[1].speakerId)
        assertEquals(2L, segments[2].speakerId)
    }

    @Test
    fun `zero word timestamps keep chunk text intact and label by dominant turn`() = runTest {
        val repo = FakeTranscriptionRepository(job(jobId = "j1"))
        // sherpa whisper reports token-level words with start==end==0.
        val speech = object : SpeechEngine {
            override suspend fun transcribe(
                pcm: FloatArray,
                model: ModelDescriptor,
                language: String?,
            ): RecognitionResult = RecognitionResult(
                text = "Да. Помнишь там письмо?",
                language = language,
                words = listOf(
                    RecognizedWord("Да", 0L, 0L, 0.9f),
                    RecognizedWord("Пом", 0L, 0L, 0.9f),
                    RecognizedWord("ни", 0L, 0L, 0.9f),
                    RecognizedWord("шь", 0L, 0L, 0.9f),
                ),
            )
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = speech,
            logger = fakeLogger,
        )

        useCase.invoke("j1").toList()

        // Merged chunk [0-3000]; turns (0-1500, 1500-3000) tie -> first wins.
        val segments = repo.savedTranscript?.segments.orEmpty()
        assertEquals(1, segments.size)
        assertEquals(0L, segments[0].startUs)
        assertEquals(3000L, segments[0].endUs)
        assertEquals("Да. Помнишь там письмо?", segments[0].text)
        assertEquals(1L, segments[0].speakerId)
        assertEquals(4, repo.savedTranscript?.words?.size)
    }

    @Test
    fun `VAD disabled treats whole audio as one segment`() = runTest {
        val repo = FakeTranscriptionRepository(
            job(jobId = "j1", config = TranscriptionConfig(useVad = false)),
        )
        var vadCalled = false
        val vad = object : VadEngine {
            override suspend fun detectSpeech(pcm: FloatArray): List<SpeechSegment> {
                vadCalled = true
                return listOf()
            }
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = vad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        useCase.invoke("j1").toList()

        assertTrue("VAD must not be called when disabled", !vadCalled)
        assertEquals(1, repo.savedTranscript?.segments?.size)
    }

    @Test
    fun `diarization disabled still walks the state machine and reaches COMPLETED in DB`() = runTest {
        val repo = FakeTranscriptionRepository(
            job(jobId = "j1", config = TranscriptionConfig(diarizationMode = DiarizationMode.DISABLED)),
        )
        var diarizeCalled = false
        val diarization = object : DiarizationEngine {
            override suspend fun diarize(
                pcm: FloatArray,
                numSpeakers: Int?,
            ): List<SpeakerSegment> {
                diarizeCalled = true
                return listOf()
            }
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = diarization,
            language = fakeLanguage,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        val progress = useCase.invoke("j1").toList()

        // §22 forbids skipping stages: DIARIZING must be visited even when
        // disabled, otherwise TRANSCRIBING/COMPLETED never persist (job gets
        // stuck in PREPROCESSING).
        assertTrue(progress.map { it.state }.contains(JobState.DIARIZING))
        assertTrue("diarization engine must not run when disabled", !diarizeCalled)
        assertEquals(JobState.COMPLETED, progress.last().state)
        assertEquals(JobState.COMPLETED, repo.savedJob?.status)
        assertEquals(null, repo.savedTranscript?.segments?.first()?.speakerId)
    }

    @Test
    fun `VAD segment near EOF is not sliced into empty audio`() = runTest {
        val repo = object : FakeTranscriptionRepository(job(jobId = "j1")) {
            override fun streamPcm(jobId: String): Flow<FloatArray> =
                flowOf(FloatArray(480_000) { 0.1f })
        }
        var lastSliceSize = -1
        val speech = object : SpeechEngine {
            override suspend fun transcribe(
                pcm: FloatArray,
                model: ModelDescriptor,
                language: String?,
            ): RecognitionResult {
                lastSliceSize = pcm.size
                return RecognitionResult(
                    text = "hi",
                    language = language,
                    words = listOf(RecognizedWord("hi", 0L, 100L, 0.9f)),
                )
            }
        }
        val vad = object : VadEngine {
            // 29.8s..29.9s of a 30s buffer: the old `us / 62` conversion
            // clamped past the buffer end and produced an empty slice.
            override suspend fun detectSpeech(pcm: FloatArray): List<SpeechSegment> =
                listOf(SpeechSegment(29_800_000L, 29_900_000L))
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = vad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = speech,
            logger = fakeLogger,
        )

        val progress = useCase.invoke("j1").toList()

        assertEquals(JobState.COMPLETED, progress.last().state)
        assertEquals(1_600, lastSliceSize)
    }

    @Test
    fun `missing job maps to FAILED`() = runTest {
        val repo = FakeTranscriptionRepository(null)
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        val progress = useCase.invoke("nope").toList()

        assertEquals(JobState.FAILED, progress.last().state)
    }

    @Test
    fun `model missing maps to FAILED`() = runTest {
        val repo = FakeTranscriptionRepository(job(jobId = "j1"))
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(),
            vad = fakeVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        val progress = useCase.invoke("j1").toList()

        assertEquals(JobState.FAILED, progress.last().state)
    }

    @Test
    fun `decoder throwing maps to FAILED`() = runTest {
        val repo = object : FakeTranscriptionRepository(job(jobId = "j1")) {
            override fun streamPcm(jobId: String): Flow<FloatArray> = flow { throw DecodingException("codec") }
        }
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        val progress = useCase.invoke("j1").toList()

        assertEquals(JobState.FAILED, progress.last().state)
        assertEquals(JobState.FAILED, repo.savedJob?.status)
    }

    @Test
    fun `already-terminal job stays terminal`() = runTest {
        val repo = FakeTranscriptionRepository(job(jobId = "j1", status = JobState.COMPLETED))
        val useCase = DefaultRunTranscriptionUseCase(
            jobs = repo,
            models = FakeModelRepository(model),
            vad = fakeVad,
            diarization = fakeDiarization,
            language = fakeLanguage,
            speech = fakeSpeech,
            logger = fakeLogger,
        )

        val progress = useCase.invoke("j1").toList()

        assertEquals(JobState.FAILED, progress.last().state)
        assertEquals(JobState.COMPLETED, repo.savedJob?.status)
    }

    private fun job(
        jobId: String,
        status: JobState = JobState.SUBMITTED,
        config: TranscriptionConfig = TranscriptionConfig(),
    ) = TranscriptionJob(
        id = jobId,
        status = status,
        filePath = "/audio.mp3",
        createdAtUs = 0L,
        updatedAtUs = 0L,
        config = config,
    )

    private class FakeModelRepository(vararg installed: ModelDescriptor) : ModelRepository {
        private val catalog = flowOf(listOf(*installed))
        private val installedModels = flowOf(listOf(*installed))
        private val activeModelId: String? = installed.firstOrNull()?.id

        override fun observeCatalog(): Flow<List<ModelDescriptor>> = catalog
        override fun observeInstalledModels(): Flow<List<ModelDescriptor>> = installedModels
        override fun observeActiveModelId(): Flow<String?> = flowOf(activeModelId)
        override suspend fun isInstalled(modelId: String): Boolean = installedModels.first().any { it.id == modelId }
        override suspend fun download(model: ModelDescriptor, onProgress: (Float) -> Unit) {}
        override suspend fun delete(modelId: String) {}
        override suspend fun setActive(modelId: String) {}
    }

    private open class FakeTranscriptionRepository(initial: TranscriptionJob?) : TranscriptionRepository {
        var savedJob: TranscriptionJob? = initial
        var savedTranscript: Transcript? = null
        private val current = initial

        override fun observeJob(jobId: String): Flow<TranscriptionJob?> = flowOf(current)
        override fun observeJobs(): Flow<List<TranscriptionJob>> = flowOf(listOfNotNull(current))
        override fun streamPcm(jobId: String): Flow<FloatArray> = flowOf(FloatArray(48) { 0.1f })
        override suspend fun getJob(jobId: String): TranscriptionJob? = savedJob
        override suspend fun saveJob(job: TranscriptionJob) {
            savedJob = job
        }

        override suspend fun saveTranscript(jobId: String, segments: List<TranscriptionSegment>, words: List<Word>) {
            savedTranscript = Transcript(jobId, segments, words)
        }

        override suspend fun getTranscript(jobId: String): Transcript? = savedTranscript
        override fun observeTranscript(jobId: String): Flow<Transcript> =
            flowOf(savedTranscript ?: Transcript(jobId, emptyList(), emptyList()))
        override suspend fun renameSpeaker(jobId: String, speakerId: Long, displayName: String) {}
        override suspend fun searchInJob(jobId: String, query: String, limit: Int): List<TranscriptionSegment> = emptyList()
        override suspend fun deleteJob(jobId: String) {}
        override suspend fun reconcileStaleJobs() {}
    }
}