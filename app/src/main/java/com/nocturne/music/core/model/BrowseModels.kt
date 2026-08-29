package com.nocturne.music.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BrowseItem(
    val kind: String, // "song" | "playlist" | "album" | "artist"
    val id: String,   // videoId or browseId
    val title: String,
    val subtitle: String? = null,
    val thumbnail: String? = null,
    val duration: String? = null,
    @SerialName("artist_runs")
    val artistRuns: List<ArtistRun> = emptyList(),
    @SerialName("play_count")
    val playCount: String? = null,
    @SerialName("is_video")
    val isVideo: Boolean = false,
    @SerialName("is_upload")
    val isUpload: Boolean = false,
    val explicit: Boolean = false
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artists = subtitle ?: "",
        artistRuns = artistRuns,
        duration = duration,
        thumbnail = thumbnail,
        playCount = playCount,
        isVideo = isVideo,
        isUpload = isUpload,
        explicit = explicit
    )
}

@Serializable
data class Section(
    val title: String,
    val items: List<BrowseItem>,
    @SerialName("more_browse_id")
    val moreBrowseId: String? = null,
    @SerialName("more_params")
    val moreParams: String? = null
)

@Serializable
data class HomeChip(
    val title: String,
    val params: String
)

@Serializable
data class HomePage(
    val chips: List<HomeChip> = emptyList(),
    val sections: List<Section> = emptyList(),
    @SerialName("continuation_token")
    val continuationToken: String? = null
)

@Serializable
data class SearchResults(
    val top: List<BrowseItem> = emptyList(),
    val songs: List<BrowseItem> = emptyList(),
    val albums: List<BrowseItem> = emptyList(),
    val artists: List<BrowseItem> = emptyList(),
    val playlists: List<BrowseItem> = emptyList()
)
