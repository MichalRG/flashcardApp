package com.krzyzowski.vocabboost.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    fun `a fresh install has 5 new words a day, a 10-minute goal and no notification question asked`() = runTest {
        assertEquals(5, settings.newWordsPerDay.first())
        assertEquals(10, settings.dailyGoalMinutes.first())
        assertFalse(settings.notificationsAsked.first())
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
    fun `settings cap new words at five and still offer a five-minute goal`() {
        assertEquals(listOf(0, 3, 5), SettingsStore.NEW_WORDS_PER_DAY_OPTIONS)
        assertEquals(listOf(1, 3, 5, 10, 15, 20), SettingsStore.DAILY_GOAL_MINUTES_OPTIONS)
        assertTrue(SettingsStore.DEFAULT_NEW_WORDS_PER_DAY in SettingsStore.NEW_WORDS_PER_DAY_OPTIONS)
        assertTrue(SettingsStore.DEFAULT_DAILY_GOAL_MINUTES in SettingsStore.DAILY_GOAL_MINUTES_OPTIONS)
    }

    @Test
    fun `a larger limit saved by an older version is capped at five after updating`() = runTest {
        val oldRun = Job()
        val oldStore = openStore(CoroutineScope(Dispatchers.IO + oldRun))
        oldStore.edit {
            it[intPreferencesKey("new_words_per_day")] = 20
            it[intPreferencesKey("daily_goal_minutes")] = 5
        }
        oldRun.cancelAndJoin()

        assertEquals(5, settings.newWordsPerDay.first())
        assertEquals(5, settings.dailyGoalMinutes.first())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a new setting cannot introduce more than five new words per day`() = runTest {
        settings.setNewWordsPerDay(6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a new setting cannot introduce a negative number of words`() = runTest {
        settings.setNewWordsPerDay(-1)
    }
}
