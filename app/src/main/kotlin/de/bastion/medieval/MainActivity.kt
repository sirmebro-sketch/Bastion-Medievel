package de.bastion.medieval

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import de.bastion.medieval.ui.BastionTheme
import de.bastion.medieval.ui.GameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The frame is dark leather at the top and bottom, so the system bars get light icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            BastionTheme {
                val game: GameViewModel = viewModel()
                GameScreen(
                    state = game.ui,
                    onSubmit = game::submit,
                    onLanguage = game::setLanguage,
                    onNewGame = game::newGame,
                )
            }
        }
    }
}
