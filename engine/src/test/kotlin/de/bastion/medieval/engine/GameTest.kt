package de.bastion.medieval.engine

import de.bastion.medieval.engine.Paragraph.Kind
import de.bastion.medieval.engine.TestSupport.playing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameTest {
    private val world = TestSupport.world

    @Test
    fun `German walkthrough of the test area`() {
        val game = playing()
        val opening = game.opening(Language.DE)
        assertEquals(Kind.TITLE, opening[1].kind)
        assertEquals("Kreuzweg im Nebel", opening[1].text)

        game.say("geh nach Norden")
        assertEquals("sunken_lane", game.state.location)
        game.say("n")
        assertEquals("bastion_gate", game.state.location)
        assertTrue("ohne Schlüssel" in game.say("geh durch das Tor"))
        assertEquals("bastion_gate", game.state.location)

        game.say("s")
        game.say("s")
        game.say("geh in den Wald")
        assertEquals("forest_edge", game.state.location)
        assertTrue("Alter Brunnen" in game.say("geh zum Brunnen"))
        assertTrue("einen rostigen Schlüssel" in game.say("schau dich um"))
        assertEquals("Du nimmst den rostigen Schlüssel.", game.say("nimm den rostigen Schlüssel").lines().last())
        assertEquals(GameState.INVENTORY, game.state.itemPlaces["rusty_key"])

        game.say("o")
        game.say("o")
        game.say("n")
        game.say("n")
        val gate = game.say("n")
        assertTrue("Der rostige Schlüssel passt" in gate, gate)
        assertEquals("courtyard", game.state.location)

        // The gate stays open: no second unlock message.
        game.say("s")
        assertTrue("Der rostige Schlüssel passt" !in game.say("n"))
    }

    @Test
    fun `English play uses the same state`() {
        val game = playing()
        game.say("go east", Language.EN)
        assertEquals("village_square", game.state.location)
        val tavern = game.say("go in the tavern", Language.EN)
        assertTrue("The Crooked Horn Tavern" in tavern, tavern)
        assertTrue("Marta is here." in tavern, tavern)
        assertEquals("You take the bread.", game.say("take bread", Language.EN).lines().last())
        assertEquals("You are carrying some bread.", game.say("i", Language.EN).lines().last())
        assertEquals("Du trägst bei dir: einen Brotlaib.", game.say("inventar").lines().last())
    }

    @Test
    fun `talking cycles through lines and repeats the last one`() {
        val game = playing()
        game.say("o")
        game.say("n")
        val first = game.say("sprich mit der Wirtin")
        val second = game.say("rede mit Marta")
        val third = game.say("sprich mit marta")
        val fourth = game.say("sprich mit marta")
        assertTrue("Fremde kommen selten" in first)
        assertTrue("Brunnen im Wald" in second)
        assertTrue("Mehr weiß ich nicht" in third)
        assertEquals(third.lines().last(), fourth.lines().last())
        // With only one person present, "sprich" alone is enough.
        assertEquals(Kind.DIALOGUE, game.submit("sprich", Language.DE).last().kind)
    }

    @Test
    fun `things and typos`() {
        val game = playing()
        assertTrue("Die Inschrift ist fast verwittert" in game.say("untersuche den wegstien"))
        assertTrue("Die Inschrift" in game.say("Wegstein"))
        assertEquals("Der verwitterte Wegstein lässt sich nicht mitnehmen.", game.say("nimm den Stein").lines().last())
        assertEquals("Das siehst du hier nirgends.", game.say("nimm das Schwert").lines().last())
        assertEquals("Das verstehe ich nicht.", game.say("singe ein Lied").lines()[1])
    }

    @Test
    fun `dropping puts things into the current location`() {
        val game = playing()
        game.say("n")
        game.say("nimm den Pfeil")
        game.say("s")
        assertEquals("Du legst den abgebrochenen Pfeil ab.", game.say("leg den Pfeil ab").lines().last())
        assertEquals("crossroads", game.state.itemPlaces["broken_arrow"])
        assertEquals("Den abgebrochenen Pfeil trägst du nicht bei dir.", game.say("leg den Pfeil ab").lines().last())
    }

    @Test
    fun `locked paths can be opened without walking through`() {
        val game = playing()
        assertEquals("Der verwitterte Wegstein lässt sich nicht öffnen.", game.say("öffne den Wegstein").lines().last())
        game.say("n")
        game.say("n")
        assertTrue("ohne Schlüssel" in game.say("schließ das Tor auf"))
        val withKey = Game(world, TestSupport.rules, game.state.copy(itemPlaces = game.state.itemPlaces + ("rusty_key" to GameState.INVENTORY)))
        assertTrue("The rusty key fits" in withKey.say("unlock the gate", Language.EN))
        assertEquals("bastion_gate", withKey.state.location)
        assertEquals("Der Weg dorthin ist bereits offen.", withKey.say("öffne das Tor").lines().last())
        assertTrue("Der rostige Schlüssel passt" !in withKey.say("n"))
        assertEquals("courtyard", withKey.state.location)
        assertEquals("Da gibt es nichts aufzuschließen – der Weg ist frei.", withKey.say("öffne das Tor").lines().last())
    }

    @Test
    fun `back returns to the previous location`() {
        val game = playing()
        assertEquals("Du weißt nicht mehr genau, woher du gekommen bist.", game.say("zurück").lines().last())
        game.say("w")
        game.say("zurück")
        assertEquals("crossroads", game.state.location)
    }

    @Test
    fun `suggestions offer exits, people and things`() {
        val game = playing()
        game.say("o")
        game.say("n")
        val labels = game.suggestions(Language.DE).map { it.label }
        assertTrue("Nach Süden" in labels, labels.toString())
        assertTrue("Hinaus" in labels, labels.toString())
        assertTrue("Mit Marta sprechen" in labels, labels.toString())
        assertTrue("Brotlaib nehmen" in labels, labels.toString())
        val take = game.suggestions(Language.EN).first { it.label == "Take bread" }
        assertEquals("take the bread", take.command)
        // Every suggestion must be understood by the game itself.
        for (language in Language.entries) {
            for (suggestion in game.suggestions(language)) {
                val probe = Game(world, TestSupport.rules, game.state)
                val answer = probe.submit(suggestion.command, language)
                assertTrue(answer.none { it.text == Messages.notUnderstood[language] }, "${suggestion.command} -> $answer")
            }
        }
    }
}
