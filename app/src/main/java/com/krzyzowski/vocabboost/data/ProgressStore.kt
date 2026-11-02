package com.krzyzowski.vocabboost.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.DifficultyProgress
import com.krzyzowski.vocabboost.schedule.DirectionalDifficulty
import com.krzyzowski.vocabboost.schedule.DirectionalProgress
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressStore(private val dataStore: DataStore<Preferences>, private val clock: AppClock) {
    val deck: Flow<DirectionalProgress> = dataStore.data.map { it.deck() }

    val difficulty: Flow<DirectionalDifficulty> = dataStore.data.map { it.difficulty() }

    suspend fun recordAnswer(id: String, direction: CardDirection, knew: Boolean) {
        dataStore.edit { prefs ->
            val deck = prefs.deck().recordAnswer(id, direction, knew, clock.today())
            val difficulty = prefs.difficulty().recordAnswer(id, direction, knew)
            prefs[DIRECTIONAL_PROGRESS] = deck.encode()
            prefs[DIRECTIONAL_DIFFICULTY] = difficulty.encode()
        }
    }

    private fun Preferences.deck(): DirectionalProgress =
        this[DIRECTIONAL_PROGRESS]?.let(DirectionalProgress::decode) ?:
            DirectionalProgress.fromLegacy(this[WORD_PROGRESS]?.let(DeckProgress::decode) ?: DeckProgress.NONE)

    private fun Preferences.difficulty(): DirectionalDifficulty =
        this[DIRECTIONAL_DIFFICULTY]?.let(DirectionalDifficulty::decode) ?:
            DirectionalDifficulty.fromLegacy(this[DIFFICULTY_PROGRESS]?.let(DifficultyProgress::decode) ?: DifficultyProgress.NONE)

    private companion object {
        val WORD_PROGRESS = stringPreferencesKey("word_progress_v1")
        val DIFFICULTY_PROGRESS = stringPreferencesKey("difficulty_progress_v1")
        val DIRECTIONAL_PROGRESS = stringPreferencesKey("directional_word_progress_v2")
        val DIRECTIONAL_DIFFICULTY = stringPreferencesKey("directional_difficulty_v2")
    }
}
