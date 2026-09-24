package de.bastion.medieval.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WordsTest {
    @Test
    fun `umlauts and case are folded`() {
        assertEquals(listOf("schluessel", "strasse"), Words.tokens("SCHLÜSSEL, Straße!"))
        assertEquals(listOf("lets", "go"), Words.tokens("Let's go"))
    }

    @Test
    fun `inflected words and small typos match`() {
        assertTrue(Words.matches("rostigen", "rostig"))
        assertTrue(Words.matches("keys", "key"))
        // "Schlussel" typed without the umlaut, "Schlüsel" with a missing letter.
        assertTrue(Words.matches("schlussel", "schluessel"))
        assertTrue(Words.matches("schluesel", "schluessel"))
        assertFalse(Words.matches("schlusel", "schluessel"))
        assertTrue(Words.matches("wegstien", "wegstein"))
        assertFalse(Words.matches("weg", "wegstein"))
        assertFalse(Words.matches("tor", "turm"))
    }
}
