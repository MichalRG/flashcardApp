package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.session.TypedRecallGate.Action
import com.krzyzowski.vocabboost.session.TypedRecallGate.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypedRecallGateTest {
    private val expected = "wysypać, wylać, opróżnić"
    private val en = CardDirection.ENGLISH_FIRST
    private val pl = CardDirection.POLISH_FIRST

    @Test
    fun `a new gate is unsubmitted and allows no grading action`() {
        val gate = TypedRecallGate()

        assertEquals(Status.UNSUBMITTED, gate.status)
        assertEquals("", gate.submitted)
        assertEquals(emptySet<Action>(), gate.actions)
    }

    @Test
    fun `only non blank input can be submitted`() {
        val gate = TypedRecallGate()

        assertFalse(gate.canSubmit(""))
        assertFalse(gate.canSubmit("   "))
        assertFalse(gate.canSubmit("\t\n"))
        assertTrue(gate.canSubmit("wylać"))
        assertTrue(gate.canSubmit(" x "))
    }

    @Test
    fun `submitting blank input is rejected and the gate stays unsubmitted`() {
        listOf("", " ", "   ", "\t\n").forEach { blank ->
            val gate = TypedRecallGate().submit(blank, expected, en)

            assertEquals("'$blank'", Status.UNSUBMITTED, gate.status)
            assertEquals(emptySet<Action>(), gate.actions)
            assertEquals(TypedRecallGate(), gate)
        }
    }

    @Test
    fun `a correct submission allows exactly knew and did not know`() {
        val gate = TypedRecallGate().submit("WYLAC", expected, en)

        assertEquals(Status.CORRECT, gate.status)
        assertEquals("WYLAC", gate.submitted)
        assertEquals(setOf(Action.KNEW, Action.DID_NOT_KNOW), gate.actions)
    }

    @Test
    fun `a wrong submission allows exactly continue and keeps the typed text`() {
        val gate = TypedRecallGate().submit("wyla", expected, en)

        assertEquals(Status.WRONG, gate.status)
        assertEquals("wyla", gate.submitted)
        assertEquals(setOf(Action.CONTINUE), gate.actions)
    }

    @Test
    fun `a wrong submission never allows a grading action`() {
        val actions = TypedRecallGate().submit("pić", expected, en).actions

        assertFalse(Action.KNEW in actions)
        assertFalse(Action.DID_NOT_KNOW in actions)
    }

    @Test
    fun `the typed text is kept exactly as entered`() {
        assertEquals("  Wylać!  ", TypedRecallGate().submit("  Wylać!  ", expected, en).submitted)
    }

    @Test
    fun `a correct gate cannot be resubmitted`() {
        val correct = TypedRecallGate().submit("wylać", expected, en)
        val again = correct.submit("zupełnie inne", expected, en)

        assertEquals(correct, again)
        assertEquals(Status.CORRECT, again.status)
        assertEquals("wylać", again.submitted)
        assertFalse(correct.canSubmit("anything"))
    }

    @Test
    fun `a wrong gate cannot be retried with a correct answer`() {
        val wrong = TypedRecallGate().submit("wyla", expected, en)
        val again = wrong.submit("wylać", expected, en)

        assertEquals(wrong, again)
        assertEquals(Status.WRONG, again.status)
        assertEquals(setOf(Action.CONTINUE), again.actions)
        assertFalse(wrong.canSubmit("wylać"))
    }

    @Test
    fun `the direction selects the matching rules`() {
        assertEquals(Status.CORRECT, TypedRecallGate().submit("flat tire", "flat tyre / flat tire", pl).status)
        assertEquals(Status.WRONG, TypedRecallGate().submit("no pain", "no pain, no gain", pl).status)
        assertEquals(Status.CORRECT, TypedRecallGate().submit("no pain", "no pain, no gain", en).status)
    }

    @Test
    fun `a gate rebuilt from its saved status and text behaves the same`() {
        val wrong = TypedRecallGate(Status.WRONG, "wyla")
        val correct = TypedRecallGate(Status.CORRECT, "wylać")

        assertEquals(setOf(Action.CONTINUE), wrong.actions)
        assertEquals(setOf(Action.KNEW, Action.DID_NOT_KNOW), correct.actions)
        assertEquals(wrong, wrong.submit("wylać", expected, en))
        assertEquals(correct, correct.submit("zzz", expected, en))
    }
}
