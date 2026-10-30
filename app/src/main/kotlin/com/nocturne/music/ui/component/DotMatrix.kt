/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * Nothing OS / Dot-Matrix Design Language System
 */

package com.nocturne.music.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor

/**
 * Nothing OS Signature Colors
 */
val NothingRed = Color(0xFFD71921)
val NothingDarkBackground = Color(0xFF000000)
val NothingSurfaceDark = Color(0xFF0D0D0E)
val NothingBorderMuted = Color(0x2EFFFFFF)

/**
 * Modifier that draws a subtle Nothing OS dot-matrix grid over the background.
 */
fun Modifier.dotMatrixGrid(
    dotColor: Color = Color.White,
    dotRadius: Dp = 1.dp,
    spacing: Dp = 12.dp,
    alpha: Float = 0.08f,
): Modifier = this.drawBehind {
    val radiusPx = dotRadius.toPx()
    val spacingPx = spacing.toPx()
    val color = dotColor.copy(alpha = alpha)

    val cols = floor(size.width / spacingPx).toInt()
    val rows = floor(size.height / spacingPx).toInt()
    val startX = (size.width - (cols * spacingPx)) / 2f
    val startY = (size.height - (rows * spacingPx)) / 2f

    for (c in 0..cols) {
        val x = startX + c * spacingPx
        for (r in 0..rows) {
            val y = startY + r * spacingPx
            drawCircle(
                color = color,
                radius = radiusPx,
                center = Offset(x, y),
            )
        }
    }
}

/**
 * Modifier that draws a perimeter of dots around the component bounds.
 */
fun Modifier.dotMatrixBorder(
    dotColor: Color = Color.White.copy(alpha = 0.28f),
    dotSpacing: Dp = 8.dp,
    dotRadius: Dp = 1.2.dp,
): Modifier = this.drawBehind {
    val spacingPx = dotSpacing.toPx()
    val radiusPx = dotRadius.toPx()

    // Top and bottom edges
    var x = spacingPx / 2f
    while (x < size.width) {
        drawCircle(dotColor, radiusPx, Offset(x, radiusPx))
        drawCircle(dotColor, radiusPx, Offset(x, size.height - radiusPx))
        x += spacingPx
    }

    // Left and right edges
    var y = spacingPx / 2f
    while (y < size.height) {
        drawCircle(dotColor, radiusPx, Offset(radiusPx, y))
        drawCircle(dotColor, radiusPx, Offset(size.width - radiusPx, y))
        y += spacingPx
    }
}

/**
 * Nothing OS LED Indicator (flashing or solid dot like the Phone's recording glyph).
 */
@Composable
fun NothingLedDot(
    active: Boolean = true,
    pulsing: Boolean = false,
    color: Color = NothingRed,
    size: Dp = 6.dp,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nothing_led")
    val alpha by if (pulsing && active) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(850),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "led_pulse",
        )
    } else {
        androidx.compose.runtime.rememberUpdatedState(if (active) 1.0f else 0.25f)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

/**
 * Nothing-styled technical status badge (e.g. "● LOSSLESS", "● BIT CHORD", "● HI-RES").
 */
@Composable
fun DotMatrixBadge(
    text: String,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    pulsing: Boolean = false,
    showLed: Boolean = true,
    ledColor: Color = NothingRed,
    containerColor: Color = Color(0xFF141416),
    contentColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(100.dp),
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x33FFFFFF)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showLed) {
                NothingLedDot(
                    active = active,
                    pulsing = pulsing,
                    color = ledColor,
                    size = 5.dp,
                )
            }
            Text(
                text = text.uppercase(),
                color = contentColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
        }
    }
}

/**
 * Nothing OS section header with dot-matrix ornamentation and wide tracking.
 */
@Composable
fun DotMatrixHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    glyphCount: Int = 3,
    accentColor: Color = NothingRed,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Decorative dot-matrix glyphs
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(glyphCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) accentColor else Color.White.copy(alpha = 0.35f))
                    )
                }
            }

            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, top = 2.dp),
            )
        }
    }
}

/**
 * Types of player glyphs rendered using Nothing's LED dot matrix grid.
 */
enum class DotMatrixGlyph {
    PLAY,
    PAUSE,
    NEXT,
    PREVIOUS,
    REPLAY
}

/**
 * Renders an LED dot-matrix icon for playback controls (Play, Pause, Next, Prev, Replay).
 */
@Composable
fun DotMatrixIcon(
    glyph: DotMatrixGlyph,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 32.dp,
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val grid: Array<IntArray> = when (glyph) {
            DotMatrixGlyph.PLAY -> arrayOf(
                intArrayOf(1, 0, 0, 0, 0, 0, 0),
                intArrayOf(1, 1, 0, 0, 0, 0, 0),
                intArrayOf(1, 1, 1, 0, 0, 0, 0),
                intArrayOf(1, 1, 1, 1, 1, 0, 0),
                intArrayOf(1, 1, 1, 0, 0, 0, 0),
                intArrayOf(1, 1, 0, 0, 0, 0, 0),
                intArrayOf(1, 0, 0, 0, 0, 0, 0),
            )
            DotMatrixGlyph.PAUSE -> arrayOf(
                intArrayOf(0, 1, 1, 0, 1, 1, 0),
                intArrayOf(0, 1, 1, 0, 1, 1, 0),
                intArrayOf(0, 1, 1, 0, 1, 1, 0),
                intArrayOf(0, 1, 1, 0, 1, 1, 0),
                intArrayOf(0, 1, 1, 0, 1, 1, 0),
                intArrayOf(0, 1, 1, 0, 1, 1, 0),
                intArrayOf(0, 1, 1, 0, 1, 1, 0),
            )
            DotMatrixGlyph.NEXT -> arrayOf(
                intArrayOf(1, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 1, 0, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 1, 1, 0, 1),
                intArrayOf(1, 1, 1, 0, 0, 0, 1),
                intArrayOf(1, 1, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 1),
            )
            DotMatrixGlyph.PREVIOUS -> arrayOf(
                intArrayOf(1, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 1, 1),
                intArrayOf(1, 0, 0, 0, 1, 1, 1),
                intArrayOf(1, 0, 1, 1, 1, 1, 1),
                intArrayOf(1, 0, 0, 0, 1, 1, 1),
                intArrayOf(1, 0, 0, 0, 0, 1, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 1),
            )
            DotMatrixGlyph.REPLAY -> arrayOf(
                intArrayOf(0, 1, 1, 1, 1, 0, 0),
                intArrayOf(1, 0, 0, 0, 0, 1, 0),
                intArrayOf(1, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 1, 1, 0, 0, 1),
                intArrayOf(1, 0, 0, 1, 0, 1, 0),
                intArrayOf(0, 1, 0, 0, 1, 0, 0),
                intArrayOf(0, 0, 1, 1, 0, 0, 0),
            )
        }

        val cols = 7
        val rows = 7
        val cellSize = this.size.minDimension / cols
        val dotRadius = (cellSize * 0.38f).coerceAtLeast(1.5f)
        val startX = (this.size.width - (cols * cellSize)) / 2f + cellSize / 2f
        val startY = (this.size.height - (rows * cellSize)) / 2f + cellSize / 2f

        for (r in 0 until rows) {
            for (col in 0 until cols) {
                if (grid[r][col] == 1) {
                    drawCircle(
                        color = tint,
                        radius = dotRadius,
                        center = Offset(startX + col * cellSize, startY + r * cellSize),
                    )
                }
            }
        }
    }
}