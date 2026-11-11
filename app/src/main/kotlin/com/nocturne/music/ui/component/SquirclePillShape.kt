/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.ui.component

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * A pill cut as a fully rounded squircle, the way BitChord / Apple Liquid Glass draws its capsules.
 *
 * Fully rounded like a capsule — each end spans the whole height and reaches in
 * half of it, with no straight run — but each quarter of an end is a
 * superellipse, `|x|ⁿ + |y|ⁿ = 1`, rather than a circle.
 */
class SquirclePillShape(val exponent: Float = SQUIRCLE_PILL_EXPONENT) :
    CornerBasedShape(CornerSize(50), CornerSize(50), CornerSize(50), CornerSize(50)) {

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = SquirclePillShape(exponent)

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val r = size.minDimension / 2f
        if (r <= 0f) return Outline.Rectangle(size.toRect())
        val w = size.width
        val h = size.height
        val path = Path()
        // Clockwise from the top edge; each quarter centred `r` in from its corner.
        path.moveTo(r, 0f)
        path.lineTo(w - r, 0f)
        path.quarter(w - r, r, 1f, -1f, r, towardEdge = false)
        path.lineTo(w, h - r)
        path.quarter(w - r, h - r, 1f, 1f, r, towardEdge = true)
        path.lineTo(r, h)
        path.quarter(r, h - r, -1f, 1f, r, towardEdge = false)
        path.lineTo(0f, r)
        path.quarter(r, r, -1f, -1f, r, towardEdge = true)
        path.close()
        return Outline.Generic(path)
    }

    private fun Path.quarter(cx: Float, cy: Float, sx: Float, sy: Float, r: Float, towardEdge: Boolean) {
        val power = 2f / exponent
        for (i in 1..QUARTER_STEPS) {
            val step = if (towardEdge) i else QUARTER_STEPS - i
            val t = step * (Math.PI / 2) / QUARTER_STEPS
            val x = cos(t).toFloat().coerceAtLeast(0f).pow(power)
            val y = sin(t).toFloat().coerceAtLeast(0f).pow(power)
            lineTo(cx + sx * r * x, cy + sy * r * y)
        }
    }

    override fun equals(other: Any?): Boolean = other is SquirclePillShape && other.exponent == exponent

    override fun hashCode(): Int = exponent.hashCode()

    override fun toString(): String = "SquirclePillShape(exponent = $exponent)"
}

const val SQUIRCLE_PILL_EXPONENT = 2.1f
private const val QUARTER_STEPS = 32
