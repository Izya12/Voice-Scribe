package com.example.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.logging.AppLogger
import com.example.core.domain.repository.SettingsRepository
import com.example.core.model.LogLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Settings tab state holder (§Settings). Exposes the file-logging toggle and
 * the log level; persists through [SettingsRepository].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val logger: AppLogger,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun clearError() {
        _error.value = null
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.observeLoggingEnabled(),
        settings.observeLogLevel(),
    ) { loggingEnabled, level ->
        SettingsUiState(loggingEnabled = loggingEnabled, logLevel = level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settings.setLoggingEnabled(enabled)
                logger.info(TAG, "file logging ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                logger.error(TAG, "Failed to persist logging toggle", e)
                _error.value = "Не удалось сохранить настройку: ${e.message}"
            }
        }
    }

    fun setLogLevel(level: LogLevel) {
        viewModelScope.launch {
            try {
                settings.setLogLevel(level)
                logger.info(TAG, "log level set to $level")
            } catch (e: Exception) {
                logger.error(TAG, "Failed to persist log level", e)
                _error.value = "Не удалось сохранить настройку: ${e.message}"
            }
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}

/**
 * Snapshot of everything the settings tab renders.
 */
data class SettingsUiState(
    val loggingEnabled: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
)