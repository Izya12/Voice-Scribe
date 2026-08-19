package com.example.app.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.domain.usecase.JobProgress
import com.example.core.model.JobState
import com.example.core.model.LogLevel
import com.example.core.model.ModelDescriptor
import com.example.core.model.TranscriptionJob
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : androidx.activity.ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val modelsViewModel: ModelsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(
            androidx.compose.ui.platform.ComposeView(this).apply {
                setContent {
                    MaterialTheme {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "transcribe") {
                            composable("transcribe") {
                                VoiceScribeApp(
                                    state = viewModel.uiState.collectAsStateWithLifecycle().value,
                                    error = viewModel.error.collectAsStateWithLifecycle().value,
                                    onDismissError = viewModel::clearError,
                                    onPickAudio = { uri, useVad, diarize, language ->
                                        viewModel.createJobAndTranscribe(uri, useVad, diarize, language)
                                    },
                                    onCancel = viewModel::cancelJob,
                                    onDeleteJob = viewModel::deleteJob,
                                    onOpenJob = { jobId ->
                                        navController.navigate("transcript/$jobId")
                                    },
                                    modelsState = modelsViewModel.uiState.collectAsStateWithLifecycle().value,
                                    modelsError = modelsViewModel.error.collectAsStateWithLifecycle().value,
                                    onDismissModelsError = modelsViewModel::clearError,
                                    onDownload = modelsViewModel::download,
                                    onActivateModel = modelsViewModel::activate,
                                    onDeleteModel = modelsViewModel::delete,
                                    settingsState = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                                    settingsError = settingsViewModel.error.collectAsStateWithLifecycle().value,
                                    onDismissSettingsError = settingsViewModel::clearError,
                                    onLoggingEnabledChange = settingsViewModel::setLoggingEnabled,
                                    onLogLevelChange = settingsViewModel::setLogLevel,
                                )
                            }
                            composable(
                                route = "transcript/{jobId}",
                                arguments = listOf(navArgument("jobId") { type = NavType.StringType }),
                            ) {
                                TranscriptScreen(onBack = { navController.popBackStack() })
                            }
                        }
                    }
                }
            },
        )
    }
}

