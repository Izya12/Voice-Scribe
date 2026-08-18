package com.example.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Individual word with exact timestamps for playback highlighting (§82).
 */
@Entity(
    tableName = "word",
    foreignKeys = [
        ForeignKey(
            entity = SegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["segment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("segment_id")],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "segment_id")
    val segmentId: Long,
    @ColumnInfo(name = "word")
    val word: String,
    @ColumnInfo(name = "start_us")
    val startUs: Long,
    @ColumnInfo(name = "end_us")
    val endUs: Long,
    @ColumnInfo(name = "confidence")
    val confidence: Float = 0f,
)
