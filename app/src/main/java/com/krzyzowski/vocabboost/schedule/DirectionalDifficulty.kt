package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.session.CardDirection

class DirectionalDifficulty private constructor(private val directions: Map<CardDirection, DifficultyProgress>) {
    fun forDirection(direction: CardDirection): DifficultyProgress = directions.getValue(direction)

    fun of(id: String, direction: CardDirection): WordDifficulty? = forDirection(direction).of(id)

    fun isDifficult(id: String, direction: CardDirection): Boolean = forDirection(direction).isDifficult(id)

    fun recordAnswer(id: String, direction: CardDirection, knew: Boolean): DirectionalDifficulty =
        DirectionalDifficulty(directions + (direction to forDirection(direction).recordAnswer(id, knew)))

    fun encode(): String = DirectionalText.encode(directions.mapValues { it.value.encode() })

    companion object {
        val NONE = fromLegacy(DifficultyProgress.NONE)

        fun fromLegacy(progress: DifficultyProgress): DirectionalDifficulty =
            DirectionalDifficulty(CardDirection.entries.associateWith { progress })

        fun decode(encoded: String): DirectionalDifficulty =
            DirectionalDifficulty(DirectionalText.decode(encoded).mapValues { DifficultyProgress.decode(it.value) })
    }
}
