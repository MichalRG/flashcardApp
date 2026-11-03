package com.krzyzowski.vocabboost.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.krzyzowski.vocabboost.schedule.Workload
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.PracticeMode
import com.krzyzowski.vocabboost.session.TestMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsStore(private val dataStore: DataStore<Preferences>) {
    val newWordsPerDay: Flow<Int> = dataStore.data.map {
        (it[NEW_WORDS_PER_DAY] ?: DEFAULT_NEW_WORDS_PER_DAY).coerceIn(0, Workload.MAX_NEW_WORDS_PER_DAY)
    }

    val dailyGoalMinutes: Flow<Int> = dataStore.data.map { it[DAILY_GOAL_MINUTES] ?: DEFAULT_DAILY_GOAL_MINUTES }

    val notificationsAsked: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_ASKED] ?: false }

    val practiceDirection: Flow<CardDirection> = dataStore.data.map {
        if (it[PRACTICE_POLISH_FIRST] == true) CardDirection.POLISH_FIRST else CardDirection.ENGLISH_FIRST
    }

    val practiceMode: Flow<PracticeMode> = dataStore.data.map {
        PracticeMode.valueOf(it[PRACTICE_MODE] ?: PracticeMode.FLASHCARDS.name)
    }

    val testMode: Flow<TestMode> = dataStore.data.map {
        TestMode.valueOf(it[TEST_MODE] ?: TestMode.MIXED.name)
    }

    suspend fun setNewWordsPerDay(count: Int) {
        require(count in 0..Workload.MAX_NEW_WORDS_PER_DAY) {
            "New words per day must be between 0 and ${Workload.MAX_NEW_WORDS_PER_DAY}"
        }
        dataStore.edit { it[NEW_WORDS_PER_DAY] = count }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        dataStore.edit { it[DAILY_GOAL_MINUTES] = minutes }
    }

    suspend fun markNotificationsAsked() {
        dataStore.edit { it[NOTIFICATIONS_ASKED] = true }
    }

    suspend fun setPracticeDirection(direction: CardDirection) {
        dataStore.edit { it[PRACTICE_POLISH_FIRST] = direction == CardDirection.POLISH_FIRST }
    }

    suspend fun setTestMode(mode: TestMode) {
        dataStore.edit { it[TEST_MODE] = mode.name }
    }

    suspend fun setPracticeMode(mode: PracticeMode) {
        dataStore.edit { it[PRACTICE_MODE] = mode.name }
    }

    companion object {
        const val DEFAULT_NEW_WORDS_PER_DAY = Workload.MAX_NEW_WORDS_PER_DAY
        const val DEFAULT_DAILY_GOAL_MINUTES = 10
        val NEW_WORDS_PER_DAY_OPTIONS = listOf(0, 3, 5, 8)
        val DAILY_GOAL_MINUTES_OPTIONS = listOf(1, 3, 5, 10, 15, 20)
        private val NEW_WORDS_PER_DAY = intPreferencesKey("new_words_per_day")
        private val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        private val NOTIFICATIONS_ASKED = booleanPreferencesKey("notifications_asked")
        private val PRACTICE_POLISH_FIRST = booleanPreferencesKey("practice_polish_first")
        private val PRACTICE_MODE = stringPreferencesKey("practice_mode")
        private val TEST_MODE = stringPreferencesKey("test_mode")
    }
}
