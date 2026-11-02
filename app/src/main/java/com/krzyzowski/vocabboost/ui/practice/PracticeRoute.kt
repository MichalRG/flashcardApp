package com.krzyzowski.vocabboost.ui.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PracticeRoute(viewModel: PracticeViewModel = viewModel()) {
    val deck by viewModel.deck.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val direction by viewModel.direction.collectAsStateWithLifecycle()
    val currentDeck = deck?.takeIf { it.size > 0 } ?: return
    val currentDirection = direction ?: return
    PracticeScreen(
        deck = currentDeck,
        position = position,
        direction = currentDirection,
        onMove = viewModel::move,
        onDirectionChange = viewModel::setDirection,
    )
}
