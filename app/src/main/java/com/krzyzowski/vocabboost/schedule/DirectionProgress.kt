package com.krzyzowski.vocabboost.schedule

import java.time.LocalDate

class DirectionProgress private constructor(val schedule: DeckProgress, val learning: Set<String>) {
    fun introduce(id: String, day: LocalDate): DirectionProgress =
        DirectionProgress(schedule.recordAnswer(id, knew = false, day), learning + id)

    fun recordAnswer(id: String, knew: Boolean, day: LocalDate): DirectionProgress {
        if (id !in learning) return DirectionProgress(schedule.recordAnswer(id, knew, day), learning)
        if (!knew) return this
        return DirectionProgress(schedule.completeLearning(id, day), learning - id)
    }

    fun encode(): String = "${schedule.encode()}|${learning.sorted().joinToString(",")}"

    companion object {
        fun learned(schedule: DeckProgress): DirectionProgress = DirectionProgress(schedule, emptySet())

        fun decode(encoded: String): DirectionProgress {
            val parts = encoded.split('|')
            require(parts.size == 2) { "Invalid direction learning progress" }
            val schedule = DeckProgress.decode(parts[0])
            require(schedule.encode() == parts[0]) { "Invalid directional schedule" }
            val ids = if (parts[1].isEmpty()) emptyList() else parts[1].split(',')
            require(ids.distinct().size == ids.size) { "Duplicate initial-learning word" }
            require(ids.all { schedule.of(it)?.stage == Stage.S1 }) { "Invalid initial-learning word" }
            return DirectionProgress(schedule, ids.toSet())
        }
    }
}
