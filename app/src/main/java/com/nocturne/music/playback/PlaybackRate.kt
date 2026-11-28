package com.nocturne.music.playback

import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.nocturne.music.data.settings.AppSettings

/** Sets speed with the listener's pitch, because Player.setPlaybackSpeed keeps whatever pitch the player had. */
internal fun Player.setSpeedWithUserPitch(speed: Float) {
    playbackParameters = PlaybackParameters(speed, AppSettings.playbackPitch.value)
}
