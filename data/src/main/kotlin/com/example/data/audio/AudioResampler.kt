package com.example.data.audio

/**
 * Resamples mono float32 PCM (range [-1.0f, 1.0f]) to [targetSampleRate]
 * using linear interpolation (§3.1, step 2).
 *
 * Simple and deterministic — sufficient for the 44.1/48 kHz → 16 kHz path used
 * by Whisper and Silero VAD.
 */
class AudioResampler {

    /**
     * @param input mono samples at [inputSampleRate].
     * @return mono samples at [targetSampleRate]; preserves input when rates match.
     */
    fun resample(input: FloatArray, inputSampleRate: Int, targetSampleRate: Int): FloatArray {
        if (input.isEmpty()) return input
        if (inputSampleRate == targetSampleRate) return input

        val ratio = inputSampleRate.toDouble() / targetSampleRate.toDouble()
        val outputSize = (input.size / ratio).toInt()
        val out = FloatArray(outputSize)
        for (i in out.indices) {
            val srcPos = i * ratio
            val idx = srcPos.toInt()
            val frac = (srcPos - idx).toFloat()
            val s0 = input[idx.coerceAtMost(input.size - 1)]
            val s1 = input[(idx + 1).coerceAtMost(input.size - 1)]
            out[i] = s0 + (s1 - s0) * frac
        }
        return out
    }
}