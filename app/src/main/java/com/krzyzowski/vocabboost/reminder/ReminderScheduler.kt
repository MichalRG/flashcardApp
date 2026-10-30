package com.krzyzowski.vocabboost.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.krzyzowski.vocabboost.time.AppClock
import java.time.Duration
import java.time.Instant

class ReminderScheduler(private val context: Context, private val clock: AppClock) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun armAll() {
        ReminderSlot.entries.forEach(::armNext)
    }

    fun armNext(slot: ReminderSlot) {
        arm(slot, slot.nextTrigger(clock.now(), clock.zone()))
    }

    fun arm(slot: ReminderSlot, at: Instant) {
        val trigger = PendingIntent.getBroadcast(
            context,
            slot.requestCode,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION).putExtra(EXTRA_SLOT, slot.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), WINDOW.toMillis(), trigger)
    }

    companion object {
        const val ACTION = "com.krzyzowski.vocabboost.REMINDER"
        const val EXTRA_SLOT = "slot"
        private val WINDOW: Duration = Duration.ofMinutes(10)
    }
}
