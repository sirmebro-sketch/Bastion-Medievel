package de.bastion.medieval.engine

import de.bastion.medieval.engine.Parsed.Command
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {
    private val parser = Parser()

    private fun de(input: String) = parser.parse(input, Language.DE)
    private fun en(input: String) = parser.parse(input, Language.EN)

    @Test
    fun `German movement`() {
        assertEquals(Command(Verb.GO, direction = Direction.NORTH), de("geh nach Norden"))
        assertEquals(Command(Verb.GO, direction = Direction.NORTH), de("n"))
        assertEquals(Command(Verb.GO, direction = Direction.EAST), de("Osten"))
        assertEquals(Command(Verb.GO, direction = Direction.EAST), de("o"))
        assertEquals(Command(Verb.GO, direction = Direction.WEST), de("ich gehe nach Westen"))
        assertEquals(Command(Verb.GO, direction = Direction.SOUTH), de("lass uns nach Süden gehen"))
        assertEquals(Command(Verb.GO, direction = Direction.SOUTH), de("nach sueden"))
        assertEquals(Command(Verb.GO, direction = Direction.UP), de("klettere hinauf"))
        assertEquals(Command(Verb.GO, direction = Direction.IN), de("tritt ein"))
        assertEquals(Command(Verb.GO, listOf("schaenke")), de("geh in die Schänke"))
        assertEquals(Command(Verb.BACK), de("zurück"))
        assertEquals(Command(Verb.BACK), de("geh zurück"))
        assertEquals(Command(Verb.BACK), de("kehr um"))
    }

    @Test
    fun `German separable verbs and infinitives`() {
        assertEquals(Command(Verb.LOOK), de("schau dich um"))
        assertEquals(Command(Verb.LOOK), de("sieh dich um"))
        assertEquals(Command(Verb.EXAMINE, listOf("wegstein")), de("sieh dir den Wegstein an"))
        assertEquals(Command(Verb.EXAMINE, listOf("wegstein")), de("schau dir den Wegstein genauer an"))
        assertEquals(Command(Verb.TAKE, listOf("schluessel")), de("heb den Schlüssel auf"))
        assertEquals(Command(Verb.TAKE, listOf("schluessel")), de("ich möchte den Schlüssel aufheben"))
        assertEquals(Command(Verb.DROP, listOf("schluessel")), de("leg den Schlüssel ab"))
        assertEquals(Command(Verb.DROP, listOf("schluessel")), de("lass den Schlüssel fallen"))
        assertEquals(Command(Verb.TALK, listOf("marta")), de("sprich Marta an"))
        assertEquals(Command(Verb.OPEN, listOf("tor")), de("schließ das Tor auf"))
        assertEquals(Command(Verb.OPEN, listOf("tor")), de("öffne das Tor"))
    }

    @Test
    fun `German everyday commands`() {
        assertEquals(Command(Verb.TAKE, listOf("rostigen", "schluessel")), de("Nimm den rostigen Schlüssel!"))
        assertEquals(Command(Verb.EXAMINE, listOf("wegstein")), de("untersuche den Wegstein"))
        assertEquals(Command(Verb.TALK, listOf("wirtin")), de("rede mit der Wirtin"))
        assertEquals(Command(Verb.INVENTORY), de("i"))
        assertEquals(Command(Verb.INVENTORY), de("Inventar"))
        assertEquals(Command(Verb.LOOK), de("l"))
        assertEquals(Command(Verb.HELP), de("hilfe"))
        assertEquals(Command(Verb.HELP), de("?"))
        assertEquals(Command(Verb.WAIT), de("warte"))
    }

    @Test
    fun `English commands`() {
        assertEquals(Command(Verb.GO, direction = Direction.NORTH), en("go north"))
        assertEquals(Command(Verb.GO, direction = Direction.EAST), en("e"))
        assertEquals(Command(Verb.GO, direction = Direction.WEST), en("I want to go west"))
        assertEquals(Command(Verb.GO, listOf("tavern"), Direction.IN), en("go in the tavern"))
        assertEquals(Command(Verb.GO, direction = Direction.IN), en("go in"))
        assertEquals(Command(Verb.BACK), en("go back"))
        assertEquals(Command(Verb.LOOK), en("look around"))
        assertEquals(Command(Verb.EXAMINE, listOf("waystone")), en("look at the waystone"))
        assertEquals(Command(Verb.EXAMINE, listOf("stone")), en("x stone"))
        assertEquals(Command(Verb.TAKE, listOf("rusty", "key")), en("pick up the rusty key"))
        assertEquals(Command(Verb.TAKE, listOf("key")), en("pick the key up"))
        assertEquals(Command(Verb.DROP, listOf("key")), en("put down the key"))
        assertEquals(Command(Verb.TALK, listOf("marta")), en("talk to Marta"))
        assertEquals(Command(Verb.OPEN, listOf("gate")), en("unlock the gate"))
        assertEquals(Command(Verb.INVENTORY), en("i"))
        assertEquals(Command(Verb.INVENTORY), en("inventory"))
    }

    @Test
    fun `the other language is understood as a fallback`() {
        assertEquals(Command(Verb.TAKE, listOf("key")), de("take the key"))
        assertEquals(Command(Verb.GO, direction = Direction.NORTH), en("geh nach Norden"))
    }

    @Test
    fun `input without a verb becomes an implicit look at that thing`() {
        assertEquals(Command(Verb.EXAMINE, listOf("schluessel"), implicit = true), de("Schlüssel"))
        assertEquals(Parsed.NotUnderstood, de("der die das"))
        assertEquals(Parsed.Empty, de("   "))
    }
}
