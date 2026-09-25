package de.bastion.medieval.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Rules adapted from the System Reference Document 5.2.1 (CC-BY-4.0), see Credits.
// German names are the official ones from the German SRD 5.2.1.

private fun t(de: String, en: String) = LocalizedText(de, en)

/** The six abilities. [short] are the abbreviations used in stat blocks. */
@Serializable
enum class Ability(val displayName: LocalizedText, val short: LocalizedText) {
    @SerialName("str") STRENGTH(t("Stärke", "Strength"), t("STÄ", "STR")),
    @SerialName("dex") DEXTERITY(t("Geschick", "Dexterity"), t("GES", "DEX")),
    @SerialName("con") CONSTITUTION(t("Konstitution", "Constitution"), t("KON", "CON")),
    @SerialName("int") INTELLIGENCE(t("Intelligenz", "Intelligence"), t("INT", "INT")),
    @SerialName("wis") WISDOM(t("Weisheit", "Wisdom"), t("WEI", "WIS")),
    @SerialName("cha") CHARISMA(t("Charisma", "Charisma"), t("CHA", "CHA")),
}

/** The 18 skills and the ability each one uses. */
@Serializable
enum class Skill(val ability: Ability, val displayName: LocalizedText) {
    @SerialName("acrobatics") ACROBATICS(Ability.DEXTERITY, t("Akrobatik", "Acrobatics")),
    @SerialName("arcana") ARCANA(Ability.INTELLIGENCE, t("Arkane Kunde", "Arcana")),
    @SerialName("athletics") ATHLETICS(Ability.STRENGTH, t("Athletik", "Athletics")),
    @SerialName("performance") PERFORMANCE(Ability.CHARISMA, t("Auftreten", "Performance")),
    @SerialName("intimidation") INTIMIDATION(Ability.CHARISMA, t("Einschüchtern", "Intimidation")),
    @SerialName("sleight_of_hand") SLEIGHT_OF_HAND(Ability.DEXTERITY, t("Fingerfertigkeit", "Sleight of Hand")),
    @SerialName("history") HISTORY(Ability.INTELLIGENCE, t("Geschichte", "History")),
    @SerialName("medicine") MEDICINE(Ability.WISDOM, t("Heilkunde", "Medicine")),
    @SerialName("stealth") STEALTH(Ability.DEXTERITY, t("Heimlichkeit", "Stealth")),
    @SerialName("animal_handling") ANIMAL_HANDLING(Ability.WISDOM, t("Mit Tieren umgehen", "Animal Handling")),
    @SerialName("insight") INSIGHT(Ability.WISDOM, t("Motiv erkennen", "Insight")),
    @SerialName("investigation") INVESTIGATION(Ability.INTELLIGENCE, t("Nachforschungen", "Investigation")),
    @SerialName("nature") NATURE(Ability.INTELLIGENCE, t("Naturkunde", "Nature")),
    @SerialName("religion") RELIGION(Ability.INTELLIGENCE, t("Religion", "Religion")),
    @SerialName("deception") DECEPTION(Ability.CHARISMA, t("Täuschen", "Deception")),
    @SerialName("survival") SURVIVAL(Ability.WISDOM, t("Überlebenskunst", "Survival")),
    @SerialName("persuasion") PERSUASION(Ability.CHARISMA, t("Überzeugen", "Persuasion")),
    @SerialName("perception") PERCEPTION(Ability.WISDOM, t("Wahrnehmung", "Perception")),
}

/** Advantage: roll two d20 and keep the higher; disadvantage: keep the lower. */
enum class Edge { NONE, ADVANTAGE, DISADVANTAGE }

object Rules {
    const val POINT_BUY_BUDGET = 27
    const val POINT_BUY_MIN = 8
    const val POINT_BUY_MAX = 15
    const val MAX_SCORE = 20
    const val MAX_LEVEL = 20

    /** The standard array; the recommended point-buy spread uses it (it costs exactly 27). */
    val STANDARD_ARRAY = listOf(15, 14, 13, 12, 10, 8)

    private val POINT_COSTS = mapOf(8 to 0, 9 to 1, 10 to 2, 11 to 3, 12 to 4, 13 to 5, 14 to 7, 15 to 9)

    /** Experience needed for levels 1 to 20. */
    private val XP_FOR_LEVEL = listOf(
        0, 300, 900, 2_700, 6_500, 14_000, 23_000, 34_000, 48_000, 64_000,
        85_000, 100_000, 120_000, 140_000, 165_000, 195_000, 225_000, 265_000, 305_000, 355_000,
    )

    fun pointCost(score: Int): Int = POINT_COSTS[score] ?: throw IllegalArgumentException("No point-buy cost for $score")

    fun pointsSpent(scores: Map<Ability, Int>): Int = Ability.entries.sumOf { pointCost(scores[it] ?: POINT_BUY_MIN) }

    fun modifier(score: Int): Int = Math.floorDiv(score - 10, 2)

    fun level(xp: Int): Int = XP_FOR_LEVEL.indexOfLast { xp >= it } + 1

    /** Experience needed to reach [level], or null beyond the last level. */
    fun xpFor(level: Int): Int? = XP_FOR_LEVEL.getOrNull(level - 1)

    fun proficiencyBonus(level: Int): Int = 2 + (level - 1) / 4

    /** Combines several sources: advantage and disadvantage together cancel out. */
    fun combine(advantage: Boolean, disadvantage: Boolean): Edge = when {
        advantage && !disadvantage -> Edge.ADVANTAGE
        disadvantage && !advantage -> Edge.DISADVANTAGE
        else -> Edge.NONE
    }

    /** "+2", "+0", "−1" with a real minus sign. */
    fun signed(value: Int): String = if (value >= 0) "+$value" else "−${-value}"
}

/**
 * Deterministic dice: roll number [index] of a game with [seed] always gives the same
 * result, so saves and tests are reproducible (SplitMix64).
 */
object Dice {
    fun roll(seed: Long, index: Int, sides: Int): Int {
        var z = seed + (index + 1) * -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        z = z xor (z ushr 31)
        return Math.floorMod(z, sides.toLong()).toInt() + 1
    }
}
