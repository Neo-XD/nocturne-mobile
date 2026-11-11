/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.nocturne.music.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.nocturne.music.ui.utils.stackBlur
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bounded session cache of baked blurred backdrops, keyed by
 * "source|decodeSize|radiusPx|finishKey". One entry per track per background style.
 */
private val blurredBackdropCache = object : android.util.LruCache<String, Bitmap>(32 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}

/**
 * BitChord / Square zero-flicker double-buffered blurred backdrop.
 * Bakes and caches a low-resolution artwork blur on a background worker once per source.
 * Holds the previous track's blurred bitmap until the new one is fully ready, then crossfades
 * smoothly — preventing black flashes and render stutter during track skips.
 */
@Composable
fun BlurredBackdropImage(
    model: Any?,
    screenBlurRadiusDp: Float,
    modifier: Modifier = Modifier,
    decodeSize: Int = 48,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    colorFilter: ColorFilter? = null,
    onReady: (() -> Unit)? = null,
    finish: ((Bitmap) -> Bitmap)? = null,
    finishKey: String = "",
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenRadiusPx = with(density) { screenBlurRadiusDp.dp.toPx() }
    val cacheKey = "$model|$decodeSize|$screenBlurRadiusDp|${configuration.screenWidthDp}x${configuration.screenHeightDp}|$finishKey"

    var currentBitmap by remember { mutableStateOf<Bitmap?>(blurredBackdropCache.get(cacheKey)) }
    var previousBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val crossfade = remember { Animatable(if (currentBitmap != null) 1f else 0f) }

    val readyCallback by rememberUpdatedState(onReady)

    LaunchedEffect(cacheKey) {
        val cached = blurredBackdropCache.get(cacheKey)
        if (cached != null) {
            if (currentBitmap != cached) {
                previousBitmap = currentBitmap
                currentBitmap = cached
                crossfade.snapTo(0f)
                readyCallback?.invoke()
                crossfade.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
                previousBitmap = null
            }
            return@LaunchedEffect
        }

        val source = model ?: return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(source)
            .size(decodeSize)
            .allowHardware(false)
            .build()
        val bmp = context.imageLoader.execute(request).image?.toBitmap() ?: return@LaunchedEffect
        val upscale = maxOf(
            screenWidthPx / bmp.width.coerceAtLeast(1),
            screenHeightPx / bmp.height.coerceAtLeast(1),
        ).coerceAtLeast(1f)
        val sourceRadius = (screenRadiusPx / upscale).roundToInt().coerceAtLeast(1)
        val blurred = withContext(Dispatchers.Default) {
            bmp.copy(Bitmap.Config.ARGB_8888, true).stackBlur(sourceRadius)
                .let { finish?.invoke(it) ?: it }
        }
        blurredBackdropCache.put(cacheKey, blurred)

        if (currentBitmap != blurred) {
            previousBitmap = currentBitmap
            currentBitmap = blurred
            crossfade.snapTo(0f)
            readyCallback?.invoke()
            crossfade.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
            previousBitmap = null
        }
    }

    Box(modifier = modifier) {
        val prev = previousBitmap
        val curr = currentBitmap

        if (prev != null && crossfade.value < 1f) {
            Image(
                bitmap = prev.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 1f - crossfade.value },
            )
        }

        if (curr != null) {
            Image(
                bitmap = curr.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (prev != null) crossfade.value else 1f },
            )
        }
    }
}
