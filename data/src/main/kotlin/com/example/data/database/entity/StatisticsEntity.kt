package com.example.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One-to-one statistics with a job (§10.1).
 */
@Entity(
    tableName = "transcription_statistics",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["job_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("job_id")],
)
data class StatisticsEntity(
    @PrimaryKey
    @ColumnInfo(name = "job_id")
    val jobId: String,
    @ColumnInfo(name = "duration_us")
    val durationUs: Long = 0L,
    @ColumnInfo(name = "processing_time_ms")
    val processingTimeMs: Long = 0L,
    @ColumnInfo(name = "rtf")
    val rtf: Float = 0f,
)
