/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 */
package com.nocturne.music.ui.component.backdrop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import com.nocturne.music.ui.component.backdrop.backdrops.LayerBackdrop
import com.nocturne.music.ui.component.backdrop.highlight.Highlight
import com.nocturne.music.ui.component.backdrop.highlight.HighlightElement
import com.nocturne.music.ui.component.backdrop.internal.ShapeProvider
import com.nocturne.music.ui.component.backdrop.internal.recordLayer
import com.nocturne.music.ui.component.backdrop.shadow.InnerShadow
import com.nocturne.music.ui.component.backdrop.shadow.InnerShadowElement
import com.nocturne.music.ui.component.backdrop.shadow.Shadow
import com.nocturne.music.ui.component.backdrop.shadow.ShadowElement

private val DefaultHighlight = { Highlight.Default }
private val DefaultShadow = { Shadow.Default }
private val DefaultOnDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() }
private val NeverFrozen: () -> Boolean = { false }

private const val LoopPoolMaxSize = 64

fun Modifier.drawPlainBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
    backdropScale: Float = 0.33f,
    frozen: () -> Boolean = NeverFrozen
): Modifier {
    val shapeProvider = ShapeProvider(shape)
    return this
        .then(
            if (layerBlock != null) {
                Modifier.graphicsLayer(layerBlock)
            } else {
                Modifier
            }
        )
        .then(
            DrawBackdropElement(
                backdrop = backdrop,
                shapeProvider = shapeProvider,
                effects = effects,
                layerBlock = layerBlock,
                exportedBackdrop = exportedBackdrop,
                onDrawBehind = onDrawBehind,
                onDrawBackdrop = onDrawBackdrop,
                onDrawSurface = onDrawSurface,
                onDrawFront = onDrawFront,
                backdropScale = backdropScale.coerceIn(0.05f, 1f),
                frozen = frozen,
                loopBucket = null
            )
        )
}

fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    highlight: (() -> Highlight?)? = DefaultHighlight,
    shadow: (() -> Shadow?)? = DefaultShadow,
    innerShadow: (() -> InnerShadow?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = DefaultOnDrawBackdrop,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
    backdropScale: Float = 0.33f,
    frozen: () -> Boolean = NeverFrozen,
    loopBucket: (() -> Int)? = null
): Modifier {
    val shapeProvider = ShapeProvider(shape)
    return this
        .then(
            if (layerBlock != null) {
                Modifier.graphicsLayer(layerBlock)
            } else {
                Modifier
            }
        )
        .then(
            if (innerShadow != null) {
                InnerShadowElement(
                    shapeProvider = shapeProvider,
                    shadow = innerShadow
                )
            } else {
                Modifier
            }
        )
        .then(
            if (shadow != null) {
                ShadowElement(
                    shapeProvider = shapeProvider,
                    shadow = shadow
                )
            } else {
                Modifier
            }
        )
        .then(
            if (highlight != null) {
                HighlightElement(
                    shapeProvider = shapeProvider,
                    highlight = highlight
                )
            } else {
                Modifier
            }
        )
        .then(
            DrawBackdropElement(
                backdrop = backdrop,
                shapeProvider = shapeProvider,
                effects = effects,
                layerBlock = layerBlock,
                exportedBackdrop = exportedBackdrop,
                onDrawBehind = onDrawBehind,
                onDrawBackdrop = onDrawBackdrop,
                onDrawSurface = onDrawSurface,
                onDrawFront = onDrawFront,
                backdropScale = backdropScale.coerceIn(0.05f, 1f),
                frozen = frozen,
                loopBucket = loopBucket
            )
        )
}

