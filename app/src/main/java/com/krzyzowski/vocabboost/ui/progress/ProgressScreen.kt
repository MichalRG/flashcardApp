package com.krzyzowski.vocabboost.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.schedule.ProgressSummary
import com.krzyzowski.vocabboost.ui.StageLabel

@Composable
fun ProgressScreen(summary: ProgressSummary) {
    val sections = listOf(
        R.string.progress_today to listOf(
            R.string.progress_due_today to summary.dueToday,
            R.string.progress_new_today to summary.newToday,
            R.string.progress_due_tomorrow to summary.dueTomorrow,
        ),
        R.string.progress_by_stage to listOf(StageLabel.of(null) to summary.notStarted) +
            summary.perStage.map { (stage, count) -> StageLabel.of(stage) to count },
    )
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.headlineMedium)
            sections.forEach { (title, rows) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                        rows.forEach { (label, count) ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(label), modifier = Modifier.weight(1f))
                                Text(count.toString(), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
