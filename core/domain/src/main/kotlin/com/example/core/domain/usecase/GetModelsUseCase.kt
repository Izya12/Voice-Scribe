package com.example.core.domain.usecase

import com.example.core.model.JobState
import com.example.core.model.ModelDescriptor

/**
 * Reactive progress payload emitted throughout pipeline execution (§2, step 6).
 */
data class JobProgress(
    val jobId: String,
    val state: JobState,
    val fraction: Float = 0f,
)

/**
 * Reads the model catalog and the currently installed models (§1.1, §8).
 */
interface GetModelsUseCase {

    /** Catalog of all registered models (installed or not), tier-annotated. */
    suspend operator fun invoke(): List<ModelDescriptor>
}

/**
 * Model management operations: install, remove, activate (§1.1, §8.2).
 */
interface ManageModelUseCase {

    /** Installs [model], reporting download progress in [0.0, 1.0]. */
    suspend fun install(model: ModelDescriptor, onProgress: (Float) -> Unit)

    /** Removes an installed model; the active model is protected (§37). */
    suspend fun remove(modelId: String)

    /** Switches the active model (unload old → GC → load new, §38). */
    suspend fun activate(modelId: String)

    /** Returns the id of the currently active model, or null. */
    suspend fun activeModelId(): String?
}
