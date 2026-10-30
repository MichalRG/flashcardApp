package com.krzyzowski.vocabboost.ui.practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krzyzowski.vocabboost.VocabBoostApp
import com.krzyzowski.vocabboost.session.PracticeDeck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class PracticeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as VocabBoostApp).container
    private val _position = MutableStateFlow(0)

    val deck: StateFlow<PracticeDeck?> = container.progressStore.deck
        .map { PracticeDeck(container.words, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val position: StateFlow<Int> = _position.asStateFlow()

    fun move(step: Int) {
        _position.update { (it + step).coerceAtMost(container.words.lastIndex).coerceAtLeast(0) }
    }
}
