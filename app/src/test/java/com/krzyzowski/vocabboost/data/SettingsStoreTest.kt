package com.krzyzowski.vocabboost.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.TestMode
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

class SettingsStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())

    private fun openStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "vocabboost.preferences_pb") }

    private val settings by lazy { SettingsStore(openStore(dataStoreScope)) }

    @After
    fun closeDataStore() {
        dataStoreScope.cancel()
    }

    @Test
    fun `a fresh install has 8 new words a day, mixed tests, a 10-minute goal and no notification question asked`() = runTest {
        assertEquals(8, settings.newWordsPerDay.first())
        assertEquals(10, settings.dailyGoalMinutes.first())
        assertFalse(settings.notificationsAsked.first())
        assertEquals(CardDirection.ENGLISH_FIRST, settings.practiceDirection.first())
        assertEquals(TestMode.MIXED, settings.testMode.first())
    }

    @Test
    fun `changed settings are remembered, each one on its own`() = runTest {
        settings.setNewWordsPerDay(3)
        settings.setDailyGoalMinutes(5)
        settings.markNotificationsAsked()

        assertEquals(3, settings.newWordsPerDay.first())
        assertEquals(5, settings.dailyGoalMinutes.first())
        assertTrue(settings.notificationsAsked.first())
    }

    @Test
    fun `settings are still there after the app restarts`() = runTest {
        val firstRun = Job()
        val before = SettingsStore(openStore(CoroutineScope(Dispatchers.IO + firstRun)))
        before.setNewWordsPerDay(3)
        before.setDailyGoalMinutes(3)
        before.markNotificationsAsked()
        firstRun.cancelAndJoin()

        assertEquals(3, settings.newWordsPerDay.first())
        assertEquals(3, settings.dailyGoalMinutes.first())
        assertTrue(settings.notificationsAsked.first())
    }

    @Test
    fun `settings cap new words at eight and still offer gentler new-word and time goals`() {
        assertEquals(listOf(0, 3, 5, 8), SettingsStore.NEW_WORDS_PER_DAY_OPTIONS)
        assertEquals(listOf(1, 3, 5, 10, 15, 20), SettingsStore.DAILY_GOAL_MINUTES_OPTIONS)
        assertTrue(SettingsStore.DEFAULT_NEW_WORDS_PER_DAY in SettingsStore.NEW_WORDS_PER_DAY_OPTIONS)
        assertTrue(SettingsStore.DEFAULT_DAILY_GOAL_MINUTES in SettingsStore.DAILY_GOAL_MINUTES_OPTIONS)
    }

    @Test
    fun `a larger limit saved by an older version is capped at eight after updating`() = runTest {
        val oldRun = Job()
        val oldStore = openStore(CoroutineScope(Dispatchers.IO + oldRun))
        oldStore.edit {
            it[intPreferencesKey("new_words_per_day")] = 20
            it[intPreferencesKey("daily_goal_minutes")] = 5
        }
        oldRun.cancelAndJoin()

        assertEquals(8, settings.newWordsPerDay.first())
        assertEquals(5, settings.dailyGoalMinutes.first())
        assertEquals(CardDirection.ENGLISH_FIRST, settings.practiceDirection.first())
        assertEquals(TestMode.MIXED, settings.testMode.first())
    }

    @Test
    fun `an existing choice of five new words survives an update and can be raised to eight`() = runTest {
        settings.setNewWordsPerDay(5)
        assertEquals(5, settings.newWordsPerDay.first())

        settings.setNewWordsPerDay(8)

        assertEquals(8, settings.newWordsPerDay.first())
    }

    @Test
    fun `the selected test mode survives an app restart independently of Practice direction`() = runTest {
        val firstRun = Job()
        val before = SettingsStore(openStore(CoroutineScope(Dispatchers.IO + firstRun)))
        before.setTestMode(TestMode.ENGLISH_FIRST)
        before.setPracticeDirection(CardDirection.POLISH_FIRST)
        firstRun.cancelAndJoin()

        assertEquals(TestMode.ENGLISH_FIRST, settings.testMode.first())
        assertEquals(CardDirection.POLISH_FIRST, settings.practiceDirection.first())
    }

    @Test
    fun `test modes can change without altering Practice language or learning progress`() = runTest {
        val dataStore = openStore(dataStoreScope)
        val testSettings = SettingsStore(dataStore)
        val progress = ProgressStore(dataStore, FakeClock(Instant.parse("2026-10-30T10:00:00Z")))
        progress.recordAnswer("dump", knew = true)
        testSettings.setPracticeDirection(CardDirection.POLISH_FIRST)
        val savedProgress = progress.deck.first().encode()

        TestMode.entries.forEach { mode ->
            testSettings.setTestMode(mode)
            assertEquals(mode, testSettings.testMode.first())
        }

        assertEquals(savedProgress, progress.deck.first().encode())
        assertEquals(CardDirection.POLISH_FIRST, testSettings.practiceDirection.first())
        assertEquals(8, testSettings.newWordsPerDay.first())
    }

    @Test
    fun `Polish first in Practice is still selected after an app restart`() = runTest {
        val firstRun = Job()
        val before = SettingsStore(openStore(CoroutineScope(Dispatchers.IO + firstRun)))
        before.setPracticeDirection(CardDirection.POLISH_FIRST)
        firstRun.cancelAndJoin()

        assertEquals(CardDirection.POLISH_FIRST, settings.practiceDirection.first())
    }

    @Test
    fun `Practice can switch back to English first`() = runTest {
        settings.setPracticeDirection(CardDirection.POLISH_FIRST)
        assertEquals(CardDirection.POLISH_FIRST, settings.practiceDirection.first())

        settings.setPracticeDirection(CardDirection.ENGLISH_FIRST)

        assertEquals(CardDirection.ENGLISH_FIRST, settings.practiceDirection.first())
    }

    @Test
    fun `switching Practice language leaves learning progress and other settings unchanged`() = runTest {
        val dataStore = openStore(dataStoreScope)
        val practiceSettings = SettingsStore(dataStore)
        val progress = ProgressStore(dataStore, FakeClock(Instant.parse("2026-10-30T10:00:00Z")))
        progress.recordAnswer("dump", knew = true)
        practiceSettings.setNewWordsPerDay(3)
        practiceSettings.setDailyGoalMinutes(5)
        practiceSettings.markNotificationsAsked()
        val savedProgress = progress.deck.first().encode()

        practiceSettings.setPracticeDirection(CardDirection.POLISH_FIRST)
        practiceSettings.setPracticeDirection(CardDirection.ENGLISH_FIRST)

        assertEquals(savedProgress, progress.deck.first().encode())
        assertEquals(3, practiceSettings.newWordsPerDay.first())
        assertEquals(5, practiceSettings.dailyGoalMinutes.first())
        assertTrue(practiceSettings.notificationsAsked.first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a new setting cannot introduce more than eight new words per day`() = runTest {
        settings.setNewWordsPerDay(9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a new setting cannot introduce a negative number of words`() = runTest {
        settings.setNewWordsPerDay(-1)
    }
}
