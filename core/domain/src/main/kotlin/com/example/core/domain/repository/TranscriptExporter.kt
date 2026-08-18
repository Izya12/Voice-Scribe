package com.example.core.domain.repository

import com.example.core.model.TranscriptionSegment

/**
 * Stateless transcript export engine (§12). Implemented in `:data`.
 */
interface TranscriptExporter {

    fun exportToTxt(transcript: List<TranscriptionSegment>): String

    fun exportToSrt(transcript: List<TranscriptionSegment>): String

    fun exportToVtt(transcript: List<TranscriptionSegment>): String

    fun exportToJson(transcript: List<TranscriptionSegment>, schemaVersion: Int): String
}