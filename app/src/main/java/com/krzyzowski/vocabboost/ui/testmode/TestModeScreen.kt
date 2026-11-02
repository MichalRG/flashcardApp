package com.krzyzowski.vocabboost.ui.testmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.session.TestSession
import com.krzyzowski.vocabboost.ui.FlipCard
import java.time.Duration

@Composable
fun TestModeScreen(
    session: TestSession,
    elapsed: Duration,
    fineToStop: Boolean,
    timeUp: Boolean,
    onAnswer: (Boolean) -> Unit,
    onEnd: () -> Unit,
) {
    val card = session.current ?: return
    var flipped by rememberSaveable(session.position) { mutableStateOf(false) }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.test_position, session.position + 1, session.cards.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.test_timer, elapsed.toMinutes(), elapsed.seconds % 60),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("test_timer"),
                )
            }
            when {
                timeUp -> Text(stringResource(R.string.test_time_up), color = MaterialTheme.colorScheme.error)
                fineToStop -> Text(
                    stringResource(R.string.test_fine_to_stop),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            FlipCard(
                word = card.word,
                front = card.direction.front(card.word),
                isNew = card.isNew,
                flipped = flipped,
                onFlip = { flipped = !flipped },
            )
            if (flipped) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onAnswer(false) },
                        modifier = Modifier.weight(1f).testTag("did_not_know"),
                    ) {
                        Text(stringResource(R.string.test_did_not_know))
                    }
                    Button(
                        onClick = { onAnswer(true) },
                        modifier = Modifier.weight(1f).testTag("knew_it"),
                    ) {
                        Text(stringResource(R.string.test_knew_it))
                    }
                }
            }
            TextButton(onClick = onEnd, modifier = Modifier.testTag("end_test")) {
                Text(stringResource(R.string.test_end))
            }
        }
    }
}
