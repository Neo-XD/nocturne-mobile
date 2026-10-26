/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 */
package com.nocturne.music.ui.component.backdrop.backdrops

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import com.nocturne.music.ui.component.backdrop.internal.recordLayer

fun Modifier.layerBackdrop(
    backdrop: LayerBackdrop,
    frozen: () -> Boolean = { false }
): Modifier = this then LayerBackdropElement(backdrop, frozen)

private class LayerBackdropElement(
    val backdrop: LayerBackdrop,
    val frozen: () -> Boolean
) : ModifierNodeElement<LayerBackdropNode>() {

    override fun create(): LayerBackdropNode {
        return LayerBackdropNode(backdrop, frozen)
    }

    override fun update(node: LayerBackdropNode) {
        if (node.backdrop != backdrop) {
            node.backdrop.layerCoordinates = null
            node.backdrop = backdrop
        }
        node.frozen = frozen
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layerBackdrop"
        properties["backdrop"] = backdrop
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LayerBackdropElement) return false

        if (backdrop != other.backdrop) return false
        if (frozen != other.frozen) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * backdrop.hashCode() + frozen.hashCode()
    }
}

private class LayerBackdropNode(
    var backdrop: LayerBackdrop,
    var frozen: () -> Boolean
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    override fun ContentDrawScope.draw() {
        backdrop.staticRegions.clear()

        // Record the subtree ONCE into the hardware GraphicsLayer and composite
        // that layer directly to the screen. Sampling glass surfaces replay this
        // RenderNode at 0.33f scale on the GPU for live, zero-lag blur & refraction.
        recordLayer(this@LayerBackdropNode, backdrop.graphicsLayer) { backdrop.onDraw(this@draw) }
        drawLayer(backdrop.graphicsLayer)
        backdrop.contentRecorded()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val wasAttached = backdrop.layerCoordinates != null
        if (coordinates.isAttached) {
            backdrop.layerCoordinates = coordinates
            if (!wasAttached) {
                backdrop.contentRecorded()
                invalidateDraw()
            }
        }
    }

    override fun onDetach() {
        backdrop.layerCoordinates = null
    }
}
