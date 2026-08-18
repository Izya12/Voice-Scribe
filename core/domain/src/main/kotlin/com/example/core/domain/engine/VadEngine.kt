package com.example.core.domain.engine

/**
 * Silero VAD boundary (§3, step 3).
 *
 * Detects speech/silence boundaries in a 16 kHz mono float PCM stream and
 * returns lightweight timestamped segments so downstream stages skip silence.
 */
interface VadEngine {

    /**
     * Runs VAD over [pcm] and returns speech segments in chronological order.
     *
     * Segments are non-overlapping and monotonic:
     * `segment[n].endUs <= segment[n+1].startUs` (§10.1).
     */
    suspend fun detectSpeech(pcm: FloatArray): List<SpeechSegment>
}

/**
 * A detected speech interval in microsecond time (§3.1).
 */
data class SpeechSegment(
    val startUs: Long,
    val endUs: Long,
)
