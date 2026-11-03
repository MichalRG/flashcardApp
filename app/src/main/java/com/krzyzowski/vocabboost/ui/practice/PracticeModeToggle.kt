package com.krzyzowski.vocabboost.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.session.PracticeMode

@Composable
fun PracticeModeToggle(mode: PracticeMode, onModeChange: (PracticeMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PracticeMode.entries.forEach { option ->
            val flashcards = option == PracticeMode.FLASHCARDS
            FilterChip(
                selected = mode == option,
                onClick = { onModeChange(option) },
                label = {
                    Text(stringResource(if (flashcards) R.string.practice_flashcards else R.string.practice_context))
                },
                modifier = Modifier.testTag(if (flashcards) "practice_flashcards" else "practice_context"),
            )
        }
    }
}
