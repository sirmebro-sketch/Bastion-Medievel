package de.bastion.medieval

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.bastion.medieval.engine.CharacterRules
import de.bastion.medieval.engine.Game
import de.bastion.medieval.engine.GameState
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
    private val rules = CharacterRules.loadDefault()
    private val store = SaveStore(application)
    private val settings = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val pendingSaves = Channel<SaveGame>(Channel.CONFLATED)
    private var nextId = 0L
    private var game: Game

    var ui: GameUiState by mutableStateOf(emptyState())
        private set

    init {
        val language = Language.fromCode(settings.getString(KEY_LANGUAGE, null)) ?: deviceLanguage()
        val showRolls = settings.getBoolean(KEY_SHOW_ROLLS, true)
        val save = store.load(world)
        game = if (save != null) Game(world, rules, save.state) else newGameInstance()
        val log = when {
            save == null -> game.opening(language)
            // A save from before character creation existed: start creating one now.
            save.state.character == null && save.state.creation == null -> save.log + game.resume(language)
            else -> save.log
        }
        ui = stateFor(language, entries(log), latestAnswerId = null, showRolls = showRolls)

        // One writer, and only the newest save matters.
        viewModelScope.launch(Dispatchers.IO) {
            for (save in pendingSaves) store.write(save)
        }
    }

    fun submit(input: String) = append(game.submit(input, ui.language))

    fun showSheet() = append(game.sheetView(ui.language))

    fun showCredits() = append(game.credits(ui.language))

    fun setShowRolls(show: Boolean) {
        settings.edit { putBoolean(KEY_SHOW_ROLLS, show) }
        ui = ui.copy(showRolls = show)
    }

    fun setLanguage(language: Language) {
        if (language == ui.language) return
        settings.edit { putString(KEY_LANGUAGE, language.code) }
        ui = stateFor(language, ui.log, ui.latestAnswerId, ui.showRolls)
    }

    fun newGame() {
        game = newGameInstance()
        val opening = entries(game.opening(ui.language))
        ui = stateFor(ui.language, opening, latestAnswerId = opening.first().id, showRolls = ui.showRolls)
        persist()
    }

    private fun append(paragraphs: List<Paragraph>) {
        if (paragraphs.isEmpty()) return
        val added = entries(paragraphs)
        val log = mergeLog(ui.log, added, SaveGame.MAX_LOG)
        ui = stateFor(ui.language, log, latestAnswerId = added.firstOrNull { it.paragraph.text.isNotEmpty() }?.id, showRolls = ui.showRolls)
        persist()
    }

    private fun newGameInstance() = Game(world, rules, GameState.new(world, seed = System.nanoTime()))

    private fun persist() {
        pendingSaves.trySend(SaveGame(state = game.state, log = ui.log.map { it.paragraph }))
    }

    private fun entries(paragraphs: List<Paragraph>) = paragraphs.map { LogEntry(nextId++, it) }

    private fun stateFor(language: Language, log: List<LogEntry>, latestAnswerId: Long?, showRolls: Boolean) = GameUiState(
        language = language,
        log = log,
        suggestions = game.suggestions(language),
        locationName = game.locationName(language),
        latestAnswerId = latestAnswerId,
        showRolls = showRolls,
        inCreation = game.inCreation,
    )

    private fun deviceLanguage(): Language =
        if (Locale.getDefault().language == Language.EN.code) Language.EN else Language.DE

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_SHOW_ROLLS = "show_rolls"

        fun emptyState() = GameUiState(Language.DE, emptyList(), emptyList(), "", null)
    }
}
