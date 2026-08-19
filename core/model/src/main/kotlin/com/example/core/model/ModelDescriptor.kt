package com.example.core.model

/**
 * Pre-packaged metadata registry entry for a downloadable model (§8).
 *
 * [sha256] is the exact integrity checksum used for atomic installation
 * verification (§35–36). Licensed models are never bundled/auto-downloaded
 * without a confirmed license card (§15).
 */
data class ModelDescriptor(
    val id: String,
    val displayName: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val sourceUrl: String,
    val sha256: String,
    val license: String,
    val tier: ModelTier,
    val extraFiles: List<ModelExtraFile> = emptyList(),
)

/**
 * Sidecar file downloaded alongside a plain (non-archive) model — e.g. the
 * token list for a GigaAM CTC model hosted as a separate file.
 */
data class ModelExtraFile(
    val name: String,
    val sourceUrl: String,
    val sha256: String,
)

/**
 * Device capability tiers used to select the default model set (§8.1).
 */
enum class ModelTier {
    ENTRY,
    MID,
    HIGH,
}
