package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.DifficultyProgress
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.words.VocabWord

class PracticeDeck(
    private val words: List<VocabWord>,
    private val progress: DeckProgress,
    val direction: CardDirection = CardDirection.ENGLISH_FIRST,
    private val learning: Set<String> = emptySet(),
    val mode: PracticeMode = PracticeMode.FLASHCARDS,
) {
    val size: Int
        get() = words.size

    fun wordAt(position: Int): VocabWord = words[position]

    fun stageAt(position: Int): Stage? = progress.of(words[position].id)?.stage

    fun isLearningAt(position: Int): Boolean = words[position].id in learning

    fun boundedPosition(position: Int): Int = position.coerceIn(0, (size - 1).coerceAtLeast(0))

    companion object {
        fun difficult(
            words: List<VocabWord>,
            progress: DeckProgress,
            difficulty: DifficultyProgress,
            direction: CardDirection = CardDirection.ENGLISH_FIRST,
            learning: Set<String> = emptySet(),
        ): PracticeDeck = PracticeDeck(words.filter { difficulty.isDifficult(it.id) }, progress, direction, learning)
    }
}
