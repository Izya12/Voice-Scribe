package com.example.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.JobState

/**
 * Master job record (§10.1). All timeline values are microsecond [Long] (§30).
 */
@Entity(tableName = "transcription_job")
data class JobEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "status")
    val status: JobState,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "created_at_us")
    val createdAtUs: Long,
    @ColumnInfo(name = "updated_at_us")
    val updatedAtUs: Long,
    @Embedded
    val config: ConfigEntity,
)

/**
 * Embedded transcription configuration (§10.1).
 */
data class ConfigEntity(
    @ColumnInfo(name = "language_mode")
    val languageMode: String = "AUTO",
    @ColumnInfo(name = "language")
    val language: String? = null,
    @ColumnInfo(name = "diarization_mode")
    val diarizationMode: String = "AUTOMATIC",
    @ColumnInfo(name = "num_speakers")
    val numSpeakers: Int? = null,
    @ColumnInfo(name = "use_vad")
    val useVad: Boolean = true,
    @ColumnInfo(name = "model_id")
    val modelId: String? = null,
)
