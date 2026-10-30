package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.words.VocabWord
import java.time.LocalDate

data class ProgressSummary(
    val notStarted: Int,
    val perStage: Map<Stage, Int>,
    val dueToday: Int,
    val newToday: Int,
    val dueTomorrow: Int,
) {
    companion object {
        fun of(words: List<VocabWord>, deck: DeckProgress, day: LocalDate, newWordsPerDay: Int): ProgressSummary {
            val started = words.mapNotNull { deck.of(it.id) }
            val workload = Workload.of(words, deck, day, newWordsPerDay)
            return ProgressSummary(
                notStarted = words.size - started.size,
                perStage = Stage.entries.associateWith { stage -> started.count { it.stage == stage } },
                dueToday = workload.reviews.size,
                newToday = workload.newWords.size,
                dueTomorrow = started.count { it.due == day.plusDays(1) },
            )
        }
    }
}
