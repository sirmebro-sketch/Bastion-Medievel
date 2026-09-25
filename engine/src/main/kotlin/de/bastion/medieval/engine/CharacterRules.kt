package de.bastion.medieval.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A special ability; its [id] can be used in conditions and world content ("lockpicking"). */
@Serializable
data class Feature(val id: String, val name: LocalizedText, val description: LocalizedText)

/** The fitting weak spot of a former life: disadvantage on [disadvantage] checks. */
@Serializable
data class Weakness(
    val id: String,
    val name: LocalizedText,
    val description: LocalizedText,
    val disadvantage: List<Skill> = emptyList(),
)

/**
 * A people (the SRD calls it species). [extraSkill] and [extraTrait] give humans their
 * versatility; [placeholder] marks a people whose rules are not written yet.
 */
@Serializable
data class SpeciesDef(
    val id: String,
    val name: LocalizedText,
    val summary: LocalizedText,
    val description: LocalizedText,
    val features: List<Feature> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val advantages: List<Skill> = emptyList(),
    val hpPerLevel: Int = 0,
    val extraSkill: Boolean = false,
    val extraTrait: Boolean = false,
    val placeholder: Boolean = false,
)

/**
 * A former life ("Werdegang"): what the young character has done so far. Replaces
 * classes. [focus] orders the abilities for the recommended point-buy spread.
 */
@Serializable
data class CareerDef(
    val id: String,
    val name: LocalizedText,
    val summary: LocalizedText,
    val description: LocalizedText,
    val skills: List<Skill>,
    val feature: Feature,
    val weakness: Weakness,
    val hitDie: Int,
    val focus: List<Ability>,
    val advantages: List<Skill> = emptyList(),
    val equipment: List<LocalizedText> = emptyList(),
    val coins: Int = 0,
    /** The peoples who can have this former life; empty means every people. */
    val species: List<String> = emptyList(),
)

/**
 * A background: the story before the game. [bonus] are the SRD's +2/+1 ability
 * increases; [flags] are set at the start and let the story react.
 */
@Serializable
data class BackgroundDef(
    val id: String,
    val name: LocalizedText,
    val summary: LocalizedText,
    val description: LocalizedText,
    val consequences: LocalizedText,
    val bonus: Map<Ability, Int>,
    val flags: List<String> = emptyList(),
    val equipment: List<LocalizedText> = emptyList(),
    val coins: Int = 0,
)

/** A personal trait chosen at the end of character creation. */
@Serializable
data class TraitDef(
    val id: String,
    val name: LocalizedText,
    val description: LocalizedText,
    val skills: List<Skill> = emptyList(),
    val advantages: List<Skill> = emptyList(),
    val abilityBonus: Map<Ability, Int> = emptyMap(),
    val hpPerLevel: Int = 0,
    /** Luck points equal to the proficiency bonus: a failed check is rolled again. */
    val luck: Boolean = false,
    val features: List<Feature> = emptyList(),
)

@Serializable
data class NameSuggestions(val male: List<String>, val female: List<String>)

@Serializable
data class CharacterRules(
    val species: List<SpeciesDef>,
    val careers: List<CareerDef>,
    val backgrounds: List<BackgroundDef>,
    val traits: List<TraitDef>,
    val names: NameSuggestions,
) {
    fun species(id: String): SpeciesDef = species.first { it.id == id }
    fun career(id: String): CareerDef = careers.first { it.id == id }

    /** The former lives open to [speciesId], or all of them while no people is chosen. */
    fun careersFor(speciesId: String?): List<CareerDef> =
        careers.filter { it.species.isEmpty() || speciesId == null || speciesId in it.species }
    fun background(id: String): BackgroundDef = backgrounds.first { it.id == id }
    fun trait(id: String): TraitDef = traits.first { it.id == id }

    /** Every feature id that some species, former life or trait can grant. */
    val featureIds: Set<String> by lazy {
        (species.flatMap { s -> s.features.map { it.id } } + careers.map { it.feature.id } +
            traits.flatMap { t -> t.features.map { it.id } }).toSet()
    }

    /** All content errors; an empty list means the rules are consistent. */
    fun validate(): List<String> = buildList {
        fun duplicates(ids: List<String>, kind: String) =
            ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { add("$kind '$it' is defined more than once") }
        duplicates(species.map { it.id }, "Species")
        duplicates(careers.map { it.id }, "Career")
        duplicates(backgrounds.map { it.id }, "Background")
        duplicates(traits.map { it.id }, "Trait")

        fun text(where: String, text: LocalizedText) {
            if (text.de.isBlank()) add("$where: German text is missing")
            if (text.en.isBlank()) add("$where: English text is missing")
        }
        fun feature(where: String, feature: Feature) {
            text("$where feature ${feature.id} name", feature.name)
            text("$where feature ${feature.id} description", feature.description)
        }

        for (s in species) {
            text("Species '${s.id}' name", s.name)
            text("Species '${s.id}' summary", s.summary)
            text("Species '${s.id}' description", s.description)
            s.features.forEach { feature("Species '${s.id}'", it) }
        }
        for (c in careers) {
            val where = "Career '${c.id}'"
            text("$where name", c.name)
            text("$where summary", c.summary)
            text("$where description", c.description)
            feature(where, c.feature)
            text("$where weakness name", c.weakness.name)
            text("$where weakness description", c.weakness.description)
            if (c.skills.size != 2) add("$where must give exactly two skills")
            if (c.weakness.disadvantage.any { it in c.skills }) add("$where: weakness hits one of its own skills")
            if (c.focus.toSet() != Ability.entries.toSet() || c.focus.size != Ability.entries.size) {
                add("$where: focus must list every ability exactly once")
            }
            if (c.hitDie !in listOf(6, 8, 10, 12)) add("$where: hit die must be d6, d8, d10 or d12")
            c.equipment.forEachIndexed { i, e -> text("$where equipment[$i]", e) }
            c.species.filter { id -> species.none { it.id == id } }.forEach { add("$where: unknown people '$it'") }
        }
        for (s in species) {
            if (careersFor(s.id).isEmpty()) add("Species '${s.id}' has no former life to choose")
        }
        for (b in backgrounds) {
            val where = "Background '${b.id}'"
            text("$where name", b.name)
            text("$where summary", b.summary)
            text("$where description", b.description)
            text("$where consequences", b.consequences)
            if (b.bonus.values.sorted() != listOf(1, 2)) add("$where: bonus must be +2 and +1")
            b.equipment.forEachIndexed { i, e -> text("$where equipment[$i]", e) }
        }
        for (t in traits) {
            text("Trait '${t.id}' name", t.name)
            text("Trait '${t.id}' description", t.description)
            t.features.forEach { feature("Trait '${t.id}'", it) }
        }
        if (names.male.isEmpty() || names.female.isEmpty()) add("Name suggestions are missing")
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = false }

        fun parse(text: String): CharacterRules = json.decodeFromString(serializer(), text)

        /** The rules bundled with the game. */
        fun loadDefault(): CharacterRules {
            val stream = CharacterRules::class.java.getResourceAsStream("/rules/character.json")
                ?: error("rules/character.json is missing from the engine resources")
            return parse(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        }
    }
}
