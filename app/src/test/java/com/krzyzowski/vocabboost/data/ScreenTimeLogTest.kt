package com.krzyzowski.vocabboost.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.krzyzowski.vocabboost.time.FakeClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ScreenTimeLogTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())

    private val warsaw = ZoneId.of("Europe/Warsaw")
    private val nov2 = LocalDate.parse("2026-11-02")
    private val nov3 = LocalDate.parse("2026-11-03")
    private val clock = FakeClock(Instant.EPOCH, warsaw)

    private fun openStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "vocabboost.preferences_pb") }

    private val log by lazy { ScreenTimeLog(openStore(dataStoreScope), clock) }

    private fun logOn(scope: CoroutineScope) = ScreenTimeLog(openStore(scope), clock)

    @After
    fun closeDataStore() {
        dataStoreScope.cancel()
    }

    @Test
    fun `time on screen adds up through the day and is still there after the app restarts`() = runTest {
        val firstRun = Job()
        val before = logOn(CoroutineScope(Dispatchers.IO + firstRun))
        before.record(Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2026-11-02T10:01:30Z"), goalMinutes = 5)
        before.record(Instant.parse("2026-11-02T15:00:00Z"), Instant.parse("2026-11-02T15:00:45Z"), goalMinutes = 5)
        firstRun.cancelAndJoin()

        assertEquals(135, log.dailySeconds.first().secondsOn(nov2))
    }

    @Test
    fun `reaching the daily goal starts the streak and the streak is still there after the app restarts`() = runTest {
        val firstRun = Job()
        val before = logOn(CoroutineScope(Dispatchers.IO + firstRun))
        before.record(Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2026-11-02T10:00:59Z"), goalMinutes = 1)
        assertEquals(Streak.NONE, before.streak.first())
        before.record(Instant.parse("2026-11-02T10:00:59Z"), Instant.parse("2026-11-02T10:01:00Z"), goalMinutes = 1)
        firstRun.cancelAndJoin()

        assertEquals(Streak(1, nov2), log.streak.first())
    }

    @Test
    fun `goal days on the next days build the streak and a missed day ends it`() = runTest {
        log.record(Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2026-11-02T10:01:00Z"), goalMinutes = 1)
        log.record(Instant.parse("2026-11-03T10:00:00Z"), Instant.parse("2026-11-03T10:01:00Z"), goalMinutes = 1)

        val built = log.streak.first()
        log.record(Instant.parse("2026-11-05T10:00:00Z"), Instant.parse("2026-11-05T10:00:30Z"), goalMinutes = 1)

        assertEquals(Streak(2, nov3), built)
        assertEquals(0, log.streak.first().shownOn(LocalDate.parse("2026-11-05")))
    }

    @Test
    fun `a session over local midnight can meet the goal on both days`() = runTest {
        // 23:59 to 00:01 in Warsaw: one minute on each side of midnight
        log.record(Instant.parse("2026-11-02T22:59:00Z"), Instant.parse("2026-11-02T23:01:00Z"), goalMinutes = 1)

        assertEquals(Streak(2, nov3), log.streak.first())
        assertEquals(60, log.dailySeconds.first().secondsOn(nov2))
        assertEquals(60, log.dailySeconds.first().secondsOn(nov3))
    }

    @Test
    fun `lowering the goal later does not count the earlier days that now would have met it`() = runTest {
        log.record(Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2026-11-02T10:03:00Z"), goalMinutes = 5)

        log.record(Instant.parse("2026-11-03T10:00:00Z"), Instant.parse("2026-11-03T10:01:00Z"), goalMinutes = 1)

        assertEquals(Streak(1, nov3), log.streak.first())
    }
}
