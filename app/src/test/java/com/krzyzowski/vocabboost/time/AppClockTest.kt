package com.krzyzowski.vocabboost.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class AppClockTest {
    @Test
    fun `today is the date in the clock's own zone, not the UTC date`() {
        val lateEveningInUtc = Instant.parse("2026-11-02T23:30:00Z")

        val inWarsaw = FakeClock(lateEveningInUtc, ZoneId.of("Europe/Warsaw")).today()
        val inUtc = FakeClock(lateEveningInUtc, ZoneOffset.UTC).today()

        assertEquals(LocalDate.parse("2026-11-03"), inWarsaw)
        assertEquals(LocalDate.parse("2026-11-02"), inUtc)
    }
}
