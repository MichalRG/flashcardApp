package com.krzyzowski.vocabboost.ui.difficult

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DifficultWordsRoute(viewModel: DifficultWordsViewModel = viewModel()) {
    val deck by viewModel.deck.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val direction by viewModel.direction.collectAsStateWithLifecycle()
    val currentDeck = deck ?: return
    val currentDirection = direction ?: return
    DifficultWordsScreen(
        deck = currentDeck,
        position = position,
        direction = currentDirection,
        onMove = viewModel::move,
        onDirectionChange = viewModel::setDirection,
    )
}
