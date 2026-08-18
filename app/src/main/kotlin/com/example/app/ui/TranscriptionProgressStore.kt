package com.example.app.ui

import com.example.core.domain.usecase.JobProgress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory bridge between the FGS pipeline and the UI (§46).
 *
 * [MediaProcessingService] publishes pipeline [JobProgress] here so the UI can
 * render stage + fraction progress; the job list itself only carries the
 * persisted state. Survives configuration changes; on process death entries
 * are simply gone (the job is reconciled to FAILED at startup anyway).
 */
@Singleton
class TranscriptionProgressStore @Inject constructor() {

    private val _progress = MutableStateFlow<Map<String, JobProgress>>(emptyMap())
    val progress: StateFlow<Map<String, JobProgress>> = _progress

    fun update(progress: JobProgress) {
        _progress.value = _progress.value + (progress.jobId to progress)
    }

    fun remove(jobId: String) {
        _progress.value = _progress.value - jobId
    }
}