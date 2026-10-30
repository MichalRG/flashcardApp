package com.krzyzowski.vocabboost.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class PermissionStatus(val notificationsAllowed: Boolean, val mustAskForNotifications: Boolean) {
    companion object {
        fun read(context: Context): PermissionStatus {
            val mustAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            return PermissionStatus(
                notificationsAllowed = !mustAsk && NotificationManagerCompat.from(context).areNotificationsEnabled(),
                mustAskForNotifications = mustAsk,
            )
        }
    }
}
