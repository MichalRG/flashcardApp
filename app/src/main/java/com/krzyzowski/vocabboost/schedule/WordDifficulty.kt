package com.krzyzowski.vocabboost.schedule

data class WordDifficulty(
    val misses: Int = 0,
    val recoveries: Int = 0,
    val isDifficult: Boolean = false,
) {
    init {
        require(misses in 0..MISSES_TO_ENTER) { "Invalid consecutive-miss count" }
        require(recoveries in 0 until SUCCESSES_TO_LEAVE) { "Invalid recovery count" }
        require(misses == 0 || recoveries == 0) { "Miss and recovery streaks cannot overlap" }
        require(isDifficult || (misses < MISSES_TO_ENTER && recoveries == 0)) {
            "Difficulty flag does not match the answer streaks"
        }
    }

    fun answered(knew: Boolean): WordDifficulty {
        if (!knew) {
            val nextMisses = (misses + 1).coerceAtMost(MISSES_TO_ENTER)
            return WordDifficulty(misses = nextMisses, isDifficult = isDifficult || nextMisses == MISSES_TO_ENTER)
        }
        if (!isDifficult || recoveries + 1 == SUCCESSES_TO_LEAVE) return WordDifficulty()
        return WordDifficulty(recoveries = recoveries + 1, isDifficult = true)
    }

    companion object {
        const val MISSES_TO_ENTER = 3
        const val SUCCESSES_TO_LEAVE = 2
    }
}
