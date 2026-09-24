package de.bastion.medieval.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class GrammarTest {
    private val key = GermanNoun("Schlüssel", Gender.MASCULINE, adjectives = listOf("rostig"))
    private val lantern = GermanNoun("Laterne", Gender.FEMININE, adjectives = listOf("alt"))
    private val knife = GermanNoun("Messer", Gender.NEUTER, adjectives = listOf("klein"))

    @Test
    fun `German definite forms follow the weak declension`() {
        assertEquals("der rostige Schlüssel", key.definite(Case.NOMINATIVE))
        assertEquals("den rostigen Schlüssel", key.definite(Case.ACCUSATIVE))
        assertEquals("dem rostigen Schlüssel", key.definite(Case.DATIVE))
        assertEquals("die alte Laterne", lantern.definite(Case.ACCUSATIVE))
        assertEquals("der alten Laterne", lantern.definite(Case.DATIVE))
        assertEquals("das kleine Messer", knife.definite(Case.ACCUSATIVE))
        assertEquals("dem kleinen Messer", knife.definite(Case.DATIVE))
    }

    @Test
    fun `German indefinite forms follow the mixed declension`() {
        assertEquals("ein rostiger Schlüssel", key.indefinite(Case.NOMINATIVE))
        assertEquals("einen rostigen Schlüssel", key.indefinite(Case.ACCUSATIVE))
        assertEquals("eine alte Laterne", lantern.indefinite(Case.NOMINATIVE))
        assertEquals("ein kleines Messer", knife.indefinite(Case.ACCUSATIVE))
        assertEquals("einem kleinen Messer", knife.indefinite(Case.DATIVE))
    }

    @Test
    fun `adjective stems ending in e are not doubled`() {
        val traveller = GermanNoun("Wanderer", Gender.MASCULINE, adjectives = listOf("müde"))
        assertEquals("den müden Wanderer", traveller.definite(Case.ACCUSATIVE))
        assertEquals("ein müder Wanderer", traveller.indefinite(Case.NOMINATIVE))
    }

    @Test
    fun `n-declension overrides and proper names`() {
        val lord = GermanNoun("Herr", Gender.MASCULINE, accusative = "Herrn", dative = "Herrn")
        assertEquals("den Herrn", lord.definite(Case.ACCUSATIVE))
        assertEquals("einem Herrn", lord.indefinite(Case.DATIVE))
        val marta = GermanNoun("Wirtin", Gender.FEMININE, name = "Marta")
        assertEquals("Marta", marta.definite(Case.DATIVE))
    }

    @Test
    fun `English articles`() {
        assertEquals("the rusty key", EnglishNoun("key", listOf("rusty")).definite())
        assertEquals("a rusty key", EnglishNoun("key", listOf("rusty")).indefinite())
        assertEquals("an old lantern", EnglishNoun("lantern", listOf("old")).indefinite())
        assertEquals("some bread", EnglishNoun("bread", indefiniteArticle = "some").indefinite())
        assertEquals("Marta", EnglishNoun("innkeeper", name = "Marta").indefinite())
    }

    @Test
    fun `lists are joined naturally`() {
        assertEquals("a", joinNatural(listOf("a"), "und"))
        assertEquals("a und b", joinNatural(listOf("a", "b"), "und"))
        assertEquals("a, b and c", joinNatural(listOf("a", "b", "c"), "and"))
    }
}
