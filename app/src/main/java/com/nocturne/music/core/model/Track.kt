package com.nocturne.music.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Rating {
    @SerialName("like")
    LIKE,
    @SerialName("dislike")
    DISLIKE,
    @SerialName("indifferent")
    INDIFFERENT
}

@Serializable
data class ArtistRun(
    val name: String,
    val id: String? = null
)

@Serializable
data class Track(
    @SerialName("video_id")
    val id: String,
    val title: String,
    val artists: String,
    @SerialName("artist_id")
    val artistId: String? = null,
    @SerialName("artist_runs")
    val artistRuns: List<ArtistRun> = emptyList(),
    val album: String? = null,
    @SerialName("album_id")
    val albumId: String? = null,
    val duration: String? = null,
    @SerialName("duration_ms")
    val durationMs: Long = 0L,
    val thumbnail: String? = null,
    @SerialName("play_count")
    val playCount: String? = null,
    @SerialName("set_video_id")
    val setVideoId: String? = null,
    @SerialName("added_by")
    val addedBy: String? = null,
    @SerialName("added_by_avatar")
    val addedByAvatar: String? = null,
    val rating: Rating? = null,
    @SerialName("queued_by")
    val queuedBy: String? = null,
    val queued: Boolean = false,
    @SerialName("queued_end")
    val queuedEnd: Boolean = false,
    @SerialName("queued_from")
    val queuedFrom: String? = null,
    val autoplay: Boolean = false,
    @SerialName("is_video")
    val isVideo: Boolean = false,
    @SerialName("is_upload")
    val isUpload: Boolean = false,
    val explicit: Boolean = false,
    @SerialName("loudness_db")
    val loudnessDb: Double? = null
)
