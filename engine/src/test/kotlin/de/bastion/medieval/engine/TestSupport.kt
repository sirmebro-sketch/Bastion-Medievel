package de.bastion.medieval.engine

/** Shared fixtures: the bundled content and a ready-made character. */
internal object TestSupport {
    val world: World = World.loadDefault()
    val rules: CharacterRules = CharacterRules.loadDefault()

    /** 15/14/13/12/10/8, a legal point-buy spread. */
    val standardScores: Map<Ability, Int> = mapOf(
        Ability.STRENGTH to 15,
        Ability.CONSTITUTION to 14,
        Ability.DEXTERITY to 13,
        Ability.WISDOM to 12,
        Ability.CHARISMA to 10,
        Ability.INTELLIGENCE to 8,
    )

    fun character(
        career: String = "brawler",
        species: String = "dwarf",
        background: String = "gutter_child",
        gender: CharacterGender = CharacterGender.MALE,
        traits: List<String> = listOf("tough"),
        name: String = "Konrad",
        extraSkill: Skill? = null,
        scores: Map<Ability, Int> = standardScores,
        xp: Int = 0,
    ) = Character(
        name = name,
        gender = gender,
        species = species,
        career = career,
        background = background,
        traits = traits,
        baseScores = scores,
        extraSkill = extraSkill,
        coins = rules.career(career).coins + rules.background(background).coins,
        xp = xp,
    )

    /** A game past character creation, standing at the start. */
    fun playing(character: Character = character(), seed: Long = 1): Game {
        val background = rules.background(character.background)
        val state = GameState.new(world, seed).copy(character = character, creation = null, flags = background.flags.toSet())
        return Game(world, rules, state)
    }
}

internal fun Game.say(input: String, language: Language = Language.DE): String =
    submit(input, language).joinToString("\n") { it.text }
