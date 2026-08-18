package com.example.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.repository.Transcript
import com.example.core.domain.repository.TranscriptExporter
import com.example.core.domain.repository.TranscriptionRepository
import com.example.core.model.TranscriptionJob
import com.example.core.model.TranscriptionSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Export formats offered by the transcript screen (§12, §70).
 */
enum class ExportFormat(val label: String, val extension: String, val mime: String) {
    TXT("TXT", "txt", "text/plain"),
    SRT("SRT", "srt", "application/x-subrip"),
    VTT("VTT", "vtt", "text/vtt"),
    JSON("JSON", "json", "application/json"),
}

/**
 * Transcript review screen state holder (§64, §71-75). Loads the persisted
 * transcript, runs in-job FTS search (§13) and produces export payloads.
 */
@HiltViewModel
class TranscriptDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jobs: TranscriptionRepository,
    private val exporter: TranscriptExporter,
) : ViewModel() {

    private val jobId: String = checkNotNull(savedStateHandle["jobId"])

    private val query = MutableStateFlow("")

    private val searchResults: Flow<List<TranscriptionSegment>> =
        query
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.isBlank()) flowOf(emptyList())
                else flow { emit(jobs.searchInJob(jobId, q, SEARCH_LIMIT)) }
            }

    val uiState: StateFlow<TranscriptDetailUiState> = combine(
        jobs.observeJob(jobId),
        jobs.observeTranscript(jobId),
        searchResults,
    ) { job, transcript, results ->
        TranscriptDetailUiState(
            job = job,
            transcript = transcript,
            searchQuery = query.value,
            searchResults = results,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TranscriptDetailUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun renameSpeaker(speakerId: Long, displayName: String) {
        val name = displayName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            jobs.renameSpeaker(jobId, speakerId, name)
        }
    }

    /** Renders the transcript in [format]; null when there is nothing to export. */
    fun exportText(format: ExportFormat): String? {
        val segments = uiState.value.transcript.segments
        if (segments.isEmpty()) return null
        return when (format) {
            ExportFormat.TXT -> exporter.exportToTxt(segments)
            ExportFormat.SRT -> exporter.exportToSrt(segments)
            ExportFormat.VTT -> exporter.exportToVtt(segments)
            ExportFormat.JSON -> exporter.exportToJson(segments, JSON_SCHEMA_VERSION)
        }
    }

    private companion object {
        const val SEARCH_LIMIT = 50
        const val JSON_SCHEMA_VERSION = 1
    }
}

/**
 * Snapshot of everything the transcript detail screen renders.
 */
data class TranscriptDetailUiState(
    val job: TranscriptionJob? = null,
    val transcript: Transcript = Transcript("", emptyList(), emptyList(), emptyList()),
    val searchQuery: String = "",
    val searchResults: List<TranscriptionSegment> = emptyList(),
)