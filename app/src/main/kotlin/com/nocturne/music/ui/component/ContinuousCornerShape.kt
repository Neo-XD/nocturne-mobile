/**
 * Nocturne Music Project (C) 2026
 * Continuous Corner Squircle - 1:1 Apple continuous curvature smoothing
 */

package com.nocturne.music.ui.component

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

const val GLASS_CORNER_SMOOTHING = 0.607f

class ContinuousCornerShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    val smoothing: Float = GLASS_CORNER_SMOOTHING,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    constructor(percent: Int, smoothing: Float = GLASS_CORNER_SMOOTHING) :
        this(CornerSize(percent), CornerSize(percent), CornerSize(percent), CornerSize(percent), smoothing)

    constructor(size: Dp, smoothing: Float = GLASS_CORNER_SMOOTHING) :
        this(CornerSize(size), CornerSize(size), CornerSize(size), CornerSize(size), smoothing)

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = ContinuousCornerShape(topStart, topEnd, bottomEnd, bottomStart, smoothing)

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        if (topStart + topEnd + bottomEnd + bottomStart == 0f) return Outline.Rectangle(size.toRect())
        val ltr = layoutDirection == LayoutDirection.Ltr
        val maxRadius = size.minDimension / 2f
        val topLeft = (if (ltr) topStart else topEnd).coerceAtMost(maxRadius)
        val topRight = (if (ltr) topEnd else topStart).coerceAtMost(maxRadius)
        val bottomRight = (if (ltr) bottomEnd else bottomStart).coerceAtMost(maxRadius)
        val bottomLeft = (if (ltr) bottomStart else bottomEnd).coerceAtMost(maxRadius)
        val w = size.width
        val h = size.height
        val s = smoothing.coerceIn(0f, 1f)

        val path = Path()
        path.moveTo(topLeft * (1 + edgeSmoothing(s, topLeft, topRight, w)), 0f)
        path.corner(
            corner = Offset(w, 0f), radius = topRight,
            inward = Offset(1f, 0f), outward = Offset(0f, 1f),
            smoothingIn = edgeSmoothing(s, topRight, topLeft, w),
            smoothingOut = edgeSmoothing(s, topRight, bottomRight, h),
        )
        path.corner(
            corner = Offset(w, h), radius = bottomRight,
            inward = Offset(0f, 1f), outward = Offset(-1f, 0f),
            smoothingIn = edgeSmoothing(s, bottomRight, topRight, h),
            smoothingOut = edgeSmoothing(s, bottomRight, bottomLeft, w),
        )
        path.corner(
            corner = Offset(0f, h), radius = bottomLeft,
            inward = Offset(-1f, 0f), outward = Offset(0f, -1f),
            smoothingIn = edgeSmoothing(s, bottomLeft, bottomRight, w),
            smoothingOut = edgeSmoothing(s, bottomLeft, topLeft, h),
        )
        path.corner(
            corner = Offset.Zero, radius = topLeft,
            inward = Offset(0f, -1f), outward = Offset(1f, 0f),
            smoothingIn = edgeSmoothing(s, topLeft, bottomLeft, h),
            smoothingOut = edgeSmoothing(s, topLeft, topRight, w),
        )
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContinuousCornerShape) return false
        return topStart == other.topStart && topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd && bottomStart == other.bottomStart &&
            smoothing == other.smoothing
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + smoothing.hashCode()
        return result
    }

    override fun toString(): String =
        "ContinuousCornerShape(topStart = $topStart, topEnd = $topEnd, bottomEnd = " +
            "$bottomEnd, bottomStart = $bottomStart, smoothing = $smoothing)"
}

fun CornerBasedShape.continuous(smoothing: Float = GLASS_CORNER_SMOOTHING): CornerBasedShape =
    when (this) {
        is ContinuousCornerShape -> this
        else -> ContinuousCornerShape(topStart, topEnd, bottomEnd, bottomStart, smoothing)
    }

private fun edgeSmoothing(smoothing: Float, radius: Float, otherRadius: Float, length: Float): Float {
    if (radius <= 0f) return 0f
    val straight = (length - radius - otherRadius).coerceAtLeast(0f)
    val share = straight * radius / (radius + otherRadius)
    return smoothing.coerceAtMost(share / radius)
}

private fun Path.corner(
    corner: Offset,
    radius: Float,
    inward: Offset,
    outward: Offset,
    smoothingIn: Float,
    smoothingOut: Float,
) {
    if (radius <= 0f) {
        lineTo(corner.x, corner.y)
        return
    }
    val center = corner - inward * radius + outward * radius

    val inHalf = CornerHalf(radius, smoothingIn)
    fun alongIn(distance: Float, depth: Float = 0f) = corner - inward * distance + outward * depth
    lineTo(alongIn(inHalf.edge))
    val arcStart = alongIn(inHalf.arcAlong, inHalf.arcDepth)
    if (smoothingIn > 0f) {
        cubicTo(alongIn(inHalf.edge - inHalf.a), alongIn(inHalf.edge - inHalf.a - inHalf.b), arcStart)
    }

    val outHalf = CornerHalf(radius, smoothingOut)
    fun alongOut(distance: Float, depth: Float = 0f) = corner + outward * distance - inward * depth
    val arcEnd = alongOut(outHalf.arcAlong, outHalf.arcDepth)
    val startAngle = angleOf(arcStart - center)
    val sweep = 90f - 45f * smoothingIn - 45f * smoothingOut
    arcTo(
        rect = Rect(center, radius),
        startAngleDegrees = startAngle,
        sweepAngleDegrees = sweep,
        forceMoveTo = false,
    )
    if (smoothingOut > 0f) {
        cubicTo(
            alongOut(outHalf.edge - outHalf.a - outHalf.b),
            alongOut(outHalf.edge - outHalf.a),
            alongOut(outHalf.edge),
        )
    } else {
        lineTo(arcEnd)
    }
}

private class CornerHalf(radius: Float, smoothing: Float) {
    private val turn = Math.toRadians(45.0 * smoothing)
    val edge = (1 + smoothing) * radius
    val arcAlong = radius - radius * sin(turn).toFloat()
    val arcDepth = radius * (1 - cos(turn).toFloat())
    private val c = radius * (tan(turn / 2) * cos(turn)).toFloat()
    val b = (edge - arcAlong - c) / 3f
    val a = 2f * b
}

private fun angleOf(v: Offset): Float = Math.toDegrees(atan2(v.y, v.x).toDouble()).toFloat()
private fun Path.lineTo(p: Offset) = lineTo(p.x, p.y)
private fun Path.cubicTo(p1: Offset, p2: Offset, p3: Offset) = cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
