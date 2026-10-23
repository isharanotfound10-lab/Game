package com.velvet.superkeyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePredictionEngineTest {
    @Test fun correctsCommonTyposWithoutNetwork() {
        assertEquals("the", OfflinePredictionEngine.correct("teh"))
        assertEquals("because", OfflinePredictionEngine.correct("becuase"))
        assertEquals("Hello", OfflinePredictionEngine.correct("Hello"))
    }

    @Test fun suggestionsMatchTypedPrefix() {
        val result = OfflinePredictionEngine.suggestions("hel")
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.startsWith("hel") })
    }

    @Test fun contextSuggestionsAreReturnedWhenNoPrefix() {
        assertTrue(OfflinePredictionEngine.suggestions("", "thank").contains("you"))
    }
}
