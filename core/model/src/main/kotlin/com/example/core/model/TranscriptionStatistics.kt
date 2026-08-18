package com.example.core.model

/**
 * One-to-one stats with a [TranscriptionJob] (§10.1).
 */
data class TranscriptionStatistics(
    val jobId: String,
    val durationUs: Long = 0L,
    val processingTimeMs: Long = 0L,
    val rtf: Float = 0f,
)
