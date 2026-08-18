package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.core.domain.repository.Transcript
import com.example.core.domain.repository.TranscriptionRepository
import com.example.core.model.Speaker
import com.example.core.model.TranscriptionJob
import com.example.core.model.JobState
import com.example.core.model.TranscriptionSegment
import com.example.core.model.Word
import com.example.data.audio.AudioDecoder
import com.example.data.database.DomainMapper
import com.example.data.database.VoiceScribeDao
import com.example.data.database.entity.SpeakerEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room + Media3 backed [TranscriptionRepository] (§2, §10).
 */
@Singleton
class TranscriptionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: VoiceScribeDao,
    private val decoder: AudioDecoder,
) : TranscriptionRepository {

    override fun observeJob(jobId: String): Flow<TranscriptionJob?> =
        dao.observeJob(jobId).map { it?.let(DomainMapper::entityToJob) }

    override fun observeJobs(): Flow<List<TranscriptionJob>> =
        dao.observeJobs().map { jobs -> jobs.map(DomainMapper::entityToJob) }

    override fun streamPcm(jobId: String): Flow<FloatArray> = kotlinx.coroutines.flow.flow {
        val job = dao.getJob(jobId) ?: return@flow
        emitAll(decoder.decode(Uri.parse(job.filePath)))
    }

    override suspend fun getJob(jobId: String): TranscriptionJob? =
        dao.getJob(jobId)?.let(DomainMapper::entityToJob)

    override suspend fun saveJob(job: TranscriptionJob) {
        dao.upsertJob(DomainMapper.jobToEntity(job))
    }

    override suspend fun saveTranscript(
        jobId: String,
        segments: List<TranscriptionSegment>,
        words: List<Word>,
    ) {
        // Speakers are derived from the segments' stable cluster ids (§26):
        // ids never change, only displayName can be renamed later. The PK is
        // a global autoGenerate rowid — the cluster id is passed separately so
        // the DAO can remap segment.speaker_id after the insert (per-job
        // cluster ids 1..N must not be written into the global PK).
        val clusters = segments.mapNotNull { it.speakerId }.distinct()
        val speakers = clusters.map { id ->
            SpeakerEntity(
                id = 0L,
                jobId = jobId,
                displayName = "Говорящий $id",
                colorIndex = id.toInt(),
            )
        }
        dao.replaceTranscript(
            jobId = jobId,
            segments = segments.map(DomainMapper::segmentToEntity),
            words = words.map(DomainMapper::wordToEntity),
            speakerClusters = clusters,
            speakers = speakers,
        )
    }

    override suspend fun getTranscript(jobId: String): Transcript? {
        val segments = dao.getSegments(jobId).map(DomainMapper::entityToSegment)
        if (segments.isEmpty()) return null
        val words = dao.getWords(jobId).map(DomainMapper::entityToWord)
        val speakers = dao.getSpeakers(jobId).map(DomainMapper::entityToSpeaker)
        return Transcript(jobId, segments, words, speakers)
    }

    override fun observeTranscript(jobId: String): Flow<Transcript> =
        combine(
            dao.observeSegments(jobId),
            dao.observeWords(jobId),
            dao.observeSpeakers(jobId),
        ) { segments, words, speakers ->
            Transcript(
                jobId = jobId,
                segments = segments.map(DomainMapper::entityToSegment),
                words = words.map(DomainMapper::entityToWord),
                speakers = speakers.map(DomainMapper::entityToSpeaker),
            )
        }

    override suspend fun renameSpeaker(jobId: String, speakerId: Long, displayName: String) {
        dao.renameSpeaker(jobId, speakerId, displayName)
    }

    override suspend fun searchInJob(jobId: String, query: String, limit: Int): List<TranscriptionSegment> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return dao.searchInJob(jobId, trimmed, limit).map(DomainMapper::entityToSegment)
    }

    override suspend fun reconcileStaleJobs() {
        dao.failAllNonTerminal(JobState.FAILED, System.currentTimeMillis() * 1000L)
    }
}