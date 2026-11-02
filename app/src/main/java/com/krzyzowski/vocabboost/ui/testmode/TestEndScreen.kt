package com.krzyzowski.vocabboost.ui.testmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.krzyzowski.vocabboost.session.TestSession

@Composable
fun TestEndScreen(session: TestSession, onBackHome: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.test_end_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                pluralStringResource(R.plurals.test_end_answered, session.answered, session.answered),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag("end_answered"),
            )
            Text(stringResource(R.string.test_end_knew, session.knew), modifier = Modifier.testTag("end_knew"))
            Text(
                stringResource(R.string.test_end_did_not_know, session.didNotKnow),
                modifier = Modifier.testTag("end_did_not_know"),
            )
            if (session.pendingLearning.isNotEmpty()) {
                Text(
                    pluralStringResource(
                        R.plurals.test_end_learning_left,
                        session.pendingLearning.size,
                        session.pendingLearning.size,
                    ),
                )
            }
            Button(onClick = onBackHome, modifier = Modifier.fillMaxWidth().testTag("back_home")) {
                Text(stringResource(R.string.test_end_back_home))
            }
        }
    }
}
