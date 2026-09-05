/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard Nocturne Specular Border gradient creating the acrylic glass edge reflection.
 */
@Composable
fun rememberNocturneGlassBorderBrush(
    highlightAlpha: Float = 0.32f,
    shadowAlpha: Float = 0.08f
): Brush {
    return remember(highlightAlpha, shadowAlpha) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = highlightAlpha),
                Color.White.copy(alpha = shadowAlpha),
                Color.Transparent,
                Color.White.copy(alpha = highlightAlpha * 0.4f)
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }
}

/**
 * Nocturne Frosted Glass Modifier
 * Applies a translucent acrylic frosted layer with optional specular glass border.
 */
fun Modifier.nocturneFrostedGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color.Unspecified,
    showBorder: Boolean = true,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .then(
        if (showBorder && borderBrush != null) {
            Modifier.border(borderWidth, borderBrush, shape)
        } else {
            Modifier
        }
    )
    .background(backgroundColor, shape)

/**
 * High-performance frosted glass container card.
 */
@Composable
fun NocturneGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderBrush: Brush = rememberNocturneGlassBorderBrush(),
    showBorder: Boolean = true,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.then(
            if (showBorder) {
                Modifier.border(BorderStroke(1.dp, borderBrush), shape)
            } else {
                Modifier
            }
        ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = elevation
    ) {
        content()
    }
}

/**
 * High-performance frosted glass container box.
 */
@Composable
fun NocturneGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
    showBorder: Boolean = true,
    borderBrush: Brush = rememberNocturneGlassBorderBrush(),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (showBorder) {
                    Modifier.border(BorderStroke(1.dp, borderBrush), shape)
                } else {
                    Modifier
                }
            )
            .background(backgroundColor, shape),
        content = content
    )
}
