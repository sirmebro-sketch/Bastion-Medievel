package de.bastion.medieval

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.bastion.medieval.engine.Game
import de.bastion.medieval.engine.Language
import de.bastion.medieval.engine.Paragraph
import de.bastion.medieval.engine.SaveGame
import de.bastion.medieval.engine.World
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.Locale

/** Owns the running game, the story log and saving after every turn. */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val world = World.loadDefault()
    private val store = SaveStore(application)
    private val settings = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val pendingSaves = Channel<SaveGame>(Channel.CONFLATED)
    private var nextId = 0L
    private var game: Game

    var ui: GameUiState by mutableStateOf(emptyState())
        private set

    init {
        val language = Language.fromCode(settings.getString(KEY_LANGUAGE, null)) ?: deviceLanguage()
        val save = store.load(world)
        game = if (save != null) Game(world, save.state) else Game(world)
        val log = save?.log ?: game.opening(language)
        ui = stateFor(language, entries(log), latestAnswerId = null)

        // One writer, and only the newest save matters.
        viewModelScope.launch(Dispatchers.IO) {
            for (save in pendingSaves) store.write(save)
        }
    }

    fun submit(input: String) {
        val answer = game.submit(input, ui.language)
        if (answer.isEmpty()) return
        val added = entries(answer)
        ui = stateFor(ui.language, (ui.log + added).takeLast(SaveGame.MAX_LOG), latestAnswerId = added.first().id)
        persist()
    }

    fun setLanguage(language: Language) {
        if (language == ui.language) return
        settings.edit { putString(KEY_LANGUAGE, language.code) }
        ui = stateFor(language, ui.log, ui.latestAnswerId)
    }

    fun newGame() {
        game = Game(world)
        val opening = entries(game.opening(ui.language))
        ui = stateFor(ui.language, opening, latestAnswerId = opening.first().id)
        persist()
    }

    private fun persist() {
        pendingSaves.trySend(SaveGame(state = game.state, log = ui.log.map { it.paragraph }))
    }

    private fun entries(paragraphs: List<Paragraph>) = paragraphs.map { LogEntry(nextId++, it) }

    private fun stateFor(language: Language, log: List<LogEntry>, latestAnswerId: Long?) = GameUiState(
        language = language,
        log = log,
        suggestions = game.suggestions(language),
        locationName = game.locationName(language),
        latestAnswerId = latestAnswerId,
    )

    private fun deviceLanguage(): Language =
        if (Locale.getDefault().language == Language.EN.code) Language.EN else Language.DE

    private companion object {
        const val KEY_LANGUAGE = "language"

        fun emptyState() = GameUiState(Language.DE, emptyList(), emptyList(), "", null)
    }
}
