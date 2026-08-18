package com.example.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.DiarizationMode
import com.example.core.model.LanguageMode
import com.example.core.model.TranscriptionJob
import com.example.core.model.TranscriptionSegment
import java.util.Locale

/**
 * Transcript review screen (§64): segments with speaker chips and timestamps,
 * in-job FTS search (§13), speaker renaming (§26) and SAF export (§70-75).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptScreen(
    onBack: () -> Unit,
    viewModel: TranscriptDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingExport by remember { mutableStateOf<ExportFormat?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val format = pendingExport
        pendingExport = null
        if (uri == null || format == null) return@rememberLauncherForActivityResult
        val content = viewModel.exportText(format) ?: return@rememberLauncherForActivityResult
        writeExport(context, uri, content)
    }

    var speakerToRename by remember { mutableStateOf<Pair<Long, String>?>(null) }
    speakerToRename?.let { (speakerId, currentName) ->
        RenameSpeakerDialog(
            currentName = currentName,
            onDismiss = { speakerToRename = null },
            onConfirm = { newName ->
                viewModel.renameSpeaker(speakerId, newName)
                speakerToRename = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.job?.filePath?.substringAfterLast('/') ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    ExportMenu(
                        enabled = state.transcript.segments.isNotEmpty(),
                        suggestedBaseName = exportBaseName(state.job?.filePath),
                        onExport = { format ->
                            pendingExport = format
                            exportLauncher.launch(
                                "${exportBaseName(state.job?.filePath)}.${format.extension}",
                            )
                        },
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            state.job?.let { job ->
                item { JobMeta(job = job) }
            }
            item {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Поиск по транскрипту…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                )
            }
            item { Spacer(Modifier.height(12.dp)) }

            val searching = state.searchQuery.isNotBlank()
            when {
                searching && state.searchResults.isEmpty() -> item {
                    Text("Ничего не найдено", style = MaterialTheme.typography.bodyMedium)
                }
                searching -> {
                    item { Text("Результаты: ${state.searchResults.size}", style = MaterialTheme.typography.titleSmall) }
                    items(state.searchResults, key = { it.id }) { segment ->
                        SegmentCard(
                            segment = segment,
                            speakerName = speakerName(segment, state),
                            onRenameSpeaker = { id, name -> speakerToRename = id to name },
                        )
                    }
                }
                state.transcript.segments.isEmpty() -> item {
                    Text("Транскрипция пуста", style = MaterialTheme.typography.bodyMedium)
                }
                else -> items(state.transcript.segments, key = { it.id }) { segment ->
                    SegmentCard(
                        segment = segment,
                        speakerName = speakerName(segment, state),
                        onRenameSpeaker = { id, name -> speakerToRename = id to name },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportMenu(
    enabled: Boolean,
    suggestedBaseName: String,
    onExport: (ExportFormat) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, enabled = enabled) {
        Icon(Icons.Filled.MoreVert, contentDescription = "Экспорт")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        ExportFormat.entries.forEach { format ->
            DropdownMenuItem(
                text = { Text("Экспорт ${format.label}") },
                onClick = {
                    expanded = false
                    onExport(format)
                },
            )
        }
    }
}

@Composable
private fun JobMeta(job: TranscriptionJob) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            "Дата: ${formatDateTime(job.createdAtUs)}",
            style = MaterialTheme.typography.bodySmall,
        )
        val config = job.config
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaChip(if (config.useVad) "VAD" else "Без VAD")
            MetaChip(
                if (config.diarizationMode == DiarizationMode.DISABLED) "Без диаризации" else "Диаризация",
            )
            MetaChip(
                if (config.languageMode == LanguageMode.MANUAL && !config.language.isNullOrBlank()) {
                    "Язык: ${config.language}"
                } else {
                    "Язык: авто"
                },
            )
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun SegmentCard(
    segment: TranscriptionSegment,
    speakerName: String?,
    onRenameSpeaker: (Long, String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(segment.startUs), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                val speakerId = segment.speakerId
                if (speakerName != null && speakerId != null) {
                    val color = speakerColor(speakerId)
                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.clickable { onRenameSpeaker(speakerId, speakerName) },
                    ) {
                        Text(
                            speakerName,
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(segment.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun RenameSpeakerDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать говорящего") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private fun speakerName(
    segment: TranscriptionSegment,
    state: TranscriptDetailUiState,
): String? {
    val id = segment.speakerId ?: return null
    return state.transcript.speakers.firstOrNull { it.id == id }?.displayName
        ?: "Говорящий $id"
}

private fun speakerColor(speakerId: Long): Color =
    SpeakerPalette[((speakerId % SpeakerPalette.size).toInt() + SpeakerPalette.size) % SpeakerPalette.size]

private fun writeExport(context: Context, uri: Uri, content: String) {
    try {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) }
    } catch (_: Exception) {
        // Export failure is surfaced by the OS (file dialog already confirmed);
        // silently dropping keeps the review screen from crashing.
    }
}

private fun exportBaseName(filePath: String?): String {
    val raw = filePath?.substringAfterLast('/') ?: return "transcript"
    val withoutExt = raw.substringBeforeLast('.', raw)
    return withoutExt.ifBlank { "transcript" }
}

private fun formatTime(us: Long): String {
    val totalSec = us / 1_000_000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

private fun formatDateTime(us: Long): String {
    val millis = us / 1000
    return java.text.DateFormat.getDateTimeInstance().format(java.util.Date(millis))
}

private val SpeakerPalette = listOf(
    Color(0xFFE53935),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
    Color(0xFFF4511E),
    Color(0xFF8E24AA),
    Color(0xFF00897B),
    Color(0xFFF9A825),
    Color(0xFF6D4C41),
)