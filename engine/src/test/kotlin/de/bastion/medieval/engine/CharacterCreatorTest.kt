package de.bastion.medieval.engine

import de.bastion.medieval.engine.Paragraph.Kind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CharacterCreatorTest {
    private fun newGame() = Game(TestSupport.world, TestSupport.rules, GameState.new(TestSupport.world, seed = 7))

    private fun Game.step() = CharacterCreator(TestSupport.rules).step(requireNotNull(state.creation))

    @Test
    fun `a new game starts with character creation`() {
        val game = newGame()
        assertTrue(game.inCreation)
        val opening = game.opening(Language.DE)
        assertTrue(opening.first().text.startsWith("Bevor deine Geschichte beginnt"))
        assertEquals("Charaktererschaffung", game.locationName(Language.DE))
        assertEquals(listOf("Männlich", "Weiblich"), game.suggestions(Language.DE).map { it.label })
    }

    @Test
    fun `German creation from start to story`() {
        val game = newGame()
        game.say("weiblich")
        assertEquals(CreationStep.NAME, game.step())
        assertTrue("Wiebke also." in game.say("Ich heiße Wiebke"))
        assertTrue("Elfe" in game.say("Elfe"))
        val career = game.say("Langfinger")
        assertTrue("Schlösser knacken" in career, career)
        assertTrue("Ungebildet" in career, career)
        game.say("Gossenkind")
        assertEquals(CreationStep.ABILITIES, game.step())
        game.say("vorschlag")
        assertEquals(15, game.state.creation!!.scores[Ability.DEXTERITY])
        game.say("fertig")
        assertEquals(CreationStep.TRAITS, game.step())
        game.say("Wachsam")
        assertEquals(CreationStep.SUMMARY, game.step())
        val story = game.submit("los", Language.DE)
        assertNull(game.state.creation)
        val character = assertNotNull(game.state.character)
        assertEquals("Wiebke", character.name)
        assertEquals(CharacterGender.FEMALE, character.gender)
        assertEquals("elf", character.species)
        assertEquals("pickpocket", character.career)
        assertEquals(listOf("alert"), character.traits)
        assertTrue(story.any { it.kind == Kind.TITLE && it.text == "Kreuzweg im Nebel" })
        // The background's flags are set for the story.
        assertTrue("thieves_cant" in game.state.flags)
    }

    @Test
    fun `English creation with numbers, point buy commands and the human extras`() {
        val game = newGame()
        fun say(text: String) = game.say(text, Language.EN)
        say("male")
        say("my name is konrad")
        assertEquals("Konrad", game.state.creation!!.name)
        say("1") // human
        say("choirboy")
        say("3") // gutter child
        say("str 15 con 14 dex 13")
        assertEquals(15, game.state.creation!!.scores[Ability.STRENGTH])
        val notDone = say("done")
        assertTrue("points left" in notDone, notDone)
        say("wisdom 12")
        say("charisma +")
        say("charisma +")
        assertEquals(10, game.state.creation!!.scores[Ability.CHARISMA])
        say("done")
        assertEquals(CreationStep.TRAITS, game.step())
        val second = say("lucky")
        assertTrue("second trait" in second, second) // humans pick two
        say("tough")
        assertEquals(listOf("lucky", "tough"), game.state.creation!!.traits)
        assertEquals(CreationStep.EXTRA_SKILL, game.step())
        say("stealth")
        assertEquals(Skill.STEALTH, game.state.creation!!.extraSkill)
        assertEquals(CreationStep.SUMMARY, game.step())
        say("begin")
        val character = assertNotNull(game.state.character)
        assertEquals("choir_child", character.career)
        assertEquals(Skill.STEALTH, character.extraSkill)
    }

    @Test
    fun `point buy keeps within budget and range`() {
        val game = newGame()
        listOf("männlich", "Falk", "Zwerg", "Raufbold", "Deserteur").forEach(game::say)
        assertTrue("zwischen 8 und 15" in game.say("Stärke 16"))
        game.say("Stärke 15")
        game.say("Konstitution 15")
        game.say("Geschick 15")
        assertTrue("reichen deine Punkte nicht" in game.say("Weisheit 12"))
        assertEquals(27, Rules.pointsSpent(game.state.creation!!.scores))
        game.say("Stärke -")
        assertEquals(14, game.state.creation!!.scores[Ability.STRENGTH])
        // Table and status replace their previous versions.
        val output = game.submit("Stärke +", Language.DE)
        assertEquals(CharacterCreator.POINT_BUY_SLOT_INPUT, output.first().slot)
        assertTrue(output.any { it.kind == Kind.TABLE && it.slot == CharacterCreator.POINT_BUY_SLOT_TABLE })
    }

    @Test
    fun `back and changes from the summary`() {
        val game = newGame()
        listOf("weiblich", "Hedda", "Halbling").forEach(game::say)
        assertEquals(CreationStep.CAREER, game.step())
        game.say("zurück")
        assertEquals(CreationStep.SPECIES, game.step())
        listOf("Zwergin", "Knappin", "Schuldnerin", "vorschlag", "fertig", "Kräftig").forEach(game::say)
        assertEquals(CreationStep.SUMMARY, game.step())
        game.say("Werdegang ändern")
        assertEquals(CreationStep.CAREER, game.step())
        game.say("Handwerkerin")
        // Only the changed choice is asked again.
        assertEquals(CreationStep.SUMMARY, game.step())
        assertEquals("artisan", game.state.creation!!.career)
        // Changing to a human asks for the extras.
        game.say("Volk ändern")
        game.say("Mensch")
        assertEquals(CreationStep.TRAITS, game.step())
    }

    @Test
    fun `every suggestion during creation is understood`() {
        val game = newGame()
        val answers = listOf("weiblich", "Ida", "Mensch", "Künstlerin", "Grenzlandflüchtling", "vorschlag", "fertig", "Flink", "Zäh", "Religion")
        for (answer in answers) {
            val creation = requireNotNull(game.state.creation)
            for (suggestion in game.suggestions(Language.DE)) {
                val probe = Game(TestSupport.world, TestSupport.rules, game.state)
                val out = probe.submit(suggestion.command, Language.DE)
                assertTrue(out.none { it.text == "Das habe ich nicht verstanden." }, "${suggestion.command} at ${CharacterCreator(TestSupport.rules).step(creation)}")
            }
            game.say(answer)
        }
        assertEquals(CreationStep.SUMMARY, game.step())
    }
}
