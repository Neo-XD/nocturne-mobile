/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 */
package com.nocturne.music.ui.component.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import com.nocturne.music.ui.component.backdrop.Backdrop
import com.nocturne.music.ui.component.backdrop.internal.InverseLayerScope

private val DefaultOnDraw: ContentDrawScope.() -> Unit = { drawContent() }

@Composable
fun rememberLayerBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    onDraw: ContentDrawScope.() -> Unit = DefaultOnDraw
): LayerBackdrop {
    return remember(graphicsLayer, onDraw) {
        LayerBackdrop(graphicsLayer, onDraw)
    }
}

@Stable
class LayerBackdrop internal constructor(
    val graphicsLayer: GraphicsLayer,
    internal val onDraw: ContentDrawScope.() -> Unit
) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    internal var layerCoordinates: LayoutCoordinates? by mutableStateOf(null)

    internal var contentVersion: Int = 0
        private set

    private val invalidationListeners = ArrayList<() -> Unit>()
    private var isNotifying = false

    internal fun addInvalidationListener(listener: () -> Unit) {
        if (!invalidationListeners.contains(listener)) {
            invalidationListeners.add(listener)
        }
    }

    internal fun removeInvalidationListener(listener: () -> Unit) {
        invalidationListeners.remove(listener)
    }

    /** Called by the recording node after a successful record. */
    internal fun contentRecorded() {
        contentVersion++
        if (!isNotifying && invalidationListeners.isNotEmpty()) {
            isNotifying = true
            try {
                val snapshot = invalidationListeners.toTypedArray()
                for (listener in snapshot) {
                    listener.invoke()
                }
            } finally {
                isNotifying = false
            }
        }
    }

    internal val staticRegions = ArrayList<Rect>()

    internal fun isFullyStatic(rect: Rect): Boolean =
        staticRegions.any { region ->
            region.left <= rect.left && region.top <= rect.top &&
                region.right >= rect.right && region.bottom >= rect.bottom
        }

    internal val hasRecording: Boolean get() = contentVersion > 0

    private var inverseLayerScope: InverseLayerScope? = null

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        val coordinates = coordinates ?: return
        val layerCoordinates = layerCoordinates ?: return
        withTransform({
            if (layerBlock != null) {
                with(obtainInverseLayerScope()) { inverseTransform(density, layerBlock) }
            }
            val offset =
                try {
                    layerCoordinates.localPositionOf(coordinates)
                } catch (_: Exception) {
                    coordinates.positionInWindow() - layerCoordinates.positionInWindow()
                }
            translate(-offset.x, -offset.y)
        }) {
            drawLayer(graphicsLayer)
        }
    }

    private fun obtainInverseLayerScope(): InverseLayerScope {
        return inverseLayerScope?.apply { reset() }
            ?: InverseLayerScope().also { inverseLayerScope = it }
    }
}
