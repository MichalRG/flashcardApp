package com.krzyzowski.vocabboost.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class TestTimerTest {
    private val start = Instant.parse("2026-11-02T08:00:00Z")

    @Test
    fun `the fine-to-stop cue starts at 5 minutes on screen`() {
        val timer = TestTimer().shown(start)

        assertFalse(timer.fineToStop(start.plusSeconds(299)))
        assertTrue(timer.fineToStop(start.plusSeconds(300)))
    }

    @Test
    fun `time is up at 10 minutes on screen`() {
        val timer = TestTimer().shown(start)

        assertFalse(timer.timeUp(start.plusSeconds(599)))
        assertTrue(timer.timeUp(start.plusSeconds(600)))
    }

    @Test
    fun `the clock stops while the test is hidden and resumes when it is shown again`() {
        val shownAgain = start.plus(Duration.ofHours(1))

        val hidden = TestTimer().shown(start).hidden(start.plus(Duration.ofMinutes(3)))
        val resumed = hidden.shown(shownAgain)

        assertEquals(Duration.ofMinutes(3), hidden.elapsed(shownAgain))
        assertEquals(Duration.ofMinutes(4), resumed.elapsed(shownAgain.plus(Duration.ofMinutes(1))))
        assertFalse(resumed.fineToStop(shownAgain.plus(Duration.ofMinutes(1))))
        assertTrue(resumed.fineToStop(shownAgain.plus(Duration.ofMinutes(2))))
    }

    @Test
    fun `showing the test twice does not restart the clock`() {
        val timer = TestTimer().shown(start).shown(start.plusSeconds(120))

        assertEquals(Duration.ofSeconds(180), timer.elapsed(start.plusSeconds(180)))
    }

    @Test
    fun `a device clock moved backwards never makes the elapsed time negative`() {
        val timer = TestTimer().shown(start)

        assertEquals(Duration.ZERO, timer.elapsed(start.minusSeconds(3600)))
    }

    @Test
    fun `a timer that was never shown has no time on screen`() {
        assertEquals(Duration.ZERO, TestTimer().elapsed(start.plus(Duration.ofHours(2))))
    }
}
