package com.example.core.domain.engine

/**
 * Offline speaker diarization boundary (§3, step 4).
 *
 * Processes speech segments with pyannote-segmentation + ERes2Net/CAM++
 * embeddings and assigns a stable speaker id per segment. Because the canonical
 * model stores a single speaker per segment, overlapping speech is mapped to
 * the dominant speaker (§16, risk 3).
 */
interface DiarizationEngine {

    /**
     * Assigns speaker ids to the audio in [pcm].
     *
     * sherpa-onnx diarization runs its own internal VAD/segmentation over the
     * full waveform, so the engine receives [pcm] rather than pre-cut segments.
     *
     * @param numSpeakers known speaker count for KNOWN_SPEAKER_COUNT mode,
     *   or `null` for AUTOMATIC clustering.
     * @return speaker-annotated segments in chronological order.
     */
    suspend fun diarize(
        pcm: FloatArray,
        numSpeakers: Int?,
    ): List<SpeakerSegment>
}

/**
 * A speech segment annotated with its speaker.
 *
 * [speakerId] is stable per job and assigned by order of first appearance (§26).
 */
data class SpeakerSegment(
    val startUs: Long,
    val endUs: Long,
    val speakerId: Int,
)
