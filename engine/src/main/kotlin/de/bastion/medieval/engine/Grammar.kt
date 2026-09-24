package de.bastion.medieval.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Gender {
    @SerialName("m") MASCULINE,
    @SerialName("f") FEMININE,
    @SerialName("n") NEUTER,
}

enum class Case { NOMINATIVE, ACCUSATIVE, DATIVE }

/**
 * How a thing or person is called in German. Articles and adjective endings are
 * generated from [gender], so content only states the base forms:
 * `{"noun": "Schlüssel", "gender": "m", "adjectives": ["rostig"]}` becomes
 * "der rostige Schlüssel", "den rostigen Schlüssel", "einen rostigen Schlüssel", …
 *
 * [adjectives] are declension stems ("rostig", "alt", "dunkl"). [synonyms] are extra
 * nouns the parser understands. A [name] (proper noun) replaces article and noun in output.
 * [accusative]/[dative] override the noun itself for n-declension ("Herr" → "Herrn").
 */
@Serializable
data class GermanNoun(
    val noun: String,
    val gender: Gender,
    val adjectives: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val name: String? = null,
    val accusative: String? = null,
    val dative: String? = null,
) {
    fun definite(case: Case = Case.NOMINATIVE): String =
        name ?: phrase(definiteArticle(case), case, ::weakEnding)

    fun indefinite(case: Case = Case.NOMINATIVE): String =
        name ?: phrase(indefiniteArticle(case), case, ::mixedEnding)

    private fun phrase(article: String, case: Case, ending: (Case, Gender) -> String): String {
        val suffix = ending(case, gender)
        val declined = adjectives.map { inflect(it, suffix) }
        val nounForm = when (case) {
            Case.NOMINATIVE -> noun
            Case.ACCUSATIVE -> accusative ?: noun
            Case.DATIVE -> dative ?: noun
        }
        return buildString {
            append(article)
            if (declined.isNotEmpty()) append(' ').append(declined.joinToString(", "))
            append(' ').append(nounForm)
        }
    }

    private fun definiteArticle(case: Case): String = when (case) {
        Case.NOMINATIVE -> when (gender) {
            Gender.MASCULINE -> "der"
            Gender.FEMININE -> "die"
            Gender.NEUTER -> "das"
        }
        Case.ACCUSATIVE -> when (gender) {
            Gender.MASCULINE -> "den"
            Gender.FEMININE -> "die"
            Gender.NEUTER -> "das"
        }
        Case.DATIVE -> when (gender) {
            Gender.FEMININE -> "der"
            else -> "dem"
        }
    }

    private fun indefiniteArticle(case: Case): String = when (case) {
        Case.NOMINATIVE -> if (gender == Gender.FEMININE) "eine" else "ein"
        Case.ACCUSATIVE -> when (gender) {
            Gender.MASCULINE -> "einen"
            Gender.FEMININE -> "eine"
            Gender.NEUTER -> "ein"
        }
        Case.DATIVE -> if (gender == Gender.FEMININE) "einer" else "einem"
    }

    private companion object {
        /** Adjective ending after a definite article (weak declension, singular). */
        fun weakEnding(case: Case, gender: Gender): String = when (case) {
            Case.NOMINATIVE -> "e"
            Case.ACCUSATIVE -> if (gender == Gender.MASCULINE) "en" else "e"
            Case.DATIVE -> "en"
        }

        /** Adjective ending after an indefinite article (mixed declension, singular). */
        fun mixedEnding(case: Case, gender: Gender): String = when (case) {
            Case.NOMINATIVE -> when (gender) {
                Gender.MASCULINE -> "er"
                Gender.FEMININE -> "e"
                Gender.NEUTER -> "es"
            }
            Case.ACCUSATIVE -> when (gender) {
                Gender.MASCULINE -> "en"
                Gender.FEMININE -> "e"
                Gender.NEUTER -> "es"
            }
            Case.DATIVE -> "en"
        }

        /** "müde" + "en" → "müden", not "müdeen". */
        fun inflect(stem: String, ending: String): String =
            if (stem.endsWith('e') && ending.startsWith('e')) stem + ending.drop(1) else stem + ending
    }
}

/**
 * How a thing or person is called in English. [indefiniteArticle] overrides the
 * automatic "a"/"an" (for example "some" for bread or "an" for "hour").
 */
@Serializable
data class EnglishNoun(
    val noun: String,
    val adjectives: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val name: String? = null,
    val indefiniteArticle: String? = null,
) {
    fun definite(): String = name ?: "the ${phrase()}"

    fun indefinite(): String {
        if (name != null) return name
        val body = phrase()
        val article = indefiniteArticle ?: if (body.first().lowercaseChar() in "aeiou") "an" else "a"
        return "$article $body"
    }

    private fun phrase(): String =
        if (adjectives.isEmpty()) noun else adjectives.joinToString(", ") + " " + noun
}

/** Words the parser accepts for one thing, already folded (see [Words.fold]). */
data class Vocabulary(val nouns: Set<String>, val adjectives: Set<String>)

/** The German and English naming of one thing or person. */
@Serializable
data class Naming(val de: GermanNoun, val en: EnglishNoun) {
    fun definite(language: Language, case: Case = Case.NOMINATIVE): String = when (language) {
        Language.DE -> de.definite(case)
        Language.EN -> en.definite()
    }

    fun indefinite(language: Language, case: Case = Case.NOMINATIVE): String = when (language) {
        Language.DE -> de.indefinite(case)
        Language.EN -> en.indefinite()
    }

    /** Short label for buttons: the proper name, or just the noun ("Schlüssel", "key"). */
    fun label(language: Language): String = when (language) {
        Language.DE -> de.name ?: de.noun
        Language.EN -> en.name ?: en.noun
    }

    fun vocabulary(language: Language): Vocabulary = when (language) {
        Language.DE -> Vocabulary(
            nouns = Words.foldAll(listOf(de.noun) + de.synonyms + listOfNotNull(de.name) + listOfNotNull(de.accusative, de.dative)),
            adjectives = Words.foldAll(de.adjectives),
        )
        Language.EN -> Vocabulary(
            nouns = Words.foldAll(listOf(en.noun) + en.synonyms + listOfNotNull(en.name)),
            adjectives = Words.foldAll(en.adjectives),
        )
    }
}

/** Joins "a, b and c" / "a, b und c". */
internal fun joinNatural(parts: List<String>, conjunction: String): String = when (parts.size) {
    0 -> ""
    1 -> parts[0]
    else -> parts.dropLast(1).joinToString(", ") + " $conjunction " + parts.last()
}
