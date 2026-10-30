package com.krzyzowski.vocabboost.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressStore(private val dataStore: DataStore<Preferences>, private val clock: AppClock) {
    val deck: Flow<DeckProgress> = dataStore.data.map { it.deck() }

    suspend fun recordAnswer(id: String, knew: Boolean) {
        dataStore.edit { it[WORD_PROGRESS] = it.deck().recordAnswer(id, knew, clock.today()).encode() }
    }

    private fun Preferences.deck(): DeckProgress =
        this[WORD_PROGRESS]?.let(DeckProgress::decode) ?: DeckProgress.NONE

    private companion object {
        val WORD_PROGRESS = stringPreferencesKey("word_progress_v1")
    }
}
