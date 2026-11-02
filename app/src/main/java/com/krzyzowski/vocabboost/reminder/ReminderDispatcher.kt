package com.krzyzowski.vocabboost.reminder

import com.krzyzowski.vocabboost.data.ProgressStore
import com.krzyzowski.vocabboost.data.ScreenTimeLog
import com.krzyzowski.vocabboost.data.SettingsStore
import com.krzyzowski.vocabboost.time.AppClock
import kotlinx.coroutines.flow.first

class ReminderDispatcher(
    private val progressStore: ProgressStore,
    private val settings: SettingsStore,
    private val screenTimeLog: ScreenTimeLog,
    private val decision: ReminderDecision,
    private val notifier: ReminderNotifier,
    private val scheduler: ReminderScheduler,
    private val clock: AppClock,
) {
    suspend fun dispatch(slot: ReminderSlot) {
        scheduler.armNext(slot)
        decision.decide(
            deck = progressStore.deck.first(),
            day = clock.today(),
            newWordsPerDay = settings.newWordsPerDay.first(),
            lastGoalDay = screenTimeLog.streak.first().lastGoalDay,
            mode = settings.testMode.first(),
        )?.let(notifier::post)
    }
}
