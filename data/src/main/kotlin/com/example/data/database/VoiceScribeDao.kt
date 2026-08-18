package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.data.database.entity.JobEntity
import com.example.data.database.entity.ModelEntity
import com.example.data.database.entity.SegmentEntity
import com.example.data.database.entity.SegmentFtsEntity
import com.example.data.database.entity.SpeakerEntity
import com.example.data.database.entity.StatisticsEntity
import com.example.data.database.entity.WordEntity
import com.example.core.model.JobState
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for transcription jobs, transcripts, and models (§10, §13).
 */
@Dao
interface VoiceScribeDao {

    // --- Jobs ---
    @Query("SELECT * FROM transcription_job WHERE id = :jobId")
    fun observeJob(jobId: String): Flow<JobEntity?>

    @Query("SELECT * FROM transcription_job ORDER BY created_at_us DESC")
    fun observeJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM transcription_job WHERE id = :jobId")
    suspend fun getJob(jobId: String): JobEntity?

    // NOTE: @Upsert (not @Insert REPLACE) — REPLACE does DELETE+INSERT, and
    // the FK ON DELETE CASCADE on segment/word/speaker would wipe the
    // transcript every time the parent job row is updated.
    @Upsert
    suspend fun upsertJob(job: JobEntity)

    @Query("UPDATE transcription_job SET status = :status, updated_at_us = :updatedAtUs WHERE id = :jobId")
    suspend fun updateStatus(jobId: String, status: JobState, updatedAtUs: Long)

    @Query(
        """
        UPDATE transcription_job
        SET status = :newStatus, updated_at_us = :updatedAtUs
        WHERE status NOT IN ('COMPLETED', 'FAILED', 'CANCELLED')
        """,
    )
    suspend fun failAllNonTerminal(newStatus: JobState, updatedAtUs: Long)

    // --- Segments / words ---
    @Query("SELECT * FROM transcription_segment WHERE job_id = :jobId ORDER BY start_us ASC")
    suspend fun getSegments(jobId: String): List<SegmentEntity>

    @Query("SELECT * FROM transcription_segment WHERE job_id = :jobId ORDER BY start_us ASC")
    fun observeSegments(jobId: String): Flow<List<SegmentEntity>>

    @Query("SELECT * FROM word WHERE segment_id IN (SELECT id FROM transcription_segment WHERE job_id = :jobId) ORDER BY start_us ASC")
    suspend fun getWords(jobId: String): List<WordEntity>

    @Query("SELECT * FROM word WHERE segment_id IN (SELECT id FROM transcription_segment WHERE job_id = :jobId) ORDER BY start_us ASC")
    fun observeWords(jobId: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM speaker WHERE job_id = :jobId ORDER BY id ASC")
    suspend fun getSpeakers(jobId: String): List<SpeakerEntity>

    @Query("SELECT * FROM speaker WHERE job_id = :jobId ORDER BY id ASC")
    fun observeSpeakers(jobId: String): Flow<List<SpeakerEntity>>

    @Query("UPDATE speaker SET display_name = :displayName WHERE id = :speakerId AND job_id = :jobId")
    suspend fun renameSpeaker(jobId: String, speakerId: Long, displayName: String)

    @Insert
    suspend fun insertSegments(segments: List<SegmentEntity>): List<Long>

    @Insert
    suspend fun insertWords(words: List<WordEntity>)

    @Insert
    suspend fun insertSpeakers(speakers: List<SpeakerEntity>): List<Long>

    @Query("DELETE FROM transcription_segment WHERE job_id = :jobId")
    suspend fun deleteSegments(jobId: String)

    @Query("DELETE FROM speaker WHERE job_id = :jobId")
    suspend fun deleteSpeakers(jobId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatistics(stats: StatisticsEntity)

    // --- FTS search (§13) ---
    @Query(
        """
        SELECT * FROM segment_fts
        WHERE segment_fts MATCH :query
        LIMIT :limit
        """,
    )
    suspend fun search(query: String, limit: Int): List<SegmentFtsEntity>

    @Query(
        """
        SELECT s.* FROM transcription_segment s
        INNER JOIN segment_fts f ON f.rowid = s.id
        WHERE segment_fts MATCH :query AND s.job_id = :jobId
        ORDER BY s.start_us ASC
        LIMIT :limit
        """,
    )
    suspend fun searchInJob(jobId: String, query: String, limit: Int): List<SegmentEntity>

    // --- Models (§8) ---
    @Query("SELECT * FROM model")
    fun observeModels(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM model WHERE id = :modelId")
    suspend fun getModel(modelId: String): ModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModel(model: ModelEntity)

    @Query("UPDATE model SET is_active = 0")
    suspend fun clearActiveModels()

    @Query("UPDATE model SET is_active = 1 WHERE id = :modelId")
    suspend fun setActiveModel(modelId: String)

    @Query("SELECT id FROM model WHERE is_active = 1")
    fun observeActiveModelId(): Flow<String?>

    @Query("DELETE FROM model WHERE id = :modelId")
    suspend fun deleteModel(modelId: String)

    @Transaction
    suspend fun replaceTranscript(
        jobId: String,
        segments: List<SegmentEntity>,
        words: List<WordEntity>,
        speakerClusters: List<Long>,
        speakers: List<SpeakerEntity>,
    ) {
        deleteSegments(jobId)
        deleteSpeakers(jobId)
        // Speaker PK is a global autoGenerate rowid, so per-job cluster ids
        // (1..N) must NOT be written as-is — they would collide with speaker
        // rows of other jobs (UNIQUE constraint on speaker.id). Insert with
        // generated ids, then remap segment.speaker_id cluster -> rowid.
        val speakerIds = insertSpeakers(speakers)
        val clusterToRowId = speakerClusters.zip(speakerIds).toMap()
        val remappedSegments = segments.map { seg ->
            if (seg.speakerId == null) seg else seg.copy(speakerId = clusterToRowId[seg.speakerId])
        }
        val ids = insertSegments(remappedSegments)
        val remapped = words.mapIndexed { index, word ->
            // segmentId in the domain pipeline is a 0-based index into the
            // segments list; remap it to the generated Room row id.
            val segId = ids.getOrElse(word.segmentId.toInt()) { 0L }
            word.copy(segmentId = segId)
        }
        insertWords(remapped)
    }
}