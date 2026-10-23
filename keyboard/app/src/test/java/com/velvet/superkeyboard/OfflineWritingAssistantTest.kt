package com.velvet.superkeyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineWritingAssistantTest {
    @Test fun grammarHelperFixesCommonTyposAndPunctuation() {
        assertEquals("I hello you.", OfflineWritingAssistant.improve("i helo you", "Fix grammar"))
    }

    @Test fun conciseHelperRemovesCommonFillerWords() {
        assertEquals("This works.", OfflineWritingAssistant.improve("This really works.", "Make concise"))
    }

    @Test fun friendlyHelperAddsFriendlyTone() {
        assertEquals("Hello there 😊", OfflineWritingAssistant.improve("hello there", "Friendly tone"))
    }

    @Test fun assistantPreservesEmptyInput() {
        assertEquals("   ", OfflineWritingAssistant.improve("   ", "Fix grammar"))
    }
}
