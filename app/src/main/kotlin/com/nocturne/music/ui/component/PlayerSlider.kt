/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

import com.nocturne.music.ui.theme.LocalIsNothingTheme
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    trackHeight: Dp = 10.dp
) {
    val isNothingTheme = LocalIsNothingTheme.current
    val inactiveTrackColor = colors.inactiveTrackColor
    val activeTrackColor = colors.activeTrackColor
    val inactiveTickColor = colors.inactiveTickColor
    val activeTickColor = colors.activeTickColor
    val valueRange = sliderState.valueRange
    val currentFraction = calcFraction(
        valueRange.start,
        valueRange.endInclusive,
        sliderState.value.coerceIn(valueRange.start, valueRange.endInclusive)
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(trackHeight)
    ) {
        if (isNothingTheme) {
            drawDotMatrixTrack(
                fraction = currentFraction,
                activeColor = activeTrackColor,
                inactiveColor = inactiveTrackColor,
                trackHeight = trackHeight
            )
        } else {
            drawTrack(
                stepsToTickFractions(sliderState.steps),
                0f,
                currentFraction,
                inactiveTrackColor,
                activeTrackColor,
                inactiveTickColor,
                activeTickColor,
                trackHeight
            )
        }
    }
}

private fun DrawScope.drawDotMatrixTrack(
    fraction: Float,
    activeColor: Color,
    inactiveColor: Color,
    trackHeight: Dp
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val dotRadius = (trackHeight.toPx() / 3f).coerceIn(2.5f.dp.toPx(), 4.5f.dp.toPx())
    val strokeWidth = 1.5f.dp.toPx()
    val dotDiameter = dotRadius * 2
    val minSpacing = dotDiameter + 5.dp.toPx()
    val availableWidth = size.width - dotDiameter
    if (availableWidth <= 0f) return

    val count = (availableWidth / minSpacing).toInt().coerceAtLeast(8)
    val stepX = availableWidth / count
    val currentDotIndex = (fraction.coerceIn(0f, 1f) * count).toInt().coerceIn(0, count)

    for (i in 0..count) {
        val dotFraction = i.toFloat() / count
        val effectiveDotFraction = if (isRtl) (1f - dotFraction) else dotFraction
        val cx = dotRadius + i * stepX
        val cy = center.y

        val isPlayed = if (isRtl) (effectiveDotFraction >= (1f - fraction)) else (effectiveDotFraction <= fraction)
        val isCurrent = if (isRtl) (i == (count - currentDotIndex)) else (i == currentDotIndex)
        val currentRadius = if (isCurrent) (dotRadius * 1.5f).coerceAtMost(stepX * 0.48f) else dotRadius

        if (isPlayed) {
            // Full (filled) dot for played parts (slightly larger at current position)
            drawCircle(
                color = activeColor,
                radius = currentRadius,
                center = Offset(cx, cy),
                style = Fill
            )
        } else {
            // Hollow (stroked) dot for unplayed parts
            drawCircle(
                color = inactiveColor.copy(alpha = 0.55f),
                radius = (currentRadius - strokeWidth / 2f).coerceAtLeast(1f),
                center = Offset(cx, cy),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    inactiveTickColor: Color,
    activeTickColor: Color,
    trackHeight: Dp = 2.dp
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val sliderLeft = Offset(0f, center.y)
    val sliderRight = Offset(size.width, center.y)
    val sliderStart = if (isRtl) sliderRight else sliderLeft
    val sliderEnd = if (isRtl) sliderLeft else sliderRight
    val tickSize = 2.0.dp.toPx()
    val trackStrokeWidth = trackHeight.toPx()
    drawLine(
        inactiveTrackColor,
        sliderStart,
        sliderEnd,
        trackStrokeWidth,
        StrokeCap.Round
    )
    val sliderValueEnd = Offset(
        sliderStart.x +
                (sliderEnd.x - sliderStart.x) * activeRangeEnd,
        center.y
    )
    val sliderValueStart = Offset(
        sliderStart.x +
                (sliderEnd.x - sliderStart.x) * activeRangeStart,
        center.y
    )
    drawLine(
        activeTrackColor,
        sliderValueStart,
        sliderValueEnd,
        trackStrokeWidth,
        StrokeCap.Round
    )
    for (tick in tickFractions) {
        val outsideFraction = tick > activeRangeEnd || tick < activeRangeStart
        drawCircle(
            color = if (outsideFraction) inactiveTickColor else activeTickColor,
            center = Offset(lerp(sliderStart, sliderEnd, tick).x, center.y),
            radius = tickSize / 2f
        )
    }
}

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
}

private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)


