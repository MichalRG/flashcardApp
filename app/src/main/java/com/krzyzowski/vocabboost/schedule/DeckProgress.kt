package com.krzyzowski.vocabboost.schedule

import java.time.LocalDate

class DeckProgress private constructor(private val words: Map<String, WordProgress>) {

    fun of(id: String): WordProgress? = words[id]

    fun recordAnswer(id: String, knew: Boolean, day: LocalDate): DeckProgress {
        val progress = words[id]?.answered(knew, day) ?: WordProgress.introduced(day)
        return DeckProgress(words + (id to progress))
    }

    fun introducedOn(day: LocalDate): Int = words.values.count { it.introducedOn == day }

    fun encode(): String = words.entries
        .sortedBy { it.key }
        .joinToString(",") { (id, progress) ->
            "$id=${progress.stage.code};${progress.due ?: RETIRED_DUE};${progress.introducedOn}"
        }

    companion object {
        private const val RETIRED_DUE = "-"

        val NONE = DeckProgress(emptyMap())

        fun decode(encoded: String): DeckProgress = DeckProgress(
            encoded.split(',')
                .filter { it.isNotBlank() }
                .mapNotNull { entry ->
                    runCatching {
                        val (id, value) = entry.split('=', limit = 2)
                        val (stage, due, introducedOn) = value.split(';')
                        id to WordProgress(
                            stage = Stage.fromCode(stage),
                            due = due.takeUnless { it == RETIRED_DUE }?.let(LocalDate::parse),
                            introducedOn = LocalDate.parse(introducedOn),
                        )
                    }.getOrNull()
                }
                .toMap(),
        )
    }
}
