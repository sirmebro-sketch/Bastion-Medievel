package de.bastion.medieval.ui

import de.bastion.medieval.engine.Language

/**
 * Texts of the app frame. They follow the in-game language setting rather than the
 * device language, so switching languages switches everything at once.
 */
data class UiStrings(
    val inputPlaceholder: String,
    val send: String,
    val menu: String,
    val help: String,
    val helpCommand: String,
    val newGame: String,
    val newGameTitle: String,
    val newGameText: String,
    val newGameConfirm: String,
    val cancel: String,
    val languageGerman: String,
    val languageEnglish: String,
) {
    companion object {
        private val GERMAN = UiStrings(
            inputPlaceholder = "Was tust du?",
            send = "Senden",
            menu = "Menü",
            help = "Hilfe",
            helpCommand = "hilfe",
            newGame = "Neues Spiel …",
            newGameTitle = "Neues Spiel beginnen?",
            newGameText = "Dein bisheriger Fortschritt geht dabei verloren.",
            newGameConfirm = "Neu beginnen",
            cancel = "Abbrechen",
            languageGerman = "Deutsch",
            languageEnglish = "English",
        )

        private val ENGLISH = UiStrings(
            inputPlaceholder = "What do you do?",
            send = "Send",
            menu = "Menu",
            help = "Help",
            helpCommand = "help",
            newGame = "New game …",
            newGameTitle = "Start a new game?",
            newGameText = "Your current progress will be lost.",
            newGameConfirm = "Start over",
            cancel = "Cancel",
            languageGerman = "Deutsch",
            languageEnglish = "English",
        )

        fun of(language: Language): UiStrings = when (language) {
            Language.DE -> GERMAN
            Language.EN -> ENGLISH
        }
    }
}
