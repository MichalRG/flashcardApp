package com.krzyzowski.vocabboost.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.krzyzowski.vocabboost.session.PracticeDeck
import com.krzyzowski.vocabboost.ui.FlipCard
import com.krzyzowski.vocabboost.ui.StageLabel

@Composable
fun PracticeScreen(deck: PracticeDeck, position: Int, onMove: (Int) -> Unit) {
    val word = deck.wordAt(position)
    var flipped by rememberSaveable(position) { mutableStateOf(false) }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.practice_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.practice_position, position + 1, deck.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.practice_stage, stringResource(StageLabel.of(deck.stageAt(position)))),
                modifier = Modifier.testTag("practice_stage"),
            )
            FlipCard(
                word = word,
                front = word.english,
                isNew = false,
                flipped = flipped,
                onFlip = { flipped = !flipped },
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onMove(-1) },
                    enabled = position > 0,
                    modifier = Modifier.weight(1f).testTag("previous"),
                ) {
                    Text(stringResource(R.string.practice_previous))
                }
                OutlinedButton(
                    onClick = { onMove(1) },
                    enabled = position < deck.size - 1,
                    modifier = Modifier.weight(1f).testTag("next"),
                ) {
                    Text(stringResource(R.string.practice_next))
                }
            }
        }
    }
}
