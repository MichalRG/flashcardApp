package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWordBuilder.word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Seeded, bounded random sessions: nothing is lost or invented and the test always completes. */
class SessionCompletionPropertyTest {
    private fun randomDeck(random: Random): List<TestCard> {
        val wordCount = random.nextInt(1, 9)
        val mixed = random.nextBoolean()
        val directions = if (mixed) TestMode.MIXED.directions else listOf(CardDirection.POLISH_FIRST)
        val cards = (1..wordCount).flatMap { index ->
            val kind = random.nextInt(3) // 0 = new, 1 = mature review, 2 = unfinished learning review
            directions.map { direction ->
                TestCard(word("w$index"), isNew = kind == 0, direction = direction, isLearning = kind != 1)
            }
        }
        return cards.shuffled(random)
    }

    @Test
    fun `random decks always complete with nothing lost or invented and best-effort spacing`() {
        val random = Random(20261116)
        repeat(2000) { run ->
            val deck = randomDeck(random)
            val label = "run $run, deck ${deck.map { "${it.word.id}/${it.direction}/${it.isNew}/${it.isLearning}" }}"
            val missesAllowed = mutableMapOf<LearningKey, Int>()
            val shown = mutableListOf<TestCard>()
            var session = TestSession.start(deck)

            while (!session.isOver) {
                assertTrue("endless test: $label", shown.size < 400)
                val card = requireNotNull(session.current)
                assertTrue("remaining must count the card on screen: $label", remainingOf(session) >= 1)
                assertTrue(
                    "counter total (position + remaining) never below the card number: $label",
                    session.position + remainingOf(session) >= session.position + 1,
                )

                val violation = bestEffortViolation(
                    shown.map { it.word.id },
                    session.cards.drop(session.position).map { it.word.id },
                )
                assertNull("$violation; $label", violation)

                shown += card
                val knew = if (card.isLearning) {
                    val left = missesAllowed.getOrPut(card.key) { random.nextInt(0, 5) }
                    if (left > 0) {
                        missesAllowed[card.key] = left - 1
                        false
                    } else {
                        true
                    }
                } else {
                    random.nextBoolean()
                }
                val before = session
                val remainingBefore = remainingOf(before)
                session = before.answer(knew = knew, timeUp = false)

                assertEquals("one answer recorded: $label", before.answered + 1, session.answered)
                assertEquals(before.knew + if (knew) 1 else 0, session.knew)
                assertEquals(before.didNotKnow + if (knew) 0 else 1, session.didNotKnow)
                val retryOwed = card.isLearning && !knew
                assertEquals(
                    "a miss on a learning card owes exactly one retry, anything else owes one card less: $label",
                    remainingBefore - 1 + if (retryOwed) 1 else 0,
                    remainingOf(session),
                )
            }

            assertNull(session.current)
            assertTrue("completed: $label", completedOf(session))
            assertEquals(0, remainingOf(session))
            assertTrue("pending learning left: $label", session.pendingLearning.isEmpty())
            assertTrue("deferred left: $label", session.deferredCards.isEmpty())
            assertEquals("every answer belongs to a shown card: $label", shown.size, session.answered)
            assertEquals(deck.map { it.key }.toSet() - shown.map { it.key }.toSet(), emptySet<LearningKey>())
        }
    }

    @Test
    fun `random sessions ended at a random point report exactly the unanswered work`() {
        val random = Random(77)
        repeat(500) { run ->
            val deck = randomDeck(random)
            var session = TestSession.start(deck)
            repeat(random.nextInt(0, deck.size + 1)) {
                if (!session.isOver) session = session.answer(knew = random.nextBoolean(), timeUp = false)
            }
            val ended = session.end()
            val owed = if (session.isOver) 0 else remainingOf(session)

            assertTrue(ended.isOver)
            assertEquals("run $run", owed, remainingOf(ended))
            assertEquals("run $run", owed == 0, completedOf(ended))
            assertEquals(session.answered, ended.answered)
        }
    }
}
