package com.krzyzowski.vocabboost.ui.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProgressRoute(viewModel: ProgressViewModel = viewModel()) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    summary?.let { ProgressScreen(summary = it) }
}
