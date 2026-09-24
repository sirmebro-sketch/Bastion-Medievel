package de.bastion.medieval

import androidx.compose.runtime.Immutable
import de.bastion.medieval.engine.Language
import de.bastion.medieval.engine.Paragraph
import de.bastion.medieval.engine.Suggestion

/** A paragraph of the story log with a stable id for list animations. */
@Immutable
data class LogEntry(val id: Long, val paragraph: Paragraph)

@Immutable
data class GameUiState(
    val language: Language,
    val log: List<LogEntry>,
    val suggestions: List<Suggestion>,
    val locationName: String,
    /** Id of the first entry of the latest answer; the screen scrolls there. */
    val latestAnswerId: Long?,
    /** Show dice rolls in the story (menu toggle, on by default). */
    val showRolls: Boolean = true,
    /** Character creation is still running: no character sheet yet. */
    val inCreation: Boolean = false,
)

/**
 * Appends [added] to [log]. Paragraphs with a slot replace earlier ones of the same
 * slot; empty paragraphs only clear their slot. Keeps at most [max] entries.
 */
fun mergeLog(log: List<LogEntry>, added: List<LogEntry>, max: Int): List<LogEntry> {
    val slots = added.mapNotNull { it.paragraph.slot }.toSet()
    val kept = if (slots.isEmpty()) log else log.filter { it.paragraph.slot !in slots }
    return (kept + added.filter { it.paragraph.text.isNotEmpty() }).takeLast(max)
}
