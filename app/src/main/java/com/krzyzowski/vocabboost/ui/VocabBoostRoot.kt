package com.krzyzowski.vocabboost.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krzyzowski.vocabboost.ui.home.HomeRoute
import com.krzyzowski.vocabboost.ui.practice.PracticeRoute
import com.krzyzowski.vocabboost.ui.progress.ProgressRoute
import com.krzyzowski.vocabboost.ui.settings.SettingsRoute
import com.krzyzowski.vocabboost.ui.testmode.TestModeRoute
import com.krzyzowski.vocabboost.ui.testmode.TestModeViewModel

@Composable
fun VocabBoostRoot() {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val testModeViewModel: TestModeViewModel = viewModel()
    val goHome = { screen = Screen.HOME }

    BackHandler(enabled = screen != Screen.HOME, onBack = goHome)

    Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
        when (screen) {
            Screen.HOME -> HomeRoute(
                onOpen = { target ->
                    if (target == Screen.TEST) testModeViewModel.start()
                    screen = target
                },
            )
            Screen.TEST -> TestModeRoute(testModeViewModel, onExit = goHome)
            Screen.PRACTICE -> PracticeRoute()
            Screen.PROGRESS -> ProgressRoute()
            Screen.SETTINGS -> SettingsRoute()
        }
    }
}
