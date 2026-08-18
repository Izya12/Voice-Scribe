package com.example.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.domain.repository.ModelRepository
import com.example.core.model.ModelDescriptor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Models tab state holder (§1.2 / MVVM). Exposes the model catalog, install
 * status and download progress.
 */
@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val models: ModelRepository,
) : ViewModel() {

    private val downloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun clearError() {
        _error.value = null
    }

    val uiState: StateFlow<ModelsUiState> = combine(
        models.observeCatalog(),
        models.observeInstalledModels(),
        models.observeActiveModelId(),
        downloads,
    ) { catalog, installed, activeId, dl ->
        ModelsUiState(
            catalog = catalog,
            installedIds = installed.map { it.id }.toSet(),
            activeModelId = activeId,
            downloads = dl,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelsUiState())

    fun download(model: ModelDescriptor) {
        viewModelScope.launch {
            try {
                models.download(model) { progress ->
                    downloads.value = downloads.value + (model.id to progress)
                }
                downloads.value = downloads.value - model.id
                models.setActive(model.id)
            } catch (e: Exception) {
                downloads.value = downloads.value - model.id
                _error.value = "Ошибка скачивания ${model.id}: ${e.message}"
            }
        }
    }

    fun activate(modelId: String) {
        viewModelScope.launch {
            try {
                models.setActive(modelId)
            } catch (e: Exception) {
                _error.value = "Не удалось активировать модель: ${e.message}"
            }
        }
    }

    fun delete(modelId: String) {
        viewModelScope.launch {
            try {
                models.delete(modelId)
            } catch (e: Exception) {
                _error.value = "Не удалось удалить модель: ${e.message}"
            }
        }
    }
}

/**
 * Snapshot of everything the models tab renders.
 */
data class ModelsUiState(
    val catalog: List<ModelDescriptor> = emptyList(),
    val installedIds: Set<String> = emptySet(),
    val activeModelId: String? = null,
    val downloads: Map<String, Float> = emptyMap(),
)