package com.example.data.repository

import android.content.Context
import com.example.core.domain.error.ModelManagerException
import com.example.core.domain.repository.ModelRepository
import com.example.core.model.ModelDescriptor
import com.example.data.database.VoiceScribeDao
import com.example.data.database.entity.ModelEntity
import com.example.data.model.ModelCatalog
import com.example.engine.model.DiarizationModelFiles
import com.example.engine.model.SherpaModelFiles
import com.example.engine.model.WhisperModelFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room + filesystem backed [ModelRepository] and [SherpaModelFiles] (§8).
 *
 * Models live in `filesDir/models` and are installed atomically: download to a
 * `.tmp` in cacheDir, verify SHA-256 on the fly (§8.2), then either atomically
 * move a plain `.onnx` into place or extract a `.tar.bz2` archive into
 * `filesDir/models/<modelId>/`. The active model is protected from deletion
 * (§37) and switching is a sequential unload→GC→load (§38).
 */
@Singleton
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: VoiceScribeDao,
) : ModelRepository, SherpaModelFiles {

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    private val tmpDir: File
        get() = File(context.cacheDir, "downloads").apply { mkdirs() }

    // --- Registry / installed ---

    override fun observeCatalog(): Flow<List<ModelDescriptor>> = flowOf(ModelCatalog.catalog)

    override fun observeInstalledModels(): Flow<List<ModelDescriptor>> =
        dao.observeModels().map { entities ->
            entities.mapNotNull { entity ->
                ModelCatalog.catalog.firstOrNull { it.id == entity.id }
            }
        }

    override fun observeActiveModelId(): Flow<String?> = dao.observeActiveModelId()

    override suspend fun isInstalled(modelId: String): Boolean =
        dao.getModel(modelId) != null && modelFilesOnDisk(modelId)

    // --- Download / install (§8.2) ---

    override suspend fun download(model: ModelDescriptor, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val tmp = File(tmpDir, "${model.fileName}.tmp")
        try {
            downloadWithChecksum(model, tmp, onProgress)
            installVerified(model, tmp)
            dao.upsertModel(ModelEntity(id = model.id, fileName = model.fileName))
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    /**
     * Moves a verified download into its final location. Plain `.onnx` files
     * are placed directly in `models/`; `.tar.bz2` archives are extracted into
     * `models/<modelId>/`.
     */
    private fun installVerified(model: ModelDescriptor, tmp: File) {
        if (model.fileName.endsWith(".tar.bz2")) {
            val dir = File(modelsDir, model.id)
            if (dir.exists()) dir.deleteRecursively()
            dir.mkdirs()
            extractTarBz2(tmp, dir)
        } else {
            val target = File(modelsDir, model.fileName)
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
            }
        }
    }

    private fun extractTarBz2(archive: File, destDir: File) {
        try {
            var extractedFiles = 0
            BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive))).use { bz2 ->
                TarArchiveInputStream(bz2).use { tar ->
                    var entry: TarArchiveEntry? = tar.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            // Strip the leading package folder (e.g. sherpa-onnx-whisper-tiny/).
                            val name = entry.name.substringAfter('/')
                            if (name.isNotBlank()) {
                                val out = File(destDir, name)
                                out.parentFile?.mkdirs()
                                tar.copyTo(out.outputStream())
                                extractedFiles++
                            }
                        }
                        entry = tar.nextEntry
                    }
                }
            }
            if (extractedFiles == 0) {
                throw ModelManagerException("Archive ${archive.name} contained no files")
            }
        } catch (e: Exception) {
            throw ModelManagerException("Failed to extract ${archive.name}", e)
        }
    }

    private fun downloadWithChecksum(
        model: ModelDescriptor,
        tmp: File,
        onProgress: (Float) -> Unit,
    ) {
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = java.net.URL(model.sourceUrl).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        try {
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read: Int
                var downloaded = 0L
                tmp.outputStream().use { out ->
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(downloaded.toFloat() / total.toFloat())
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != model.sha256.lowercase()) {
            throw ModelManagerException(
                "SHA-256 mismatch for ${model.id}: expected ${model.sha256}, got $actual",
            )
        }
    }

    override suspend fun delete(modelId: String) = withContext(Dispatchers.IO) {
        val entity = dao.getModel(modelId) ?: return@withContext
        if (entity.isActive) {
            throw ModelManagerException("Cannot delete the active model (§37): $modelId")
        }
        modelFilesOnDiskList(modelId).forEach { it.deleteRecursively() }
        dao.deleteModel(modelId)
    }

    override suspend fun setActive(modelId: String) = withContext(Dispatchers.IO) {
        if (!isInstalled(modelId)) throw ModelManagerException("Model not installed: $modelId")
        dao.clearActiveModels()
        dao.setActiveModel(modelId)
    }

    // --- SherpaModelFiles (engine contract) ---

    override fun whisper(model: ModelDescriptor): WhisperModelFiles? {
        if (!modelFilesOnDisk(model.id)) return null
        val dir = modelDir(model.id)
        val prefix = whisperFilePrefix(model.id) ?: return null
        // Prefer int8 quantized variants when present (matching the catalog
        // "int8" tier); fall back to fp32 files inside the extracted archive.
        val encoder = firstExisting(dir, "$prefix-encoder.int8.onnx", "$prefix-encoder.onnx")
            ?: return null
        val decoder = firstExisting(
            dir,
            "$prefix-decoder.int8.onnx",
            "$prefix-decoder-onnx.int8.onnx",
            "$prefix-decoder.onnx",
            "$prefix-decoder-onnx",
        ) ?: return null
        val tokens = firstExisting(dir, "$prefix-tokens.txt")
            ?: return null
        return WhisperModelFiles(encoder.absolutePath, decoder.absolutePath, tokens.absolutePath)
    }

    /** Maps model ids like `whisper-tiny` to the file prefix inside the archive (`tiny-`). */
    private fun whisperFilePrefix(modelId: String): String? = when (modelId) {
        "whisper-tiny" -> "tiny"
        "whisper-base" -> "base"
        "whisper-medium" -> "medium"
        "whisper-large-v3" -> "large-v3"
        "whisper-large-v2" -> "large-v2"
        "whisper-large-v1" -> "large-v1"
        else -> null
    }

    private fun firstExisting(dir: File, vararg names: String): File? =
        names.firstOrNull { File(dir, it).exists() }?.let { File(dir, it) }

    override fun vadModel(): String? {
        val file = File(modelsDir, "silero_vad_v5.onnx")
        return file.takeIf { it.exists() }?.absolutePath
    }

    override fun diarizationModels(): DiarizationModelFiles? {
        val seg = File(modelDir("pyannote-segmentation"), "model.int8.onnx")
        val emb = File(modelsDir, "3dspeaker_speech_campplus_sv_en_voxceleb_16k.onnx")
        if (!seg.exists() || !emb.exists()) return null
        return DiarizationModelFiles(seg.absolutePath, emb.absolutePath)
    }

    // --- helpers ---

    private fun modelDir(modelId: String): File = File(modelsDir, modelId).apply { mkdirs() }

    private fun modelFilesOnDisk(modelId: String): Boolean {
        val catalogModel = ModelCatalog.catalog.firstOrNull { it.id == modelId } ?: return false
        return if (catalogModel.fileName.endsWith(".tar.bz2")) {
            val dir = File(modelsDir, modelId)
            // An empty extraction dir means the archive failed to unpack — treat as not installed.
            dir.exists() && (dir.listFiles()?.isNotEmpty() == true)
        } else {
            File(modelsDir, catalogModel.fileName).exists()
        }
    }

    private fun modelFilesOnDiskList(modelId: String): List<File> {
        val catalogModel = ModelCatalog.catalog.firstOrNull { it.id == modelId } ?: return emptyList()
        val file = if (catalogModel.fileName.endsWith(".tar.bz2")) {
            File(modelsDir, modelId)
        } else {
            File(modelsDir, catalogModel.fileName)
        }
        return if (file.exists()) listOf(file) else emptyList()
    }
}