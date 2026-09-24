package de.bastion.medieval.engine

enum class Verb { LOOK, EXAMINE, GO, BACK, TAKE, DROP, OPEN, INVENTORY, TALK, HELP, WAIT }

sealed interface Parsed {
    /**
     * A recognized command. [words] are the remaining content words (folded, without
     * articles and fillers). For [Verb.GO], [direction] is either the explicit target or,
     * when [words] name a place, a fallback ("go in the tavern").
     * [implicit] marks input without any verb ("schlüssel"), read as a look at that thing.
     */
    data class Command(
        val verb: Verb,
        val words: List<String> = emptyList(),
        val direction: Direction? = null,
        val implicit: Boolean = false,
    ) : Parsed

    data object Empty : Parsed

    data object NotUnderstood : Parsed
}

/**
 * Turns free text into a [Parsed.Command]. The active language is tried first, then the
 * other one, so "take key" also works while playing in German.
 *
 * German needs more than a verb-first grammar: separable verbs ("heb den Schlüssel auf",
 * "sieh dich um"), infinitives at the end ("ich will den Schlüssel nehmen") and
 * inflected words ("den rostigen Schlüssel"). Inflection and typos are handled later by
 * [Words.matches] when the words are matched against things in the world.
 */
class Parser {
    fun parse(input: String, language: Language): Parsed {
        if (input.trim() == "?") return Parsed.Command(Verb.HELP)
        val tokens = Words.tokens(input)
        if (tokens.isEmpty()) return Parsed.Empty
        parse(tokens, lexicon(language))?.let { return it }
        parse(tokens, lexicon(language.other))?.let { return it }
        val content = lexicon(language).content(tokens)
        return if (content.isEmpty()) Parsed.NotUnderstood else Parsed.Command(Verb.EXAMINE, content, implicit = true)
    }

    private fun parse(input: List<String>, lexicon: Lexicon): Parsed.Command? {
        val tokens = lexicon.stripLeadingNoise(input)

        if (tokens.size == 1) {
            lexicon.directions[tokens[0]]?.let { return Parsed.Command(Verb.GO, direction = it) }
            if (tokens[0] in lexicon.back) return Parsed.Command(Verb.BACK)
        }

        lexicon.phrases.firstOrNull { (phrase, _) -> tokens.size >= phrase.size && tokens.subList(0, phrase.size) == phrase }
            ?.let { (phrase, verb) -> return command(verb, tokens.drop(phrase.size), lexicon) }

        val first = tokens[0]
        val rest = tokens.drop(1)
        rest.lastOrNull()?.let { particle -> lexicon.separable[first to particle] }
            ?.let { verb -> return command(verb, rest.dropLast(1), lexicon) }
        lexicon.verbs[first]?.let { verb -> return command(verb, rest, lexicon) }

        // German infinitive at the end: "(ich will) den Schlüssel nehmen".
        if (tokens.size > 1) lexicon.verbs[tokens.last()]?.let { verb -> return command(verb, tokens.dropLast(1), lexicon) }

        // No verb, but a direction: "nach Norden", "back please".
        val content = lexicon.content(tokens)
        if (content.size == 1) {
            lexicon.directions[content[0]]?.let { return Parsed.Command(Verb.GO, direction = it) }
            if (content[0] in lexicon.back) return Parsed.Command(Verb.BACK)
        }
        return null
    }

    private fun command(verb: Verb, rest: List<String>, lexicon: Lexicon): Parsed.Command {
        val words = lexicon.content(rest)
        return when (verb) {
            Verb.GO -> {
                val explicit = words.firstNotNullOfOrNull { lexicon.directions[it] }
                // Directions that are also fillers ("in") or particles ("tritt ein").
                val weak = rest.firstNotNullOfOrNull { lexicon.directions[it] }
                    ?: rest.lastOrNull()?.let { lexicon.goParticles[it] }
                when {
                    explicit != null -> Parsed.Command(Verb.GO, direction = explicit)
                    words.any { it in lexicon.back } -> Parsed.Command(Verb.BACK)
                    else -> Parsed.Command(Verb.GO, words, weak)
                }
            }
            Verb.LOOK -> if (words.isEmpty()) Parsed.Command(Verb.LOOK) else Parsed.Command(Verb.EXAMINE, words)
            Verb.INVENTORY, Verb.HELP, Verb.WAIT, Verb.BACK -> Parsed.Command(verb)
            else -> Parsed.Command(verb, words)
        }
    }

