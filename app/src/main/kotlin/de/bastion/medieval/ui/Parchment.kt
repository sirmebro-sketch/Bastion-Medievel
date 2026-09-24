package de.bastion.medieval.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

val LeatherBrush = Brush.verticalGradient(listOf(Palette.LeatherLight, Palette.Leather))

/**
 * Paints an aged parchment page: warm gradient, faint stains, paper grain and darker
 * edges. Everything is generated from a fixed seed, so the page looks the same each time.
 */
fun Modifier.parchment(): Modifier = drawWithCache {
    val width = size.width
    val height = size.height
    val random = Random(1_214)

    val base = Brush.radialGradient(
        0f to Palette.ParchmentLight,
        0.6f to Palette.Parchment,
        1f to Palette.ParchmentEdge,
        center = Offset(width * 0.5f, height * 0.35f),
        radius = max(width, height) * 0.85f,
    )
    val stains = List(14) {
        val center = Offset(random.nextFloat() * width, random.nextFloat() * height)
        val radius = (0.08f + random.nextFloat() * 0.22f) * min(width, height)
        val brush = Brush.radialGradient(
            listOf(Palette.Stain.copy(alpha = 0.025f + random.nextFloat() * 0.05f), Color.Transparent),
            center = center,
            radius = radius,
        )
        Triple(center, radius, brush)
    }
    val grain = List(2_000) { Offset(random.nextFloat() * width, random.nextFloat() * height) }
    val darkGrain = grain.subList(0, grain.size / 2)
    val lightGrain = grain.subList(grain.size / 2, grain.size)
    val edgeTopBottom = Brush.verticalGradient(
        0f to Palette.Stain.copy(alpha = 0.22f),
        0.04f to Color.Transparent,
        0.96f to Color.Transparent,
        1f to Palette.Stain.copy(alpha = 0.25f),
    )
    val edgeSides = Brush.horizontalGradient(
        0f to Palette.Stain.copy(alpha = 0.18f),
        0.035f to Color.Transparent,
        0.965f to Color.Transparent,
        1f to Palette.Stain.copy(alpha = 0.18f),
    )

    onDrawBehind {
        drawRect(base)
        stains.forEach { (center, radius, brush) -> drawCircle(brush, radius, center) }
        drawPoints(darkGrain, PointMode.Points, Palette.Stain.copy(alpha = 0.10f), strokeWidth = 1.2f)
        drawPoints(lightGrain, PointMode.Points, Color.White.copy(alpha = 0.18f), strokeWidth = 1.2f)
        drawRect(edgeTopBottom)
        drawRect(edgeSides)
    }
}

/** A thin gold line that fades out at both ends. */
@Composable
fun GoldRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.5.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Palette.GoldDim, Palette.Gold, Palette.GoldDim, Color.Transparent))),
    )
}

/** Line – diamond – line, drawn under location titles. */
@Composable
fun Ornament(modifier: Modifier = Modifier, color: Color = Palette.GoldDim) {
    Canvas(modifier) {
        val middle = size.width / 2
        val y = size.height / 2
        val diamond = size.height / 2
        val stroke = 1.2.dp.toPx()
        drawLine(color, Offset(0f, y), Offset(middle - diamond * 1.8f, y), stroke)
        drawLine(color, Offset(middle + diamond * 1.8f, y), Offset(size.width, y), stroke)
        val path = Path().apply {
            moveTo(middle, y - diamond)
            lineTo(middle + diamond, y)
            lineTo(middle, y + diamond)
            lineTo(middle - diamond, y)
            close()
        }
        drawPath(path, color)
    }
}
