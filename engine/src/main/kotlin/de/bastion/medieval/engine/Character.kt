package de.bastion.medieval.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CharacterGender(val displayName: LocalizedText) {
    @SerialName("m") MALE(LocalizedText("männlich", "male")),
    @SerialName("f") FEMALE(LocalizedText("weiblich", "female")),
}

/**
 * The player character as chosen in character creation plus what changed since.
 * Everything derived (final scores, bonuses, hit points) lives in [Sheet].
 */
@Serializable
data class Character(
    val name: String,
    val gender: CharacterGender,
    val species: String,
    val career: String,
    val background: String,
    val traits: List<String>,
    /** Point-buy scores before background and trait bonuses. */
    val baseScores: Map<Ability, Int>,
    /** The human's extra skill of choice. */
    val extraSkill: Skill? = null,
    val coins: Int = 0,
    val xp: Int = 0,
    val damage: Int = 0,
    val luckUsed: Int = 0,
)

/** Everything the rules derive from a [Character]. */
class Sheet(val character: Character, val rules: CharacterRules) {
    val species: SpeciesDef = rules.species(character.species)
    val career: CareerDef = rules.career(character.career)
    val background: BackgroundDef = rules.background(character.background)
    val traits: List<TraitDef> = character.traits.map(rules::trait)

    val level: Int = Rules.level(character.xp)
    val proficiencyBonus: Int = Rules.proficiencyBonus(level)

    fun score(ability: Ability): Int {
        val bonus = (background.bonus[ability] ?: 0) + traits.sumOf { it.abilityBonus[ability] ?: 0 }
        return minOf(Rules.MAX_SCORE, (character.baseScores[ability] ?: Rules.POINT_BUY_MIN) + bonus)
    }

    fun modifier(ability: Ability): Int = Rules.modifier(score(ability))

    val proficientSkills: Set<Skill> =
        (career.skills + species.skills + traits.flatMap { it.skills } + listOfNotNull(character.extraSkill)).toSet()

    fun skillBonus(skill: Skill): Int =
        modifier(skill.ability) + if (skill in proficientSkills) proficiencyBonus else 0

    private val advantages: Set<Skill> = (career.advantages + species.advantages + traits.flatMap { it.advantages }).toSet()
    private val disadvantages: Set<Skill> = career.weakness.disadvantage.toSet()

    fun edge(skill: Skill): Edge = Rules.combine(skill in advantages, skill in disadvantages)

    /** Ids of every special ability the character has, for conditions and obstacles. */
    val features: Set<String> =
        (species.features.map { it.id } + career.feature.id + traits.flatMap { t -> t.features.map { it.id } }).toSet()

    val maxHp: Int
        get() {
            val con = modifier(Ability.CONSTITUTION)
            val perLevelBonus = species.hpPerLevel + traits.sumOf { it.hpPerLevel }
            val first = maxOf(1, career.hitDie + con)
            val later = (level - 1) * maxOf(1, career.hitDie / 2 + 1 + con)
            return first + later + perLevelBonus * level
        }

    val hp: Int get() = maxOf(0, maxHp - character.damage)

    /** Luck points (trait "Glückspilz"): as many as the proficiency bonus, refilled on level up. */
    val luckLeft: Int
        get() = if (traits.any { it.luck }) maxOf(0, proficiencyBonus - character.luckUsed) else 0

    val equipment: List<LocalizedText> = career.equipment + background.equipment
}
