package de.bastion.medieval.engine

import de.bastion.medieval.engine.Paragraph.Kind
import kotlinx.serialization.Serializable

enum class CreationStep { GENDER, NAME, SPECIES, CAREER, BACKGROUND, ABILITIES, TRAITS, EXTRA_SKILL, SUMMARY }

/**
 * The choices made so far. The current step is always the first one that is still
 * open, so going back or changing something from the summary just clears a choice.
 */
@Serializable
data class CreationState(
    val gender: CharacterGender? = null,
    val name: String? = null,
    val species: String? = null,
    val career: String? = null,
    val background: String? = null,
    val scores: Map<Ability, Int> = Ability.entries.associateWith { Rules.POINT_BUY_MIN },
    val scoresConfirmed: Boolean = false,
    val traits: List<String> = emptyList(),
    val extraSkill: Skill? = null,
)

/**
 * Character creation as a conversation: every step asks one question, the player
 * answers by typing (German or English) or by tapping a suggestion.
 */
class CharacterCreator(private val rules: CharacterRules) {

    sealed interface Result {
        data class Continue(val state: CreationState, val output: List<Paragraph>) : Result
        data class Finished(val character: Character, val output: List<Paragraph>) : Result
    }

    fun step(state: CreationState): CreationStep {
        val human = state.species?.let { rules.species(it) }
        return when {
            state.gender == null -> CreationStep.GENDER
            state.name == null -> CreationStep.NAME
            state.species == null -> CreationStep.SPECIES
            state.career == null -> CreationStep.CAREER
            state.background == null -> CreationStep.BACKGROUND
            !state.scoresConfirmed -> CreationStep.ABILITIES
            state.traits.size < traitsNeeded(state) -> CreationStep.TRAITS
            human?.extraSkill == true && state.extraSkill == null -> CreationStep.EXTRA_SKILL
            else -> CreationStep.SUMMARY
        }
    }

    fun intro(state: CreationState, language: Language): List<Paragraph> =
        listOf(Paragraph(Kind.TEXT, T.intro[language])) + prompt(state, language)

    /** The question for the current step, with its options. */
    fun prompt(state: CreationState, language: Language): List<Paragraph> {
        val ctx = context(state)
        return when (step(state)) {
            CreationStep.GENDER -> listOf(
                Paragraph(Kind.TITLE, T.genderTitle[language]),
                Paragraph(Kind.TEXT, T.genderQuestion[language]),
                Paragraph(Kind.HINT, T.genderHint[language]),
            )
            CreationStep.NAME -> listOf(
                Paragraph(Kind.TITLE, T.nameTitle[language]),
                Paragraph(Kind.TEXT, T.nameQuestion.render(language, ctx)),
                Paragraph(Kind.HINT, T.nameHint[language]),
            )
            CreationStep.SPECIES -> choice(T.speciesTitle, T.speciesQuestion, speciesOptions(), language, ctx)
            CreationStep.CAREER -> choice(T.careerTitle, T.careerQuestion, careerOptions(), language, ctx)
            CreationStep.BACKGROUND -> choice(T.backgroundTitle, T.backgroundQuestion, backgroundOptions(), language, ctx)
            CreationStep.ABILITIES -> listOf(
                Paragraph(Kind.TITLE, T.abilitiesTitle[language]),
                Paragraph(Kind.TEXT, T.abilitiesQuestion[language]),
            ) + pointBuyTable(state, language) + Paragraph(Kind.HINT, T.abilitiesHint[language])
            CreationStep.TRAITS -> {
                val question = if (state.traits.isEmpty()) T.traitsQuestion else T.secondTraitQuestion
                choice(T.traitsTitle, question, traitOptions(state), language, ctx)
            }
            CreationStep.EXTRA_SKILL -> choice(T.skillTitle, T.skillQuestion, skillOptions(state), language, ctx)
            CreationStep.SUMMARY -> SheetView.render(Sheet(draft(state), rules), language) +
                Paragraph(Kind.TEXT, T.summaryQuestion.render(language, ctx)) +
                Paragraph(Kind.HINT, T.summaryHint[language])
        }
    }

