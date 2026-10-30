package com.krzyzowski.vocabboost.schedule

import java.time.LocalDate

object DeckBuilder {
    // Answers "Knew it" until the word sits on [stage], all on one day: due = that day + the stage's interval.
    fun DeckProgress.reaching(id: String, stage: Stage, answeredOn: LocalDate): DeckProgress =
        (0..stage.ordinal).fold(this) { deck, _ -> deck.recordAnswer(id, knew = true, day = answeredOn) }
}
