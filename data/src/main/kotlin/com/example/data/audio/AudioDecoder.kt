package com.example.data.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.core.domain.error.DecodingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes a media URI to 16 kHz mono float32 PCM chunks (§3, step 1-2).
 *
 * Uses MediaExtractor + MediaCodec (platform decoders). The decoded PCM is
 * converted from the native sample rate / channel count to 16 kHz mono via
 * [AudioResampler] and emitted in bounded chunks to respect RAM limits (§7.1).
 */
class AudioDecoder(
    private val context: Context,
    private val resampler: AudioResampler,
    private val chunkSamples: Int = 480_000, // 30s @ 16 kHz
) {

    fun decode(uri: Uri): Flow<FloatArray> = flow {
        val extractor = MediaExtractor()
        try {
            Log.d(TAG, "setDataSource uri=$uri")
            extractor.setDataSource(context, uri, null)
            val trackIndex = selectAudioTrack(extractor)
                ?: throw DecodingException("No audio track found in ${uri.lastPathSegment}")
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: throw DecodingException("Audio track has no MIME type")
            Log.d(TAG, "track=$trackIndex mime=$mime sr=$sampleRate ch=$channels durationUs=${format.containsKey(MediaFormat.KEY_DURATION)?.let { format.getLong(MediaFormat.KEY_DURATION) }}")

            val decoder = MediaCodec.createDecoderByType(mime)
            Log.d(TAG, "decoder=${decoder.name}")
            try {
                decoder.configure(format, null, null, 0)
                decoder.start()
                val bufferInfo = MediaCodec.BufferInfo()
                var inputEos = false
                var outputEos = false
                var drainIdleTicks = 0
                var pending = FloatArray(0)
                var fedSamples = 0
                var emittedSamples = 0
                var loopCount = 0L

                while (!outputEos) {
                    if (++loopCount % 1000L == 0L) {
                        Log.d(TAG, "loop=$loopCount fed=$fedSamples emitted=$emittedSamples pending=${pending.size}")
                    }
                    if (!inputEos) {
                        val inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val inputBuffer = decoder.getInputBuffer(inputIndex) ?: continue
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                Log.d(TAG, "inputEOS fed=$fedSamples")
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEos = true
                            } else {
                                fedSamples += sampleSize
                                decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outputIndex >= 0 -> {
                            drainIdleTicks = 0
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputEos = true
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && bufferInfo.size > 0) {
                                decoder.getOutputBuffer(outputIndex)?.let { outBuf ->
                                    val pcm16 = copyBytes(outBuf, bufferInfo.size)
                                    val floats = toFloatMono(pcm16, channels)
                                    val resampled = resampler.resample(floats, sampleRate, TARGET_SAMPLE_RATE)
                                    pending = pending + resampled
                                    emittedSamples += resampled.size
                                    while (pending.size >= chunkSamples) {
                                        val chunk = pending.copyOfRange(0, chunkSamples)
                                        emit(chunk)
                                        pending = pending.copyOfRange(chunkSamples, pending.size)
                                    }
                                }
                            }
                            decoder.releaseOutputBuffer(outputIndex, false)
                        }

                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            drainIdleTicks = 0
                        }

                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            drainIdleTicks++
                            if (inputEos && drainIdleTicks >= DRAIN_IDLE_TICKS) {
                                Log.d(TAG, "drain timeout after ${bufferInfo.presentationTimeUs}us, emitted=$emittedSamples")
                                outputEos = true
                            }
                        }
                    }
                }

                Log.d(TAG, "done fed=$fedSamples emitted=$emittedSamples pending=${pending.size}")
                if (pending.isNotEmpty()) emit(pending)
            } finally {
                runCatching { decoder.stop() }
                runCatching { decoder.release() }
            }
        } catch (e: DecodingException) {
            Log.e(TAG, "decode failed", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "decode error", e)
            throw DecodingException("Failed to decode audio: ${e.message}", e)
        } finally {
            Log.d(TAG, "extractor.release()")
            extractor.release()
        }
    }.flowOn(Dispatchers.IO)

    private fun selectAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return null
    }

    private fun copyBytes(buffer: ByteBuffer, size: Int): ByteArray {
        val b = ByteArray(size)
        buffer.position(0)
        buffer.get(b, 0, size)
        return b
    }

    private fun toFloatMono(pcm16: ByteArray, channels: Int): FloatArray {
        val shorts = ShortArray(pcm16.size / 2)
        ByteBuffer.wrap(pcm16).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        val mono = FloatArray(shorts.size / channels)
        for (i in mono.indices) {
            var sum = 0
            for (c in 0 until channels) sum += shorts[i * channels + c].toInt()
            mono[i] = (sum / channels) / 32768f
        }
        return mono
    }

    private companion object {
        const val TARGET_SAMPLE_RATE = 16_000
        const val TIMEOUT_US = 10_000L
        const val TAG = "AudioDecoder"

        /** How many consecutive INFO_TRY_AGAIN after input EOS before declaring the drain finished. */
        const val DRAIN_IDLE_TICKS = 100
    }
}