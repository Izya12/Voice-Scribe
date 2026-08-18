package com.example.core.domain.engine

import com.example.core.model.ModelDescriptor

/**
 * Language detection & selection subsystem boundary (§4).
 *
 * AUTO mode uses the native SpokenLanguageIdentification API over the already
 * installed Whisper model; MANUAL mode bypasses this interface entirely and
 * passes the user-selected language straight to the ASR config (§4.2).
 */
interface LanguageDetector {

    /**
     * Detects the spoken language of [pcm].
     *
     * @param pcm 16 kHz mono float32 audio (first 30s is sufficient, §4.1).
     * @return an ISO-639-1 code with confidence in [0.0, 1.0].
     */
    suspend fun detectLanguage(
        pcm: FloatArray,
        model: ModelDescriptor,
    ): LanguageDetectionResult
}

/**
 * Result of automatic language identification (§4).
 */
data class LanguageDetectionResult(
    val languageCode: String, // ISO-639-1, e.g. "en", "ru"
    val confidence: Float,     // Range [0.0, 1.0]
)
