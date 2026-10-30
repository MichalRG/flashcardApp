package com.krzyzowski.vocabboost.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.words.VocabWord

@Composable
fun FlipCard(word: VocabWord, front: String, isNew: Boolean, flipped: Boolean, onFlip: () -> Unit) {
    Card(
        onClick = onFlip,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .testTag("flip_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isNew) {
                Text(
                    stringResource(R.string.card_new),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (flipped) {
                Text(word.english, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    word.example,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                )
                Text(
                    word.polish,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(front, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(stringResource(R.string.card_tap_to_flip), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
