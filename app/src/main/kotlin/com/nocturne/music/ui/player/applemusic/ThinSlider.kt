/**
 * Nocturne Music Project (C) 2026
 * Apple Music Thin Scrubber Slider
 */

package com.nocturne.music.ui.player.applemusic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeTrackColor: Color = Color.White,
    inactiveTrackColor: Color = Color.White.copy(alpha = 0.22f),
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "thinSliderHeight",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDragging = true
                    dragProgress = (down.position.x / size.width).coerceIn(0f, 1f)
                    onValueChange(dragProgress)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(dragProgress)
                        change.consume()
                    }
                    isDragging = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight),
        ) {
            val h = size.height
            val w = size.width
            val currentFrac = if (isDragging) dragProgress else value.coerceIn(0f, 1f)
            val corner = CornerRadius(h / 2f, h / 2f)

            // Inactive track
            drawRoundRect(
                color = inactiveTrackColor,
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                cornerRadius = corner,
            )

            // Active track
            if (currentFrac > 0f) {
                drawRoundRect(
                    color = activeTrackColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(w * currentFrac, h),
                    cornerRadius = corner,
                )
            }
        }
    }
}
