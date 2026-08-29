package com.nocturne.music.ui.player

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nocturne.canvas.CanvasArtwork
import com.nocturne.canvas.TidalCanvasProvider
import com.nocturne.music.applecanvas.AppleMusicCanvasProvider
import com.nocturne.nocturnecanvas.NocturneCanvasProvider
import com.nocturne.music.constants.CanvasSource
import com.nocturne.music.constants.CanvasSourceKey
import com.nocturne.music.models.MediaMetadata
import com.nocturne.music.utils.rememberEnumPreference
import com.nocturne.music.utils.rememberPreference
import com.nocturne.music.constants.CanvasThumbnailAnimationKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun PlayerV2Canvas(
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    if (mediaMetadata == null) return

    val enableCanvas by rememberPreference(CanvasThumbnailAnimationKey, defaultValue = true)
    if (!enableCanvas) return

    val (canvasSource) = rememberEnumPreference(CanvasSourceKey, defaultValue = CanvasSource.AUTO)
    val albumTitle = mediaMetadata.album?.title
    var canvasArtwork by remember(mediaMetadata.id, albumTitle) { mutableStateOf<CanvasArtwork?>(null) }
    var canvasFetchInFlight by remember(mediaMetadata.id, albumTitle) { mutableStateOf(false) }
    
    val storefront = remember {
        val country = Locale.getDefault().country
        if (country.length == 2) country.lowercase(Locale.ROOT) else "us"
    }

    LaunchedEffect(mediaMetadata.id, albumTitle, canvasSource) {
        val cacheKey = "${mediaMetadata.id}:${canvasSource.name}"
        CanvasArtworkPlaybackCache.get(cacheKey)?.let { cached ->
            canvasArtwork = cached
            return@LaunchedEffect
        }

        if (canvasFetchInFlight) return@LaunchedEffect
        canvasFetchInFlight = true

        val fetched = withContext(Dispatchers.IO) {
            val songTitle = mediaMetadata.title ?: ""
            val artistName = mediaMetadata.artists.joinToString { it.name }.ifBlank { mediaMetadata.artists.firstOrNull()?.name ?: "" }
            val albumName = albumTitle ?: ""

            when (canvasSource) {
                CanvasSource.AUTO -> {
                    AppleMusicCanvasProvider.getBySongArtist(songTitle, artistName, albumName, storefront)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                        ?: TidalCanvasProvider.getBySongArtist(songTitle, artistName, albumName)
                            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                        ?: NocturneCanvasProvider.getBySongArtist(songTitle, artistName, albumName)
                            ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                }
                CanvasSource.APPLE_MUSIC -> {
                    AppleMusicCanvasProvider.getBySongArtist(songTitle, artistName, albumName, storefront)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                }
                CanvasSource.VIVIMUSIC -> {
                    NocturneCanvasProvider.getBySongArtist(songTitle, artistName, albumName)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                }
                CanvasSource.TIDAL -> {
                    TidalCanvasProvider.getBySongArtist(songTitle, artistName, albumName)
                        ?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }
                }
                else -> null
            }
        }

        if (fetched != null) {
            canvasArtwork = fetched
            CanvasArtworkPlaybackCache.put(cacheKey, fetched)
        }
        canvasFetchInFlight = false
    }

    canvasArtwork?.let { artwork ->
        CanvasArtworkPlayer(
            primaryUrl = artwork.preferredAnimationUrl ?: artwork.animatedTall,
            fallbackUrl = artwork.animatedTall,
            isPlaying = isPlaying,
            modifier = modifier
        )
    }
}

