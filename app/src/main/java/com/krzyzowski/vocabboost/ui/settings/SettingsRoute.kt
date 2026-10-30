package com.krzyzowski.vocabboost.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krzyzowski.vocabboost.ui.PermissionStatus

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val newWordsPerDay by viewModel.newWordsPerDay.collectAsStateWithLifecycle()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsStateWithLifecycle()
    var permissions by remember { mutableStateOf(PermissionStatus.read(context)) }
    val openNotificationSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }
    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) permissions = PermissionStatus.read(context) else openNotificationSettings()
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            permissions = PermissionStatus.read(context)
        }
    }

    SettingsScreen(
        newWordsPerDay = newWordsPerDay,
        dailyGoalMinutes = dailyGoalMinutes,
        remindersAllowed = permissions.notificationsAllowed,
        onNewWordsPerDayChange = viewModel::setNewWordsPerDay,
        onDailyGoalChange = viewModel::setDailyGoalMinutes,
        onAllowReminders = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && permissions.mustAskForNotifications) {
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings()
            }
        },
    )
}
