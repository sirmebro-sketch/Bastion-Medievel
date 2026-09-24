package de.bastion.medieval.engine

import de.bastion.medieval.engine.Paragraph.Kind

/**
 * Runs the game: reads what the player typed, changes the [GameState] and answers with
 * paragraphs. Every call takes the language to answer in, so the player can switch
 * languages at any time without losing progress.
 *
 * A new game starts with character creation ([CharacterCreator]); once the character
 * exists, input goes to the world.
 */
class Game(
    val world: World,
    val rules: CharacterRules = CharacterRules.loadDefault(),
    state: GameState = GameState.new(world),
) {
    var state: GameState =
        if (state.character == null && state.creation == null) state.copy(creation = CreationState()) else state
        private set

    private val parser = Parser()
    private val creator = CharacterCreator(rules)

    private val here: Location get() = world.location(state.location)

    val inCreation: Boolean get() = state.creation != null

    private val sheet: Sheet? get() = state.character?.let { Sheet(it, rules) }

    private fun context() = TextContext(state.character, sheet?.features ?: emptySet(), state.flags)

    private fun LocalizedText.r(language: Language): String = render(language, context())

    fun title(language: Language): String = world.data.title[language]

    fun locationName(language: Language): String =
        if (inCreation) Messages.creationTitle[language] else here.name.r(language)

    /** The very first paragraphs of a new game. */
    fun opening(language: Language): List<Paragraph> {
        val creation = state.creation
        return if (creation != null) creator.intro(creation, language) else storyStart(language)
    }

    /** The character sheet (menu), or nothing while the character is being created. */
    fun sheetView(language: Language): List<Paragraph> = sheet?.let { SheetView.render(it, language) } ?: emptyList()

    /** Licence and attribution notes (menu). */
    fun credits(language: Language): List<Paragraph> = listOf(Paragraph(Kind.HINT, Messages.credits[language]))

    /** What to show after loading a save that stopped in the middle of character creation. */
    fun resume(language: Language): List<Paragraph> =
        state.creation?.let { creator.prompt(it, language) } ?: emptyList()

    /** Handles one line of player input; the answer starts with the input itself. */
    fun submit(input: String, language: Language): List<Paragraph> {
        val text = input.trim()
        if (text.isEmpty()) return emptyList()

        val creation = state.creation
        if (creation != null) {
            val slot = if (creator.step(creation) == CreationStep.ABILITIES) CharacterCreator.POINT_BUY_SLOT_INPUT else null
            val echo = Paragraph(Kind.INPUT, text, slot)
            return when (val result = creator.handle(creation, text, language)) {
                is CharacterCreator.Result.Continue -> {
                    state = state.copy(creation = result.state)
                    listOf(echo) + result.output
                }
                is CharacterCreator.Result.Finished -> {
                    val background = rules.background(result.character.background)
                    state = state.copy(character = result.character, creation = null, flags = state.flags + background.flags)
                    listOf(echo) + result.output + storyStart(language)
                }
            }
        }

        val answer = when (val parsed = parser.parse(text, language)) {
            Parsed.Empty -> return emptyList()
            Parsed.NotUnderstood -> notUnderstood(language)
            is Parsed.Command -> execute(parsed, language)
        }
        state = state.copy(turns = state.turns + 1)
        return listOf(Paragraph(Kind.INPUT, text)) + answer
    }

    /** Commands that make sense right now, for players who don't know what to type. */
    fun suggestions(language: Language): List<Suggestion> {
        state.creation?.let { return creator.suggestions(it, language) }
        return buildList {
            for (exit in here.exits.sortedBy { it.direction.ordinal }) {
                val toward = exit.direction.toward[language]
                add(Suggestion(Messages.goLabel.format(language, toward).capitalizeFirst(), Messages.goCommand.format(language, toward)))
            }
            val features = sheet?.features ?: emptySet()
            for (exit in lockedExits()) {
                if (exit.pick != null && (exit.pick.feature == null || exit.pick.feature in features) && triedFlag("pick", exit) !in state.flags) {
                    add(Suggestion(Messages.pickLabel[language], Messages.pickCommand[language]))
                }
                if (exit.force != null && triedFlag("force", exit) !in state.flags) {
                    add(Suggestion(Messages.forceLabel[language], Messages.forceCommand[language]))
                }
            }
            for (person in peopleHere()) {
                val name = person.naming.definite(language, Case.DATIVE)
                add(Suggestion(Messages.talkLabel.format(language, name), Messages.talkCommand.format(language, name)))
            }
            val items = itemsHere()
            for (item in items.filter { it.portable }) {
                add(
                    Suggestion(
                        Messages.takeLabel.format(language, item.naming.label(language)).capitalizeFirst(),
                        Messages.takeCommand.format(language, item.naming.definite(language, Case.ACCUSATIVE)),
                    ),
                )
            }
            for (item in items.filter { examinedFlag(it) !in state.flags }) {
                add(
                    Suggestion(
                        Messages.examineLabel.format(language, item.naming.label(language)).capitalizeFirst(),
                        Messages.examineCommand.format(language, item.naming.definite(language, Case.ACCUSATIVE)),
                    ),
                )
            }
            add(Suggestion(Messages.lookLabel[language], Messages.lookCommand[language]))
            if (state.inventory.isNotEmpty()) add(Suggestion(Messages.inventoryLabel[language], Messages.inventoryCommand[language]))
            add(Suggestion(Messages.sheetLabel[language], Messages.sheetCommand[language]))
        }
    }

    private fun storyStart(language: Language): List<Paragraph> =
        listOf(Paragraph(Kind.TEXT, world.data.intro.r(language))) + describe(language)

    private fun execute(command: Parsed.Command, language: Language): List<Paragraph> = when (command.verb) {
        Verb.LOOK -> describe(language)
        Verb.EXAMINE -> examine(command, language)
        Verb.GO -> go(command, language)
        Verb.BACK -> back(language)
        Verb.TAKE -> take(command.words, language)
        Verb.DROP -> drop(command.words, language)
        Verb.OPEN -> open(command.words, language)
        Verb.PICK -> overcome(command.words, ApproachKind.PICK, language)
        Verb.FORCE -> overcome(command.words, ApproachKind.FORCE, language)
        Verb.INVENTORY -> inventory(language)
        Verb.TALK -> talk(command.words, language)
        Verb.SHEET -> sheetView(language)
        Verb.CREDITS -> credits(language)
        Verb.HELP -> listOf(Paragraph(Kind.HINT, Messages.help[language]))
        Verb.WAIT -> text(Messages.wait[language])
    }

    // --- Looking --------------------------------------------------------------------------

    private fun describe(language: Language): List<Paragraph> = buildList {
        val location = here
        add(Paragraph(Kind.TITLE, location.name.r(language)))
        add(Paragraph(Kind.SCENE, location.description.r(language)))

        val visible = itemsHere().filterNot { it.scenery }
        if (visible.isNotEmpty()) {
            val list = joinNatural(visible.map { it.naming.indefinite(language, Case.ACCUSATIVE) }, Messages.and[language])
            add(Paragraph(Kind.TEXT, Messages.youSee.format(language, list)))
        }

        val people = peopleHere().map { it.naming.definite(language) }
        when (people.size) {
            0 -> Unit
            1 -> add(Paragraph(Kind.TEXT, Messages.personHere.format(language, people[0]).capitalizeFirst()))
            else -> add(Paragraph(Kind.TEXT, Messages.peopleHere.format(language, joinNatural(people, Messages.and[language])).capitalizeFirst()))
        }

        add(Paragraph(Kind.HINT, exitsLine(location, language)))
    }

    private fun exitsLine(location: Location, language: Language): String {
        val towards = location.exits.sortedBy { it.direction.ordinal }.map { it.direction.toward[language] }
        return when (towards.size) {
            0 -> Messages.noExits[language]
            1 -> Messages.exitOne.format(language, towards[0])
            else -> Messages.exitsMany.format(language, joinNatural(towards, Messages.and[language]))
        }
    }

    private fun examine(command: Parsed.Command, language: Language): List<Paragraph> {
        if (command.words.isEmpty()) return text(Messages.whatExamine[language])
        val candidates = inventoryItems().map(Target::Thing) + itemsHere().map(Target::Thing) + peopleHere().map(Target::Someone)
        return when (val found = resolve(command.words, candidates, language)) {
            Resolution.None -> if (command.implicit) notUnderstood(language) else text(Messages.noSuchThing[language])
            is Resolution.Many -> ambiguous(found.targets, language)
            is Resolution.One -> when (val target = found.target) {
                is Target.Thing -> {
                    state = state.copy(flags = state.flags + examinedFlag(target.item))
                    text(target.item.description.r(language)) + revealSecret(target.item, language)
                }
                is Target.Someone -> text(target.person.description.r(language))
            }
        }
    }

    /** The first close look at a thing with a secret triggers one check. */
    private fun revealSecret(item: Item, language: Language): List<Paragraph> {
        val secret = item.secret ?: return emptyList()
        val flag = "secret:${item.id}"
        if (flag in state.flags || state.character == null) return emptyList()
        state = state.copy(flags = state.flags + flag)
        val check = check(secret.skill, secret.dc, language)
        if (!check.success) return check.paragraphs
        secret.flag?.let { state = state.copy(flags = state.flags + it) }
        return check.paragraphs + Paragraph(Kind.TEXT, secret.text.r(language)) + gainXp(secret.xp, language)
    }

    // --- Things ---------------------------------------------------------------------------

    private fun take(words: List<String>, language: Language): List<Paragraph> {
        if (words.isEmpty()) return text(Messages.whatTake[language])
        val candidates = itemsHere().map(Target::Thing) + inventoryItems().map(Target::Thing) + peopleHere().map(Target::Someone)
        return when (val found = resolve(words, candidates, language)) {
            Resolution.None -> text(Messages.noSuchThing[language])
            is Resolution.Many -> ambiguous(found.targets, language)
            is Resolution.One -> when (val target = found.target) {
                is Target.Someone -> text(Messages.personNotPortable.format(language, target.naming.definite(language)).capitalizeFirst())
                is Target.Thing -> {
                    val item = target.item
                    when {
                        state.itemPlaces[item.id] == GameState.INVENTORY ->
                            text(Messages.alreadyCarrying.format(language, item.naming.definite(language, Case.ACCUSATIVE)).capitalizeFirst())
                        !item.portable ->
                            text(Messages.notPortable.format(language, item.naming.definite(language)).capitalizeFirst())
                        else -> {
                            state = state.copy(itemPlaces = state.itemPlaces + (item.id to GameState.INVENTORY))
                            text(Messages.taken.format(language, item.naming.definite(language, Case.ACCUSATIVE)))
                        }
                    }
                }
            }
        }
    }

    private fun drop(words: List<String>, language: Language): List<Paragraph> {
        if (words.isEmpty()) return text(Messages.whatDrop[language])
        val candidates = inventoryItems().map(Target::Thing) + itemsHere().map(Target::Thing)
        return when (val found = resolve(words, candidates, language)) {
            Resolution.None -> text(Messages.noSuchThing[language])
            is Resolution.Many -> ambiguous(found.targets, language)
            is Resolution.One -> {
                val item = (found.target as Target.Thing).item
                if (state.itemPlaces[item.id] != GameState.INVENTORY) {
                    text(Messages.notCarrying.format(language, item.naming.definite(language, Case.ACCUSATIVE)).capitalizeFirst())
                } else {
                    state = state.copy(itemPlaces = state.itemPlaces + (item.id to here.id))
                    text(Messages.dropped.format(language, item.naming.definite(language, Case.ACCUSATIVE)))
                }
            }
        }
    }

    private fun inventory(language: Language): List<Paragraph> {
        val carried = inventoryItems()
        if (carried.isEmpty()) return text(Messages.inventoryEmpty[language])
        val list = joinNatural(carried.map { it.naming.indefinite(language, Case.ACCUSATIVE) }, Messages.and[language])
        return text(Messages.inventory.format(language, list))
    }

    // --- People ---------------------------------------------------------------------------

    private fun talk(words: List<String>, language: Language): List<Paragraph> {
        val people = peopleHere()
        if (words.isEmpty()) {
            return when (people.size) {
                0 -> text(Messages.nobodyHere[language])
                1 -> talkTo(people[0], language)
                else -> text(Messages.whoTalk[language])
            }
        }
        val candidates = people.map(Target::Someone) + itemsHere().map(Target::Thing) + inventoryItems().map(Target::Thing)
        return when (val found = resolve(words, candidates, language)) {
            Resolution.None -> text(if (people.isEmpty()) Messages.nobodyHere[language] else Messages.noSuchThing[language])
            is Resolution.Many -> ambiguous(found.targets, language)
            is Resolution.One -> when (val target = found.target) {
                is Target.Someone -> talkTo(target.person, language)
                is Target.Thing -> text(Messages.talkToThing.format(language, target.naming.definite(language)).capitalizeFirst())
            }
        }
    }

    private fun talkTo(person: Person, language: Language): List<Paragraph> {
        if (person.talk.isEmpty()) {
            return text(Messages.talkSilent.format(language, person.naming.definite(language)).capitalizeFirst())
        }
        val index = state.talkProgress[person.id] ?: 0
        val line = person.talk[minOf(index, person.talk.lastIndex)]
        state = state.copy(talkProgress = state.talkProgress + (person.id to minOf(index + 1, person.talk.lastIndex)))
        return listOf(Paragraph(Kind.DIALOGUE, line.r(language)))
    }

    // --- Moving and obstacles -------------------------------------------------------------

    private fun go(command: Parsed.Command, language: Language): List<Paragraph> {
        val exits = here.exits
        if (command.words.isNotEmpty()) {
            val matches = matchingExits(command.words, language)
            when {
                matches.map { it.to }.distinct().size == 1 -> return travel(matches.first(), language)
                matches.size > 1 -> {
                    val options = joinNatural(matches.map { it.direction.toward[language] }, Messages.or[language])
                    return text(Messages.ambiguous.format(language, options))
                }
            }
        }
        val direction = command.direction
        if (direction != null) {
            val exit = exits.firstOrNull { it.direction == direction }
            return if (exit != null) travel(exit, language) else text(Messages.noWay[language])
        }
        if (command.words.isNotEmpty()) return text(Messages.noWayThere[language])
        return listOf(Paragraph(Kind.TEXT, Messages.whereGo[language]), Paragraph(Kind.HINT, exitsLine(here, language)))
    }

    /** Unlocks a path without walking through it ("öffne das Tor"). */
    private fun open(words: List<String>, language: Language): List<Paragraph> {
        if (words.isEmpty()) return text(Messages.whatOpen[language])
        val exits = matchingExits(words, language)
        val exit = exits.firstOrNull { it.requires != null } ?: exits.firstOrNull()
        if (exit != null) {
            val requirement = exit.requires ?: return text(Messages.notLocked[language])
            if (isOpen(exit)) return text(Messages.alreadyOpen[language])
            if (state.itemPlaces[requirement] != GameState.INVENTORY) return text((exit.locked ?: Messages.lockedDefault).r(language))
            return listOf(Paragraph(Kind.TEXT, (exit.unlock ?: Messages.unlockDefault).r(language))) + markOpen(exit, language)
        }
        val candidates = itemsHere().map(Target::Thing) + inventoryItems().map(Target::Thing) + peopleHere().map(Target::Someone)
        return when (val found = resolve(words, candidates, language)) {
            Resolution.None -> text(Messages.noSuchThing[language])
            is Resolution.Many -> ambiguous(found.targets, language)
            is Resolution.One -> text(Messages.cannotOpen.format(language, found.target.naming.definite(language)).capitalizeFirst())
        }
    }

    private enum class ApproachKind(val id: String) { PICK("pick"), FORCE("force") }

    /** Picking a lock or forcing a locked path open: one check, one attempt per path. */
    private fun overcome(words: List<String>, kind: ApproachKind, language: Language): List<Paragraph> {
        val nothing = if (kind == ApproachKind.PICK) Messages.nothingToPick else Messages.nothingToForce
        val locked = lockedExits()
        val named = if (words.isEmpty()) emptyList() else matchingExits(words, language)
        val exit = named.singleOrNull()?.takeIf { it.requires != null } ?: locked.singleOrNull()
            ?: return text(nothing[language])
        if (isOpen(exit)) return text(Messages.alreadyOpen[language])
        val approach = (if (kind == ApproachKind.PICK) exit.pick else exit.force) ?: return text(nothing[language])
        val features = sheet?.features ?: emptySet()
        if (approach.feature != null && approach.feature !in features) {
            return text((approach.denied ?: Messages.pickDenied).r(language))
        }
        val tried = triedFlag(kind.id, exit)
        if (tried in state.flags) return text(Messages.alreadyTried[language])
        state = state.copy(flags = state.flags + tried)

        val check = check(approach.skill, approach.dc, language)
        return if (check.success) {
            check.paragraphs + Paragraph(Kind.TEXT, approach.success.r(language)) + markOpen(exit, language)
        } else {
            check.paragraphs + Paragraph(Kind.TEXT, approach.failure.r(language))
        }
    }

    private fun back(language: Language): List<Paragraph> {
        val previous = state.previous ?: return text(Messages.noBack[language])
        val exit = here.exits.firstOrNull { it.to == previous } ?: return text(Messages.noBack[language])
        return travel(exit, language)
    }

    private fun travel(exit: Exit, language: Language): List<Paragraph> {
        val out = mutableListOf<Paragraph>()
        val from = here
        val requirement = exit.requires
        if (requirement != null && !isOpen(exit)) {
            if (state.itemPlaces[requirement] != GameState.INVENTORY) {
                return text((exit.locked ?: Messages.lockedDefault).r(language))
            }
            out += Paragraph(Kind.TEXT, (exit.unlock ?: Messages.unlockDefault).r(language))
            out += markOpen(exit, language)
        }
        val firstVisit = exit.to !in state.visited
        state = state.copy(previous = from.id, location = exit.to, visited = state.visited + exit.to)
        out += describe(language)
        if (firstVisit) out += gainXp(here.xp, language)
        return out
    }

    private fun matchingExits(words: List<String>, language: Language): List<Exit> =
        best(words, here.exits) { exitVocabulary(it, language) }
            .ifEmpty { best(words, here.exits) { exitVocabulary(it, language.other) } }

    private fun exitVocabulary(exit: Exit, language: Language): Vocabulary {
        val destination = world.location(exit.to).name[language]
        return Vocabulary(Words.foldAll(exit.keywords[language] + destination), emptySet())
    }

    private fun lockedExits(): List<Exit> = here.exits.filter { it.requires != null && !isOpen(it) }

    private fun isOpen(exit: Exit): Boolean = openFlag(here, exit) in state.flags

    private fun markOpen(exit: Exit, language: Language): List<Paragraph> {
        state = state.copy(flags = state.flags + openFlag(here, exit))
        return gainXp(exit.xp, language)
    }

    // --- Checks and experience ------------------------------------------------------------

    private class CheckResult(val success: Boolean, val paragraphs: List<Paragraph>)

    /**
     * An ability check as in the SRD: d20 + ability modifier (+ proficiency bonus if
     * trained) against the DC, with advantage/disadvantage, halfling luck and luck points.
     */
    private fun check(skill: Skill, dc: Int, language: Language): CheckResult {
        val sheet = sheet ?: return CheckResult(false, emptyList())
        val bonus = sheet.skillBonus(skill)
        val halfling = "halfling_luck" in sheet.features
        val lines = mutableListOf<String>()

        fun d20(): Int {
            val value = Dice.roll(state.seed, state.rolls, 20)
            state = state.copy(rolls = state.rolls + 1)
            return value
        }
        fun die(): Int {
            val value = d20()
            if (halfling && value == 1) {
                lines += Messages.halflingLuck[language]
                return d20()
            }
            return value
        }
        fun attempt(): Boolean {
            val first = die()
            val (kept, note) = when (sheet.edge(skill)) {
                Edge.NONE -> first to ""
                Edge.ADVANTAGE -> die().let { second ->
                    maxOf(first, second) to Messages.advantageNote.format(language, "${minOf(first, second)}")
                }
                Edge.DISADVANTAGE -> die().let { second ->
                    minOf(first, second) to Messages.disadvantageNote.format(language, "${maxOf(first, second)}")
                }
            }
            val total = kept + bonus
            val success = total >= dc
            val modifier = if (bonus >= 0) "+ $bonus" else "− ${-bonus}"
            val result = if (success) Messages.checkSuccess[language] else Messages.checkFailure[language]
            lines += Messages.check.format(language, skill.displayName[language], "$dc", "$kept", note, modifier, "$total", result)
            return success
        }

        var success = attempt()
        if (!success && sheet.luckLeft > 0) {
            val character = sheet.character.copy(luckUsed = sheet.character.luckUsed + 1)
            state = state.copy(character = character)
            lines += Messages.luckUsed.format(language, "${Sheet(character, rules).luckLeft}")
            success = attempt()
        }
        return CheckResult(success, lines.map { Paragraph(Kind.ROLL, it) })
    }

    private fun gainXp(amount: Int, language: Language): List<Paragraph> {
        val character = state.character ?: return emptyList()
        if (amount <= 0) return emptyList()
        val before = Rules.level(character.xp)
        val gained = character.copy(xp = character.xp + amount)
        val after = Rules.level(gained.xp)
        // A new level refills luck points.
        val updated = if (after > before) gained.copy(luckUsed = 0) else gained
        state = state.copy(character = updated)
        return buildList {
            add(Paragraph(Kind.HINT, Messages.xpGained.format(language, "$amount")))
            if (after > before) {
                add(Paragraph(Kind.TEXT, Messages.levelUp.format(language, "$after", "${Sheet(updated, rules).maxHp}")))
            }
        }
    }

    // --- Helpers --------------------------------------------------------------------------

    private fun itemsHere(): List<Item> = state.itemsAt(here.id).mapNotNull { world.items[it] }

    private fun inventoryItems(): List<Item> = state.inventory.mapNotNull { world.items[it] }

    private fun peopleHere(): List<Person> = here.people.mapNotNull { world.people[it] }

    private fun examinedFlag(item: Item) = "examined:${item.id}"

    private fun openFlag(location: Location, exit: Exit) = "open:${location.id}:${exit.direction.name.lowercase()}"

    private fun triedFlag(kind: String, exit: Exit) = "tried:$kind:${here.id}:${exit.direction.name.lowercase()}"

    private fun text(text: String) = listOf(Paragraph(Kind.TEXT, text))

    private fun notUnderstood(language: Language) = listOf(
        Paragraph(Kind.TEXT, Messages.notUnderstood[language]),
        Paragraph(Kind.HINT, Messages.notUnderstoodHint[language]),
    )

    private fun ambiguous(targets: List<Target>, language: Language): List<Paragraph> {
        val options = joinNatural(targets.map { it.naming.definite(language, Case.ACCUSATIVE) }, Messages.or[language])
        return text(Messages.ambiguous.format(language, options))
    }

    private sealed interface Target {
        val naming: Naming

        data class Thing(val item: Item) : Target {
            override val naming: Naming get() = item.naming
        }

        data class Someone(val person: Person) : Target {
            override val naming: Naming get() = person.naming
        }
    }

    private sealed interface Resolution {
        data object None : Resolution
        data class One(val target: Target) : Resolution
        data class Many(val targets: List<Target>) : Resolution
    }

    /** Finds what the player means, in the active language first and then in the other one. */
    private fun resolve(words: List<String>, candidates: List<Target>, language: Language): Resolution {
        val unique = candidates.distinct()
        val found = best(words, unique) { it.naming.vocabulary(language) }
            .ifEmpty { best(words, unique) { it.naming.vocabulary(language.other) } }
        return when (found.size) {
            0 -> Resolution.None
            1 -> Resolution.One(found[0])
            else -> Resolution.Many(found)
        }
    }

    /**
     * Scores every candidate against the typed words. A candidate needs at least one
     * matching noun; matching adjectives raise the score, unknown words lower it. All
     * candidates with the top score are returned.
     */
    private fun <T> best(words: List<String>, candidates: List<T>, vocabulary: (T) -> Vocabulary): List<T> {
        val scored = candidates.mapNotNull { candidate ->
            val vocab = vocabulary(candidate)
            var nouns = 0
            var adjectives = 0
            var unknown = 0
            for (word in words) {
                when {
                    vocab.nouns.any { Words.matches(word, it) } -> nouns++
                    vocab.adjectives.any { Words.matches(word, it) } -> adjectives++
                    else -> unknown++
                }
            }
            if (nouns == 0) null else candidate to (nouns * 10 + adjectives * 3 - unknown * 4)
        }
        val top = scored.maxOfOrNull { it.second } ?: return emptyList()
        return scored.filter { it.second == top }.map { it.first }
    }
}
