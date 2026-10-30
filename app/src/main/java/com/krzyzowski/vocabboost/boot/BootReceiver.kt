package com.krzyzowski.vocabboost.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.krzyzowski.vocabboost.VocabBoostApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ARM_ACTIONS) return
        (context.applicationContext as VocabBoostApp).container.reminderScheduler.armAll()
    }

    private companion object {
        val ARM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
        )
    }
}
