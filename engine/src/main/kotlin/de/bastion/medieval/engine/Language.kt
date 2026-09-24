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
 */
@Serializable
data class LocalizedText(val de: String, val en: String) {
    operator fun get(language: Language): String = when (language) {
        Language.DE -> de
        Language.EN -> en
    }

    /** Replaces `{0}`, `{1}`, … with the given arguments. */
    fun format(language: Language, vararg args: String): String =
        args.foldIndexed(get(language)) { index, text, arg -> text.replace("{$index}", arg) }
}

internal fun String.capitalizeFirst(): String =
    if (isEmpty()) this else substring(0, 1).uppercase() + substring(1)