    private fun lexicon(language: Language): Lexicon = when (language) {
        Language.DE -> GERMAN
        Language.EN -> ENGLISH
    }

    internal class Lexicon(
        val noise: List<List<String>>,
        val verbs: Map<String, Verb>,
        val separable: Map<Pair<String, String>, Verb>,
        val phrases: List<Pair<List<String>, Verb>>,
        val stopwords: Set<String>,
        val directions: Map<String, Direction>,
        val goParticles: Map<String, Direction>,
        val back: Set<String>,
    ) {
        /** Drops "ich will", "I want to", … but always keeps at least one word. */
        fun stripLeadingNoise(tokens: List<String>): List<String> {
            var result = tokens
            while (true) {
                val match = noise.firstOrNull { result.size > it.size && result.subList(0, it.size) == it } ?: return result
                result = result.drop(match.size)
            }
        }

        fun content(tokens: List<String>): List<String> = tokens.filter { it !in stopwords }
    }

    private class LexiconBuilder {
        val noise = mutableListOf<List<String>>()
        val verbs = mutableMapOf<String, Verb>()
        val separable = mutableMapOf<Pair<String, String>, Verb>()
        val phrases = mutableListOf<Pair<List<String>, Verb>>()
        val stopwords = mutableSetOf<String>()
        val directions = mutableMapOf<String, Direction>()
        val goParticles = mutableMapOf<String, Direction>()
        val back = mutableSetOf<String>()

        fun noise(vararg sequences: String) = sequences.forEach { noise += Words.tokens(it) }
        fun verb(verb: Verb, vararg words: String) = words.forEach { verbs[Words.fold(it)] = verb }
        fun separable(verb: Verb, stems: List<String>, particles: List<String>) {
            for (stem in stems) for (particle in particles) separable[Words.fold(stem) to Words.fold(particle)] = verb
        }
        fun phrase(verb: Verb, phrase: String) {
            phrases += Words.tokens(phrase) to verb
        }
        fun stop(vararg words: String) = words.forEach { stopwords += Words.fold(it) }
        fun direction(direction: Direction, vararg words: String) = words.forEach { directions[Words.fold(it)] = direction }
        fun back(vararg words: String) = words.forEach { back += Words.fold(it) }

        fun build() = Lexicon(
            // Longest sequences first, so "i want to" wins over "i".
            noise.sortedByDescending { it.size }, verbs, separable,
            phrases.sortedByDescending { it.first.size }, stopwords, directions, goParticles, back,
        )
    }

