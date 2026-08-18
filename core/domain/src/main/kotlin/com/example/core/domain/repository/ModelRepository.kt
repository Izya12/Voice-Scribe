package com.example.core.domain.repository

import com.example.core.model.ModelDescriptor
import kotlinx.coroutines.flow.Flow

/**
 * Model registry, download, and lifecycle boundary (§8).
 *
 * Implemented by `:data` (`ModelDownloadManager`). Models are installed
 * atomically in `filesDir/models` with on-the-fly SHA-256 verification (§8.2);
 * a model that fails verification is deleted and a
 * [com.example.core.domain.error.ModelManagerException] is thrown.
 */
interface ModelRepository {

    /** Hardcoded registry catalog (verified models with checksums, §8.1). */
    fun observeCatalog(): Flow<List<ModelDescriptor>>

    /** Models present on disk, annotated with their installed tier. */
    fun observeInstalledModels(): Flow<List<ModelDescriptor>>

    /** Id of the currently active model, or null when none is activated (§38). */
    fun observeActiveModelId(): Flow<String?>

    /** True once [modelId] is atomically installed and verified. */
    suspend fun isInstalled(modelId: String): Boolean

    /**
     * Downloads and atomically installs [model], reporting progress in
     * [0.0, 1.0]. Verifies SHA-256 against [ModelDescriptor.sha256].
     */
    suspend fun download(model: ModelDescriptor, onProgress: (Float) -> Unit)

    /** Deletes a model. Refuses to delete the currently active one (§37). */
    suspend fun delete(modelId: String)

    /** Replaces the active model atomically: unload old → GC → load new (§38). */
    suspend fun setActive(modelId: String)
}
