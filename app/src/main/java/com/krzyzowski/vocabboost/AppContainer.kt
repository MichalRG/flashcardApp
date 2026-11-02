package com.krzyzowski.vocabboost

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.krzyzowski.vocabboost.data.ProgressStore
import com.krzyzowski.vocabboost.data.ScreenTimeLog
import com.krzyzowski.vocabboost.data.SettingsStore
import com.krzyzowski.vocabboost.reminder.ReminderDecision
import com.krzyzowski.vocabboost.reminder.ReminderDispatcher
import com.krzyzowski.vocabboost.reminder.ReminderNotifier
import com.krzyzowski.vocabboost.reminder.ReminderScheduler
import com.krzyzowski.vocabboost.time.ScreenTimeTracker
import com.krzyzowski.vocabboost.time.VariantClock
import com.krzyzowski.vocabboost.words.VOCAB_WORDS

class AppContainer(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("vocabboost") },
    )

    val words = VOCAB_WORDS
    val clock = VariantClock.create(context)
    val settings = SettingsStore(dataStore)
    val progressStore = ProgressStore(dataStore, clock)
    val screenTimeLog = ScreenTimeLog(dataStore, clock)
    val screenTimeTracker = ScreenTimeTracker(clock, screenTimeLog, settings)
    val reminderScheduler = ReminderScheduler(context, clock)
    val reminderDispatcher = ReminderDispatcher(
        progressStore = progressStore,
        settings = settings,
        screenTimeLog = screenTimeLog,
        decision = ReminderDecision(words),
        notifier = ReminderNotifier(context),
        scheduler = reminderScheduler,
        clock = clock,
    )
}
