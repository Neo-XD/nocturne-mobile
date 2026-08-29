package com.nocturne.music.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlbumPage(
    val title: String,
    val artists: String,
    val year: String? = null,
    val thumbnail: String? = null,
    val description: String? = null,
    val tracks: List<Track> = emptyList(),
    val duration: String? = null,
    @SerialName("track_count")
    val trackCount: Int = 0
)

@Serializable
data class ArtistPage(
    val name: String,
    val description: String? = null,
    val thumbnail: String? = null,
    val subscribers: String? = null,
    @SerialName("top_songs")
    val topSongs: List<Track> = emptyList(),
    val albums: List<BrowseItem> = emptyList(),
    val singles: List<BrowseItem> = emptyList(),
    @SerialName("similar_artists")
    val similarArtists: List<BrowseItem> = emptyList()
)

@Serializable
data class PlaylistPage(
    val id: String,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val thumbnail: String? = null,
    val tracks: List<Track> = emptyList(),
    @SerialName("track_count")
    val trackCount: Int = 0,
    val continuation: String? = null
)
