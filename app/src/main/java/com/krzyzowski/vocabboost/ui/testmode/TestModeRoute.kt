package com.krzyzowski.vocabboost.ui.testmode

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
fun TestModeRoute(viewModel: TestModeViewModel, onExit: () -> Unit) {
    if (!viewModel.isOpen) {
        LaunchedEffect(Unit) { onExit() }
        return
    }
    val session by viewModel.session.collectAsStateWithLifecycle()
    val timer by viewModel.timer.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val close = {
        viewModel.close()
        onExit()
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.shown()
            try {
                while (true) {
                    delay(1_000)
                    viewModel.tick()
                }
            } finally {
                viewModel.hidden()
            }
        }
    }

    BackHandler {
        if (session?.isOver != false) close() else viewModel.end()
    }

    val current = session ?: return
    if (current.isOver) {
        TestEndScreen(session = current, onBackHome = close)
    } else {
        TestModeScreen(
            session = current,
            elapsed = timer.elapsed(now),
            fineToStop = timer.fineToStop(now),
            timeUp = timer.timeUp(now),
            onAnswer = viewModel::answer,
            onEnd = viewModel::end,
        )
    }
}
