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
 * Room database (§10). Schema version 2; migrations are managed with
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
    version = 2,
    exportSchema = false,
)
@TypeConverters(JobStateConverter::class)
abstract class VoiceScribeDatabase : RoomDatabase() {
    abstract fun dao(): VoiceScribeDao

    companion object {
        /**
         * v1 -> v2: `transcription_job.error_message` for the failure reason
         * shown in the UI (§Settings). Pure additive column — no data loss.
         */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transcription_job ADD COLUMN error_message TEXT")
            }
        }
    }
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