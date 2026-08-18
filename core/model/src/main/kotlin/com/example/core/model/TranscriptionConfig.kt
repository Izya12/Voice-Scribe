package com.example.core.model

/**
 * Language selection mode (§4).
 */
enum class LanguageMode {
    /** Auto-detect via Whisper SpokenLanguageIdentification. */
    AUTO,

    /** Explicit user override (en / ru). */
    MANUAL,
}

/**
 * Speaker diarization modes (§25).
 */
enum class DiarizationMode {
    DISABLED,
    AUTOMATIC,
    KNOWN_SPEAKER_COUNT,
}

/**
 * Transcription configuration embedded into a [TranscriptionJob] (§10.1).
 */
data class TranscriptionConfig(
    val languageMode: LanguageMode = LanguageMode.AUTO,
    val language: String? = null,
    val diarizationMode: DiarizationMode = DiarizationMode.AUTOMATIC,
    val numSpeakers: Int? = null,
    val useVad: Boolean = true,
    val modelId: String? = null,
)
