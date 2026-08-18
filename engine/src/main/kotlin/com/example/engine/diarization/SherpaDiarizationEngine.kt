package com.example.engine.diarization

import android.content.Context
import com.example.core.domain.engine.DiarizationEngine
import com.example.core.domain.engine.SpeakerSegment
import com.example.core.domain.error.DiarizationException
import com.example.engine.model.SherpaModelFiles
import com.k2fsa.sherpa.onnx.FastClusteringConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationPyannoteModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline speaker diarization adapter over `sherpa-onnx`
 * OfflineSpeakerDiarization (§3, step 4).
 *
 * Runs pyannote segmentation + ERes2Net/CAM++ embeddings + agglomerative
 * clustering over the full waveform and returns speaker-annotated segments.
 */
class SherpaDiarizationEngine(
    context: Context,
    private val modelFiles: SherpaModelFiles,
    private val numThreads: Int = 4,
) : DiarizationEngine, AutoCloseable {

    private val lock = Any()
    @Volatile
    private var diarizer: OfflineSpeakerDiarization? = null

    override suspend fun diarize(
        pcm: FloatArray,
        numSpeakers: Int?,
    ): List<SpeakerSegment> = withContext(Dispatchers.Default) {
        synchronized(lock) {
            val diarizer = diarizer() ?: return@withContext emptyList()
            try {
                if (numSpeakers != null) {
                    val config = diarizer.config
                    config.clustering.numClusters = numSpeakers
                    diarizer.setConfig(config)
                }
                val segments = diarizer.process(pcm)
                segments.map { seg ->
                    SpeakerSegment(
                        startUs = (seg.start * 1_000_000L).toLong(),
                        endUs = (seg.end * 1_000_000L).toLong(),
                        speakerId = seg.speaker,
                    )
                }
            } catch (e: RuntimeException) {
                throw DiarizationException("Speaker diarization failed", e)
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            diarizer?.let { runCatching { it.release() } }
            diarizer = null
        }
    }

    private fun diarizer(): OfflineSpeakerDiarization? {
        diarizer?.let { return it }
        val files = modelFiles.diarizationModels() ?: return null
        checkModelFile(files.segmentation, "pyannote segmentation")
        checkModelFile(files.embedding, "speaker embedding")
        val segmentation = OfflineSpeakerSegmentationModelConfig(
            pyannote = OfflineSpeakerSegmentationPyannoteModelConfig(files.segmentation),
            numThreads = numThreads,
            debug = false,
            provider = "cpu",
        )
        val embedding = SpeakerEmbeddingExtractorConfig(
            model = files.embedding,
            numThreads = numThreads,
            debug = false,
            provider = "cpu",
        )
        val clustering = FastClusteringConfig(numClusters = 0, threshold = 0.5f)
        val config = OfflineSpeakerDiarizationConfig(
            segmentation = segmentation,
            embedding = embedding,
            clustering = clustering,
            minDurationOn = 0.5f,
            minDurationOff = 0.5f,
        )
        return try {
            // Absolute filesystem paths (filesDir) -> null AssetManager, else
            // sherpa-onnx reads via AAssetManager and abort()s (#2562).
            OfflineSpeakerDiarization(null, config).also { diarizer = it }
        } catch (e: RuntimeException) {
            throw DiarizationException("Failed to load diarization models", e)
        }
    }

    /** Guard against a missing/empty model file, which onnxruntime may exit(-1) on. */
    private fun checkModelFile(path: String, label: String) {
        val f = java.io.File(path)
        if (!f.exists() || f.length() == 0L) {
            throw DiarizationException("$label model file missing or empty: $path")
        }
    }
}