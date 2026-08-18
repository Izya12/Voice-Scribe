package com.example.engine.model

import com.example.core.model.ModelDescriptor

/**
 * Resolves absolute on-disk paths for the sherpa-onnx model files.
 *
 * Implemented by `:data` (knows the `filesDir/models` layout) and injected into
 * the `:engine` adapters. Models are installed atomically with SHA-256
 * verification before any path is requested (§8.2).
 */
interface SherpaModelFiles {

    /** Whisper encoder/decoder/tokens for an ASR model, or null if not installed. */
    fun whisper(model: ModelDescriptor): WhisperModelFiles?

    /** Silero VAD ONNX model path, or null if not installed. */
    fun vadModel(): String?

    /** Speaker segmentation + embedding model paths, or null if not installed. */
    fun diarizationModels(): DiarizationModelFiles?
}

/**
 * Whisper model file set (encoder + decoder + token list).
 */
data class WhisperModelFiles(
    val encoder: String,
    val decoder: String,
    val tokens: String,
)

/**
 * Speaker diarization file set (pyannote segmentation + speaker embedder).
 */
data class DiarizationModelFiles(
    val segmentation: String,
    val embedding: String,
)