    fun suggestions(state: CreationState, language: Language): List<Suggestion> {
        val ctx = context(state)
        val back = Suggestion(T.backLabel[language], T.backCommand[language])
        return when (step(state)) {
            CreationStep.GENDER -> CharacterGender.entries.map { Suggestion(it.displayName[language].capitalizeFirst(), it.displayName[language]) }
            CreationStep.NAME -> {
                val names = if (state.gender == CharacterGender.FEMALE) rules.names.female else rules.names.male
                names.take(6).map { Suggestion(it, it) } + back
            }
            CreationStep.SPECIES -> speciesOptions().map { it.suggestion(language, ctx) } + back
            CreationStep.CAREER -> careerOptions().map { it.suggestion(language, ctx) } + back
            CreationStep.BACKGROUND -> backgroundOptions().map { it.suggestion(language, ctx) } + back
            CreationStep.ABILITIES -> listOf(
                Suggestion(T.recommendLabel[language], T.recommendCommand[language]),
                Suggestion(T.doneLabel[language], T.doneCommand[language]),
            ) + Ability.entries.flatMap { ability ->
                val name = ability.displayName[language]
                listOf(Suggestion("$name +", "$name +"), Suggestion("$name −", "$name -"))
            } + Suggestion(T.resetLabel[language], T.resetCommand[language]) + back
            CreationStep.TRAITS -> traitOptions(state).map { it.suggestion(language, ctx) } + back
            CreationStep.EXTRA_SKILL -> skillOptions(state).map { it.suggestion(language, ctx) } + back
            CreationStep.SUMMARY -> listOf(Suggestion(T.confirmLabel[language], T.confirmCommand[language])) +
                changeTargets(state).map { (label, _) ->
                    val name = label[language]
                    Suggestion(T.changeLabel.format(language, name), T.changeCommand.format(language, name.lowercase()))
                } + back
        }
    }

    fun handle(state: CreationState, input: String, language: Language): Result {
        val tokens = Words.tokens(input)
        val step = step(state)
        if (tokens.any { it in BACK_WORDS } && tokens.size <= 2) return back(state, language)
        if (tokens.size == 1 && tokens[0] in HELP_WORDS || input.trim() == "?") {
            return Result.Continue(state, listOf(Paragraph(Kind.HINT, T.help[language])) + prompt(state, language))
        }
        return when (step) {
            CreationStep.GENDER -> chooseGender(state, tokens, language)
            CreationStep.NAME -> chooseName(state, input, language)
            CreationStep.SPECIES -> choose(state, tokens, speciesOptions(), language) { s, id -> s.copy(species = id).fitToSpecies(rules) }
            CreationStep.CAREER -> choose(state, tokens, careerOptions(), language) { s, id ->
                s.copy(career = id, extraSkill = s.extraSkill?.takeIf { it !in rules.career(id).skills })
            }
            CreationStep.BACKGROUND -> choose(state, tokens, backgroundOptions(), language) { s, id -> s.copy(background = id) }
            CreationStep.ABILITIES -> pointBuy(state, input, tokens, language)
            CreationStep.TRAITS -> choose(state, tokens, traitOptions(state), language) { s, id -> s.copy(traits = s.traits + id) }
            CreationStep.EXTRA_SKILL -> choose(state, tokens, skillOptions(state), language) { s, id -> s.copy(extraSkill = Skill.valueOf(id)) }
            CreationStep.SUMMARY -> summary(state, tokens, language)
        }
    }

    // --- Steps --------------------------------------------------------------------------

    private fun chooseGender(state: CreationState, tokens: List<String>, language: Language): Result {
        val gender = when {
            tokens.any { it in MALE_WORDS } -> CharacterGender.MALE
            tokens.any { it in FEMALE_WORDS } -> CharacterGender.FEMALE
            else -> return retry(state, language)
        }
        return advance(state.copy(gender = gender), language)
    }

