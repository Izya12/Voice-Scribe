package com.example.data.logging

import com.example.core.domain.logging.AppLogger
import com.example.core.model.LogLevel
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Appender-based file logger (§Settings).
 *
 * Writes timestamped lines to `logs/voicescribe.log` inside the app-private
 * directory (no storage permissions required). Thread-safe via a single lock;
 * each write opens the file in append mode so no buffered tail is lost on a
 * process kill.
 *
 * The constructor takes a plain [File] (not `Context`) so the rotation and
 * filtering logic is unit-testable on the JVM.
 */
@Singleton
class FileAppLogger @Inject constructor(logDir: File) : AppLogger {

    private val logFile: File = File(logDir, LOG_FILE_NAME)

    @Volatile
    private var enabled: Boolean = false

    @Volatile
    private var level: LogLevel = LogLevel.INFO

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val lock = Any()

    /** Applies new settings; takes effect immediately (no restart required). */
    fun configure(enabled: Boolean, level: LogLevel) {
        this.enabled = enabled
        this.level = level
    }

    override fun isEnabled(level: LogLevel): Boolean = enabled && this.level.canLog(level)

    override fun debug(tag: String, message: String, throwable: Throwable?) =
        write(LogLevel.DEBUG, tag, message, throwable)

    override fun info(tag: String, message: String, throwable: Throwable?) =
        write(LogLevel.INFO, tag, message, throwable)

    override fun warn(tag: String, message: String, throwable: Throwable?) =
        write(LogLevel.WARN, tag, message, throwable)

    override fun error(tag: String, message: String, throwable: Throwable?) =
        write(LogLevel.ERROR, tag, message, throwable)

    private fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!isEnabled(level)) return
        val line = buildString {
            append(dateFormat.format(Date()))
            append(" [").append(level.name).append("] [").append(tag).append("] ").append(message)
        }
        val stack = throwable?.let { stackTrace(it) }
        synchronized(lock) {
            try {
                rotateIfNeeded()
                BufferedWriter(FileWriter(logFile, /* append = */ true)).use { writer ->
                    writer.write(line)
                    writer.newLine()
                    if (stack != null) {
                        writer.write(stack)
                        writer.newLine()
                    }
                }
            } catch (_: Exception) {
                // Logging must never crash the app; a failed write is dropped.
            }
        }
    }

    private fun stackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    /**
     * Rotates when the current file exceeds [MAX_FILE_BYTES]: renames
     * `.N` -> `.N+1` (oldest dropped) and starts a fresh file.
     */
    private fun rotateIfNeeded() {
        if (logFile.length() < MAX_FILE_BYTES) return
        for (i in MAX_ROTATIONS - 2 downTo 0) {
            val src = rotatedFile(i)
            val dst = rotatedFile(i + 1)
            if (src.exists()) src.renameTo(dst)
        }
        logFile.delete()
    }

    private fun rotatedFile(index: Int): File =
        if (index == 0) logFile else File(logFile.parentFile, "$LOG_FILE_NAME.$index")

    companion object {
        const val LOG_FILE_NAME = "voicescribe.log"
        private const val MAX_FILE_BYTES = 5 * 1024 * 1024L
        private const val MAX_ROTATIONS = 3
    }
}