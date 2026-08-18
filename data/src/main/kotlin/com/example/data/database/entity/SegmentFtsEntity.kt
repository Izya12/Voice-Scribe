package com.example.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 auxiliary table for full-text search (§13). The manifest documents
 * FTS5; Room's `@Fts4` maps to SQLite FTS4, which is compatible with the
 * `MATCH` operator used by the search DAO.
 */
@Fts4(contentEntity = SegmentEntity::class)
@Entity(tableName = "segment_fts")
data class SegmentFtsEntity(
    @ColumnInfo(name = "text")
    val text: String,
)
