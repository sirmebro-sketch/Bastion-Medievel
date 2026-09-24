package de.bastion.medieval.engine

import kotlinx.serialization.Serializable

/** German is the primary language; every text must also exist in English. */
enum class Language(val code: String) {
    DE("de"),
    EN("en"),
    ;

    val other: Language get() = if (this == DE) EN else DE

    companion object {
        fun fromCode(code: String?): Language? = entries.firstOrNull { it.code == code }
    }
}

/**
 * A text in both languages. Both fields are required, so content without a
 * translation fails to load instead of silently showing the wrong language.
 *
 * Texts the player reads may adapt to the character (see [render]):
 * - [variants] replace the whole text when their condition matches ("an elf enters…"),
 * - `[[male|female]]` picks a word by the character's gender ("[[Fremder|Fremde]]"),
 * - `{name}` inserts the character's name.
 */
@Serializable
data class LocalizedText(
    val de: String,
    val en: String,
    val variants: List<TextVariant> = emptyList(),
) {
    operator fun get(language: Language): String = when (language) {
        Language.DE -> de
        Language.EN -> en
    }

    /** Replaces `{0}`, `{1}`, … with the given arguments. */
    fun format(language: Language, vararg args: String): String =
        args.foldIndexed(get(language)) { index, text, arg -> text.replace("{$index}", arg) }

    /** The text for this character: first matching variant, gendered words and name filled in. */
    fun render(language: Language, context: TextContext): String {
        val chosen = variants.firstOrNull { it.condition.matches(context) }?.text ?: this
        return context.fill(chosen[language])
    }
}

/** An alternative text used when [condition] matches the character or the story so far. */
@Serializable
data class TextVariant(
    @kotlinx.serialization.SerialName("when") val condition: Condition,
    val text: LocalizedText,
)

internal fun String.capitalizeFirst(): String =
    if (isEmpty()) this else substring(0, 1).uppercase() + substring(1)
