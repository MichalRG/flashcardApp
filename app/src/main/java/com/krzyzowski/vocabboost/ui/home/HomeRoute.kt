package com.krzyzowski.vocabboost.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krzyzowski.vocabboost.ui.PermissionStatus
import com.krzyzowski.vocabboost.ui.Screen
import kotlinx.coroutines.delay

@Composable
fun HomeRoute(onOpen: (Screen) -> Unit, viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val day by viewModel.day.collectAsStateWithLifecycle()
    val dailySeconds by viewModel.dailySeconds.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val goalMinutes by viewModel.goalMinutes.collectAsStateWithLifecycle()
    val workload by viewModel.workload.collectAsStateWithLifecycle()
    val notificationsAsked by viewModel.notificationsAsked.collectAsStateWithLifecycle()
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshDay()
                delay(15_000)
            }
        }
    }

    LaunchedEffect(notificationsAsked) {
        if (notificationsAsked != false) return@LaunchedEffect
        viewModel.markNotificationsAsked()
        val mustAsk = PermissionStatus.read(context).mustAskForNotifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && mustAsk) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    workload?.let {
        HomeScreen(
            secondsToday = dailySeconds.secondsOn(day),
            goalMinutes = goalMinutes,
            goalMetToday = streak.lastGoalDay == day,
            streak = streak.shownOn(day),
            workload = it,
            onOpen = onOpen,
        )
    }
}
