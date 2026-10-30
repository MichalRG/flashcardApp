package com.krzyzowski.vocabboost.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.krzyzowski.vocabboost.schedule.Workload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsStore(private val dataStore: DataStore<Preferences>) {
    val newWordsPerDay: Flow<Int> = dataStore.data.map {
        (it[NEW_WORDS_PER_DAY] ?: DEFAULT_NEW_WORDS_PER_DAY).coerceIn(0, Workload.MAX_NEW_WORDS_PER_DAY)
    }

    val dailyGoalMinutes: Flow<Int> = dataStore.data.map { it[DAILY_GOAL_MINUTES] ?: DEFAULT_DAILY_GOAL_MINUTES }

    val notificationsAsked: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_ASKED] ?: false }

    suspend fun setNewWordsPerDay(count: Int) {
        require(count in 0..Workload.MAX_NEW_WORDS_PER_DAY) { "New words per day must be between 0 and 5" }
        dataStore.edit { it[NEW_WORDS_PER_DAY] = count }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        dataStore.edit { it[DAILY_GOAL_MINUTES] = minutes }
    }

    suspend fun markNotificationsAsked() {
        dataStore.edit { it[NOTIFICATIONS_ASKED] = true }
    }

    companion object {
        const val DEFAULT_NEW_WORDS_PER_DAY = Workload.MAX_NEW_WORDS_PER_DAY
        const val DEFAULT_DAILY_GOAL_MINUTES = 10
        val NEW_WORDS_PER_DAY_OPTIONS = listOf(0, 3, 5)
        val DAILY_GOAL_MINUTES_OPTIONS = listOf(1, 3, 5, 10, 15, 20)
        private val NEW_WORDS_PER_DAY = intPreferencesKey("new_words_per_day")
        private val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        private val NOTIFICATIONS_ASKED = booleanPreferencesKey("notifications_asked")
    }
}
