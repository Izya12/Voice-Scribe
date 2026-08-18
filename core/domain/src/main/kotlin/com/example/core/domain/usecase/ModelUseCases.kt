package com.example.core.domain.usecase

import com.example.core.domain.repository.ModelRepository
import com.example.core.model.ModelDescriptor
import kotlinx.coroutines.flow.first

/**
 * [GetModelsUseCase] backed by the [ModelRepository] catalog.
 */
class DefaultGetModelsUseCase(
    private val models: ModelRepository,
) : GetModelsUseCase {

    override suspend fun invoke(): List<ModelDescriptor> =
        models.observeCatalog().first()
}

/**
 * [ManageModelUseCase] backed by the [ModelRepository].
 */
class DefaultManageModelUseCase(
    private val models: ModelRepository,
) : ManageModelUseCase {

    override suspend fun install(model: ModelDescriptor, onProgress: (Float) -> Unit) =
        models.download(model, onProgress)

    override suspend fun remove(modelId: String) = models.delete(modelId)

    override suspend fun activate(modelId: String) = models.setActive(modelId)

    override suspend fun activeModelId(): String? {
        val installed = models.observeInstalledModels().first()
        // The active model is the one the repository has activated; for now
        // fall back to the first installed model by tier (mirrors resolveModel).
        return installed.minByOrNull { it.tier.ordinal }?.id
    }
}