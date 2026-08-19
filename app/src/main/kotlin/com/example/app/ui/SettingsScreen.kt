package com.example.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.model.LogLevel

/**
 * Settings tab: file-logging toggle and log level selection (§Settings).
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onLoggingEnabledChange: (Boolean) -> Unit,
    onLogLevelChange: (LogLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val logDir = remember { context.filesDir }
    val logPath = "${logDir.absolutePath}/logs/voicescribe.log"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Настройки", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Логирование в файл", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Запись работы приложения в файл для диагностики ошибок",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = state.loggingEnabled, onCheckedChange = onLoggingEnabledChange)
        }

        if (state.loggingEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Уровень логирования", Modifier.weight(1f))
                LogLevelMenu(selected = state.logLevel, onSelect = onLogLevelChange)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Файл логов: $logPath",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "При достижении 5 МБ файл ротируется (хранятся 3 копии).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val LogLevelLabels = listOf(
    LogLevel.DEBUG to "Отладка",
    LogLevel.INFO to "Инфо",
    LogLevel.WARN to "Предупреждения",
    LogLevel.ERROR to "Ошибки",
)

@Composable
private fun LogLevelMenu(selected: LogLevel, onSelect: (LogLevel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(LogLevelLabels.firstOrNull { it.first == selected }?.second ?: selected.name)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LogLevelLabels.forEach { (level, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelect(level)
                    },
                )
            }
        }
    }
}