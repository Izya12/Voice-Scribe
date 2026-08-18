package com.example.core.model

/**
 * A block of text spoken by a single speaker (§10.1).
 *
 * [startUs] and [endUs] are microsecond timestamps; text is aligned to
 * monotonic time (segment[n].endUs <= segment[n+1].startUs) for export.
 */
data class TranscriptionSegment(
    val id: Long = 0L,
    val jobId: String,
    val startUs: Long,
    val endUs: Long,
    val text: String,
    val speakerId: Long? = null,
)
