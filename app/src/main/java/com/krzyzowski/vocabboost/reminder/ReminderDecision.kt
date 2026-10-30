package com.krzyzowski.vocabboost.reminder

import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.Workload
import com.krzyzowski.vocabboost.words.VocabWord
import java.time.LocalDate

class ReminderDecision(private val words: List<VocabWord>) {

    fun decide(deck: DeckProgress, day: LocalDate, newWordsPerDay: Int, lastGoalDay: LocalDate?): Workload? {
        if (lastGoalDay == day) return null
        return Workload.of(words, deck, day, newWordsPerDay).takeIf { it.waiting > 0 }
    }
}
