package com.krzyzowski.vocabboost.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.DifficultyProgress
import com.krzyzowski.vocabboost.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressStore(private val dataStore: DataStore<Preferences>, private val clock: AppClock) {
    val deck: Flow<DeckProgress> = dataStore.data.map { it.deck() }

    val difficulty: Flow<DifficultyProgress> = dataStore.data.map { it.difficulty() }

    suspend fun recordAnswer(id: String, knew: Boolean) {
        dataStore.edit { prefs ->
            val deck = prefs.deck().recordAnswer(id, knew, clock.today())
            val difficulty = prefs.difficulty().recordAnswer(id, knew)
            prefs[WORD_PROGRESS] = deck.encode()
            prefs[DIFFICULTY_PROGRESS] = difficulty.encode()
        }
    }

    private fun Preferences.deck(): DeckProgress =
        this[WORD_PROGRESS]?.let(DeckProgress::decode) ?: DeckProgress.NONE

    private fun Preferences.difficulty(): DifficultyProgress =
        this[DIFFICULTY_PROGRESS]?.let(DifficultyProgress::decode) ?: DifficultyProgress.NONE

    private companion object {
        val WORD_PROGRESS = stringPreferencesKey("word_progress_v1")
        val DIFFICULTY_PROGRESS = stringPreferencesKey("difficulty_progress_v1")
    }
}
