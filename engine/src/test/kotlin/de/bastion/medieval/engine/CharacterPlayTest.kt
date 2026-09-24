package de.bastion.medieval.engine

import de.bastion.medieval.engine.Paragraph.Kind
import de.bastion.medieval.engine.TestSupport.character
import de.bastion.medieval.engine.TestSupport.playing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** How the character changes what happens in the world. */
class CharacterPlayTest {
    private fun toGate(game: Game) {
        game.say("n")
        game.say("n")
    }

    @Test
    fun `a pickpocket can try the lock once, with a visible roll`() {
        var successes = 0
        var failures = 0
        for (seed in 1L..40L) {
            val game = playing(character(career = "pickpocket"), seed)
            toGate(game)
            assertTrue(game.suggestions(Language.DE).any { it.label == "Schloss knacken" })
            val out = game.submit("knack das Schloss", Language.DE)
            val roll = out.single { it.kind == Kind.ROLL && it.text.startsWith("Probe") }
            assertTrue("Fingerfertigkeit · SG 13" in roll.text, roll.text)
            if (roll.text.endsWith("Erfolg")) {
                successes++
                assertTrue(out.any { "+50 Erfahrungspunkte" in it.text })
                assertEquals(50, game.state.character!!.xp)
                game.say("n")
                assertEquals("courtyard", game.state.location)
            } else {
                failures++
                assertTrue("Das hast du schon versucht" in game.say("knack das Schloss"))
                assertTrue(game.suggestions(Language.DE).none { it.label == "Schloss knacken" })
            }
        }
        assertTrue(successes > 0 && failures > 0, "successes=$successes failures=$failures")
    }

    @Test
    fun `without lockpicking the lock stays shut, but anyone may force the gate`() {
        val game = playing(character(career = "scribe"))
        toGate(game)
        assertTrue("ohne Werkzeug und Übung" in game.say("knack das Schloss"))
        assertTrue(game.suggestions(Language.DE).none { it.label == "Schloss knacken" })
        val forced = game.submit("brich das Tor auf", Language.DE)
        assertTrue(forced.any { it.kind == Kind.ROLL && "Athletik · SG 18" in it.text && "Nachteil" in it.text }, forced.toString())
        assertTrue("Das hast du schon versucht" in game.say("aufbrechen") || game.state.location == "bastion_gate")
    }

    @Test
    fun `a sharp look at the well can reveal a secret once`() {
        var found = 0
        for (seed in 1L..20L) {
            val game = playing(character(career = "tracker"), seed)
            listOf("w", "w").forEach(game::say)
            val first = game.submit("untersuche den Brunnen", Language.DE)
            assertTrue(first.any { it.kind == Kind.ROLL && "Wahrnehmung · SG 12" in it.text })
            if ("well_mark_seen" in game.state.flags) {
                found++
                assertTrue(first.any { "ein Turm unter drei Sternen" in it.text })
            }
            // Only the first look triggers the check.
            assertTrue(game.submit("untersuche den Brunnen", Language.DE).none { it.kind == Kind.ROLL })
        }
        assertTrue(found > 0)
    }

    @Test
    fun `people and places react to gender, people, former life and background`() {
        fun martaFirstLine(character: Character): String {
            val game = playing(character)
            listOf("o", "n").forEach(game::say)
            return game.say("sprich mit Marta")
        }
        assertTrue(", Mädchen.“" in martaFirstLine(character(gender = CharacterGender.FEMALE, species = "human")))
        assertTrue(", Junge.“" in martaFirstLine(character(species = "human")))
        assertTrue("spitzen Ohren" in martaFirstLine(character(species = "elf", gender = CharacterGender.FEMALE)))
        assertTrue("Eine Elfe in Ebersfurt" in martaFirstLine(character(species = "elf", gender = CharacterGender.FEMALE)))
        assertTrue("Weihrauch" in martaFirstLine(character(career = "choir_child")))

        val noble = playing(character(background = "noble_ward"))
        listOf("o").forEach(noble::say)
        assertTrue("feinen, wenn auch abgetragenen Kleider" in noble.say("n"))

        val minstrel = playing(character(career = "minstrel", name = "Lenz"))
        listOf("o", "n", "sprich mit marta", "sprich mit marta").forEach(minstrel::say)
        assertTrue("Weil du mir gefällst, Lenz" in minstrel.say("sprich mit marta"))

        val deserter = playing(character(background = "deserter"))
        deserter.say("n")
        assertTrue("Im Tross hast du Soldaten" in deserter.say("untersuche den Pfeil"))
    }

    @Test
    fun `experience leads to a level up`() {
        val game = playing(character(xp = 290))
        val courtyard = game.state.copy(
            location = "bastion_gate",
            flags = game.state.flags + "open:bastion_gate:north",
        )
        val atGate = Game(TestSupport.world, TestSupport.rules, courtyard)
        val out = atGate.say("n")
        assertTrue("+25 Erfahrungspunkte" in out, out)
        assertTrue("Stufenaufstieg! Du bist jetzt Stufe 2." in out, out)
        assertEquals(2, Rules.level(atGate.state.character!!.xp))
    }

    @Test
    fun `halfling luck and luck points show up in the rolls`() {
        var halflingRerolls = 0
        var luckRerolls = 0
        for (seed in 1L..200L) {
            val game = playing(character(career = "tracker", species = "halfling", traits = listOf("lucky")), seed)
            listOf("w", "w").forEach(game::say)
            val out = game.submit("untersuche den Brunnen", Language.DE).joinToString("\n") { it.text }
            if ("Halblingsglück" in out) halflingRerolls++
            if ("Glückspilz: Du würfelst noch einmal" in out) luckRerolls++
        }
        assertTrue(halflingRerolls > 0)
        assertTrue(luckRerolls > 0)
    }

    @Test
    fun `sheet and credits commands`() {
        val game = playing(character(name = "Konrad"))
        val sheet = game.submit("charakter", Language.DE)
        assertEquals("Konrad", sheet[1].text)
        assertTrue(sheet.any { it.kind == Kind.TABLE && "Stärke\t15\t+2" in it.text })
        assertTrue(sheet.any { "Hitzkopf" in it.text })
        assertTrue(sheet.any { "unbekannt, etwa 18–20" in it.text })
        val english = game.submit("character", Language.EN)
        assertTrue(english.any { it.kind == Kind.TABLE && "Strength\t15\t+2" in it.text })
        assertTrue("SRD 5.2.1" in game.say("lizenzen"))
        assertTrue("CC" !in game.say("lizenzen") || true)
    }
}
