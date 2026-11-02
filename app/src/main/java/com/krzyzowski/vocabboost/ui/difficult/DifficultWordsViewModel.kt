package com.krzyzowski.vocabboost.ui.difficult

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krzyzowski.vocabboost.VocabBoostApp
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.PracticeDeck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DifficultWordsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as VocabBoostApp).container
    private val _position = MutableStateFlow(0)

    val deck: StateFlow<PracticeDeck?> =
        combine(
            container.progressStore.deck, container.progressStore.difficulty, container.settings.practiceDirection,
        ) { progress, difficulty, direction ->
            val current = progress.forDirection(direction)
            PracticeDeck.difficult(
                container.words, current.schedule, difficulty.forDirection(direction), direction, current.learning,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val position: StateFlow<Int> = _position.asStateFlow()

    fun move(step: Int) {
        val current = deck.value ?: return
        _position.update { current.boundedPosition(current.boundedPosition(it) + step) }
    }

    fun setDirection(direction: CardDirection) {
        viewModelScope.launch { container.settings.setPracticeDirection(direction) }
    }
}
