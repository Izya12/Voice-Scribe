package com.example.core.domain.error

/**
 * Root of the typed, non-leaking domain exception hierarchy (§9).
 *
 * Native JNI errors are caught inside the C++ wrapper and re-thrown as a
 * standard [RuntimeException]; each `:engine` adapter maps that into the
 * matching domain subtype below.
 */
sealed class TranscriptionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Codec/MediaExtractor failure (§9). */
class DecodingException(
    message: String,
    cause: Throwable? = null,
) : TranscriptionException(message, cause)

/** Silero VAD failure (§9). */
class VadException(
    message: String,
    cause: Throwable? = null,
) : TranscriptionException(message, cause)

/** FastClustering / pyannote speaker-diarization failure (§9). */
class DiarizationException(
    message: String,
    cause: Throwable? = null,
) : TranscriptionException(message, cause)

/** Whisper inference failure (§9). */
class RecognitionException(
    message: String,
    cause: Throwable? = null,
) : TranscriptionException(message, cause)

/** Corrupted model / SHA-256 verification failure (§9, §35–36). */
class ModelManagerException(
    message: String,
    cause: Throwable? = null,
) : TranscriptionException(message, cause)

/** Room / filesystem persistence failure (§9). */
class StorageException(
    message: String,
    cause: Throwable? = null,
) : TranscriptionException(message, cause)
