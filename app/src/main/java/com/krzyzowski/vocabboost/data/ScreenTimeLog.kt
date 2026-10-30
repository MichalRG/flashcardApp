package com.krzyzowski.vocabboost.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.krzyzowski.vocabboost.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class ScreenTimeLog(private val dataStore: DataStore<Preferences>, private val clock: AppClock) {
    val dailySeconds: Flow<DailySeconds> = dataStore.data.map { it.dailySeconds() }

    val streak: Flow<Streak> = dataStore.data.map { it.streak() }

    suspend fun record(from: Instant, to: Instant, goalMinutes: Int) {
        dataStore.edit { prefs ->
            val zone = clock.zone()
            val seconds = prefs.dailySeconds().add(from, to, zone)
            val streak = listOf(from, to)
                .map { it.atZone(zone).toLocalDate() }
                .distinct()
                .filter { seconds.secondsOn(it) >= goalMinutes * SECONDS_PER_MINUTE }
                .fold(prefs.streak(), Streak::goalMetOn)
            prefs[DAILY_SECONDS] = seconds.encode()
            prefs[STREAK] = streak.encode()
        }
    }

    private fun Preferences.dailySeconds(): DailySeconds =
        this[DAILY_SECONDS]?.let(DailySeconds::decode) ?: DailySeconds.NONE

    private fun Preferences.streak(): Streak = this[STREAK]?.let(Streak::decode) ?: Streak.NONE

    private companion object {
        const val SECONDS_PER_MINUTE = 60L
        val DAILY_SECONDS = stringPreferencesKey("daily_seconds_v1")
        val STREAK = stringPreferencesKey("streak_v1")
    }
}
