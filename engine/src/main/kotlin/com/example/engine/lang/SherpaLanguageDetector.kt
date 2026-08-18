package com.example.engine.lang

import android.content.Context
import com.example.core.domain.engine.LanguageDetector
import com.example.core.domain.engine.LanguageDetectionResult
import com.example.core.domain.error.RecognitionException
import com.example.core.model.ModelDescriptor
import com.example.engine.model.SherpaModelFiles
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentification
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentificationConfig
import com.k2fsa.sherpa.onnx.SpokenLanguageIdentificationWhisperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Language detection adapter over `sherpa-onnx` SpokenLanguageIdentification
 * (§4.1). Uses the already-installed Whisper encoder/decoder — no separate
 * language-ID model is downloaded.
 */
class SherpaLanguageDetector(
    context: Context,
    private val modelFiles: SherpaModelFiles,
    private val numThreads: Int = 4,
) : LanguageDetector, AutoCloseable {

    private val lock = Any()
    @Volatile
    private var detector: SpokenLanguageIdentification? = null

    override suspend fun detectLanguage(
        pcm: FloatArray,
        model: ModelDescriptor,
    ): LanguageDetectionResult = withContext(Dispatchers.Default) {
        synchronized(lock) {
            val detector = detector(model) ?: return@withContext LanguageDetectionResult("en", 0f)
            try {
                val stream = detector.createStream()
                try {
                    stream.acceptWaveform(pcm, SAMPLE_RATE_HZ)
                    val lang = detector.compute(stream)
                    // SpokenLanguageIdentification returns the top language
                    // string; confidence is an empirical constant (arch §4.1).
                    // An empty result must NOT default to "en" — the caller
                    // treats a blank code as "let Whisper detect internally".
                    LanguageDetectionResult(lang, 0.9f)
                } finally {
                    stream.release()
                }
            } catch (e: RuntimeException) {
                throw RecognitionException("Language identification failed", e)
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            detector?.let { runCatching { it.release() } }
            detector = null
        }
    }

    private fun detector(model: ModelDescriptor): SpokenLanguageIdentification? {
        detector?.let { return it }
        val files = modelFiles.whisper(model) ?: return null
        checkModelFile(files.encoder, "whisper encoder")
        checkModelFile(files.decoder, "whisper decoder")
        val whisper = SpokenLanguageIdentificationWhisperConfig(
            encoder = files.encoder,
            decoder = files.decoder,
            tailPaddings = -1,
        )
        val config = SpokenLanguageIdentificationConfig(
            whisper = whisper,
            numThreads = numThreads,
            debug = false,
            provider = "cpu",
        )
return try {
            // Absolute filesystem paths (filesDir) -> null AssetManager, else
            // sherpa-onnx reads via AAssetManager and abort()s (#2562).
            SpokenLanguageIdentification(null, config).also { detector = it }
        } catch (e: RuntimeException) {
            throw RecognitionException("Failed to load language-ID model", e)
        }
    }

    /** Guard against a missing/empty model file, which onnxruntime may exit(-1) on. */
    private fun checkModelFile(path: String, label: String) {
        val f = java.io.File(path)
        if (!f.exists() || f.length() == 0L) {
            throw RecognitionException("$label model file missing or empty: $path")
        }
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 16_000
    }
}