package com.example.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.example.core.domain.repository.SettingsRepository
import com.example.core.model.LogLevel
import com.example.data.logging.FileAppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * SharedPreferences-backed [SettingsRepository] (§Settings).
 *
 * On every write the [FileAppLogger] is reconfigured so the toggle and the
 * level apply immediately, without an app restart.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val logger: FileAppLogger,
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Apply persisted settings to the logger at startup.
        logger.configure(
            enabled = prefs.getBoolean(KEY_LOGGING_ENABLED, DEFAULT_LOGGING_ENABLED),
            level = readLevel(),
        )
    }

    override fun observeLoggingEnabled(): Flow<Boolean> = callbackFlow {
        fun emitCurrent() {
            trySend(prefs.getBoolean(KEY_LOGGING_ENABLED, DEFAULT_LOGGING_ENABLED))
        }
        emitCurrent()
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LOGGING_ENABLED) emitCurrent()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun observeLogLevel(): Flow<LogLevel> = callbackFlow {
        fun emitCurrent() {
            trySend(readLevel())
        }
        emitCurrent()
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LOG_LEVEL) emitCurrent()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setLoggingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOGGING_ENABLED, enabled).apply()
        logger.configure(enabled = enabled, level = readLevel())
    }

    override suspend fun setLogLevel(level: LogLevel) {
        prefs.edit().putString(KEY_LOG_LEVEL, level.name).apply()
        logger.configure(enabled = prefs.getBoolean(KEY_LOGGING_ENABLED, DEFAULT_LOGGING_ENABLED), level = level)
    }

    private fun readLevel(): LogLevel =
        runCatching { LogLevel.valueOf(prefs.getString(KEY_LOG_LEVEL, null) ?: DEFAULT_LOG_LEVEL.name) }
            .getOrDefault(DEFAULT_LOG_LEVEL)

    companion object {
        private const val PREFS_NAME = "settings"
        private const val KEY_LOGGING_ENABLED = "logging_enabled"
        private const val KEY_LOG_LEVEL = "log_level"
        private const val DEFAULT_LOGGING_ENABLED = false
        private val DEFAULT_LOG_LEVEL = LogLevel.INFO
    }
}