package com.krzyzowski.vocabboost.data

import java.time.LocalDate

data class Streak(val count: Int, val lastGoalDay: LocalDate?) {

    fun goalMetOn(day: LocalDate): Streak = when {
        lastGoalDay != null && !day.isAfter(lastGoalDay) -> this
        lastGoalDay == day.minusDays(1) -> Streak(count + 1, day)
        else -> Streak(1, day)
    }

    fun shownOn(day: LocalDate): Int =
        if (lastGoalDay == day || lastGoalDay == day.minusDays(1)) count else 0

    fun encode(): String = "$count;${lastGoalDay ?: NO_DAY}"

    companion object {
        private const val NO_DAY = "-"

        val NONE = Streak(0, null)

        fun decode(encoded: String): Streak = runCatching {
            val (count, lastGoalDay) = encoded.split(';')
            Streak(count.toInt(), lastGoalDay.takeUnless { it == NO_DAY }?.let(LocalDate::parse))
        }.getOrDefault(NONE)
    }
}
