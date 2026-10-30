package com.krzyzowski.vocabboost.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.krzyzowski.vocabboost.VocabBoostApp
import com.krzyzowski.vocabboost.reminder.ReminderSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

class DebugCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as VocabBoostApp).container
        val slot = ReminderSlot.entries.find { it.name == intent.getStringExtra("slot") }
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                pending.resultData = when (intent.action) {
                    SET_TIME_OFFSET -> {
                        val minutes = intent.getIntExtra("minutes", 0).toLong()
                        DebugTimeOffset(context).set(minutes)
                        container.reminderScheduler.armAll()
                        "offset_minutes=$minutes app_now=${container.clock.now()}"
                    }
                    FIRE_REMINDER -> slot?.let {
                        container.reminderDispatcher.dispatch(it)
                        "fired $it"
                    }
                    ARM_REMINDER_IN -> slot?.let {
                        val at = Instant.now().plusSeconds(intent.getIntExtra("seconds", 60).toLong())
                        container.reminderScheduler.arm(it, at)
                        "armed $it at $at"
                    }
                    else -> "unknown action ${intent.action}"
                } ?: "unknown slot, use MORNING or EVENING"
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val SET_TIME_OFFSET = "com.krzyzowski.vocabboost.debug.SET_TIME_OFFSET"
        const val FIRE_REMINDER = "com.krzyzowski.vocabboost.debug.FIRE_REMINDER"
        const val ARM_REMINDER_IN = "com.krzyzowski.vocabboost.debug.ARM_REMINDER_IN"
    }
}
