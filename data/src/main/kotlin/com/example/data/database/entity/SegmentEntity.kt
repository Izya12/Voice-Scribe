package com.example.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A block of text spoken by a single speaker (§10.1).
 */
@Entity(
    tableName = "transcription_segment",
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
data class SegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "job_id")
    val jobId: String,
    @ColumnInfo(name = "start_us")
    val startUs: Long,
    @ColumnInfo(name = "end_us")
    val endUs: Long,
    @ColumnInfo(name = "text")
    val text: String,
    @ColumnInfo(name = "speaker_id")
    val speakerId: Long? = null,
)