    private fun chooseName(state: CreationState, input: String, language: Language): Result {
        var name = input.trim().replace(Regex("\\s+"), " ")
        NAME_PREFIXES.firstOrNull { name.lowercase().startsWith(it) }?.let { name = name.substring(it.length).trim() }
        name = name.trim('.', '!', ',', '"', '„', '“')
        if (!NAME_PATTERN.matches(name)) {
            return Result.Continue(state, listOf(Paragraph(Kind.TEXT, T.badName[language])))
        }
        name = name.split(' ').joinToString(" ") { it.capitalizeFirst() }
        val next = state.copy(name = name)
        return advance(next, language, listOf(Paragraph(Kind.TEXT, T.nameAccepted.render(language, context(next)))))
    }

    private fun choose(
        state: CreationState,
        tokens: List<String>,
        options: List<Option>,
        language: Language,
        apply: (CreationState, String) -> CreationState,
    ): Result {
        val chosen = pick(tokens, options) ?: return retry(state, language)
        val next = apply(state, chosen.id)
        val ctx = context(next)
        val echo = buildList {
            add(Paragraph(Kind.OPTION, "${chosen.name.render(language, ctx)} — ${chosen.detail.render(language, ctx)}"))
            addAll(chosen.extra(language, ctx))
        }
        return advance(next, language, echo)
    }

    private fun summary(state: CreationState, tokens: List<String>, language: Language): Result {
        if (tokens.any { it in CONFIRM_WORDS }) {
            val character = draft(state)
            val ctx = TextContext(character)
            return Result.Finished(character, listOf(Paragraph(Kind.TEXT, T.finished.render(language, ctx))))
        }
        val target = changeTargets(state).firstOrNull { (label, _) ->
            val words = Words.foldAll(listOf(label.de, label.en))
            tokens.any { token -> words.any { Words.matches(token, it) } }
        } ?: return retry(state, language)
        return advance(target.second(state), language)
    }

    private fun changeTargets(state: CreationState): List<Pair<LocalizedText, (CreationState) -> CreationState>> = buildList {
        add(LocalizedText("Name", "Name") to { s: CreationState -> s.copy(name = null) })
        add(LocalizedText("Geschlecht", "Gender") to { s: CreationState -> s.copy(gender = null) })
        add(LocalizedText("Volk", "People") to { s: CreationState -> s.copy(species = null) })
        add(LocalizedText("Werdegang", "Former life") to { s: CreationState -> s.copy(career = null) })
        add(LocalizedText("Hintergrund", "Background") to { s: CreationState -> s.copy(background = null) })
        add(LocalizedText("Attribute", "Abilities") to { s: CreationState -> s.copy(scoresConfirmed = false) })
        add(LocalizedText("Merkmale", "Traits") to { s: CreationState -> s.copy(traits = emptyList()) })
        if (state.species?.let { rules.species(it).extraSkill } == true) {
            add(LocalizedText("Fertigkeit", "Skill") to { s: CreationState -> s.copy(extraSkill = null) })
        }
    }

    private fun back(state: CreationState, language: Language): Result {
        val previous: CreationState = when (step(state)) {
            CreationStep.GENDER -> return Result.Continue(state, listOf(Paragraph(Kind.TEXT, T.atStart[language])))
            CreationStep.NAME -> state.copy(gender = null)
            CreationStep.SPECIES -> state.copy(name = null)
            CreationStep.CAREER -> state.copy(species = null)
            CreationStep.BACKGROUND -> state.copy(career = null)
            CreationStep.ABILITIES -> state.copy(background = null)
            CreationStep.TRAITS -> if (state.traits.isEmpty()) state.copy(scoresConfirmed = false) else state.copy(traits = state.traits.dropLast(1))
            CreationStep.EXTRA_SKILL -> state.copy(traits = state.traits.dropLast(1))
            CreationStep.SUMMARY -> if (state.extraSkill != null) state.copy(extraSkill = null) else state.copy(traits = state.traits.dropLast(1))
        }
        return Result.Continue(previous, prompt(previous, language))
    }

