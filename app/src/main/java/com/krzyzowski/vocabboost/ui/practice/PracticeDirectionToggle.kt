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
import com.krzyzowski.vocabboost.session.CardDirection

@Composable
fun PracticeDirectionToggle(direction: CardDirection, onDirectionChange: (CardDirection) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CardDirection.entries.forEach { option ->
            val englishFirst = option == CardDirection.ENGLISH_FIRST
            FilterChip(
                selected = direction == option,
                onClick = { onDirectionChange(option) },
                label = {
                    Text(stringResource(if (englishFirst) R.string.practice_en_first else R.string.practice_pl_first))
                },
                modifier = Modifier.testTag(if (englishFirst) "practice_en_first" else "practice_pl_first"),
            )
        }
    }
}
