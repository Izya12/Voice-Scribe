package com.example.core.model

/**
 * Individual word with exact timestamps, used for playback highlighting (§82).
 */
data class Word(
    val id: Long = 0L,
    val segmentId: Long,
    val word: String,
    val startUs: Long,
    val endUs: Long,
    val confidence: Float = 0f,
)
