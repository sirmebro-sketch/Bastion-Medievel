package de.bastion.medieval.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RulesTest {
    @Test
    fun `modifiers follow the SRD table`() {
        assertEquals(-5, Rules.modifier(1))
        assertEquals(-1, Rules.modifier(8))
        assertEquals(-1, Rules.modifier(9))
        assertEquals(0, Rules.modifier(10))
        assertEquals(0, Rules.modifier(11))
        assertEquals(2, Rules.modifier(15))
        assertEquals(5, Rules.modifier(20))
        assertEquals("−1", Rules.signed(-1))
        assertEquals("+0", Rules.signed(0))
    }

    @Test
    fun `point buy costs 27 for the standard array`() {
        assertEquals(27, Rules.STANDARD_ARRAY.sumOf(Rules::pointCost))
        assertEquals(0, Rules.pointsSpent(Ability.entries.associateWith { 8 }))
        assertEquals(7, Rules.pointCost(14))
        assertEquals(9, Rules.pointCost(15))
    }

    @Test
    fun `levels and proficiency follow the SRD advancement table`() {
        assertEquals(1, Rules.level(0))
        assertEquals(1, Rules.level(299))
        assertEquals(2, Rules.level(300))
        assertEquals(5, Rules.level(6_500))
        assertEquals(20, Rules.level(1_000_000))
        assertEquals(300, Rules.xpFor(2))
        assertEquals(null, Rules.xpFor(21))
        assertEquals(2, Rules.proficiencyBonus(1))
        assertEquals(2, Rules.proficiencyBonus(4))
        assertEquals(3, Rules.proficiencyBonus(5))
        assertEquals(6, Rules.proficiencyBonus(20))
    }

    @Test
    fun `advantage and disadvantage cancel out`() {
        assertEquals(Edge.ADVANTAGE, Rules.combine(advantage = true, disadvantage = false))
        assertEquals(Edge.DISADVANTAGE, Rules.combine(advantage = false, disadvantage = true))
        assertEquals(Edge.NONE, Rules.combine(advantage = true, disadvantage = true))
    }

    @Test
    fun `dice are deterministic and roughly fair`() {
        assertEquals(Dice.roll(42, 7, 20), Dice.roll(42, 7, 20))
        val rolls = (0 until 20_000).map { Dice.roll(1234, it, 20) }
        assertTrue(rolls.all { it in 1..20 })
        val counts = rolls.groupingBy { it }.eachCount()
        assertEquals(20, counts.size)
        // Each face should come up about 1000 times.
        assertTrue(counts.values.all { it in 850..1150 }, counts.toString())
    }
}
