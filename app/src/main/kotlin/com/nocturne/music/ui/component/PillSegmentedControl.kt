package com.nocturne.music.ui.component

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

@Composable
fun PillSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (options.isEmpty()) return
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val style = MaterialTheme.typography.labelMedium
    val measurer = rememberTextMeasurer()
    val light = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val trackColor = TRACK_FILL.copy(alpha = if (light) LIGHT_TRACK_ALPHA else DARK_TRACK_ALPHA)
    val pillColor = if (light) Color.White else DARK_PILL
    val labelColor = if (light) Color.Black else Color.White
    val selected = selectedIndex.takeIf { it in options.indices }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(TrackShape)
            .background(trackColor),
    ) {
        val inset = with(density) { TRACK_PADDING.toPx() }
        val labelPadding = with(density) { LABEL_PADDING.toPx() }
        val viewport = constraints.maxWidth.toFloat()
        val room = viewport - inset * 2f
        val natural = remember(options, style, measurer, density) {
            options.map { label ->
                measurer.measure(label, style, maxLines = 1, softWrap = false).size.width + labelPadding * 2f
            }
        }
        val needed = natural.sum()
        val scrolls = needed > room
        val widths = when {
            scrolls -> natural
            natural.max() * options.size <= room -> List(options.size) { room / options.size }
            else -> natural.map { it + (room - needed) / options.size }
        }
        val starts = widths.runningFold(inset) { start, width -> start + width }
        val contentWidth = if (scrolls) needed + inset * 2f else viewport

        val motion: AnimationSpec<Float> = spring(PILL_DAMPING, Spring.StiffnessMediumLow)
        val pillStart by animateFloatAsState(selected?.let(starts::get) ?: 0f, motion, label = "segmentStart")
        val pillWidth by animateFloatAsState(selected?.let(widths::get) ?: 0f, motion, label = "segmentWidth")

        val scroll = rememberScrollState()
        val placed = remember { booleanArrayOf(false) }
        LaunchedEffect(selected, natural, viewport) {
            if (!scrolls || selected == null) return@LaunchedEffect
            val max = snapshotFlow { scroll.maxValue }.first { it > 0 }
            val centred = (starts[selected] + widths[selected] / 2f - viewport / 2f).roundToInt().coerceIn(0, max)
            if (placed[0]) scroll.animateScrollTo(centred) else scroll.scrollTo(centred)
            placed[0] = true
        }

        Box(
            Modifier
                .fillMaxWidth()
                .then(
                    if (scrolls) {
                        Modifier.horizontalScroll(scroll)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Box(
                Modifier
                    .width(with(density) { contentWidth.toDp() })
                    .padding(vertical = TRACK_PADDING),
            ) {
                if (selected != null) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .layout { measurable, constraints ->
                                val width = pillWidth.roundToInt().coerceAtLeast(0)
                                val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    placeable.place(pillStart.roundToInt(), 0)
                                }
                            }
                            .clip(PillShape)
                            .background(pillColor),
                    )
                }
                options.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .atX(starts[index])
                            .width(with(density) { widths[index].toDp() })
                            .clickable(enabled = enabled, interactionSource = null, indication = null) {
                                if (index != selected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(index)
                                }
                            }
                            .padding(vertical = LABEL_VERTICAL_PADDING),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = style,
                            color = labelColor,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.atX(x: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) { placeable.place(IntOffset(x.roundToInt(), 0)) }
}

private val TrackShape = SquirclePillShape()
private val PillShape = SquirclePillShape()

private val TRACK_FILL = Color(0xFF767680)
private const val DARK_TRACK_ALPHA = 0.24f
private const val LIGHT_TRACK_ALPHA = 0.12f

private val DARK_PILL = Color(0xFF636366)

private val TRACK_PADDING = 4.dp
private val LABEL_PADDING = 18.dp
private val LABEL_VERTICAL_PADDING = 9.dp
private const val PILL_DAMPING = 0.8f
