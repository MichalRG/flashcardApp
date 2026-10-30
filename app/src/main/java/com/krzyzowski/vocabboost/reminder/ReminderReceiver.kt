package com.krzyzowski.vocabboost.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.krzyzowski.vocabboost.VocabBoostApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION) return
        val slotName = intent.getStringExtra(ReminderScheduler.EXTRA_SLOT)
        val slot = ReminderSlot.entries.find { it.name == slotName } ?: return
        val dispatcher = (context.applicationContext as VocabBoostApp).container.reminderDispatcher
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                dispatcher.dispatch(slot)
            } finally {
                pending.finish()
            }
        }
    }
}
