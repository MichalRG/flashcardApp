package com.krzyzowski.vocabboost.time

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.krzyzowski.vocabboost.data.ScreenTimeLog
import com.krzyzowski.vocabboost.data.SettingsStore
import com.krzyzowski.vocabboost.data.Streak
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenTimeTrackerTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val start = Instant.parse("2026-11-02T10:00:00Z")
    private val day = LocalDate.parse("2026-11-02")

    private fun TestScope.virtualClock() = object : AppClock {
        override fun now(): Instant = start.plusMillis(currentTime)

        override fun zone(): ZoneId = ZoneOffset.UTC
    }

    // The store runs on the test's virtual time so a flush and its delay stay in step.
    private fun TestScope.openStore(storeJob: Job): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = CoroutineScope(backgroundScope.coroutineContext + storeJob)) {
            File(folder.root, "vocabboost.preferences_pb")
        }

    @Test
    fun `time on screen is saved every 15 seconds and the rest when the app leaves the screen`() = runTest {
        val clock = virtualClock()
        val dataStore = openStore(Job(backgroundScope.coroutineContext[Job]))
        val log = ScreenTimeLog(dataStore, clock)
        val tracker = ScreenTimeTracker(clock, log, SettingsStore(dataStore))

        val onScreen = launch { tracker.track() }
        advanceTimeBy(20_000)
        runCurrent()
        val whileOnScreen = log.dailySeconds.first().secondsOn(day)
        onScreen.cancelAndJoin()
        val afterLeaving = log.dailySeconds.first().secondsOn(day)
        delay(10 * 60 * 1000L)

        assertEquals(15, whileOnScreen)
        assertEquals(20, afterLeaving)
        assertEquals("nothing is counted while the app is off screen", 20, log.dailySeconds.first().secondsOn(day))
    }

    @Test
    fun `meeting the goal on screen builds the streak and time and streak survive a restart`() = runTest {
        val clock = virtualClock()
        val firstRun = Job(backgroundScope.coroutineContext[Job])
        val firstStore = openStore(firstRun)
        SettingsStore(firstStore).setDailyGoalMinutes(1)
        val tracker = ScreenTimeTracker(clock, ScreenTimeLog(firstStore, clock), SettingsStore(firstStore))

        val onScreen = launch { tracker.track() }
        advanceTimeBy(75_000)
        runCurrent()
        onScreen.cancelAndJoin()
        firstRun.cancelAndJoin()

        val restarted = ScreenTimeLog(openStore(Job(backgroundScope.coroutineContext[Job])), clock)
        assertEquals(75, restarted.dailySeconds.first().secondsOn(day))
        assertEquals(Streak(1, day), restarted.streak.first())
    }

    @Test
    fun `the default daily goal is met at ten minutes not five`() = runTest {
        val clock = virtualClock()
        val dataStore = openStore(Job(backgroundScope.coroutineContext[Job]))
        val log = ScreenTimeLog(dataStore, clock)
        val tracker = ScreenTimeTracker(clock, log, SettingsStore(dataStore))

        val onScreen = launch { tracker.track() }
        advanceTimeBy(5 * 60 * 1000L)
        runCurrent()
        assertEquals(300, log.dailySeconds.first().secondsOn(day))
        assertEquals(Streak.NONE, log.streak.first())

        advanceTimeBy(5 * 60 * 1000L)
        runCurrent()
        onScreen.cancelAndJoin()
        assertEquals(600, log.dailySeconds.first().secondsOn(day))
        assertEquals(Streak(1, day), log.streak.first())
    }

    @Test
    fun `a goal that is not met on screen leaves the streak at zero`() = runTest {
        val clock = virtualClock()
        val dataStore = openStore(Job(backgroundScope.coroutineContext[Job]))
        val log = ScreenTimeLog(dataStore, clock)
        val tracker = ScreenTimeTracker(clock, log, SettingsStore(dataStore))

        val onScreen = launch { tracker.track() }
        advanceTimeBy(4 * 60 * 1000L)
        runCurrent()
        onScreen.cancelAndJoin()

        assertEquals(Streak.NONE, log.streak.first())
        assertEquals(240, log.dailySeconds.first().secondsOn(day))
    }
}
