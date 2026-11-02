package com.krzyzowski.vocabboost.schedule

class DifficultyProgress private constructor(private val words: Map<String, WordDifficulty>) {
    fun of(id: String): WordDifficulty? = words[id]

    fun isDifficult(id: String): Boolean = words[id]?.isDifficult == true

    fun recordAnswer(id: String, knew: Boolean): DifficultyProgress {
        val difficulty = (words[id] ?: WordDifficulty()).answered(knew)
        return DifficultyProgress(
            if (difficulty == WordDifficulty()) words - id else words + (id to difficulty),
        )
    }

    fun encode(): String = words.entries.sortedBy { it.key }.joinToString(",") { (id, difficulty) ->
        "$id=${difficulty.misses};${difficulty.recoveries};${difficulty.isDifficult}"
    }

    companion object {
        val NONE = DifficultyProgress(emptyMap())

        fun decode(encoded: String): DifficultyProgress {
            if (encoded.isEmpty()) return NONE
            val words = mutableMapOf<String, WordDifficulty>()
            encoded.split(',').forEach { entry ->
                val parts = entry.split('=', limit = 2)
                require(parts.size == 2 && parts[0].isNotBlank()) { "Invalid difficulty progress entry" }
                val (id, value) = parts
                val fields = value.split(';')
                require(fields.size == 3 && id !in words) { "Invalid difficulty progress for $id" }
                words[id] = WordDifficulty(fields[0].toInt(), fields[1].toInt(), fields[2].toBooleanStrict())
            }
            return DifficultyProgress(words)
        }
    }
}
