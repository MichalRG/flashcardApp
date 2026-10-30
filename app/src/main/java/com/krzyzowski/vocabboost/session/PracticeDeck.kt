package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DeckProgress
import com.krzyzowski.vocabboost.schedule.Stage
import com.krzyzowski.vocabboost.words.VocabWord

class PracticeDeck(private val words: List<VocabWord>, private val progress: DeckProgress) {
    val size: Int
        get() = words.size

    fun wordAt(position: Int): VocabWord = words[position]

    fun stageAt(position: Int): Stage? = progress.of(words[position].id)?.stage
}