    private fun advance(state: CreationState, language: Language, before: List<Paragraph> = emptyList()): Result =
        Result.Continue(state, before + prompt(state, language))

    private fun retry(state: CreationState, language: Language): Result =
        Result.Continue(state, listOf(Paragraph(Kind.TEXT, T.notUnderstood[language])) + prompt(state, language))

    // --- Point buy ----------------------------------------------------------------------

    private fun pointBuy(state: CreationState, input: String, tokens: List<String>, language: Language): Result {
        fun update(scores: Map<Ability, Int>, message: LocalizedText? = null): Result {
            val next = state.copy(scores = scores)
            val out = buildList {
                // Always sent, empty when there is nothing to say, so an old warning disappears.
                add(Paragraph(Kind.TEXT, message?.get(language).orEmpty(), slot = POINT_BUY_SLOT_MESSAGE))
                addAll(pointBuyTable(next, language))
            }
            return Result.Continue(next, out)
        }

        when {
            tokens.any { it in DONE_WORDS } -> {
                val left = Rules.POINT_BUY_BUDGET - Rules.pointsSpent(state.scores)
                if (left > 0) return update(state.scores, LocalizedText(T.pointsLeft.format(Language.DE, "$left"), T.pointsLeft.format(Language.EN, "$left")))
                return advance(state.copy(scoresConfirmed = true), language, listOf(Paragraph(Kind.TEXT, T.abilitiesDone[language])))
            }
            tokens.any { it in RECOMMEND_WORDS } -> return update(recommended(state))
            tokens.any { it in RESET_WORDS } -> return update(Ability.entries.associateWith { Rules.POINT_BUY_MIN })
        }

        val changes = parseAbilityChanges(input, tokens, state.scores) ?: return update(state.scores, T.abilityNotUnderstood)
        var scores = state.scores
        for ((ability, value) in changes) {
            if (value !in Rules.POINT_BUY_MIN..Rules.POINT_BUY_MAX) return update(scores, T.outOfRange)
            val candidate = scores + (ability to value)
            if (Rules.pointsSpent(candidate) > Rules.POINT_BUY_BUDGET) return update(scores, T.overBudget)
            scores = candidate
        }
        return update(scores)
    }

    /** "stärke 14", "str +", "geschick -", "stärke 15 konstitution 14" … */
    private fun parseAbilityChanges(input: String, tokens: List<String>, scores: Map<Ability, Int>): List<Pair<Ability, Int>>? {
        val raw = input.lowercase()
        val result = mutableListOf<Pair<Ability, Int>>()
        var index = 0
        while (index < tokens.size) {
            val ability = abilityFor(tokens[index])
            if (ability == null) {
                index++
                continue
            }
            val next = tokens.getOrNull(index + 1)
            val current = result.lastOrNull { it.first == ability }?.second ?: scores.getValue(ability)
            val value = when {
                next?.toIntOrNull() != null -> next.toInt().also { index++ }
                next in UP_WORDS -> (current + 1).also { index++ }
                next in DOWN_WORDS -> (current - 1).also { index++ }
                '+' in raw -> current + 1
                '-' in raw || '−' in raw -> current - 1
                else -> return null
            }
            result += ability to value
            index++
        }
        return result.ifEmpty { null }
    }

    private fun abilityFor(token: String): Ability? = Ability.entries.firstOrNull { ability ->
        val names = Words.foldAll(listOf(ability.displayName.de, ability.displayName.en)) +
            Words.fold(ability.short.de) + Words.fold(ability.short.en) + (ABILITY_ALIASES[ability] ?: emptySet())
        names.any { token == it || (it.length >= 5 && Words.matches(token, it)) }
    }

    /** The standard array in the order the former life favours. */
    private fun recommended(state: CreationState): Map<Ability, Int> {
        val focus = state.career?.let { rules.career(it).focus } ?: Ability.entries
        return focus.zip(Rules.STANDARD_ARRAY).toMap()
    }

