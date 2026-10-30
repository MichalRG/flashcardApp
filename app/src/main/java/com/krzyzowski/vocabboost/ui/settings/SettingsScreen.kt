package com.krzyzowski.vocabboost.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.data.SettingsStore
import com.krzyzowski.vocabboost.ui.ChoiceChips

@Composable
fun SettingsScreen(
    newWordsPerDay: Int,
    dailyGoalMinutes: Int,
    remindersAllowed: Boolean,
    onNewWordsPerDayChange: (Int) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onAllowReminders: () -> Unit,
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
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
            ChoiceChips(
                title = stringResource(R.string.settings_new_words),
                explanation = stringResource(R.string.settings_new_words_explanation),
                options = SettingsStore.NEW_WORDS_PER_DAY_OPTIONS,
                selected = newWordsPerDay,
                optionLabel = { it.toString() },
                onSelect = onNewWordsPerDayChange,
            )
            ChoiceChips(
                title = stringResource(R.string.settings_goal),
                explanation = stringResource(R.string.settings_goal_explanation),
                options = SettingsStore.DAILY_GOAL_MINUTES_OPTIONS,
                selected = dailyGoalMinutes,
                optionLabel = { stringResource(R.string.settings_goal_minutes, it) },
                onSelect = onDailyGoalChange,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_reminders), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            if (remindersAllowed) {
                                R.string.settings_reminders_on
                            } else {
                                R.string.settings_reminders_blocked
                            },
                        ),
                        modifier = Modifier.testTag("reminders_status"),
                    )
                    if (!remindersAllowed) {
                        Button(onClick = onAllowReminders, modifier = Modifier.testTag("allow_reminders")) {
                            Text(stringResource(R.string.settings_allow_reminders))
                        }
                    }
                }
            }
        }
    }
}
