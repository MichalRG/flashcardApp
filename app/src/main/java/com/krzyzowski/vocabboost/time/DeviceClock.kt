package com.krzyzowski.vocabboost.time

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class DeviceClock(private val offset: () -> Duration) : AppClock {
    override fun now(): Instant = Instant.now() + offset()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
