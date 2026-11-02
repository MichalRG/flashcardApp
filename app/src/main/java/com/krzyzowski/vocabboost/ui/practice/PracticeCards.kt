package com.krzyzowski.vocabboost.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.PracticeDeck
import com.krzyzowski.vocabboost.ui.FlipCard
import com.krzyzowski.vocabboost.ui.StageLabel

@Composable
fun PracticeCards(
    deck: PracticeDeck,
    position: Int,
    direction: CardDirection,
    onMove: (Int) -> Unit,
    onDirectionChange: (CardDirection) -> Unit,
) {
    val currentPosition = deck.boundedPosition(position)
    val word = deck.wordAt(currentPosition)
    var flipped by rememberSaveable(word.id, direction) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PracticeDirectionToggle(direction, onDirectionChange)
        Text(
            stringResource(R.string.practice_position, currentPosition + 1, deck.size),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.practice_stage, stringResource(StageLabel.of(deck.stageAt(currentPosition)))),
            modifier = Modifier.testTag("practice_stage"),
        )
        FlipCard(
            word = word,
            front = direction.front(word),
            isNew = false,
            flipped = flipped,
            onFlip = { flipped = !flipped },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onMove(-1) },
                enabled = currentPosition > 0,
                modifier = Modifier.weight(1f).testTag("previous"),
            ) {
                Text(stringResource(R.string.practice_previous))
            }
            OutlinedButton(
                onClick = { onMove(1) },
                enabled = currentPosition < deck.size - 1,
                modifier = Modifier.weight(1f).testTag("next"),
            ) {
                Text(stringResource(R.string.practice_next))
            }
        }
    }
}
