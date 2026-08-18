package com.example.engine.vad

import android.content.Context
import com.example.core.domain.engine.SpeechSegment
import com.example.core.domain.engine.VadEngine
import com.example.core.domain.error.VadException
import com.example.engine.model.SherpaModelFiles
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.SpeechSegment as SherpaSpeechSegment
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Silero VAD adapter over `sherpa-onnx` Vad (§3, step 3).
 *
 * Processes a 16 kHz mono float PCM stream in small frames and collects
 * speech intervals as microsecond timestamps. Native object lifecycle is
 * guarded like every other `:engine` adapter (§5).
 */
class SherpaVadEngine(
    context: Context,
    private val modelFiles: SherpaModelFiles,
    private val sampleRateHz: Int = 16_000,
) : VadEngine, AutoCloseable {

    private val lock = Any()
    @Volatile
    private var vad: Vad? = null

    override suspend fun detectSpeech(pcm: FloatArray): List<SpeechSegment> =
        withContext(Dispatchers.Default) {
            synchronized(lock) {
                val vad = vad()
                    ?: throw VadException("Silero VAD model not installed (filesDir/models/silero_vad_v5.onnx missing)")
                try {
                    val segments = mutableListOf<SpeechSegment>()
                    vad.reset()
                    // sherpa-onnx Vad must be fed in SMALL chunks: per-call
                    // `is_speech` is an OR over all windows of the call and
                    // segment start is derived from the buffer tail — feeding
                    // the whole file at once collapses speech to a ~0.1 s stub
                    // at the very end. 0.1 s per call is the documented pattern.
                    val chunkSamples = sampleRateHz / 10
                    var offset = 0
                    while (offset < pcm.size) {
                        val n = minOf(chunkSamples, pcm.size - offset)
                        vad.acceptWaveform(pcm.copyOfRange(offset, offset + n))
                        offset += n
                    }
                    vad.flush()
                    while (!vad.empty()) {
                        val seg: SherpaSpeechSegment = vad.front()
                        val startUs = (seg.start * 1_000_000L) / sampleRateHz
                        val endUs = ((seg.start + seg.samples.size) * 1_000_000L) / sampleRateHz
                        segments += SpeechSegment(startUs, endUs)
                        vad.pop()
                    }
                    segments
                } catch (e: RuntimeException) {
                    throw VadException("Silero VAD inference failed", e)
                }
            }
        }

    override fun close() {
        synchronized(lock) {
            vad?.let { runCatching { it.release() } }
            vad = null
        }
    }

    private fun vad(): Vad? {
        vad?.let { return it }
        val model = modelFiles.vadModel() ?: return null
        checkModelFile(model, "Silero VAD")
        val config = VadModelConfig(
            sileroVadModelConfig = SileroVadModelConfig(
                model = model,
                threshold = 0.5f,
                minSilenceDuration = 0.5f,
                minSpeechDuration = 0.25f,
                windowSize = 512,
                maxSpeechDuration = 20f,
            ),
            sampleRate = sampleRateHz,
            numThreads = 1,
            provider = "cpu",
            debug = false,
        )
        return try {
            // Models are loaded from absolute filesystem paths (filesDir), NOT
            // from APK assets: pass a null AssetManager or sherpa-onnx will try
            // AAssetManager and abort() on failure (k2-fsa/sherpa-onnx#2562).
            Vad(null, config).also { vad = it }
        } catch (e: RuntimeException) {
            throw VadException("Failed to load Silero VAD model", e)
        }
    }

    /** Guard against a missing/empty model file, which onnxruntime may exit(-1) on. */
    private fun checkModelFile(path: String, label: String) {
        val f = java.io.File(path)
        if (!f.exists() || f.length() == 0L) {
            throw VadException("$label model file missing or empty: $path")
        }
    }
}