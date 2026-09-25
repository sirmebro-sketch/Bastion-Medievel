package de.bastion.medieval.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterRulesTest {
    private val rules = TestSupport.rules

    @Test
    fun `bundled character content is consistent in both languages`() {
        assertEquals(emptyList(), rules.validate())
        assertEquals(emptyList(), TestSupport.world.validate(rules))
    }

    @Test
    fun `the agreed peoples, former lives and backgrounds exist`() {
        assertEquals(listOf("human", "dwarf", "elf", "halfling", "earthling"), rules.species.map { it.id })
        assertTrue(rules.species("earthling").placeholder)
        assertEquals(
            listOf(
                "brawler", "pickpocket", "tracker", "herbalist", "scribe", "minstrel",
                "artisan", "merchant", "artist", "squire", "choir_child",
            ),
            rules.careers.map { it.id },
        )
        assertEquals(6, rules.backgrounds.size)
        // Every former life has its own weakness on a different skill.
        assertEquals(rules.careers.size, rules.careers.flatMap { it.weakness.disadvantage }.toSet().size)
    }

    @Test
    fun `names follow the chosen gender`() {
        val male = TextContext(null, gender = CharacterGender.MALE)
        val female = TextContext(null, gender = CharacterGender.FEMALE)
        val choir = rules.career("choir_child").name
        assertEquals("Chorknabe", choir.render(Language.DE, male))
        assertEquals("Chormädchen", choir.render(Language.DE, female))
        assertEquals("Choirgirl", choir.render(Language.EN, female))
        assertEquals("Knappin", rules.career("squire").name.render(Language.DE, female))
        assertEquals("Harte", rules.species("dwarf").name.render(Language.DE, female))
    }

    @Test
    fun `the sheet adds background bonuses and derives skills and hit points`() {
        val character = TestSupport.character(career = "pickpocket", species = "halfling", background = "gutter_child", traits = listOf("tough"))
        val sheet = Sheet(character, rules)
        // Base DEX 13 + 2 from the gutter child background.
        assertEquals(15, sheet.score(Ability.DEXTERITY))
        assertEquals(2, sheet.modifier(Ability.DEXTERITY))
        // Trained: DEX +2 plus proficiency +2.
        assertEquals(4, sheet.skillBonus(Skill.STEALTH))
        assertEquals(Edge.ADVANTAGE, sheet.edge(Skill.STEALTH))
        assertEquals(Edge.DISADVANTAGE, sheet.edge(Skill.INVESTIGATION))
        assertTrue("lockpicking" in sheet.features)
        // d8 + CON 14 (+2) + tough 2 per level.
        assertEquals(12, sheet.maxHp)
        assertEquals(1, sheet.level)
    }
}
