package com.example.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Installed model record (§8). Tracks which registry models are present on
 * disk and which one is currently active.
 */
@Entity(tableName = "model")
data class ModelEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,
)