    private companion object {
        fun lexicon(block: LexiconBuilder.() -> Unit): Lexicon = LexiconBuilder().apply(block).build()

        val GERMAN = lexicon {
            noise("ich", "will", "möchte", "mochte", "werde", "würde", "versuche", "versuch", "bitte", "nun", "jetzt", "dann", "lass uns", "lasst uns", "wir")

            verb(
                Verb.GO, "geh", "gehe", "gehen", "lauf", "laufe", "laufen", "renn", "renne", "rennen", "wander", "wandere",
                "wandern", "reise", "reisen", "betritt", "betrete", "betreten", "kletter", "klettere", "klettern", "steig",
                "steige", "steigen", "folge", "folgen", "flieh", "fliehe", "fliehen", "beweg", "bewege", "bewegen",
                "schleich", "schleiche", "schleichen", "marschier", "marschiere", "tritt", "trete", "treten", "eintreten",
                "hineingehen", "reingehen",
            )
            verb(
                Verb.TAKE, "nimm", "nehme", "nehmen", "hol", "hole", "holen", "greif", "greife", "greifen", "schnapp",
                "schnappe", "schnappen", "pflück", "pflücke", "pflücken", "aufheben", "einstecken", "mitnehmen",
                "einpacken", "aufsammeln",
            )
            verb(Verb.DROP, "leg", "lege", "legen", "ablegen", "hinlegen", "wegwerfen", "fallenlassen", "wirf", "werfe", "werfen")
            verb(Verb.OPEN, "öffne", "öffnen", "aufschließen", "aufsperren", "entriegle", "entriegele", "entriegeln", "aufmachen")
            verb(
                Verb.EXAMINE, "untersuch", "untersuche", "untersuchen", "betracht", "betrachte", "betrachten", "prüf",
                "prüfe", "prüfen", "inspizier", "inspiziere", "inspizieren", "lies", "lese", "lesen", "begutachte",
                "begutachten", "ansehen", "anschauen", "angucken", "beäuge", "beäugen", "mustere", "mustern",
            )
            verb(
                Verb.LOOK, "schau", "schaue", "schauen", "sieh", "siehe", "seh", "sehe", "sehen", "guck", "gucke",
                "gucken", "blick", "blicke", "umsehen", "umschauen", "umgucken", "l",
            )
            verb(Verb.INVENTORY, "inventar", "inv", "i", "rucksack", "tasche", "taschen", "besitz", "habseligkeiten")
            verb(
                Verb.TALK, "sprich", "spreche", "sprechen", "rede", "red", "reden", "frag", "frage", "fragen", "grüß",
                "grüße", "grüßen", "unterhalte", "unterhalten", "plauder", "plaudere", "plaudern", "ansprechen", "anreden",
            )
            verb(Verb.HELP, "hilfe", "help", "befehle", "anleitung")
            verb(Verb.WAIT, "warte", "wart", "warten", "z", "ruhe", "raste", "rasten")
            verb(Verb.BACK, "umkehren")

            val look = listOf("sieh", "siehe", "seh", "sehe", "schau", "schaue", "guck", "gucke", "blick", "blicke")
            separable(Verb.EXAMINE, look, listOf("an"))
            separable(Verb.LOOK, look, listOf("um"))
            separable(Verb.TAKE, listOf("heb", "hebe", "sammel", "sammle", "sammele", "steck", "stecke", "pack", "packe"), listOf("auf", "ein"))
            separable(Verb.DROP, listOf("leg", "lege", "stell", "stelle"), listOf("ab", "hin", "weg", "nieder"))
            separable(Verb.DROP, listOf("wirf", "werfe"), listOf("weg", "fort"))
            separable(Verb.DROP, listOf("lass", "lasse"), listOf("fallen", "liegen"))
            separable(Verb.OPEN, listOf("schließ", "schließe", "sperr", "sperre", "mach", "mache"), listOf("auf"))
            separable(Verb.TALK, listOf("sprich", "spreche", "rede"), listOf("an"))
            separable(Verb.WAIT, listOf("ruh", "ruhe"), listOf("aus"))
            separable(Verb.BACK, listOf("kehr", "kehre"), listOf("um"))

            stop(
                "der", "die", "das", "den", "dem", "des", "ein", "eine", "einen", "einem", "einer", "eines", "mit", "zu",
                "zum", "zur", "nach", "auf", "im", "in", "ins", "an", "am", "ans", "vom", "von", "bei", "beim", "bitte",
                "mal", "doch", "hier", "da", "dort", "dorthin", "hin", "nochmal", "noch", "genauer", "näher", "etwas",
                "und", "gegen", "durch", "über", "richtung", "weiter", "los", "schnell", "langsam", "vorsichtig", "ganz",
                "kurz", "jetzt", "nun", "dann", "einmal", "dich", "dir", "mich", "mir", "sich", "uns", "euch",
            )

            direction(Direction.NORTH, "norden", "nord", "n")
            direction(Direction.EAST, "osten", "ost", "o")
            direction(Direction.SOUTH, "süden", "süd", "s")
            direction(Direction.WEST, "westen", "west", "w")
            direction(Direction.NORTHEAST, "nordosten", "nordost", "no")
            direction(Direction.NORTHWEST, "nordwesten", "nordwest", "nw")
            direction(Direction.SOUTHEAST, "südosten", "südost", "so")
            direction(Direction.SOUTHWEST, "südwesten", "südwest", "sw")
            direction(Direction.UP, "hoch", "rauf", "hinauf", "herauf", "oben", "aufwärts")
            direction(Direction.DOWN, "runter", "hinunter", "herunter", "hinab", "unten", "abwärts")
            direction(Direction.IN, "rein", "hinein", "herein", "drinnen")
            direction(Direction.OUT, "raus", "hinaus", "heraus", "draußen")
            goParticles[Words.fold("ein")] = Direction.IN
            back("zurück", "umkehren")
        }

        val ENGLISH = lexicon {
            noise("i want to", "i would like to", "i will", "i", "want to", "want", "will", "lets", "let us", "please", "try to", "now", "then")

            verb(Verb.GO, "go", "walk", "run", "head", "travel", "move", "enter", "climb", "follow", "flee", "proceed", "wander", "step")
            verb(Verb.TAKE, "take", "get", "grab", "collect", "pickup", "snatch", "obtain")
            verb(Verb.DROP, "drop", "discard", "throw", "toss")
            verb(Verb.OPEN, "open", "unlock", "unbolt", "unbar")
            verb(Verb.EXAMINE, "examine", "x", "inspect", "read", "check", "study", "observe", "search")
            verb(Verb.LOOK, "look", "l")
            verb(Verb.INVENTORY, "inventory", "inv", "i", "items", "bag", "possessions")
            verb(Verb.TALK, "talk", "speak", "ask", "greet", "chat", "converse", "address")
            verb(Verb.HELP, "help", "commands", "hilfe")
            verb(Verb.WAIT, "wait", "z", "rest")
            verb(Verb.BACK, "return", "retreat")

            separable(Verb.TAKE, listOf("pick"), listOf("up"))
            separable(Verb.DROP, listOf("put", "set"), listOf("down"))
            separable(Verb.BACK, listOf("turn", "go", "head"), listOf("back"))
            phrase(Verb.TAKE, "pick up")
            phrase(Verb.DROP, "put down")
            phrase(Verb.DROP, "set down")
            phrase(Verb.EXAMINE, "look at")
            phrase(Verb.EXAMINE, "look closely at")
            phrase(Verb.BACK, "turn back")
            phrase(Verb.BACK, "go back")

            stop(
                "the", "a", "an", "to", "at", "with", "on", "in", "into", "from", "please", "now", "then", "here",
                "there", "around", "closer", "closely", "carefully", "some", "and", "toward", "towards", "over",
                "through", "myself", "yourself", "again", "more",
            )

            direction(Direction.NORTH, "north", "n")
            direction(Direction.EAST, "east", "e")
            direction(Direction.SOUTH, "south", "s")
            direction(Direction.WEST, "west", "w")
            direction(Direction.NORTHEAST, "northeast", "ne")
            direction(Direction.NORTHWEST, "northwest", "nw")
            direction(Direction.SOUTHEAST, "southeast", "se")
            direction(Direction.SOUTHWEST, "southwest", "sw")
            direction(Direction.UP, "up", "u", "upstairs", "upward", "upwards")
            direction(Direction.DOWN, "down", "d", "downstairs", "downward", "downwards")
            direction(Direction.IN, "in", "inside")
            direction(Direction.OUT, "out", "outside")
            back("back", "return")
        }
    }
}
