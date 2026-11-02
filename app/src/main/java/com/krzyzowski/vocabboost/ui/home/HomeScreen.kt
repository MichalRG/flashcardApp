package com.krzyzowski.vocabboost.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.schedule.Workload
import com.krzyzowski.vocabboost.session.TestMode
import com.krzyzowski.vocabboost.ui.Screen
import com.krzyzowski.vocabboost.ui.testmode.TestModePicker

@Composable
fun HomeScreen(
    secondsToday: Long,
    goalMinutes: Int,
    goalMetToday: Boolean,
    streak: Int,
    workload: Workload,
    testMode: TestMode,
    onTestModeChange: (TestMode) -> Unit,
    onOpen: (Screen) -> Unit,
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
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            DailyGoalCard(secondsToday, goalMinutes, goalMetToday, streak)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val waiting = workload.waiting
                    val newCount = workload.newWords.size
                    Text(
                        if (newCount > 0) {
                            pluralStringResource(R.plurals.home_waiting_with_new, waiting, waiting, newCount)
                        } else {
                            pluralStringResource(R.plurals.home_waiting, waiting, waiting)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("waiting"),
                    )
                    TestModePicker(testMode, onTestModeChange)
                    Button(
                        onClick = { onOpen(Screen.TEST) },
                        enabled = waiting > 0,
                        modifier = Modifier.fillMaxWidth().testTag("start_test"),
                    ) {
                        Text(stringResource(if (waiting > 0) R.string.home_start_test else R.string.home_all_done))
                    }
                }
            }
            listOf(
                Screen.PRACTICE to R.string.home_practice,
                Screen.PROGRESS to R.string.home_progress,
                Screen.SETTINGS to R.string.home_settings,
            ).forEach { (target, label) ->
                FilledTonalButton(onClick = { onOpen(target) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(label))
                }
            }
        }
    }
}
