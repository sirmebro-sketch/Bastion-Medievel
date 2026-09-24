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
)
