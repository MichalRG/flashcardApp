package com.krzyzowski.vocabboost.ui.difficult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.schedule.WordDifficulty
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.PracticeDeck
import com.krzyzowski.vocabboost.ui.practice.PracticeCards
import com.krzyzowski.vocabboost.ui.practice.PracticeDirectionToggle

@Composable
fun DifficultWordsScreen(
    deck: PracticeDeck,
    position: Int,
    direction: CardDirection,
    onMove: (Int) -> Unit,
    onDirectionChange: (CardDirection) -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.difficult_words_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(
                    R.string.difficult_words_rule,
                    WordDifficulty.MISSES_TO_ENTER,
                    WordDifficulty.SUCCESSES_TO_LEAVE,
                ),
            )
            Text(stringResource(R.string.difficult_words_practice_only))
            PracticeDirectionToggle(direction, onDirectionChange)
            if (deck.size == 0) {
                Text(
                    stringResource(R.string.difficult_words_empty),
                    modifier = Modifier.testTag("difficult_words_empty"),
                )
            } else {
                PracticeCards(deck, position, direction, onMove)
            }
        }
    }
}