    private fun pointBuyTable(state: CreationState, language: Language): List<Paragraph> {
        val bonus = state.background?.let { rules.background(it).bonus } ?: emptyMap()
        val rows = buildList {
            add(listOf(T.colAbility[language], T.colBase[language], T.colBonus[language], T.colTotal[language], T.colMod[language]).joinToString("\t"))
            for (ability in Ability.entries) {
                val base = state.scores.getValue(ability)
                val plus = bonus[ability] ?: 0
                val total = minOf(Rules.MAX_SCORE, base + plus)
                add(listOf(ability.displayName[language], "$base", if (plus == 0) "" else Rules.signed(plus), "$total", Rules.signed(Rules.modifier(total))).joinToString("\t"))
            }
        }
        val left = Rules.POINT_BUY_BUDGET - Rules.pointsSpent(state.scores)
        return listOf(
            Paragraph(Kind.TABLE, rows.joinToString("\n"), slot = POINT_BUY_SLOT_TABLE),
            Paragraph(Kind.HINT, T.pointsStatus.format(language, "$left", "${Rules.POINT_BUY_BUDGET}"), slot = POINT_BUY_SLOT_STATUS),
        )
    }

    // --- Options ------------------------------------------------------------------------

    /** Something the player can pick; [detail] is shown in the list and on selection. */
    private class Option(
        val id: String,
        val name: LocalizedText,
        val detail: LocalizedText,
        val aliases: Set<String>,
        val extra: (Language, TextContext) -> List<Paragraph> = { _, _ -> emptyList() },
    ) {
        fun suggestion(language: Language, ctx: TextContext): Suggestion {
            val label = name.render(language, ctx)
            return Suggestion(label, label)
        }
    }

    private fun aliases(vararg texts: LocalizedText): Set<String> =
        Words.foldAll(texts.flatMap { text -> listOf(text.de, text.en) }.map { it.replace("[[", " ").replace("|", " ").replace("]]", " ") })

    private fun speciesOptions() = rules.species.map { s ->
        Option(s.id, s.name, s.summary, aliases(s.name)) { language, ctx ->
            listOf(Paragraph(Kind.TEXT, s.description.render(language, ctx))) +
                s.features.map { Paragraph(Kind.OPTION, "${it.name.render(language, ctx)} — ${it.description.render(language, ctx)}") }
        }
    }

    private fun careerOptions() = rules.careers.map { c ->
        Option(c.id, c.name, c.summary, aliases(c.name)) { language, ctx ->
            listOf(
                Paragraph(Kind.TEXT, c.description.render(language, ctx)),
                Paragraph(Kind.HINT, T.trainedIn.format(language, c.skills.joinToString(", ") { it.displayName[language] })),
                Paragraph(Kind.OPTION, "${c.feature.name.render(language, ctx)} — ${c.feature.description.render(language, ctx)}"),
                Paragraph(Kind.OPTION, T.weaknessLine.format(language, c.weakness.name.render(language, ctx), c.weakness.description.render(language, ctx))),
            )
        }
    }

    private fun backgroundOptions() = rules.backgrounds.map { b ->
        Option(b.id, b.name, b.summary, aliases(b.name)) { language, ctx ->
            val bonus = b.bonus.entries.sortedByDescending { it.value }
                .joinToString(", ") { "${it.key.displayName[language]} ${Rules.signed(it.value)}" }
            listOf(
                Paragraph(Kind.TEXT, b.description.render(language, ctx)),
                Paragraph(Kind.HINT, b.consequences.render(language, ctx)),
                Paragraph(Kind.HINT, T.bonusLine.format(language, bonus)),
            )
        }
    }

    private fun traitOptions(state: CreationState) = rules.traits.filter { it.id !in state.traits }.map { t ->
        Option(t.id, t.name, t.description, aliases(t.name))
    }

    private fun skillOptions(state: CreationState): List<Option> {
        val known = draftSkills(state)
        return Skill.entries.filter { it !in known }.map { skill ->
            Option(skill.name, skill.displayName, skill.ability.displayName, aliases(skill.displayName))
        }
    }

