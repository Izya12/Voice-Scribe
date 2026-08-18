package com.example.data.export

import com.example.core.domain.error.StorageException
import com.example.core.domain.repository.TranscriptExporter
import com.example.core.model.TranscriptionSegment
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

/**
 * TXT / SRT / VTT / JSON transcript exporters (§12).
 *
 * Enforces monotonicity before export: `startUs <= endUs` per segment and
 * `segment[n].endUs <= segment[n+1].startUs`; empty text is sanitized.
 */
@Singleton
class TranscriptExporterImpl @Inject constructor() : TranscriptExporter {

    override fun exportToTxt(transcript: List<TranscriptionSegment>): String {
        validate(transcript)
        return transcript.joinToString("\n") { it.text.trim() }.trimEnd('\n')
    }

    override fun exportToSrt(transcript: List<TranscriptionSegment>): String =
        buildString {
            validate(transcript)
            transcript.forEachIndexed { index, seg ->
                append(index + 1)
                append('\n')
                append(formatSrtTime(seg.startUs))
                append(" --> ")
                append(formatSrtTime(seg.endUs))
                append('\n')
                append(seg.text.trim())
                append("\n\n")
            }
        }.trimEnd('\n')

    override fun exportToVtt(transcript: List<TranscriptionSegment>): String =
        buildString {
            append("WEBVTT\n\n")
            validate(transcript)
            transcript.forEach { seg ->
                append(formatVttTime(seg.startUs))
                append(" --> ")
                append(formatVttTime(seg.endUs))
                append('\n')
                append(seg.text.trim())
                append("\n\n")
            }
        }.trimEnd('\n')

    override fun exportToJson(transcript: List<TranscriptionSegment>, schemaVersion: Int): String {
        validate(transcript)
        val body = transcript.joinToString(",\n") { seg ->
            """    {
      "startUs": ${seg.startUs},
      "endUs": ${seg.endUs},
      "speakerId": ${seg.speakerId ?: "null"},
      "text": ${jsonEscape(seg.text.trim())}
    }"""
        }
        return """{
  "schemaVersion": $schemaVersion,
  "segments": [
$body
  ]
}"""
    }

    private fun validate(transcript: List<TranscriptionSegment>) {
        transcript.forEach { seg ->
            if (seg.startUs < 0 || seg.endUs < seg.startUs) {
                throw StorageException("Invalid timestamps for segment: $seg")
            }
        }
        transcript.zipWithNext().forEach { (a, b) ->
            if (a.endUs > b.startUs) {
                throw StorageException("Segments overlap or out of order: [$a] [$b]")
            }
        }
    }

    private fun formatSrtTime(us: Long): String {
        val totalMs = us / 1000
        val hours = totalMs / 3_600_000
        val minutes = (totalMs % 3_600_000) / 60_000
        val seconds = (totalMs % 60_000) / 1000
        val millis = totalMs % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    private fun formatVttTime(us: Long): String {
        val totalMs = us / 1000
        val hours = totalMs / 3_600_000
        val minutes = (totalMs % 3_600_000) / 60_000
        val seconds = (totalMs % 60_000) / 1000
        val millis = totalMs % 1000
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }

    private fun jsonEscape(s: String): String = buildString {
        append('"')
        s.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
        append('"')
    }
}