package com.krzyzowski.vocabboost.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.krzyzowski.vocabboost.MainActivity
import com.krzyzowski.vocabboost.R
import com.krzyzowski.vocabboost.schedule.Workload

class ReminderNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun post(workload: Workload) {
        val denied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (denied) return
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.channel_reminders))
                .build(),
        )
        val waiting = workload.waiting
        val newCount = workload.newWords.size
        val text = if (newCount > 0) {
            context.resources.getQuantityString(R.plurals.reminder_waiting_with_new, waiting, waiting, newCount)
        } else {
            context.resources.getQuantityString(R.plurals.reminder_waiting, waiting, waiting)
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val reminder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, reminder)
    }

    private companion object {
        const val CHANNEL = "daily_reminders"
        const val NOTIFICATION_ID = 1
    }
}
