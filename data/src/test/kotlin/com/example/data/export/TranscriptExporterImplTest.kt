package com.example.data.export

import com.example.core.domain.error.StorageException
import com.example.core.model.TranscriptionSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TranscriptExporterImplTest {

    private val exporter = TranscriptExporterImpl()

    private fun segments() = listOf(
        TranscriptionSegment(id = 1L, jobId = "job1", startUs = 0L, endUs = 2_000_000L, text = "Привет мир", speakerId = 0L),
        TranscriptionSegment(id = 2L, jobId = "job1", startUs = 2_000_000L, endUs = 4_500_000L, text = "Как дела?", speakerId = 1L),
    )

    @Test
    fun `txt joins text lines`() {
        val out = exporter.exportToTxt(segments())
        assertEquals("Привет мир\nКак дела?", out)
    }

    @Test
    fun `srt produces numbered cues with ms`() {
        val out = exporter.exportToSrt(segments())
        assertEquals(
            "1\n00:00:00,000 --> 00:00:02,000\nПривет мир\n\n2\n00:00:02,000 --> 00:00:04,500\nКак дела?",
            out,
        )
    }

    @Test
    fun `vtt starts with WEBVTT header`() {
        val out = exporter.exportToVtt(segments())
        assertTrue(out.startsWith("WEBVTT"))
        assertTrue(out.contains("00:00:02.000 --> 00:00:04.500"))
    }

    @Test
    fun `json escapes quotes and newlines`() {
        val s = listOf(
            TranscriptionSegment(id = 1L, jobId = "j", startUs = 0L, endUs = 1L, text = "say \"hi\"\nnext", speakerId = null),
        )
        val out = exporter.exportToJson(s, schemaVersion = 1)
        assertTrue(out.contains("\"text\": \"say \\\"hi\\\"\\nnext\""))
        assertTrue(out.contains("\"speakerId\": null"))
        assertTrue(out.contains("\"schemaVersion\": 1"))
    }

    @Test
    fun `rejects negative start`() {
        val s = listOf(
            TranscriptionSegment(id = 1L, jobId = "j", startUs = -1L, endUs = 10L, text = "x", speakerId = null),
        )
        try {
            exporter.exportToTxt(s)
            fail("expected StorageException")
        } catch (e: StorageException) {
        }
    }

    @Test
    fun `rejects end before start`() {
        val s = listOf(
            TranscriptionSegment(id = 1L, jobId = "j", startUs = 100L, endUs = 10L, text = "x", speakerId = null),
        )
        try {
            exporter.exportToTxt(s)
            fail("expected StorageException")
        } catch (e: StorageException) {
        }
    }

    @Test
    fun `rejects overlapping segments`() {
        val s = listOf(
            TranscriptionSegment(id = 1L, jobId = "j", startUs = 0L, endUs = 100L, text = "a", speakerId = null),
            TranscriptionSegment(id = 2L, jobId = "j", startUs = 50L, endUs = 200L, text = "b", speakerId = null),
        )
        try {
            exporter.exportToSrt(s)
            fail("expected StorageException")
        } catch (e: StorageException) {
        }
    }
}