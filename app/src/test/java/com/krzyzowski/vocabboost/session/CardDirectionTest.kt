package com.krzyzowski.vocabboost.session

import com.krzyzowski.vocabboost.words.VocabWord
import org.junit.Assert.assertEquals
import org.junit.Test

class CardDirectionTest {
    private val word = VocabWord("dump", "dump", "We took the broken furniture to the dump.", "wysypisko śmieci")

    @Test
    fun `English first shows only the English word on the front`() {
        assertEquals("dump", CardDirection.ENGLISH_FIRST.front(word))
    }

    @Test
    fun `Polish first shows only the Polish translation on the front`() {
        assertEquals("wysypisko śmieci", CardDirection.POLISH_FIRST.front(word))
    }
}
