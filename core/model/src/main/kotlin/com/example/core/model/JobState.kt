package com.example.core.model

/**
 * Lifecycle state of a [TranscriptionJob].
 *
 * The state machine (§22) forbids:
 * - skipping stages (e.g. SUBMITTED -> TRANSCRIBING without DECODING),
 * - any transition out of a terminal state.
 */
enum class JobState {
    SUBMITTED,
    DECODING,
    PREPROCESSING,
    DIARIZING,
    TRANSCRIBING,
    COMPLETED,
    FAILED,
    CANCELLED;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELLED

    /**
     * Valid next states per the ratified state machine (§22).
     */
    fun allowedTransitions(): Set<JobState> = when (this) {
        SUBMITTED -> setOf(DECODING, FAILED, CANCELLED)
        DECODING -> setOf(PREPROCESSING, FAILED, CANCELLED)
        PREPROCESSING -> setOf(DIARIZING, FAILED, CANCELLED)
        DIARIZING -> setOf(TRANSCRIBING, FAILED, CANCELLED)
        TRANSCRIBING -> setOf(COMPLETED, FAILED, CANCELLED)
        COMPLETED, FAILED, CANCELLED -> emptySet()
    }
}
