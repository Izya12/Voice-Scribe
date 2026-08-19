package com.example.core.domain.repository

import com.example.core.model.LogLevel
import kotlinx.coroutines.flow.Flow

/**
 * User preferences for the app (§Settings).
 *
 * Implemented by `:data` with SharedPreferences (no storage library
 * dependency). Observables are cold [Flow] streams that re-emit the current
 * value on every change.
 */
interface SettingsRepository {

    /** True when file logging is enabled (default: off). */
    fun observeLoggingEnabled(): Flow<Boolean>

    /** Current log level filter (default: [LogLevel.INFO]). */
    fun observeLogLevel(): Flow<LogLevel>

    suspend fun setLoggingEnabled(enabled: Boolean)

    suspend fun setLogLevel(level: LogLevel)
}