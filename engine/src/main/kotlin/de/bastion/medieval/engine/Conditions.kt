package de.bastion.medieval.engine

import kotlinx.serialization.Serializable

/**
 * A condition on the character or the story so far. Every field that is set must
 * match; an empty condition always matches. Used for text variants and, later, for
 * choices that only some characters get.
 */
@Serializable
data class Condition(
    val gender: CharacterGender? = null,
    val species: String? = null,
    val career: String? = null,
    val background: String? = null,
    val trait: String? = null,
    /** A special ability from species, former life or trait, e.g. "lockpicking". */
    val feature: String? = null,
    val flag: String? = null,
    val notFlag: String? = null,
) {
    fun matches(context: TextContext): Boolean {
        val character = context.character
        if (gender != null && character?.gender != gender) return false
        if (species != null && character?.species != species) return false
        if (career != null && character?.career != career) return false
        if (background != null && character?.background != background) return false
        if (trait != null && (character == null || trait !in character.traits)) return false
        if (feature != null && feature !in context.features) return false
        if (flag != null && flag !in context.flags) return false
        if (notFlag != null && notFlag in context.flags) return false
        return true
    }
}

/**
 * What texts may depend on: the character (if already created), the story flags, and
 * during character creation the gender and name chosen so far.
 */
class TextContext(
    val character: Character?,
    val features: Set<String> = emptySet(),
    val flags: Set<String> = emptySet(),
    val gender: CharacterGender? = character?.gender,
    val name: String? = character?.name,
) {
    fun fill(text: String): String {
        val gendered = GENDERED.replace(text) { match ->
            if (gender == CharacterGender.FEMALE) match.groupValues[2] else match.groupValues[1]
        }
        return if (name == null) gendered else gendered.replace("{name}", name)
    }

    companion object {
        val NONE = TextContext(null)
        private val GENDERED = Regex("""\[\[([^|\]]*)\|([^\]]*)]]""")
    }
}