    private fun draftSkills(state: CreationState): Set<Skill> =
        (state.career?.let { rules.career(it).skills } ?: emptyList()).toSet() +
            (state.species?.let { rules.species(it).skills } ?: emptyList()) +
            state.traits.flatMap { rules.trait(it).skills }

    /** Picks by number ("2") or by name, also inflected or with a small typo; null if unclear. */
    private fun pick(tokens: List<String>, options: List<Option>): Option? {
        tokens.firstNotNullOfOrNull { it.toIntOrNull() }?.let { number -> return options.getOrNull(number - 1) }
        val scored = options.map { option ->
            option to tokens.count { token -> token.length > 1 && option.aliases.any { Words.matches(token, it) } }
        }
        val top = scored.maxOfOrNull { it.second } ?: return null
        if (top == 0) return null
        return scored.filter { it.second == top }.singleOrNull()?.first
    }

    private fun choice(title: LocalizedText, question: LocalizedText, options: List<Option>, language: Language, ctx: TextContext): List<Paragraph> =
        listOf(Paragraph(Kind.TITLE, title[language]), Paragraph(Kind.TEXT, question.render(language, ctx))) +
            options.mapIndexed { i, o -> Paragraph(Kind.OPTION, "${i + 1}. ${o.name.render(language, ctx)} — ${o.detail.render(language, ctx)}") } +
            Paragraph(Kind.HINT, T.chooseHint[language])

    // --- Helpers ------------------------------------------------------------------------

    private fun traitsNeeded(state: CreationState): Int =
        if (state.species?.let { rules.species(it).extraTrait } == true) 2 else 1

    private fun context(state: CreationState) = TextContext(null, gender = state.gender, name = state.name)

    /** The character as chosen so far; only valid once every step is done. */
    fun draft(state: CreationState): Character {
        val career = rules.career(requireNotNull(state.career))
        val background = rules.background(requireNotNull(state.background))
        return Character(
            name = requireNotNull(state.name),
            gender = requireNotNull(state.gender),
            species = requireNotNull(state.species),
            career = career.id,
            background = background.id,
            traits = state.traits,
            baseScores = state.scores,
            extraSkill = state.extraSkill,
            coins = career.coins + background.coins,
        )
    }

    private fun CreationState.fitToSpecies(rules: CharacterRules): CreationState {
        val chosen = rules.species(requireNotNull(species))
        return copy(
            traits = traits.take(if (chosen.extraTrait) 2 else 1),
            extraSkill = extraSkill.takeIf { chosen.extraSkill },
        )
    }

    companion object {
        const val POINT_BUY_SLOT_TABLE = "pointbuy-table"
        const val POINT_BUY_SLOT_STATUS = "pointbuy-status"
        const val POINT_BUY_SLOT_MESSAGE = "pointbuy-message"
        const val POINT_BUY_SLOT_INPUT = "pointbuy-input"

        private val BACK_WORDS = Words.foldAll(listOf("zurück", "back"))
        private val HELP_WORDS = Words.foldAll(listOf("hilfe", "help"))
        private val MALE_WORDS = Words.foldAll(listOf("männlich", "mann", "junge", "bursche", "er", "m", "male", "man", "boy", "he"))
        private val FEMALE_WORDS = Words.foldAll(listOf("weiblich", "frau", "mädchen", "maid", "sie", "w", "f", "female", "woman", "girl", "she"))
        private val CONFIRM_WORDS = Words.foldAll(
            listOf("ja", "fertig", "los", "beginnen", "starten", "passt", "bestätigen", "yes", "done", "begin", "start", "confirm", "go"),
        )
        private val DONE_WORDS = Words.foldAll(listOf("fertig", "weiter", "ok", "passt", "done", "next", "continue"))
        private val RECOMMEND_WORDS = Words.foldAll(listOf("vorschlag", "empfehlung", "empfohlen", "recommend", "recommended", "suggest", "suggestion"))
        private val RESET_WORDS = Words.foldAll(listOf("zurücksetzen", "reset"))
        private val UP_WORDS = Words.foldAll(listOf("plus", "hoch", "rauf", "mehr", "up", "more"))
        private val DOWN_WORDS = Words.foldAll(listOf("minus", "runter", "weniger", "down", "less"))
        private val ABILITY_ALIASES = mapOf(
            Ability.STRENGTH to setOf("st", "kraft"),
            Ability.DEXTERITY to setOf("ge", "geschicklichkeit", "gewandtheit"),
            Ability.CONSTITUTION to setOf("ko", "konsti", "ausdauer"),
            Ability.INTELLIGENCE to setOf("klugheit", "verstand"),
            Ability.WISDOM to setOf("weise"),
            Ability.CHARISMA to setOf("ausstrahlung"),
        )
        private val NAME_PREFIXES = listOf("ich heiße ", "ich heisse ", "mein name ist ", "ich bin ", "nenn mich ", "my name is ", "i am ", "i'm ", "call me ")
        private val NAME_PATTERN = Regex("^\\p{L}[\\p{L}'’ -]{1,23}$")
    }
}

