package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.words.VocabWord
import com.krzyzowski.vocabboost.session.CardDirection
import com.krzyzowski.vocabboost.session.TestMode
import java.time.LocalDate

data class ProgressSummary(
    val notStarted: Int,
    val perStage: Map<Stage, Int>,
    val dueToday: Int,
    val newToday: Int,
    val dueTomorrow: Int,
    val learning: Int = 0,
) {
    companion object {
        fun of(words: List<VocabWord>, deck: DeckProgress, day: LocalDate, newWordsPerDay: Int): ProgressSummary {
            return of(words, DirectionalProgress.fromLegacy(deck), day, newWordsPerDay, CardDirection.ENGLISH_FIRST)
        }

        fun of(
            words: List<VocabWord>,
            deck: DirectionalProgress,
            day: LocalDate,
            newWordsPerDay: Int,
            direction: CardDirection,
        ): ProgressSummary {
            val started = words.mapNotNull { deck.of(it.id, direction) }
            val learning = words.count { deck.isLearning(it.id, direction) }
            val scheduled = words.filterNot { deck.isLearning(it.id, direction) }
                .mapNotNull { deck.of(it.id, direction) }
            val mode = if (direction == CardDirection.ENGLISH_FIRST) TestMode.ENGLISH_FIRST else TestMode.POLISH_FIRST
            val workload = DirectionalWorkload.of(words, deck, day, newWordsPerDay, mode)
            return ProgressSummary(
                notStarted = words.size - started.size,
                perStage = Stage.entries.associateWith { stage -> scheduled.count { it.stage == stage } },
                dueToday = workload.reviews.size,
                newToday = workload.newWords.size,
                dueTomorrow = scheduled.count { it.due == day.plusDays(1) },
                learning = learning,
            )
        }
    }
}
