package de.bastion.medieval

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import de.bastion.medieval.engine.CharacterRules
import de.bastion.medieval.engine.Game
import de.bastion.medieval.engine.GameState
import de.bastion.medieval.engine.Language
import de.bastion.medieval.engine.Paragraph
import de.bastion.medieval.engine.SaveGame
import de.bastion.medieval.engine.World
import de.bastion.medieval.ui.BastionTheme
import de.bastion.medieval.ui.GameScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private val rules = CharacterRules.loadDefault()

    /** Plays [commands] from a new game and returns the screen state at the latest answer. */
    private fun played(language: Language, vararg commands: String, seed: Long = 3, showRolls: Boolean = true): GameUiState {
        val game = Game(world, rules, GameState.new(world, seed))
        var id = 0L
        var log = game.opening(language).map { LogEntry(id++, it) }
        var latest = log.first().id
        for (command in commands) {
            val added = game.submit(command, language).map { LogEntry(id++, it) }
            log = mergeLog(log, added, SaveGame.MAX_LOG)
            latest = added.first().id
        }
        return GameUiState(
            language = language,
            log = log,
            suggestions = game.suggestions(language),
            locationName = game.locationName(language),
            latestAnswerId = latest,
            showRolls = showRolls,
            inCreation = game.inCreation,
        )
    }

    private val createdDe = arrayOf("weiblich", "Wiebke", "Elfe", "Langfinger", "Gossenkind", "vorschlag", "fertig", "Wachsam")

    private fun show(state: GameUiState, onSubmit: (String) -> Unit = {}) {
        compose.setContent {
            BastionTheme {
                GameScreen(state = state, onSubmit = onSubmit, onLanguage = {}, onNewGame = {})
            }
        }
    }

    @Test
    fun creationStartsTheGame() {
        show(played(Language.DE))
        compose.onNodeWithText("Charaktererschaffung").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/01_creation_de.png")
    }

    @Test
    fun pointBuy() {
        show(played(Language.DE, "weiblich", "Wiebke", "Elfe", "Langfinger", "Gossenkind", "Geschick 15", "Konstitution 14"))
        compose.onAllNodesWithTag("ability-icon").assertCountEquals(6)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/02_point_buy_de.png")
    }

    @Test
    fun summarySheet() {
        show(played(Language.DE, *createdDe))
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/03_summary_de.png")
    }

    @Test
    fun pickingTheGateLock() {
        val state = played(Language.DE, *createdDe, "los", "n", "n", "knack das Schloss")
        assertTrue(state.log.any { it.paragraph.kind == Paragraph.Kind.ROLL })
        show(state)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/04_gate_roll_de.png")
    }

    @Test
    fun conversationInEnglish() {
        show(played(Language.EN, "male", "Konrad", "human", "choirboy", "gutter child", "suggestion", "done", "alert", "lucky", "stealth", "begin", "e", "go in", "talk to Marta"))
        compose.onAllNodesWithText("The Crooked Horn Tavern").assertCountEquals(2)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/05_tavern_en.png")
    }

    @Test
    fun rollsCanBeHidden() {
        val state = played(Language.DE, *createdDe, "los", "n", "n", "knack das Schloss", showRolls = false)
        show(state)
        compose.onAllNodesWithText("Probe", substring = true).assertCountEquals(0)
    }

    @Test
    fun typedCommandIsSent() {
        var sent: String? = null
        show(played(Language.DE)) { sent = it }
        compose.onNodeWithTag("input").performTextInput("  Wiebke ")
        compose.onNodeWithContentDescription("Senden").performClick()
        assertEquals("Wiebke", sent)
    }

    @Test
    fun suggestionSendsItsCommand() {
        var sent: String? = null
        show(played(Language.DE)) { sent = it }
        compose.onNodeWithText("Weiblich").performClick()
        assertEquals("weiblich", sent)
    }

    @Test
    fun everyParagraphKindIsRendered() {
        val log = Paragraph.Kind.entries.mapIndexed { index, kind ->
            val text = when (kind) {
                Paragraph.Kind.TABLE -> "Attribut\tWert\tMod.\nStärke\t15\t+2"
                Paragraph.Kind.OPTION -> "Name — Beschreibung"
                else -> "$kind"
            }
            LogEntry(index.toLong(), Paragraph(kind, text))
        }
        show(GameUiState(Language.DE, log, emptyList(), "Test", null))
        compose.onNodeWithTag("story").assertExists()
        compose.onNodeWithText("ROLL", substring = true).assertExists()
    }
}
