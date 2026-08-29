package com.nocturne.music.data.repository

import com.nocturne.music.core.model.*
import com.nocturne.music.data.remote.innertube.InnerTubeService
import com.nocturne.music.data.remote.innertube.StreamResolver
import com.nocturne.music.data.remote.lyrics.LyricsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(
    private val innerTubeService: InnerTubeService,
    private val streamResolver: StreamResolver,
    private val lyricsService: LyricsService
) {
    suspend fun search(query: String): Result<SearchResults> = withContext(Dispatchers.IO) {
        innerTubeService.searchAll(query)
    }

    suspend fun getHome(params: String? = null): Result<HomePage> = withContext(Dispatchers.IO) {
        innerTubeService.getHome(params)
    }

    suspend fun getAlbum(browseId: String): Result<AlbumPage> = withContext(Dispatchers.IO) {
        innerTubeService.getAlbum(browseId)
    }

    suspend fun getArtist(browseId: String): Result<ArtistPage> = withContext(Dispatchers.IO) {
        innerTubeService.getArtist(browseId)
    }

    suspend fun resolveStream(videoId: String): Result<ResolvedStream> = withContext(Dispatchers.IO) {
        streamResolver.resolveStream(videoId)
    }

    suspend fun getLyrics(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Long? = null
    ): Lyrics = withContext(Dispatchers.IO) {
        lyricsService.getLyrics(videoId, title, artist, durationSeconds)
    }
}
