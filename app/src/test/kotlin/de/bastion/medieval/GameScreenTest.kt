package de.bastion.medieval

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import de.bastion.medieval.engine.Game
import de.bastion.medieval.engine.Language
import de.bastion.medieval.engine.Paragraph
import de.bastion.medieval.engine.World
import de.bastion.medieval.ui.BastionTheme
import de.bastion.medieval.ui.GameScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real game screen on the JVM. Besides checking the controls, it records
 * screenshots (./gradlew :app:recordRoborazziDebug) that CI publishes for review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class GameScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val world = World.loadDefault()

    /** Plays [commands] and returns the screen state, scrolled to the latest answer. */
    private fun played(language: Language, vararg commands: String): GameUiState {
        val game = Game(world)
        var id = 0L
        val opening = game.opening(language).map { LogEntry(id++, it) }
        val answers = commands.map { command -> game.submit(command, language).map { LogEntry(id++, it) } }
        val latest = answers.lastOrNull()?.firstOrNull()?.id ?: opening.first().id
        return GameUiState(
            language = language,
            log = opening + answers.flatten(),
            suggestions = game.suggestions(language),
            locationName = game.locationName(language),
            latestAnswerId = latest,
        )
    }

    private fun show(state: GameUiState, onSubmit: (String) -> Unit = {}) {
        compose.setContent {
            BastionTheme {
                GameScreen(state = state, onSubmit = onSubmit, onLanguage = {}, onNewGame = {})
            }
        }
    }

    @Test
    fun openingInGerman() {
        show(played(Language.DE))
        // Once in the header, once as the title in the story.
        compose.onAllNodesWithText("Kreuzweg im Nebel").assertCountEquals(2)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/01_opening_de.png")
    }

    @Test
    fun conversationInEnglish() {
        show(played(Language.EN, "e", "go in", "talk to Marta"))
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/02_tavern_en.png")
    }

    @Test
    fun lockedGateInGerman() {
        show(played(Language.DE, "n", "n", "öffne das Tor"))
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/03_gate_de.png")
    }

    @Test
    fun typedCommandIsSent() {
        var sent: String? = null
        show(played(Language.DE)) { sent = it }
        compose.onNodeWithTag("input").performTextInput("  geh nach Norden ")
        compose.onNodeWithContentDescription("Senden").performClick()
        assertEquals("geh nach Norden", sent)
    }

    @Test
    fun suggestionSendsItsCommand() {
        var sent: String? = null
        show(played(Language.DE)) { sent = it }
        compose.onNodeWithText("Nach Osten").performClick()
        assertEquals("geh nach Osten", sent)
    }

    @Test
    fun everyParagraphKindIsRendered() {
        val state = played(Language.DE, "n", "nimm den Pfeil", "hilfe")
        val kinds = state.log.map { it.paragraph.kind }.toSet()
        assertEquals(Paragraph.Kind.entries.toSet() - Paragraph.Kind.DIALOGUE, kinds)
        show(state)
        compose.onNodeWithTag("story").assertExists()
    }
}
