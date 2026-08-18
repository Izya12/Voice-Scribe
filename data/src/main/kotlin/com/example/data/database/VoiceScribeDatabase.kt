package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.core.model.JobState
import com.example.data.database.entity.JobEntity
import com.example.data.database.entity.ModelEntity
import com.example.data.database.entity.SegmentEntity
import com.example.data.database.entity.SegmentFtsEntity
import com.example.data.database.entity.SpeakerEntity
import com.example.data.database.entity.StatisticsEntity
import com.example.data.database.entity.WordEntity

/**
 * Room database (§10). Schema version 1; migrations are managed with
 * `migration-architect` discipline when models change.
 */
@Database(
    entities = [
        JobEntity::class,
        SegmentEntity::class,
        WordEntity::class,
        SpeakerEntity::class,
        StatisticsEntity::class,
        SegmentFtsEntity::class,
        ModelEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(JobStateConverter::class)
abstract class VoiceScribeDatabase : RoomDatabase() {
    abstract fun dao(): VoiceScribeDao
}

/**
 * Stores [JobState] as its string name in the DB.
 */
class JobStateConverter {
    @TypeConverter
    fun fromJobState(state: JobState): String = state.name

    @TypeConverter
    fun toJobState(name: String): JobState = JobState.valueOf(name)
}