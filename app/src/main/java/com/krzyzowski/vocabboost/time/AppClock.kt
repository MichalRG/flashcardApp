package com.krzyzowski.vocabboost.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface AppClock {
    fun now(): Instant

    fun zone(): ZoneId

    fun today(): LocalDate = now().atZone(zone()).toLocalDate()
}
