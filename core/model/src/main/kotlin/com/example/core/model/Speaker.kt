package com.example.core.model

/**
 * Stable speaker profile for a specific job (§26).
 *
 * Speaker IDs are assigned by order of first appearance and must not change
 * on rename — the DB keeps a persistent [id] separate from [displayName].
 */
data class Speaker(
    val id: Long = 0L,
    val jobId: String,
    val displayName: String,
    val colorIndex: Int = 0,
)
