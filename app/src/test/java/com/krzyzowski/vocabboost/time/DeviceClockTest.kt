package com.krzyzowski.vocabboost.time

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class DeviceClockTest {
    @Test
    fun `the clock runs ahead of the real time by the offset it is given`() {
        val before = Instant.now()

        val shifted = DeviceClock { Duration.ofDays(1) }.now()

        val after = Instant.now()
        assertTrue(shifted >= before.plus(Duration.ofDays(1)) && shifted <= after.plus(Duration.ofDays(1)))
    }

    @Test
    fun `with no offset the clock shows the real time`() {
        val before = Instant.now()

        val now = DeviceClock { Duration.ZERO }.now()

        assertTrue(now >= before && now <= Instant.now())
    }
}
