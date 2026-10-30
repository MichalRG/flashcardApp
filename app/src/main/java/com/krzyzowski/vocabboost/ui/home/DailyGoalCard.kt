package com.krzyzowski.vocabboost.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R

@Composable
fun DailyGoalCard(secondsToday: Long, goalMinutes: Int, goalMetToday: Boolean, streak: Int) {
    val minutesToday = (secondsToday / 60).toInt()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.home_time_today, minutesToday),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag("time_today"),
            )
            LinearProgressIndicator(
                progress = { if (goalMetToday) 1f else (secondsToday / (goalMinutes * 60f)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.home_daily_goal, minutesToday, goalMinutes),
                modifier = Modifier.testTag("daily_goal"),
            )
            if (goalMetToday) {
                Text(
                    stringResource(R.string.home_goal_met),
                    modifier = Modifier.testTag("daily_goal_met"),
                )
            }
            Text(
                pluralStringResource(R.plurals.home_streak, streak, streak),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("streak"),
            )
        }
    }
}
