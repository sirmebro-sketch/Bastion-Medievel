package de.bastion.medieval.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Words in both languages, for example extra names of a path ("Schänke", "tavern"). */
@Serializable
data class LocalizedWords(val de: List<String> = emptyList(), val en: List<String> = emptyList()) {
    operator fun get(language: Language): List<String> = when (language) {
        Language.DE -> de
        Language.EN -> en
    }
}

/**
 * A path out of a location. If [requires] names an item, the player needs it to pass:
 * without it they read [locked], the first time with it they read [unlock].
 */
@Serializable
data class Exit(
    val direction: Direction,
    val to: String,
    val keywords: LocalizedWords = LocalizedWords(),
    val requires: String? = null,
    val locked: LocalizedText? = null,
    val unlock: LocalizedText? = null,
)

@Serializable
data class Location(
    val id: String,
    val name: LocalizedText,
    val description: LocalizedText,
    val exits: List<Exit> = emptyList(),
    val items: List<String> = emptyList(),
    val people: List<String> = emptyList(),
)

/** A thing in the world. Scenery is part of the description and never listed separately. */
@Serializable
data class Item(
    val id: String,
    val de: GermanNoun,
    val en: EnglishNoun,
    val description: LocalizedText,
    val portable: Boolean = true,
    val scenery: Boolean = false,
) {
    val naming: Naming get() = Naming(de, en)
}

/** Someone to talk to. [talk] lines are told in order; the last one repeats. */
@Serializable
data class Person(
    val id: String,
    val de: GermanNoun,
    val en: EnglishNoun,
    val description: LocalizedText,
    val talk: List<LocalizedText> = emptyList(),
) {
    val naming: Naming get() = Naming(de, en)
}

@Serializable
data class WorldData(
    val title: LocalizedText,
    val intro: LocalizedText,
    val start: String,
    val locations: List<Location>,
    val items: List<Item> = emptyList(),
    val people: List<Person> = emptyList(),
)

/** The loaded, indexed world content. It never changes while playing; see [GameState]. */
class World(val data: WorldData) {
    val locations: Map<String, Location> = data.locations.associateBy { it.id }
    val items: Map<String, Item> = data.items.associateBy { it.id }
    val people: Map<String, Person> = data.people.associateBy { it.id }

    fun location(id: String): Location = locations.getValue(id)

    /** Where each item lies when a new game starts. */
    fun initialItemPlaces(): Map<String, String> =
        data.locations.flatMap { location -> location.items.map { it to location.id } }.toMap()

    /** All content errors; an empty list means the world is consistent. */
    fun validate(): List<String> = buildList {
        fun duplicates(ids: List<String>, kind: String) =
            ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { add("$kind '$it' is defined more than once") }
        duplicates(data.locations.map { it.id }, "Location")
        duplicates(data.items.map { it.id }, "Item")
        duplicates(data.people.map { it.id }, "Person")

        if (data.start !in locations) add("Start location '${data.start}' does not exist")
        text("title", data.title)
        text("intro", data.intro)

        val placedItems = mutableMapOf<String, String>()
        for (location in data.locations) {
            val where = "Location '${location.id}'"
            text("$where name", location.name)
            text("$where description", location.description)
            location.exits.groupingBy { it.direction }.eachCount().filterValues { it > 1 }.keys
                .forEach { add("$where has more than one exit ${it.name}") }
            for (exit in location.exits) {
                if (exit.to !in locations) add("$where: exit ${exit.direction} leads to unknown '${exit.to}'")
                exit.locked?.let { text("$where exit ${exit.direction} locked", it) }
                exit.unlock?.let { text("$where exit ${exit.direction} unlock", it) }
                if (exit.requires != null) {
                    if (exit.requires !in items) add("$where: exit ${exit.direction} requires unknown item '${exit.requires}'")
                    if (exit.locked == null) add("$where: exit ${exit.direction} is locked but has no 'locked' text")
                }
            }
            for (itemId in location.items) {
                if (itemId !in items) add("$where contains unknown item '$itemId'")
                placedItems.put(itemId, location.id)?.let { add("Item '$itemId' is placed in both '$it' and '${location.id}'") }
            }
            location.people.filter { it !in people }.forEach { add("$where contains unknown person '$it'") }
        }

        for (item in data.items) {
            naming("Item '${item.id}'", item.naming)
            text("Item '${item.id}' description", item.description)
            if (item.scenery && item.portable) add("Item '${item.id}' is scenery and must not be portable")
        }
        for (person in data.people) {
            naming("Person '${person.id}'", person.naming)
            text("Person '${person.id}' description", person.description)
            person.talk.forEachIndexed { index, line -> text("Person '${person.id}' talk[$index]", line) }
        }

        if (data.start in locations) {
            val unreachable = locations.keys - reachableFrom(data.start)
            unreachable.forEach { add("Location '$it' cannot be reached from the start") }
        }
    }

    private fun reachableFrom(start: String): Set<String> {
        val seen = mutableSetOf(start)
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            locations[queue.removeFirst()]?.exits?.forEach { if (seen.add(it.to)) queue.addLast(it.to) }
        }
        return seen
    }

    private fun MutableList<String>.text(where: String, text: LocalizedText) {
        if (text.de.isBlank()) add("$where: German text is missing")
        if (text.en.isBlank()) add("$where: English text is missing")
    }

    private fun MutableList<String>.naming(where: String, naming: Naming) {
        val german = naming.de
        if (german.noun.isBlank() || !german.noun.first().isUpperCase()) add("$where: German noun must be capitalized")
        if (german.adjectives.any { it.isBlank() || it.first().isUpperCase() }) add("$where: German adjectives must be lowercase stems")
        if (naming.en.noun.isBlank()) add("$where: English noun is missing")
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = false }

        fun parse(text: String): World = World(json.decodeFromString(WorldData.serializer(), text))

        /** The world bundled with the game. */
        fun loadDefault(): World {
            val stream = World::class.java.getResourceAsStream("/world/world.json")
                ?: error("world/world.json is missing from the engine resources")
            return parse(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        }
    }
}
