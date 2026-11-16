package com.krzyzowski.vocabboost.session

import java.util.Locale

/** Lenient, typo-intolerant matching of a typed translation against a vocabulary entry. */
object TypedAnswer {
    private val parenthetical = Regex("""\([^)]*\)""")
    private val whitespace = Regex("""\s+""")
    private val trailingPunctuation = Regex("""[.!?]+$""")
    private val polishSeparators = Regex("""[,;/]""")
    private val englishSeparators = Regex("""[;/]""")
    private val folding = mapOf(
        'ą' to 'a', 'ć' to 'c', 'ę' to 'e', 'ł' to 'l', 'ń' to 'n',
        'ó' to 'o', 'ś' to 's', 'ź' to 'z', 'ż' to 'z',
    )

    fun normalize(s: String): String {
        val lowered = s.replace(parenthetical, " ").lowercase(Locale.ROOT)
        val folded = buildString(lowered.length) { lowered.forEach { append(folding[it] ?: it) } }
        return folded.replace(whitespace, " ").trim().replace(trailingPunctuation, "").trim()
    }

    /** Normalized accepted alternatives; [direction] is the card direction, which selects the separators. */
    fun alternatives(expected: String, direction: CardDirection): List<String> = parts(expected, direction).distinct()

    fun isCorrect(typed: String, expected: String, direction: CardDirection): Boolean {
        if (typed.isBlank()) return false
        val whole = normalize(typed)
        if (whole.isEmpty()) return false
        if (whole == normalize(expected)) return true
        val typedParts = parts(typed, direction)
        if (typedParts.isEmpty()) return false
        val accepted = alternatives(expected, direction).toSet()
        return typedParts.all { it in accepted }
    }

    private fun parts(text: String, direction: CardDirection): List<String> {
        val separators = when (direction) {
            CardDirection.ENGLISH_FIRST -> polishSeparators
            CardDirection.POLISH_FIRST -> englishSeparators
        }
        return text.replace(parenthetical, " ").split(separators).map(::normalize).filter { it.isNotEmpty() }
    }
}
