package de.bastion.medieval.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.bastion.medieval.R

/** Colors of ink, parchment, leather and gold. */
object Palette {
    val ParchmentLight = Color(0xFFF6EBCF)
    val Parchment = Color(0xFFEBD9B0)
    val ParchmentEdge = Color(0xFFCDB184)
    val Stain = Color(0xFF8A6A3B)
    val Ink = Color(0xFF2B1D14)
    val InkMuted = Color(0xFF6A5440)
    val Burgundy = Color(0xFF7A1F22)
    val BurgundyDark = Color(0xFF4E1215)
    val Gold = Color(0xFFD9B45A)
    val GoldDim = Color(0xFFA8863E)
    val Leather = Color(0xFF22150E)
    val LeatherLight = Color(0xFF3A261A)
}

object Fonts {
    val Cinzel = FontFamily(
        Font(R.font.cinzel_regular, FontWeight.Normal),
        Font(R.font.cinzel_bold, FontWeight.Bold),
    )
    val Garamond = FontFamily(
        Font(R.font.eb_garamond_regular, FontWeight.Normal),
        Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.eb_garamond_semibold, FontWeight.SemiBold),
    )
    val Fraktur = FontFamily(Font(R.font.unifraktur_maguntia))
}

private val BastionColors = lightColorScheme(
    primary = Palette.Burgundy,
    onPrimary = Palette.ParchmentLight,
    secondary = Palette.GoldDim,
    onSecondary = Palette.Ink,
    background = Palette.Parchment,
    onBackground = Palette.Ink,
    surface = Palette.ParchmentLight,
    onSurface = Palette.Ink,
    surfaceContainer = Palette.ParchmentLight,
    surfaceContainerHigh = Palette.ParchmentLight,
    surfaceContainerHighest = Palette.Parchment,
    onSurfaceVariant = Palette.InkMuted,
    outline = Palette.GoldDim,
)

private val BastionTypography = Typography().run {
    fun TextStyle.garamond() = copy(fontFamily = Fonts.Garamond)
    copy(
        bodyLarge = bodyLarge.garamond().copy(fontSize = 18.sp),
        bodyMedium = bodyMedium.garamond().copy(fontSize = 16.sp),
        bodySmall = bodySmall.garamond(),
        labelLarge = labelLarge.garamond().copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.garamond(),
        titleLarge = titleLarge.copy(fontFamily = Fonts.Cinzel, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = Fonts.Cinzel),
        headlineSmall = headlineSmall.copy(fontFamily = Fonts.Cinzel, fontWeight = FontWeight.Bold),
    )
}

@Composable
fun BastionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BastionColors, typography = BastionTypography, content = content)
}
