package com.example.engine.whisper

import android.content.Context
import com.example.core.domain.engine.RecognizedWord
import com.example.core.domain.engine.RecognitionResult
import com.example.core.domain.engine.SpeechEngine
import com.example.core.domain.error.RecognitionException
import com.example.core.model.ModelDescriptor
import com.example.engine.model.SherpaModelFiles
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Whisper ASR adapter over `sherpa-onnx` OfflineRecognizer (§3, step 6).
 *
 * Native objects are cached per [ModelDescriptor.id] and guarded by a
 * per-instance lock so no two coroutines touch the same `ptr` concurrently
 * (§5). Switching the active model unloads the previous native object (§38).
 */
class SherpaWhisperEngine(
    context: Context,
    private val modelFiles: SherpaModelFiles,
    private val numThreads: Int = 4,
) : SpeechEngine, AutoCloseable {

    private val cache = ConcurrentHashMap<String, OfflineRecognizer>()
    private val lock = Any()

    override suspend fun transcribe(
        pcm: FloatArray,
        model: ModelDescriptor,
        language: String?,
    ): RecognitionResult = withContext(Dispatchers.Default) {
        if (pcm.isEmpty()) {
            // Feeding an empty waveform to sherpa-onnx crashes natively
            // (SIGSEGV in OfflineRecognizer.decode).
            throw RecognitionException("Cannot transcribe empty audio")
        }
        val files = modelFiles.whisper(model)
            ?: throw RecognitionException("Whisper model files missing: ${model.id}")
        checkModelFiles(files)
        val recognizer = recognizerFor(model, files, language)
        synchronized(lock) {
            val stream = try {
                recognizer.createStream()
            } catch (e: RuntimeException) {
                throw RecognitionException("Failed to create Whisper stream", e)
            }
            try {
                stream.acceptWaveform(pcm, SAMPLE_RATE_HZ)
                recognizer.decode(stream)
                toResult(recognizer.getResult(stream))
            } catch (e: RecognitionException) {
                throw e
            } catch (e: RuntimeException) {
                throw RecognitionException("Whisper inference failed", e)
            } finally {
                stream.release()
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            cache.values.forEach { runCatching { it.release() } }
            cache.clear()
        }
    }

    private fun recognizerFor(
        model: ModelDescriptor,
        files: com.example.engine.model.WhisperModelFiles,
        language: String?,
    ): OfflineRecognizer {
        // The language is baked into the native config at construction time, so
        // the cache key MUST include it — otherwise the first recognizer ever
        // created (e.g. with "en") is reused for every later language choice.
        val key = cacheKey(model.id, language)
        return cache[key] ?: synchronized(lock) {
            cache[key] ?: createRecognizer(model, files, language).also { cache[key] = it }
        }
    }

    private fun cacheKey(modelId: String, language: String?): String = "$modelId|${language ?: ""}"

    private fun createRecognizer(
        model: ModelDescriptor,
        files: com.example.engine.model.WhisperModelFiles,
        language: String?,
    ): OfflineRecognizer {
        val whisper = OfflineWhisperModelConfig(
            encoder = files.encoder,
            decoder = files.decoder,
            language = language ?: "",
            task = "transcribe",
            tailPaddings = -1,
            enableTokenTimestamps = true,
            enableSegmentTimestamps = true,
        )
        val modelConfig = OfflineModelConfig(
            whisper = whisper,
            numThreads = numThreads,
            debug = false,
            provider = "cpu",
            modelType = "whisper",
            tokens = files.tokens,
        )
        val featureConfig = FeatureConfig(
            sampleRate = SAMPLE_RATE_HZ,
            featureDim = 80,
            dither = 0f,
        )
        val config = OfflineRecognizerConfig(
            featConfig = featureConfig,
            modelConfig = modelConfig,
            decodingMethod = "greedy_search",
            maxActivePaths = 4,
        )
        return try {
            // Absolute filesystem paths (filesDir) -> null AssetManager, else
            // sherpa-onnx reads via AAssetManager and abort()s (#2562).
            OfflineRecognizer(null, config)
        } catch (e: RuntimeException) {
            throw RecognitionException("Failed to load Whisper model ${model.id}", e)
        }
    }

    private fun checkModelFiles(files: com.example.engine.model.WhisperModelFiles) {
        val missing = listOf("encoder" to files.encoder, "decoder" to files.decoder, "tokens" to files.tokens)
            .filter { (_, p) -> !java.io.File(p).exists() || java.io.File(p).length() == 0L }
            .map { it.first }
        if (missing.isNotEmpty()) {
            throw RecognitionException("Whisper model files missing/empty: ${missing.joinToString()}")
        }
    }

    private fun toResult(result: com.k2fsa.sherpa.onnx.OfflineRecognizerResult): RecognitionResult {
        val tokens = result.tokens
        val timestamps = result.timestamps
        val words = tokens.indices.map { i ->
            val startSec = timestamps.getOrElse(i) { 0f }
            val endSec = timestamps.getOrElse(i + 1) { startSec }
            RecognizedWord(
                word = tokens[i].trim(),
                startUs = (startSec * 1_000_000L).toLong(),
                endUs = (endSec * 1_000_000L).toLong(),
                confidence = 0f,
            )
        }
        return RecognitionResult(
            text = result.text,
            words = words,
            language = result.lang,
        )
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 16_000
    }
}