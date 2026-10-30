package com.krzyzowski.vocabboost.time

import com.krzyzowski.vocabboost.data.ScreenTimeLog
import com.krzyzowski.vocabboost.data.SettingsStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant

class ScreenTimeTracker(
    private val clock: AppClock,
    private val log: ScreenTimeLog,
    private val settings: SettingsStore,
) {
    suspend fun track() {
        var from = clock.now()
        try {
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                from = flush(from)
            }
        } finally {
            withContext(NonCancellable) { flush(from) }
        }
    }

    private suspend fun flush(from: Instant): Instant {
        val to = clock.now()
        log.record(from, to, settings.dailyGoalMinutes.first())
        return to
    }

    private companion object {
        const val FLUSH_INTERVAL_MS = 15_000L
    }
}
