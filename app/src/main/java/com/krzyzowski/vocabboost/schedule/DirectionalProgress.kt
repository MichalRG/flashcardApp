package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.session.CardDirection
import java.time.LocalDate

class DirectionalProgress private constructor(private val directions: Map<CardDirection, DirectionProgress>) {
    fun forDirection(direction: CardDirection): DirectionProgress = directions.getValue(direction)

    fun of(id: String, direction: CardDirection): WordProgress? = forDirection(direction).schedule.of(id)

    fun isLearning(id: String, direction: CardDirection): Boolean = id in forDirection(direction).learning

    fun hasWord(id: String): Boolean = directions.values.any { it.schedule.of(id) != null }

    fun introducedOn(day: LocalDate): Int =
        directions.values.flatMap { it.schedule.introducedIdsOn(day) }.toSet().size

    fun recordAnswer(id: String, direction: CardDirection, knew: Boolean, day: LocalDate): DirectionalProgress {
        val started = if (hasWord(id)) directions else directions.mapValues { it.value.introduce(id, day) }
        val answered = started.getValue(direction).recordAnswer(id, knew, day)
        return DirectionalProgress(started + (direction to answered))
    }

    fun encode(): String = DirectionalText.encode(directions.mapValues { it.value.encode() })

    companion object {
        val NONE = fromLegacy(DeckProgress.NONE)

        fun fromLegacy(progress: DeckProgress): DirectionalProgress =
            DirectionalProgress(CardDirection.entries.associateWith { DirectionProgress.learned(progress) })

        fun decode(encoded: String): DirectionalProgress {
            val directions = DirectionalText.decode(encoded).mapValues { DirectionProgress.decode(it.value) }
            require(directions.values.map { it.schedule.ids }.distinct().size == 1) {
                "Directional schedules must contain the same introduced words"
            }
            return DirectionalProgress(directions)
        }
    }
}
