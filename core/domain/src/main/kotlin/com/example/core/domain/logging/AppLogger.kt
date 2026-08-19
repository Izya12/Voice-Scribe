package com.example.core.domain.logging

import com.example.core.model.LogLevel

/**
 * Minimal logging boundary for the domain layer (§Settings).
 *
 * Implemented by `:data` ([FileAppLogger]); domain code must never depend on
 * Android APIs, so the interface stays pure Kotlin. Implementations decide
 * whether a level is enabled (file toggle + [LogLevel] filter) and where the
 * output goes.
 */
interface AppLogger {

    /** True if [level] would be written right now (toggle on + severity filter). */
    fun isEnabled(level: LogLevel): Boolean

    fun debug(tag: String, message: String, throwable: Throwable? = null)

    fun info(tag: String, message: String, throwable: Throwable? = null)

    fun warn(tag: String, message: String, throwable: Throwable? = null)

    fun error(tag: String, message: String, throwable: Throwable? = null)
}