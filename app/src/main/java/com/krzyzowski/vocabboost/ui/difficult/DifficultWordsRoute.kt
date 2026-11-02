package com.krzyzowski.vocabboost.ui.difficult

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DifficultWordsRoute(viewModel: DifficultWordsViewModel = viewModel()) {
    val deck by viewModel.deck.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val currentDeck = deck ?: return
    DifficultWordsScreen(
        deck = currentDeck,
        position = position,
        direction = currentDeck.direction,
        onMove = viewModel::move,
        onDirectionChange = viewModel::setDirection,
    )
}
