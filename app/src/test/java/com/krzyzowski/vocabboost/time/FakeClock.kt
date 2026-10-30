package com.krzyzowski.vocabboost.time

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class FakeClock(var instant: Instant, var zoneId: ZoneId = ZoneOffset.UTC) : AppClock {
    override fun now(): Instant = instant

    override fun zone(): ZoneId = zoneId
}
