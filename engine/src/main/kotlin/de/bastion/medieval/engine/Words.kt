package de.bastion.medieval.engine

import kotlin.math.abs

/** Normalizes player input and content words so they can be compared. */
object Words {
    /**
     * Lowercases and folds German special letters, so "Schlüssel", "schluessel" and
     * "SCHLÜSSEL" all become "schluessel".
     */
    fun fold(text: String): String = text.lowercase()
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")

    /** Splits input into folded words; apostrophes are dropped ("let's" → "lets"). */
    fun tokens(text: String): List<String> = fold(text)
        .replace(Regex("['’`´]"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .split(' ')
        .filter { it.isNotEmpty() }

    fun foldAll(words: Iterable<String>): Set<String> = words.flatMap { tokens(it) }.toSet()

    /** German inflection endings a typed word may carry beyond its stem ("rostig" → "rostigen"). */
    private val INFLECTION_SUFFIXES = setOf("e", "en", "er", "es", "em", "n", "s", "ns")

    /**
     * True if the typed [word] means the content [term]: exact, inflected ("rostigen" for
     * "rostig", "keys" for "key") or with one typo in words of five or more letters.
     */
    fun matches(word: String, term: String): Boolean {
        if (word == term) return true
        if (word.length > term.length && word.startsWith(term) &&
            word.substring(term.length) in INFLECTION_SUFFIXES
        ) {
            return true
        }
        if (term.length >= 5 && abs(word.length - term.length) <= 1 && distance(word, term) <= 1) return true
        return false
    }

    /** Optimal string alignment distance: insertions, deletions, substitutions and swaps. */
    internal fun distance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }
}
