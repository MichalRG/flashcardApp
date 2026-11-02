package com.krzyzowski.vocabboost.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krzyzowski.vocabboost.VocabBoostApp
import com.krzyzowski.vocabboost.schedule.ProgressSummary
import com.krzyzowski.vocabboost.session.CardDirection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as VocabBoostApp).container

    val summary: StateFlow<Map<CardDirection, ProgressSummary>?> =
        combine(container.progressStore.deck, container.settings.newWordsPerDay) { deck, newWordsPerDay ->
            CardDirection.entries.associateWith {
                ProgressSummary.of(container.words, deck, container.clock.today(), newWordsPerDay, it)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
