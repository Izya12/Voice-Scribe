package com.example.data.logging

import com.example.core.model.LogLevel
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileAppLoggerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newLogger(enabled: Boolean, level: LogLevel, dir: File = tmp.root): FileAppLogger {
        val logger = FileAppLogger(dir)
        logger.configure(enabled, level)
        return logger
    }

    @Test
    fun `disabled logger writes nothing`() {
        val logger = newLogger(enabled = false, level = LogLevel.INFO)
        logger.info("T", "hello")
        assertFalse(File(tmp.root, FileAppLogger.LOG_FILE_NAME).exists())
    }

    @Test
    fun `level filter drops debug messages when INFO is configured`() {
        val logger = newLogger(enabled = true, level = LogLevel.INFO)
        logger.debug("T", "noise")
        logger.info("T", "kept")
        val content = File(tmp.root, FileAppLogger.LOG_FILE_NAME).readText()
        assertFalse(content.contains("noise"))
        assertTrue(content.contains("kept"))
        assertTrue(content.contains("[INFO] [T] kept"))
    }

    @Test
    fun `error writes message and stack trace`() {
        val logger = newLogger(enabled = true, level = LogLevel.ERROR)
        logger.error("T", "boom", IllegalStateException("kaboom"))
        val content = File(tmp.root, FileAppLogger.LOG_FILE_NAME).readText()
        assertTrue(content.contains("[ERROR] [T] boom"))
        assertTrue(content.contains("kaboom"))
        assertTrue(content.contains("FileAppLoggerTest"))
    }

    @Test
    fun `rotation keeps three files and drops the oldest`() {
        // One write > MAX_FILE_BYTES forces the rotation path on the next write.
        val big = "x".repeat(5 * 1024 * 1024 + 1024)
        val logger = newLogger(enabled = true, level = LogLevel.DEBUG)
        logger.debug("T", big)   // -> voicescribe.log (oversized)
        logger.debug("T", big)   // rotates: log -> .1, fresh log holds this write
        logger.debug("T", "tail") // rotates: log -> .1, .1 -> .2; tail lands in fresh log

        val dir = tmp.root
        assertTrue(File(dir, FileAppLogger.LOG_FILE_NAME).readText().contains("tail"))
        assertTrue(File(dir, "${FileAppLogger.LOG_FILE_NAME}.1").exists())
        assertTrue(File(dir, "${FileAppLogger.LOG_FILE_NAME}.2").exists())
        assertFalse(File(dir, "${FileAppLogger.LOG_FILE_NAME}.3").exists())
    }
}