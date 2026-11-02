package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.DifficultyProgress
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.words.VocabWord

class PracticeDeck(private val words: List<VocabWord>, private val progress: DeckProgress) {
    val size: Int
        get() = words.size

    fun wordAt(position: Int): VocabWord = words[position]

    fun stageAt(position: Int): Stage? = progress.of(words[position].id)?.stage

    fun boundedPosition(position: Int): Int = position.coerceIn(0, (size - 1).coerceAtLeast(0))

    companion object {
        fun difficult(words: List<VocabWord>, progress: DeckProgress, difficulty: DifficultyProgress): PracticeDeck =
            PracticeDeck(words.filter { difficulty.isDifficult(it.id) }, progress)
    }
}
