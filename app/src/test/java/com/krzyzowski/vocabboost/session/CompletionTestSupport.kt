package com.krzyzowski.vocabboost.session

import org.junit.Assert.fail

/**
 * Slice 2 contract: TestSession exposes `remaining: Int` and `isCompleted: Boolean`.
 *
 * The two values are read reflectively so that, before they exist, the new-behaviour tests
 * fail at run time with a clear message instead of breaking compilation of the whole
 * test source set. Once the members exist the reflective read simply returns their value.
 */
internal fun remainingOf(session: TestSession): Int = read(session, "getRemaining") as Int

internal fun completedOf(session: TestSession): Boolean = read(session, "isCompleted") as Boolean

private fun read(session: TestSession, getter: String): Any =
    try {
        TestSession::class.java.getMethod(getter).invoke(session)!!
    } catch (e: NoSuchMethodException) {
        fail("TestSession must publicly expose `$getter` (see slice 2 acceptance criteria AC1/AC2)")
        throw e
    }

/**
 * Best-effort rule (AC8). [history] are the word ids already shown, [remaining] the word ids
 * of the cards still to be shown, with the next one first. Returns null when the next card is
 * acceptable, otherwise a description of the violation.
 *
 * A word among the last 3 shown may be chosen only when no remaining card meets the
 * 3-other-card minimum; then it must be the word shown least recently.
 */
internal fun bestEffortViolation(history: List<String>, remaining: List<String>): String? {
    if (remaining.isEmpty()) return null
    val next = remaining.first()
    val blocked = history.takeLast(3).toSet()
    if (next !in blocked) return null
    if (remaining.any { it !in blocked }) {
        return "'$next' follows $history although '${remaining.first { it !in blocked }}' meets the 3-card gap"
    }
    val lastSeen = remaining.toSet().associateWith { history.lastIndexOf(it) }
    val oldest = lastSeen.values.min()
    if (lastSeen.getValue(next) != oldest) {
        return "'$next' follows $history but was not the least recently shown of ${lastSeen.keys}"
    }
    if (history.last() == next && remaining.any { it != next }) {
        return "'$next' is repeated back-to-back although other words remain: $remaining"
    }
    return null
}
