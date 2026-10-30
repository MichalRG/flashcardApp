package com.krzyzowski.vocabboost.session

import java.time.Duration
import java.time.Instant

data class TestTimer(val elapsedBefore: Duration = Duration.ZERO, val shownSince: Instant? = null) {

    fun shown(now: Instant): TestTimer = if (shownSince == null) copy(shownSince = now) else this

    fun hidden(now: Instant): TestTimer = TestTimer(elapsed(now), shownSince = null)

    fun elapsed(now: Instant): Duration =
        elapsedBefore + (shownSince?.let { Duration.between(it, now).coerceAtLeast(Duration.ZERO) } ?: Duration.ZERO)

    fun fineToStop(now: Instant): Boolean = elapsed(now) >= FINE_TO_STOP_AFTER

    fun timeUp(now: Instant): Boolean = elapsed(now) >= TIME_UP_AFTER

    companion object {
        val FINE_TO_STOP_AFTER: Duration = Duration.ofMinutes(5)
        val TIME_UP_AFTER: Duration = Duration.ofMinutes(10)
    }
}
