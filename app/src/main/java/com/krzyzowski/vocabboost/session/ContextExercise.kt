package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWord

class ContextExercise(word: VocabWord) {
    val completedSentence: String = word.contextSentence
    val answer: String
    val prompt: String

    init {
        require(word.contextAnswer.isNotBlank() && word.contextAnswer == word.contextAnswer.trim()) {
            "Invalid context answer for ${word.id}"
        }
        require(!completedSentence.contains(GAP)) { "Context sentence already has a gap for ${word.id}" }
        val pattern = Regex(
            "(?<![\\p{L}\\p{N}_])${Regex.escape(word.contextAnswer)}(?![\\p{L}\\p{N}_])",
            RegexOption.IGNORE_CASE,
        )
        val matches = pattern.findAll(completedSentence).toList()
        require(matches.size == 1) {
            "Context sentence for ${word.id} must contain its answer exactly once"
        }
        val match = matches.single()
        answer = match.value
        prompt = completedSentence.replaceRange(match.range, GAP)
    }

    companion object {
        const val GAP = "______"
    }
}
