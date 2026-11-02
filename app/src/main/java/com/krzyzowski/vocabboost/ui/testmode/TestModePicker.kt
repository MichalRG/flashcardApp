package com.krzyzowski.vocabboost.ui.testmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.session.TestMode

@Composable
fun TestModePicker(mode: TestMode, onModeChange: (TestMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.test_direction), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TestMode.entries.forEach { option ->
                val label = when (option) {
                    TestMode.MIXED -> R.string.test_mode_mixed
                    TestMode.POLISH_FIRST -> R.string.test_mode_polish_first
                    TestMode.ENGLISH_FIRST -> R.string.test_mode_english_first
                }
                FilterChip(
                    selected = mode == option,
                    onClick = { onModeChange(option) },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.testTag("test_mode_${option.name.lowercase()}"),
                )
            }
        }
        if (mode == TestMode.MIXED) {
            Text(stringResource(R.string.test_mode_mixed_explanation), style = MaterialTheme.typography.bodySmall)
        }
    }
}
