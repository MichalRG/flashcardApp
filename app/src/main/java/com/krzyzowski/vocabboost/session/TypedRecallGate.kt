package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.schedule.DirectionalDifficulty
import com.krzyzowski.vocabboost.schedule.requiresTypedRecall

/** Typed-recall step for one card display: a non-blank attempt must be submitted before any grading. */
data class TypedRecallGate(val status: Status = Status.UNSUBMITTED, val submitted: String = "") {
    enum class Status { UNSUBMITTED, CORRECT, WRONG }

    enum class Action { KNEW, DID_NOT_KNOW, CONTINUE }

    val actions: Set<Action>
        get() = when (status) {
            Status.UNSUBMITTED -> emptySet()
            Status.CORRECT -> setOf(Action.KNEW, Action.DID_NOT_KNOW)
            Status.WRONG -> setOf(Action.CONTINUE)
        }

    fun canSubmit(input: String): Boolean = status == Status.UNSUBMITTED && input.isNotBlank()

    fun submit(input: String, expected: String, direction: CardDirection): TypedRecallGate {
        if (!canSubmit(input)) return this
        val correct = TypedAnswer.isCorrect(input, expected, direction)
        return TypedRecallGate(if (correct) Status.CORRECT else Status.WRONG, input)
    }
}

fun DirectionalDifficulty?.requiresTypedRecall(card: TestCard?): Boolean =
    this != null && card != null && requiresTypedRecall(card.word.id, card.direction)
