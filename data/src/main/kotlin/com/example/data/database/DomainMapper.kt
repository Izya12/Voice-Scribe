package com.example.data.database

import com.example.core.model.DiarizationMode
import com.example.core.model.LanguageMode
import com.example.core.model.Speaker
import com.example.core.model.TranscriptionConfig
import com.example.core.model.TranscriptionJob
import com.example.core.model.TranscriptionSegment
import com.example.core.model.TranscriptionStatistics
import com.example.core.model.Word
import com.example.data.database.entity.ConfigEntity
import com.example.data.database.entity.JobEntity
import com.example.data.database.entity.SegmentEntity
import com.example.data.database.entity.SpeakerEntity
import com.example.data.database.entity.StatisticsEntity
import com.example.data.database.entity.WordEntity

/**
 * Maps between pure domain models (`:core:model`) and Room entities (`:data`).
 */
object DomainMapper {

    fun jobToEntity(job: TranscriptionJob): JobEntity = JobEntity(
        id = job.id,
        status = job.status,
        filePath = job.filePath,
        createdAtUs = job.createdAtUs,
        updatedAtUs = job.updatedAtUs,
        config = ConfigEntity(
            languageMode = job.config.languageMode.name,
            language = job.config.language,
            diarizationMode = job.config.diarizationMode.name,
            numSpeakers = job.config.numSpeakers,
            useVad = job.config.useVad,
            modelId = job.config.modelId,
        ),
    )

    fun entityToJob(entity: JobEntity): TranscriptionJob = TranscriptionJob(
        id = entity.id,
        status = entity.status,
        filePath = entity.filePath,
        createdAtUs = entity.createdAtUs,
        updatedAtUs = entity.updatedAtUs,
        config = TranscriptionConfig(
            languageMode = runCatching { LanguageMode.valueOf(entity.config.languageMode) }.getOrDefault(LanguageMode.AUTO),
            language = entity.config.language,
            diarizationMode = runCatching { DiarizationMode.valueOf(entity.config.diarizationMode) }.getOrDefault(DiarizationMode.AUTOMATIC),
            numSpeakers = entity.config.numSpeakers,
            useVad = entity.config.useVad,
            modelId = entity.config.modelId,
        ),
    )

    fun segmentToEntity(segment: TranscriptionSegment): SegmentEntity = SegmentEntity(
        id = segment.id,
        jobId = segment.jobId,
        startUs = segment.startUs,
        endUs = segment.endUs,
        text = segment.text,
        speakerId = segment.speakerId,
    )

    fun entityToSegment(entity: SegmentEntity): TranscriptionSegment = TranscriptionSegment(
        id = entity.id,
        jobId = entity.jobId,
        startUs = entity.startUs,
        endUs = entity.endUs,
        text = entity.text,
        speakerId = entity.speakerId,
    )

    fun wordToEntity(word: Word): WordEntity = WordEntity(
        id = word.id,
        segmentId = word.segmentId,
        word = word.word,
        startUs = word.startUs,
        endUs = word.endUs,
        confidence = word.confidence,
    )

    fun entityToWord(entity: WordEntity): Word = Word(
        id = entity.id,
        segmentId = entity.segmentId,
        word = entity.word,
        startUs = entity.startUs,
        endUs = entity.endUs,
        confidence = entity.confidence,
    )

    fun statsToEntity(stats: TranscriptionStatistics): StatisticsEntity = StatisticsEntity(
        jobId = stats.jobId,
        durationUs = stats.durationUs,
        processingTimeMs = stats.processingTimeMs,
        rtf = stats.rtf,
    )

    fun speakerToEntity(speaker: Speaker): SpeakerEntity = SpeakerEntity(
        id = speaker.id,
        jobId = speaker.jobId,
        displayName = speaker.displayName,
        colorIndex = speaker.colorIndex,
    )

    fun entityToSpeaker(entity: SpeakerEntity): Speaker = Speaker(
        id = entity.id,
        jobId = entity.jobId,
        displayName = entity.displayName,
        colorIndex = entity.colorIndex,
    )
}