package com.krzyzowski.vocabboost.reminder

import com.krzyzowski.vocabboost.schedule.DirectionalProgress
import com.krzyzowski.vocabboost.schedule.DirectionalWorkload
import com.krzyzowski.vocabboost.session.TestMode
import com.krzyzowski.vocabboost.words.VocabWord
import java.time.LocalDate

class ReminderDecision(private val words: List<VocabWord>) {

    fun decide(
        deck: DirectionalProgress,
        day: LocalDate,
        newWordsPerDay: Int,
        lastGoalDay: LocalDate?,
        mode: TestMode,
    ): DirectionalWorkload? {
        if (lastGoalDay == day) return null
        return DirectionalWorkload.of(words, deck, day, newWordsPerDay, mode).takeIf { it.waiting > 0 }
    }
}
