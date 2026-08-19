package com.example.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.service.MediaProcessingService
import com.example.core.domain.logging.AppLogger
import com.example.core.domain.repository.ModelRepository
import com.example.core.domain.repository.TranscriptionRepository
import com.example.core.domain.usecase.JobProgress
import com.example.core.model.DiarizationMode
import com.example.core.model.JobState
import com.example.core.model.LanguageMode
import com.example.core.model.TranscriptionConfig
import com.example.core.model.TranscriptionJob
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Main screen state holder (§1.2 / MVVM). Exposes the job list and delegates
 * transcription to the FGS.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val models: ModelRepository,
    private val jobs: TranscriptionRepository,
    private val progressStore: TranscriptionProgressStore,
    private val logger: AppLogger,
) : AndroidViewModel(application) {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun clearError() {
        _error.value = null
    }

    val uiState: StateFlow<TranscriptionUiState> = combine(
        jobs.observeJobs(),
        progressStore.progress,
    ) { jobList, progress ->
        TranscriptionUiState(jobs = jobList, progress = progress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TranscriptionUiState())

    fun createJobAndTranscribe(uri: Uri, useVad: Boolean, diarize: Boolean, language: String?) {
        val now = System.currentTimeMillis() * 1_000L
        val job = TranscriptionJob(
            id = UUID.randomUUID().toString(),
            status = JobState.SUBMITTED,
            filePath = uri.toString(),
            createdAtUs = now,
            updatedAtUs = now,
            config = TranscriptionConfig(
                languageMode = if (language == null) LanguageMode.AUTO else LanguageMode.MANUAL,
                language = language,
                diarizationMode = if (diarize) DiarizationMode.AUTOMATIC else DiarizationMode.DISABLED,
                useVad = useVad,
            ),
        )
        viewModelScope.launch {
            jobs.saveJob(job)
            logger.info(TAG, "submitted job ${job.id}")
            startService(job.id)
        }
    }

    fun cancelJob(jobId: String) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            // Hard-persist CANCELLED so the UI reflects it even if the FGS
            // is not running (service died or never started) (§22).
            val current = jobs.getJob(jobId)
            if (current != null && !current.status.isTerminal) {
                jobs.saveJob(current.copy(status = JobState.CANCELLED))
            }
            val intent = Intent(app, MediaProcessingService::class.java)
                .putExtra(MediaProcessingService.EXTRA_ACTION, MediaProcessingService.ACTION_CANCEL)
                .putExtra(MediaProcessingService.EXTRA_JOB_ID, jobId)
            try {
                app.startForegroundService(intent)
            } catch (e: Exception) {
                logger.error(TAG, "Failed to start cancel intent for job $jobId", e)
                _error.value = "Не удалось остановить обработку: ${e.message}"
            }
        }
    }

    /**
     * Removes a finished job (COMPLETED / FAILED / CANCELLED) together with
     * its transcript. Active jobs must be cancelled first.
     */
    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            try {
                val current = jobs.getJob(jobId)
                if (current != null && !current.status.isTerminal) {
                    _error.value = "Сначала остановите обработку задания"
                    return@launch
                }
                jobs.deleteJob(jobId)
                progressStore.remove(jobId)
                logger.info(TAG, "deleted job $jobId")
            } catch (e: Exception) {
                logger.error(TAG, "Failed to delete job $jobId", e)
                _error.value = "Не удалось удалить задание: ${e.message}"
            }
        }
    }

    private fun startService(jobId: String) {
        val app = getApplication<Application>()
        val intent = Intent(app, MediaProcessingService::class.java)
            .putExtra(MediaProcessingService.EXTRA_JOB_ID, jobId)
        app.startForegroundService(intent)
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}

/**
 * Snapshot of everything the transcription screen renders.
 */
data class TranscriptionUiState(
    val jobs: List<TranscriptionJob> = emptyList(),
    val progress: Map<String, JobProgress> = emptyMap(),
)