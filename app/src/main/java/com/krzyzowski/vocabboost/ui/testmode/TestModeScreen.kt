package com.krzyzowski.vocabboost.ui.testmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.TestCard
import com.krzyzowski.vocabboost.session.TestSession
import com.krzyzowski.vocabboost.session.TypedRecallGate
import com.krzyzowski.vocabboost.ui.FlipCard
import java.time.Duration

@Composable
fun TestModeScreen(
    session: TestSession,
    elapsed: Duration,
    fineToStop: Boolean,
    timeUp: Boolean,
    typedRecallRequired: Boolean,
    onAnswer: (Boolean) -> Unit,
    onEnd: () -> Unit,
) {
    val card = session.current ?: return
    var flipped by rememberSaveable(session.position) { mutableStateOf(false) }
    var typed by rememberSaveable(session.position) { mutableStateOf("") }
    var gate by rememberSaveable(session.position, stateSaver = TypedRecallGateSaver) {
        mutableStateOf(TypedRecallGate())
    }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // innerPadding already includes the navigation bar; consume it so imePadding adds only the rest.
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.test_position, session.position + 1, session.position + session.remaining),
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
            Text(
                stringResource(
                    if (card.direction == CardDirection.POLISH_FIRST) {
                        R.string.test_recall_english
                    } else {
                        R.string.test_recall_polish
                    },
                ),
                modifier = Modifier.testTag("test_recall_direction"),
            )
            if (card.isLearning) {
                Text(stringResource(R.string.test_initial_learning), modifier = Modifier.testTag("initial_learning"))
            }
            FlipCard(
                word = card.word,
                front = card.direction.front(card.word),
                isNew = card.isNew,
                flipped = flipped,
                onFlip = { flipped = !flipped },
            )
            if (flipped) {
                if (typedRecallRequired) {
                    TypedRecall(
                        card = card,
                        typed = typed,
                        onTypedChange = { typed = it },
                        gate = gate,
                        onSubmit = { gate = gate.submit(typed, card.direction.expected(card.word), card.direction) },
                        onAnswer = onAnswer,
                    )
                } else {
                    SelfGradeButtons(onAnswer)
                }
            }
            TextButton(onClick = onEnd, modifier = Modifier.testTag("end_test")) {
                Text(stringResource(R.string.test_end))
            }
        }
    }
}

private val TypedRecallGateSaver = listSaver<TypedRecallGate, String>(
    save = { listOf(it.status.name, it.submitted) },
    restore = { TypedRecallGate(TypedRecallGate.Status.valueOf(it[0]), it[1]) },
)

@Composable
private fun SelfGradeButtons(onAnswer: (Boolean) -> Unit) {
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

@Composable
private fun TypedRecall(
    card: TestCard,
    typed: String,
    onTypedChange: (String) -> Unit,
    gate: TypedRecallGate,
    onSubmit: () -> Unit,
    onAnswer: (Boolean) -> Unit,
) {
    val unsubmitted = gate.status == TypedRecallGate.Status.UNSUBMITTED
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // The field is disabled after submitting, so hide the keyboard to keep the grading buttons visible.
    val submit = {
        if (gate.canSubmit(typed)) {
            keyboard?.hide()
            focusManager.clearFocus()
            onSubmit()
        }
    }
    Text(stringResource(R.string.test_typed_prompt), modifier = Modifier.testTag("typed_answer_prompt"))
    OutlinedTextField(
        value = if (unsubmitted) typed else gate.submitted,
        onValueChange = { if (unsubmitted) onTypedChange(it) },
        enabled = unsubmitted,
        singleLine = true,
        label = { Text(stringResource(R.string.test_typed_hint)) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        modifier = Modifier.fillMaxWidth().testTag("typed_answer_input"),
    )
    when (gate.status) {
        TypedRecallGate.Status.UNSUBMITTED -> Button(
            onClick = submit,
            enabled = gate.canSubmit(typed),
            modifier = Modifier.fillMaxWidth().testTag("typed_answer_submit"),
        ) {
            Text(stringResource(R.string.test_typed_submit))
        }
        TypedRecallGate.Status.CORRECT -> {
            Text(
                stringResource(R.string.test_typed_correct),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("typed_answer_correct"),
            )
            SelfGradeButtons(onAnswer)
        }
        TypedRecallGate.Status.WRONG -> {
            Text(
                stringResource(R.string.test_typed_wrong, gate.submitted, card.direction.expected(card.word)),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("typed_answer_wrong"),
            )
            Button(
                onClick = { onAnswer(false) },
                modifier = Modifier.fillMaxWidth().testTag("typed_answer_continue"),
            ) {
                Text(stringResource(R.string.test_typed_continue))
            }
        }
    }
}