private class DrawBackdropElement(
    val backdrop: Backdrop,
    val shapeProvider: ShapeProvider,
    val effects: BackdropEffectScope.() -> Unit,
    val layerBlock: (GraphicsLayerScope.() -> Unit)?,
    val exportedBackdrop: LayerBackdrop?,
    val onDrawBehind: (DrawScope.() -> Unit)?,
    val onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    val onDrawSurface: (DrawScope.() -> Unit)?,
    val onDrawFront: (DrawScope.() -> Unit)?,
    val backdropScale: Float,
    val frozen: () -> Boolean,
    val loopBucket: (() -> Int)?
) : ModifierNodeElement<DrawBackdropNode>() {

    override fun create(): DrawBackdropNode {
        return DrawBackdropNode(
            backdrop = backdrop,
            shapeProvider = shapeProvider,
            effects = effects,
            layerBlock = layerBlock,
            exportedBackdrop = exportedBackdrop,
            onDrawBehind = onDrawBehind,
            onDrawBackdrop = onDrawBackdrop,
            onDrawSurface = onDrawSurface,
            onDrawFront = onDrawFront,
            backdropScale = backdropScale,
            frozen = frozen,
            loopBucket = loopBucket
        )
    }

    override fun update(node: DrawBackdropNode) {
        val captureChanged =
            node.backdrop !== backdrop ||
                node.shapeProvider !== shapeProvider ||
                node.effects !== effects ||
                node.layerBlock !== layerBlock ||
                node.onDrawBackdrop !== onDrawBackdrop ||
                node.backdropScale != backdropScale

        node.updateBackdropSubscription(backdrop)
        node.backdrop = backdrop
        node.shapeProvider = shapeProvider
        node.effects = effects
        node.layerBlock = layerBlock
        if (node.exportedBackdrop != exportedBackdrop) {
            node.exportedBackdrop?.layerCoordinates = null
            node.exportedBackdrop = exportedBackdrop
        }
        node.onDrawBehind = onDrawBehind
        node.onDrawBackdrop = onDrawBackdrop
        node.onDrawSurface = onDrawSurface
        node.onDrawFront = onDrawFront
        node.backdropScale = backdropScale
        node.frozen = frozen
        if (node.loopBucket !== loopBucket) {
            node.loopBucket = loopBucket
            node.surfaceDirty = true
        }
        if (captureChanged) {
            node.surfaceDirty = true
            node.clearLoopPool()
        }
        node.invalidateDrawCache()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBackdrop"
        properties["backdrop"] = backdrop
        properties["shapeProvider"] = shapeProvider
        properties["effects"] = effects
        properties["layerBlock"] = layerBlock
        properties["exportedBackdrop"] = exportedBackdrop
        properties["onDrawBehind"] = onDrawBehind
        properties["onDrawBackdrop"] = onDrawBackdrop
        properties["onDrawSurface"] = onDrawSurface
        properties["onDrawFront"] = onDrawFront
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBackdropElement) return false

        if (backdrop != other.backdrop) return false
        if (shapeProvider != other.shapeProvider) return false
        if (effects != other.effects) return false
        if (layerBlock != other.layerBlock) return false
        if (exportedBackdrop != other.exportedBackdrop) return false
        if (onDrawBehind != other.onDrawBehind) return false
        if (onDrawBackdrop != other.onDrawBackdrop) return false
        if (onDrawSurface != other.onDrawSurface) return false
        if (onDrawFront != other.onDrawFront) return false
        if (backdropScale != other.backdropScale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + shapeProvider.hashCode()
        result = 31 * result + effects.hashCode()
        result = 31 * result + (layerBlock?.hashCode() ?: 0)
        result = 31 * result + (exportedBackdrop?.hashCode() ?: 0)
        result = 31 * result + (onDrawBehind?.hashCode() ?: 0)
        result = 31 * result + onDrawBackdrop.hashCode()
        result = 31 * result + (onDrawSurface?.hashCode() ?: 0)
        result = 31 * result + (onDrawFront?.hashCode() ?: 0)
        result = 31 * result + backdropScale.hashCode()
        return result
    }
}

