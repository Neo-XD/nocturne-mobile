package com.nocturne.music.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nocturne.music.R
import com.nocturne.music.data.model.Song
import com.nocturne.music.data.settings.AppSettings
import com.nocturne.music.data.settings.AudioQuality
import com.nocturne.music.data.sources.SourceRegistry
import com.nocturne.music.ui.components.InfoAlert
import dev.chrisbanes.haze.HazeState

/**
 * Says why an addon row in the search results won't play, in an alert rather
 * than a toast, because the answer names the setting to change and a toast is
 * gone before it can be read.
 *
 * Two different reasons, told apart by asking what the listener chose for
 * this connection: anything below Lossless is theirs to change, while a
 * Lossless choice that still blocks the addon can only be the watch's
 * temporary cap — see [AppSettings.streamCeilingOverride].
 */
@Composable
fun AddonBlockedAlert(hazeState: HazeState, song: Song, onDismiss: () -> Unit) {
    val addon = SourceRegistry.parseTrackKey(song.videoId)
        ?.let { (configId, _) -> SourceRegistry.config(configId)?.displayName }
        ?: stringResource(R.string.search_source_addons)
    val metered = AppSettings.meteredConnection.value == true
    val chosen = if (metered) AppSettings.audioQualityCellular.value else AppSettings.audioQualityWifi.value
    val message = if (chosen == AudioQuality.LOSSLESS) {
        stringResource(R.string.addon_blocked_watch, addon)
    } else {
        stringResource(
            R.string.addon_blocked_quality,
            stringResource(if (metered) R.string.mobile_data else R.string.wifi),
            stringResource(chosen.labelRes),
            addon,
        )
    }
    InfoAlert(
        hazeState = hazeState,
        title = stringResource(R.string.addon_blocked_title),
        description = message,
        onDismiss = onDismiss,
    )
}

private val AudioQuality.labelRes: Int
    get() = when (this) {
        AudioQuality.LOW -> R.string.low
        AudioQuality.MEDIUM -> R.string.medium
        AudioQuality.HIGH -> R.string.high
        AudioQuality.LOSSLESS -> R.string.lossless
    }