/** Fixed texts of character creation. */
private object T {
    private fun t(de: String, en: String) = LocalizedText(de, en)

    val intro = t(
        "Bevor deine Geschichte beginnt, erzähl mir, wer du bist. Du bist jung – wie alt genau, weißt du selbst nicht. Irgendwo zwischen achtzehn und zwanzig Wintern, vielleicht.",
        "Before your story begins, tell me who you are. You are young – exactly how old, you don't know yourself. Somewhere between eighteen and twenty winters, perhaps.",
    )
    val genderTitle = t("Wer bist du?", "Who are you?")
    val genderQuestion = t("Bist du ein junger Mann oder eine junge Frau?", "Are you a young man or a young woman?")
    val genderHint = t("Deine Wahl beeinflusst, wie man dir begegnet – und manches, was dir widerfährt.", "Your choice affects how people treat you – and some of what befalls you.")
    val nameTitle = t("Dein Name", "Your name")
    val nameQuestion = t("Wie nennt man dich?", "What do people call you?")
    val nameHint = t("Schreib deinen Namen oder wähle einen Vorschlag.", "Write your name or pick a suggestion.")
    val nameAccepted = t("{name} also.", "{name}, then.")
    val badName = t(
        "Das klingt nicht nach einem Namen. Ein Name besteht aus zwei bis vierundzwanzig Buchstaben.",
        "That doesn't sound like a name. A name has two to twenty-four letters.",
    )
    val speciesTitle = t("Dein Volk", "Your people")
    val speciesQuestion = t("Welchem Volk gehörst du an, {name}?", "Which people do you belong to, {name}?")
    val careerTitle = t("Dein Werdegang", "Your former life")
    val careerQuestion = t(
        "Womit hast du dich bisher durchgeschlagen? Was du erlebt hast, bestimmt, was du kannst – und wo deine Schwäche liegt.",
        "How have you got by so far? What you lived through decides what you can do – and where your weakness lies.",
    )
    val backgroundTitle = t("Deine Vergangenheit", "Your past")
    val backgroundQuestion = t(
        "Was liegt hinter dir? Deine Vergangenheit wird dich einholen – im Guten wie im Schlechten.",
        "What lies behind you? Your past will catch up with you – for better and for worse.",
    )
    val abilitiesTitle = t("Deine Attribute", "Your abilities")
    val abilitiesQuestion = t(
        "Verteile 27 Punkte auf deine sechs Attribute. Jedes beginnt bei 8 und kann bis 15 steigen; 14 und 15 kosten je zwei Punkte. Der Bonus kommt aus deiner Vergangenheit.",
        "Spend 27 points on your six abilities. Each starts at 8 and can rise to 15; 14 and 15 cost two points each. The bonus comes from your past.",
    )
    val abilitiesHint = t(
        "Schreib z. B. „Stärke 14“ oder „Geschick +“, tippe auf die Knöpfe, oder nimm den Vorschlag für deinen Werdegang. „Fertig“, wenn alle Punkte verteilt sind.",
        "Write e.g. “Strength 14” or “Dexterity +”, tap the buttons, or take the suggestion for your former life. “Done” once all points are spent.",
    )
    val abilitiesDone = t("Deine Attribute stehen fest.", "Your abilities are set.")
    val pointsStatus = t("Noch {0} von {1} Punkten übrig.", "{0} of {1} points left.")
    val pointsLeft = t("Du hast noch {0} Punkte übrig. Verteile sie, bevor du weitermachst.", "You still have {0} points left. Spend them before you go on.")
    val overBudget = t("Dafür reichen deine Punkte nicht.", "You don't have enough points for that.")
    val outOfRange = t("Beim Punktkauf liegt jedes Attribut zwischen 8 und 15.", "With point buy every ability lies between 8 and 15.")
    val abilityNotUnderstood = t(
        "Das habe ich nicht verstanden. Schreib z. B. „Stärke 14“, „Weisheit +“ oder „Vorschlag“.",
        "I didn't understand that. Write e.g. “Strength 14”, “Wisdom +” or “suggestion”.",
    )
    val colAbility = t("Attribut", "Ability")
    val colBase = t("Wert", "Score")
    val colBonus = t("Bonus", "Bonus")
    val colTotal = t("Gesamt", "Total")
    val colMod = t("Mod.", "Mod.")
    val traitsTitle = t("Dein Merkmal", "Your trait")
    val traitsQuestion = t("Was zeichnet dich aus?", "What sets you apart?")
    val secondTraitQuestion = t("Als [[Glatter|Glatte]] bist du vielseitig. Wähle ein zweites Merkmal.", "As one of the Smooth you are versatile. Choose a second trait.")
    val skillTitle = t("Deine Begabung", "Your talent")
    val skillQuestion = t("Menschen lernen schnell. In welcher Fertigkeit bist du zusätzlich geübt?", "Humans learn quickly. Which extra skill are you trained in?")
    val summaryQuestion = t(
        "Bist du bereit, {name}? Schreib „los“, um deine Geschichte zu beginnen, oder ändere, was dir nicht gefällt.",
        "Are you ready, {name}? Write “begin” to start your story, or change whatever you don't like.",
    )
    val summaryHint = t("Zum Ändern: z. B. „Werdegang ändern“ oder „Name“.", "To change something: e.g. “change former life” or “name”.")
    val finished = t("So sei es, {name}. Deine Geschichte beginnt.", "So be it, {name}. Your story begins.")
    val chooseHint = t("Tippe auf einen Vorschlag oder schreib den Namen bzw. die Nummer.", "Tap a suggestion or write the name or number.")
    val notUnderstood = t("Das habe ich nicht verstanden.", "I didn't understand that.")
    val atStart = t("Du stehst ganz am Anfang.", "You are at the very beginning.")
    val help = t(
        "Beantworte die Fragen per Text oder mit den Vorschlägen. „zurück“ geht einen Schritt zurück; in der Zusammenfassung kannst du alles noch einmal ändern.",
        "Answer the questions by typing or with the suggestions. “back” goes back one step; in the summary you can change everything again.",
    )
    val trainedIn = t("Geübt in: {0}", "Trained in: {0}")
    val weaknessLine = t("Schwäche: {0} — {1}", "Weakness: {0} — {1}")
    val bonusLine = t("Attributsbonus: {0}", "Ability bonus: {0}")
    val backLabel = t("Zurück", "Back")
    val backCommand = t("zurück", "back")
    val recommendLabel = t("Vorschlag", "Suggestion")
    val recommendCommand = t("vorschlag", "suggestion")
    val doneLabel = t("Fertig", "Done")
    val doneCommand = t("fertig", "done")
    val resetLabel = t("Zurücksetzen", "Reset")
    val resetCommand = t("zurücksetzen", "reset")
    val confirmLabel = t("Los geht's", "Let's go")
    val confirmCommand = t("los", "begin")
    val changeLabel = t("{0} ändern", "Change {0}")
    val changeCommand = t("{0} ändern", "change {0}")
}
