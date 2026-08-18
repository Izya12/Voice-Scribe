package com.example.core.domain.repository

import com.example.core.model.Speaker
import com.example.core.model.TranscriptionJob
import com.example.core.model.TranscriptionSegment
import com.example.core.model.Word
import kotlinx.coroutines.flow.Flow

/**
 * Persistence + media-decoding boundary for transcription jobs (§1.2, §2).
 *
 * Implemented by `:data` (Room + Media3). Exposes cold [Flow] streams so the
 * UI can observe state transitions reactively (§2, step 8).
 */
interface TranscriptionRepository {

    /** Cold stream of the given job's state, null while it does not exist. */
    fun observeJob(jobId: String): Flow<TranscriptionJob?>

    /** Cold stream of all jobs, newest first. */
    fun observeJobs(): Flow<List<TranscriptionJob>>

    /** Cold stream of decoded PCM chunks for [jobId] (§7.1 chunking). */
    fun streamPcm(jobId: String): Flow<FloatArray>

    suspend fun getJob(jobId: String): TranscriptionJob?

    suspend fun saveJob(job: TranscriptionJob)

    /** Persists the final canonical transcript (segments + words) atomically. */
    suspend fun saveTranscript(
        jobId: String,
        segments: List<TranscriptionSegment>,
        words: List<Word>,
    )

    suspend fun getTranscript(jobId: String): Transcript?

    /** Cold stream of the fully persisted transcript (segments, words, speakers). */
    fun observeTranscript(jobId: String): Flow<Transcript>

    /**
     * Renames a speaker. Speaker ids are stable (§26) — only [Speaker.displayName]
     * changes; segments keep referencing the same id.
     */
    suspend fun renameSpeaker(jobId: String, speakerId: Long, displayName: String)

    /**
     * Full-text search scoped to [jobId] (§13). Returns the matching segments
     * ordered by their timeline position.
     */
    suspend fun searchInJob(jobId: String, query: String, limit: Int): List<TranscriptionSegment>

    /**
     * Marks every non-terminal job as [JobState.FAILED].
     *
     * Called at app startup: a non-terminal job means the process died mid-run
     * (e.g. a native crash), so the stuck state can never reach COMPLETED on
     * its own. Reconciliation prevents permanently stuck entries in the UI.
     */
    suspend fun reconcileStaleJobs()
}

/**
 * A fully persisted transcript with aligned segments, words and speakers (§10).
 */
data class Transcript(
    val jobId: String,
    val segments: List<TranscriptionSegment>,
    val words: List<Word>,
    val speakers: List<Speaker> = emptyList(),
)
