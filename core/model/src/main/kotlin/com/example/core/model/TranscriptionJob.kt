package com.example.core.model

/**
 * Master record for a single transcription request (§10.1).
 *
 * All timestamps are stored as microsecond [Long] values (§30).
 */
data class TranscriptionJob(
    val id: String,
    val status: JobState,
    val filePath: String,
    val createdAtUs: Long,
    val updatedAtUs: Long,
    val config: TranscriptionConfig = TranscriptionConfig(),
)