private class DrawBackdropNode(
    var backdrop: Backdrop,
    var shapeProvider: ShapeProvider,
    var effects: BackdropEffectScope.() -> Unit,
    var layerBlock: (GraphicsLayerScope.() -> Unit)?,
    var exportedBackdrop: LayerBackdrop?,
    var onDrawBehind: (DrawScope.() -> Unit)?,
    var onDrawBackdrop: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit,
    var onDrawSurface: (DrawScope.() -> Unit)?,
    var onDrawFront: (DrawScope.() -> Unit)?,
    var backdropScale: Float,
    var frozen: () -> Boolean,
    var loopBucket: (() -> Int)?
) : LayoutModifierNode, DrawModifierNode, GlobalPositionAwareModifierNode, ObserverModifierNode, Modifier.Node() {

    private val onSourceRecorded: () -> Unit = {
        if (isAttached) {
            invalidateDraw()
        }
    }

    internal fun updateBackdropSubscription(newBackdrop: Backdrop) {
        if (backdrop !== newBackdrop) {
            (backdrop as? LayerBackdrop)?.removeInvalidationListener(onSourceRecorded)
            if (isAttached) {
                (newBackdrop as? LayerBackdrop)?.addInvalidationListener(onSourceRecorded)
            }
        }
    }

    private val effectScope =
        object : BackdropEffectScopeImpl() {
            override val shape: Shape get() = shapeProvider.innerShape
        }

    private var graphicsLayer: GraphicsLayer? = null

    private var loopPool: HashMap<Int, GraphicsLayer>? = null

    internal fun clearLoopPool() {
        val pool = loopPool ?: return
        val graphicsContext = requireGraphicsContext()
        pool.values.forEach { graphicsContext.releaseGraphicsLayer(it) }
        pool.clear()
    }

    private val layoutLayerBlock: GraphicsLayerScope.() -> Unit = {
        clip = true
        shape = shapeProvider.shape
        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
    }

    private var layoutCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    private var padding by mutableFloatStateOf(0f)

    internal var surfaceDirty = true

    private var recordedBackdropVersion: Int? = null
    private var recordedBackdropOffset: Offset? = null
    private var recordedBackdropSize: IntSize = IntSize.Zero

    private fun currentBackdropVersion(): Int? = (backdrop as? LayerBackdrop)?.contentVersion

    private fun isFullyStatic(rect: Rect): Boolean =
        (backdrop as? LayerBackdrop)?.isFullyStatic(rect) == true

    private fun currentBackdropOffset(): Offset? {
        val source = backdrop as? LayerBackdrop ?: return null
        val sourceCoordinates = source.layerCoordinates ?: return null
        val selfCoordinates = layoutCoordinates ?: return null
        return try {
            sourceCoordinates.localPositionOf(selfCoordinates)
        } catch (_: Exception) {
            null
        }
    }

    private val recordBackdropBlock: (DrawScope.() -> Unit) = {
        val canvas = drawContext.canvas
        val padding = padding
        val scale = backdropScale

        if (padding != 0f) {
            canvas.translate(padding, padding)
        }
        if (scale != 1f) {
            canvas.scale(scale, scale)
        }
        onDrawBackdrop {
            with(backdrop) {
                drawBackdrop(
                    density = effectScope,
                    coordinates = layoutCoordinates,
                    layerBlock = layerBlock
                )
            }
        }
        if (scale != 1f) {
            canvas.scale(1f / scale, 1f / scale)
        }
        if (padding != 0f) {
            canvas.translate(-padding, -padding)
        }
    }

    private fun DrawScope.drawBucketedLayer(bucket: Int): Boolean {
        val padding = padding
        val scale = backdropScale
        val recordSize = IntSize(
            ((size.width * scale).toInt() + padding.toInt() * 2).coerceAtLeast(1),
            ((size.height * scale).toInt() + padding.toInt() * 2).coerceAtLeast(1)
        )
        val offset = currentBackdropOffset()
        val geometryChanged = surfaceDirty ||
            offset != recordedBackdropOffset ||
            recordSize != recordedBackdropSize
        if (geometryChanged) {
            clearLoopPool()
        }
        recordedBackdropOffset = offset
        recordedBackdropSize = recordSize

        val pool = loopPool ?: HashMap<Int, GraphicsLayer>().also { loopPool = it }
        var recorded = false
        val layer = pool.getOrPut(bucket) {
            recorded = true
            val newLayer = requireGraphicsContext().createGraphicsLayer()
            recordLayer(this@DrawBackdropNode, newLayer, size = recordSize, block = recordBackdropBlock)
            newLayer.renderEffect = effectScope.renderEffect
            newLayer
        }
        if (pool.size > LoopPoolMaxSize) {
            val staleKey = pool.keys.first { it != bucket }
            pool.remove(staleKey)?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        }

        layer.topLeft =
            if (padding != 0f) IntOffset(-padding.toInt(), -padding.toInt())
            else IntOffset.Zero
        if (scale != 1f) {
            scale(1f / scale, pivot = Offset.Zero) {
                drawLayer(layer)
            }
        } else {
            drawLayer(layer)
        }
        return recorded
    }

    private val drawBackdropLayer: DrawScope.() -> Boolean = drawBackdropLayer@{
        val bucket = loopBucket?.invoke()
        if (bucket != null) {
            return@drawBackdropLayer drawBucketedLayer(bucket)
        }
        val layer = graphicsLayer
        if (layer != null) {
            val padding = padding
            val scale = backdropScale
            val recordSize = IntSize(
                ((size.width * scale).toInt() + padding.toInt() * 2).coerceAtLeast(1),
                ((size.height * scale).toInt() + padding.toInt() * 2).coerceAtLeast(1)
            )
            val version = currentBackdropVersion()
            val offset = currentBackdropOffset()

            val geometryChanged = surfaceDirty ||
                offset != recordedBackdropOffset ||
                recordSize != recordedBackdropSize
            val versionChanged = version == null || version != recordedBackdropVersion
            val versionChangeIsElsewhere = versionChanged && !geometryChanged &&
                offset != null && isFullyStatic(Rect(offset, recordSize.toSize()))
            val needsRecord = (geometryChanged || versionChanged) && !versionChangeIsElsewhere

            if (needsRecord) {
                recordLayer(
                    this@DrawBackdropNode,
                    layer,
                    size = recordSize,
                    block = recordBackdropBlock
                )
                if (offset != null) {
                    recordedBackdropVersion = version
                    recordedBackdropOffset = offset
                    recordedBackdropSize = recordSize
                } else {
                    surfaceDirty = true
                }
            }

            layer.topLeft =
                if (padding != 0f) IntOffset(-padding.toInt(), -padding.toInt())
                else IntOffset.Zero
            if (scale != 1f) {
                scale(1f / scale, pivot = Offset.Zero) {
                    drawLayer(layer)
                }
            } else {
                drawLayer(layer)
            }
            needsRecord
        } else {
            false
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(IntOffset.Zero, layerBlock = layoutLayerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        if (effectScope.update(this, backdropScale)) {
            updateEffects()
        }

        onDrawBehind?.invoke(this)
        val contentRecorded = drawBackdropLayer()
        onDrawSurface?.invoke(this)
        drawContent()
        onDrawFront?.invoke(this)

        exportedBackdrop?.graphicsLayer?.let { layer ->
            if (surfaceDirty || contentRecorded) {
                recordLayer(this@DrawBackdropNode, layer) {
                    onDrawBehind?.invoke(this)
                    drawBackdropLayer()
                    onDrawSurface?.invoke(this)
                    onDrawFront?.invoke(this)
                }
            }
        }
        surfaceDirty = false
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            val wasAttached = layoutCoordinates != null
            if (backdrop.isCoordinatesDependent) {
                layoutCoordinates = coordinates
                if (!wasAttached || surfaceDirty) {
                    surfaceDirty = true
                    invalidateDrawCache()
                }
            } else {
                if (layoutCoordinates != null) {
                    layoutCoordinates = null
                }
            }
            exportedBackdrop?.layerCoordinates = coordinates
        }
    }

    override fun onObservedReadsChanged() {
        surfaceDirty = true
        invalidateDrawCache()
    }

    fun invalidateDrawCache() {
        observeEffects()
        if (isAttached) {
            invalidateDraw()
        }
    }

    private fun observeEffects() {
        observeReads { updateEffects() }
    }

    private fun updateEffects() {
        if (!isRenderEffectSupported()) return

        effectScope.apply(effects)
        graphicsLayer?.renderEffect = effectScope.renderEffect
        val newPadding = effectScope.padding
        if (newPadding != padding) {
            padding = newPadding
            surfaceDirty = true
        }
    }

    override fun onAttach() {
        val graphicsContext = requireGraphicsContext()
        graphicsLayer = graphicsContext.createGraphicsLayer()
        (backdrop as? LayerBackdrop)?.addInvalidationListener(onSourceRecorded)

        surfaceDirty = true
        observeEffects()
    }

    override fun onDetach() {
        (backdrop as? LayerBackdrop)?.removeInvalidationListener(onSourceRecorded)
        val graphicsContext = requireGraphicsContext()
        graphicsLayer?.let { layer ->
            graphicsContext.releaseGraphicsLayer(layer)
            graphicsLayer = null
        }
        clearLoopPool()

        effectScope.reset()
        layoutCoordinates = null
        exportedBackdrop?.layerCoordinates = null
        recordedBackdropVersion = null
        recordedBackdropOffset = null
        recordedBackdropSize = IntSize.Zero
    }
}
