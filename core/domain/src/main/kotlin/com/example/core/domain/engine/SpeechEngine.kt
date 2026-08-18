package com.example.core.domain.engine

import com.example.core.model.ModelDescriptor

/**
 * Speech recognition (Whisper ASR) boundary (§3, step 6).
 *
 * Native sherpa-onnx calls must run on a thread-safe dispatcher; all concrete
 * implementations own their native [ModelDescriptor] lifecycle and close()
 * semantics. PCM is always 16 kHz, mono, float32 in range [-1.0f, 1.0f] (§3.1).
 */
interface SpeechEngine {

    /**
     * Transcribes [pcm] into text with word-level timestamps.
     *
     * @param pcm 16 kHz mono float32 audio.
     * @param model the installed ASR model descriptor.
     * @param language ISO-639-1 code or `null` to let the native runtime pick.
     */
    suspend fun transcribe(
        pcm: FloatArray,
        model: ModelDescriptor,
        language: String?,
    ): RecognitionResult
}

/**
 * Word-level recognition output with microsecond timestamps (§10.1).
 */
data class RecognitionResult(
    val text: String,
    val words: List<RecognizedWord>,
    val language: String? = null,
)

/**
 * Single recognized word, aligned to canonical time (§82).
 */
data class RecognizedWord(
    val word: String,
    val startUs: Long,
    val endUs: Long,
    val confidence: Float = 0f,
)
