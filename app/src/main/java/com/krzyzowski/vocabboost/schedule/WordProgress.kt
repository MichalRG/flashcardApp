package com.krzyzowski.vocabboost.schedule

import java.time.LocalDate

data class WordProgress(val stage: Stage, val due: LocalDate?, val introducedOn: LocalDate) {

    fun answered(knew: Boolean, day: LocalDate): WordProgress {
        val reached = if (knew) stage.next else Stage.S1
        return copy(stage = reached, due = reached.intervalDays?.let(day::plusDays))
    }

    fun isDueBy(day: LocalDate): Boolean = due != null && !due.isAfter(day)

    companion object {
        fun introduced(day: LocalDate) = WordProgress(Stage.S1, day.plusDays(1), day)
    }
}
