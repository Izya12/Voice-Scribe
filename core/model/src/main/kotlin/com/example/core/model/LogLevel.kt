package com.example.core.model

/**
 * Severity levels for file logging (§Settings).
 *
 * Ordered by increasing severity: a logger configured with level L writes
 * messages of severity >= L and drops the rest.
 */
enum class LogLevel(val severity: Int) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    ;

    /**
     * True if a message at [level] passes the filter of this configured level.
     */
    fun canLog(level: LogLevel): Boolean = level.severity >= severity
}