private enum class AppTab(val label: String) {
    TRANSCRIBE("Транскрипция"),
    MODELS("Модели"),
    SETTINGS("Настройки"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScribeApp(
    state: TranscriptionUiState,
    error: String?,
    onDismissError: () -> Unit,
    onPickAudio: (Uri, Boolean, Boolean, String?) -> Unit,
    onCancel: (String) -> Unit,
    onDeleteJob: (String) -> Unit,
    onOpenJob: (String) -> Unit,
    modelsState: ModelsUiState,
    modelsError: String?,
    onDismissModelsError: () -> Unit,
    onDownload: (ModelDescriptor) -> Unit,
    onActivateModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    settingsState: SettingsUiState,
    settingsError: String?,
    onDismissSettingsError: () -> Unit,
    onLoggingEnabledChange: (Boolean) -> Unit,
    onLogLevelChange: (LogLevel) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.TRANSCRIBE) }

    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            onDismissError()
        }
    }
    LaunchedEffect(modelsError) {
        if (modelsError != null) {
            snackbarHostState.showSnackbar(modelsError)
            onDismissModelsError()
        }
    }
    LaunchedEffect(settingsError) {
        if (settingsError != null) {
            snackbarHostState.showSnackbar(settingsError)
            onDismissSettingsError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("VoiceScribe") }) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    AppTab.TRANSCRIBE -> Icons.Filled.PlayArrow
                                    AppTab.MODELS -> Icons.Filled.List
                                    AppTab.SETTINGS -> Icons.Filled.Settings
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (selectedTab) {
            AppTab.TRANSCRIBE -> TranscriptionScreen(
                state = state,
                onPickAudio = onPickAudio,
                onCancel = onCancel,
                onDeleteJob = onDeleteJob,
                onOpenJob = onOpenJob,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            )

            AppTab.MODELS -> ModelsScreen(
                state = modelsState,
                onDownload = onDownload,
                onActivate = onActivateModel,
                onDelete = onDeleteModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            )

            AppTab.SETTINGS -> SettingsScreen(
                state = settingsState,
                onLoggingEnabledChange = onLoggingEnabledChange,
                onLogLevelChange = onLogLevelChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
fun TranscriptionScreen(
    state: TranscriptionUiState,
    onPickAudio: (Uri, Boolean, Boolean, String?) -> Unit,
    onCancel: (String) -> Unit,
    onDeleteJob: (String) -> Unit,
    onOpenJob: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var useVad by rememberSaveable { mutableStateOf(true) }
    var diarize by rememberSaveable { mutableStateOf(false) }
    var language by rememberSaveable { mutableStateOf<String?>(null) }
    var jobToDelete by remember { mutableStateOf<TranscriptionJob?>(null) }

    jobToDelete?.let { job ->
        AlertDialog(
            onDismissRequest = { jobToDelete = null },
            title = { Text("Удалить задание?") },
            text = { Text("Транскрипция «${job.filePath.substringAfterLast('/')}» будет удалена безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteJob(job.id)
                    jobToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { jobToDelete = null }) { Text("Отмена") }
            },
        )
    }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onPickAudio(uri, useVad, diarize, language)
    }

    LazyColumn(modifier = modifier) {
        item {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Text("VAD-фильтрация", Modifier.weight(1f))
                    Switch(checked = useVad, onCheckedChange = { useVad = it })
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Text("Диаризация (распознавание говорящих)", Modifier.weight(1f))
                    Switch(checked = diarize, onCheckedChange = { diarize = it })
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Text("Язык распознавания", Modifier.weight(1f))
                    LanguageMenu(selected = language, onSelect = { language = it })
                }
            }
        }

        item {
            Button(
                onClick = {
                    audioLauncher.launch(arrayOf("audio/*", "video/*"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Выбрать аудио и распознать")
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item { Text("Задания", style = MaterialTheme.typography.titleMedium) }
        items(state.jobs, key = { it.id }) { job ->
            JobCard(
                job = job,
                progress = state.progress[job.id],
                onCancel = { onCancel(job.id) },
                onDelete = { jobToDelete = job },
                onOpen = if (job.status == JobState.COMPLETED) {
                    { onOpenJob(job.id) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
fun ModelsScreen(
    state: ModelsUiState,
    onDownload: (ModelDescriptor) -> Unit,
    onActivate: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var modelToDelete by remember { mutableStateOf<ModelDescriptor?>(null) }
    modelToDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Удалить модель?") },
            text = { Text("${model.displayName} будет удалена с устройства.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(model.id)
                    modelToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) { Text("Отмена") }
            },
        )
    }

    LazyColumn(modifier = modifier) {
        item { Text("Модели", style = MaterialTheme.typography.titleMedium) }
        items(state.catalog) { model ->
            ModelCard(
                model = model,
                isInstalled = model.id in state.installedIds,
                isActive = model.id == state.activeModelId,
                progress = state.downloads[model.id],
                onDownload = { onDownload(model) },
                onActivate = { onActivate(model.id) },
                onDeleteRequest = { modelToDelete = model },
            )
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelDescriptor,
    isInstalled: Boolean,
    isActive: Boolean,
    progress: Float?,
    onDownload: () -> Unit,
    onActivate: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(model.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            "Активна",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Text(
                "${model.fileName} · ${model.tier.name} · ${model.fileSizeBytes / 1_048_576} МБ · ${model.license}",
                style = MaterialTheme.typography.bodySmall,
            )
            when {
                progress != null -> {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
                isInstalled && !isActive -> Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onActivate) { Text("Активировать") }
                    OutlinedButton(onClick = onDeleteRequest) { Text("Удалить") }
                }
                isActive -> Text("✓ Установлена", style = MaterialTheme.typography.bodySmall)
                else -> Button(onClick = onDownload) { Text("Скачать") }
            }
        }
    }
}

@Composable
private fun JobCard(
    job: TranscriptionJob,
    progress: JobProgress?,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpen: (() -> Unit)?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(job.filePath.substringAfterLast('/'), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        statusLabel(job.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (job.status == JobState.FAILED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (!job.status.isTerminal) {
                    OutlinedButton(onClick = onCancel) { Text("Отмена") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onOpen != null) {
                            Text(
                                "Открыть",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        }
                        OutlinedButton(onClick = onDelete) { Text("Удалить") }
                    }
                }
            }
            val errorMessage = job.errorMessage
            if (job.status == JobState.FAILED && errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            val activeProgress = progress?.takeIf { !it.state.isTerminal && it.fraction > 0f }
            if (activeProgress != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { activeProgress.fraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${statusLabel(activeProgress.state)} ${(activeProgress.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun statusLabel(state: JobState): String = when (state) {
    JobState.SUBMITTED -> "В очереди"
    JobState.DECODING -> "Декодирование…"
    JobState.PREPROCESSING -> "Предобработка…"
    JobState.DIARIZING -> "Диаризация…"
    JobState.TRANSCRIBING -> "Распознавание…"
    JobState.COMPLETED -> "Готово"
    JobState.FAILED -> "Ошибка"
    JobState.CANCELLED -> "Отменено"
}

/** Whisper language codes supported by the bundled ASR models (§3.5 MANUAL). */
private val WhisperLanguages = listOf(
    "ru" to "Русский",
    "en" to "English",
    "de" to "Deutsch",
    "fr" to "Français",
    "es" to "Español",
    "it" to "Italiano",
    "pt" to "Português",
    "uk" to "Українська",
    "pl" to "Polski",
    "zh" to "中文",
    "ja" to "日本語",
    "ko" to "한국어",
    "tr" to "Türkçe",
)

@Composable
private fun LanguageMenu(selected: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(
                WhisperLanguages.firstOrNull { it.first == selected }?.second ?: "Авто",
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Авто") },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            WhisperLanguages.forEach { (code, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelect(code)
                    },
                )
            }
        }
    }
}