package com.example.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobStateTest {

    @Test
    fun `terminal states are terminal`() {
        listOf(JobState.COMPLETED, JobState.FAILED, JobState.CANCELLED).forEach {
            assertTrue("$it should be terminal", it.isTerminal)
        }
    }

    @Test
    fun `active states are not terminal`() {
        listOf(
            JobState.SUBMITTED,
            JobState.DECODING,
            JobState.PREPROCESSING,
            JobState.DIARIZING,
            JobState.TRANSCRIBING,
        ).forEach {
            assertFalse("$it should not be terminal", it.isTerminal)
        }
    }

    @Test
    fun `forward chain is legal`() {
        assertEquals(JobState.DECODING, JobState.SUBMITTED.nextOf(JobState.DECODING))
        assertTrue(JobState.SUBMITTED.allowedTransitions().contains(JobState.DECODING))
        assertTrue(JobState.DECODING.allowedTransitions().contains(JobState.PREPROCESSING))
        assertTrue(JobState.PREPROCESSING.allowedTransitions().contains(JobState.DIARIZING))
        assertTrue(JobState.DIARIZING.allowedTransitions().contains(JobState.TRANSCRIBING))
        assertTrue(JobState.TRANSCRIBING.allowedTransitions().contains(JobState.COMPLETED))
    }

    @Test
    fun `skipping steps is forbidden`() {
        assertFalse(JobState.SUBMITTED.allowedTransitions().contains(JobState.TRANSCRIBING))
        assertFalse(JobState.SUBMITTED.allowedTransitions().contains(JobState.COMPLETED))
        assertFalse(JobState.DECODING.allowedTransitions().contains(JobState.DIARIZING))
    }

    @Test
    fun `terminal states cannot transition anywhere`() {
        JobState.entries.forEach { from ->
            val next = from.allowedTransitions()
            if (from.isTerminal) {
                assertTrue("$from must not allow transitions, got $next", next.isEmpty())
            } else {
                assertFalse("$from must allow progress", next.isEmpty())
            }
        }
    }

    @Test
    fun `CANCELLED cannot lead to FAILED or COMPLETED`() {
        assertFalse(JobState.CANCELLED.allowedTransitions().contains(JobState.FAILED))
        assertFalse(JobState.CANCELLED.allowedTransitions().contains(JobState.COMPLETED))
    }

    private fun JobState.nextOf(target: JobState): JobState =
        if (allowedTransitions().contains(target)) target else throw IllegalStateException("Illegal transition")
}
