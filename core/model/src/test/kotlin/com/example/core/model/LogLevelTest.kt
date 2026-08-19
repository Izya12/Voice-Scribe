package com.example.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogLevelTest {

    @Test
    fun `DEBUG level logs everything`() {
        LogLevel.entries.forEach { assertTrue(LogLevel.DEBUG.canLog(it)) }
    }

    @Test
    fun `ERROR level logs errors only`() {
        assertFalse(LogLevel.ERROR.canLog(LogLevel.DEBUG))
        assertFalse(LogLevel.ERROR.canLog(LogLevel.INFO))
        assertFalse(LogLevel.ERROR.canLog(LogLevel.WARN))
        assertTrue(LogLevel.ERROR.canLog(LogLevel.ERROR))
    }

    @Test
    fun `INFO logs info warn and error but not debug`() {
        assertFalse(LogLevel.INFO.canLog(LogLevel.DEBUG))
        assertTrue(LogLevel.INFO.canLog(LogLevel.INFO))
        assertTrue(LogLevel.INFO.canLog(LogLevel.WARN))
        assertTrue(LogLevel.INFO.canLog(LogLevel.ERROR))
    }

    @Test
    fun `WARN logs warn and error but not info`() {
        assertFalse(LogLevel.WARN.canLog(LogLevel.DEBUG))
        assertFalse(LogLevel.WARN.canLog(LogLevel.INFO))
        assertTrue(LogLevel.WARN.canLog(LogLevel.WARN))
        assertTrue(LogLevel.WARN.canLog(LogLevel.ERROR))
    }
}