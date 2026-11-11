/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.playback

import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.music.innertube.YouTube
import com.nocturne.music.constants.AudioQuality
import com.nocturne.music.constants.AudioQualityKey
import com.nocturne.music.constants.IpVersionKey
import com.music.innertube.models.IpVersion
import com.music.innertube.models.YouTubeClient
import com.music.innertube.strategy.ContentHints
import okhttp3.Dns
import java.net.InetAddress
import java.net.Inet4Address
import java.net.Inet6Address
import com.nocturne.music.db.MusicDatabase
import com.nocturne.music.db.entities.FormatEntity
import com.nocturne.music.db.entities.SongEntity
import com.nocturne.music.di.DownloadCache
import com.nocturne.music.di.PlayerCache
import com.nocturne.music.ui.utils.resize
import com.nocturne.music.utils.YTPlayerUtils
import com.nocturne.music.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.util.concurrent.Executor
import com.nocturne.music.lyrics.LyricsHelper
import com.nocturne.music.db.entities.LyricsEntity
import com.nocturne.music.models.toMediaMetadata
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@androidx.annotation.OptIn(UnstableApi::class)
@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    @PlayerCache val playerCache: SimpleCache,
    val lyricsHelper: LyricsHelper,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val ipVersion by enumPreference(context, IpVersionKey, IpVersion.AUTO)
    private val songUrlCache = StreamUrlCache()
    // Keep a reference to context so we can read DataStore prefs for JioSaavn support
    private val appContext: Context = context

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient.Builder()
                            .dns(object : Dns {
                                override fun lookup(hostname: String): List<InetAddress> {
                                    val addresses = Dns.SYSTEM.lookup(hostname)
                                    return when (this@DownloadUtil.ipVersion) {
                                        IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                                        IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                                        IpVersion.AUTO -> addresses
                                    }
                                }
                            })
                            .proxy(YouTube.proxy)
                            .proxyAuthenticator { _, response ->
                                YouTube.proxyAuth?.let { auth ->
                                    response.request.newBuilder()
                                        .header("Proxy-Authorization", auth)
                                        .build()
                                } ?: response.request
                            }
                            .build(),
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val contentLength = runBlocking(Dispatchers.IO) { database.song(mediaId).firstOrNull()?.format?.contentLength }
            val requiredLength = when {
                dataSpec.length >= 0 -> dataSpec.length
                contentLength != null && contentLength > dataSpec.position -> contentLength - dataSpec.position
                else -> null
            }
            if (requiredLength != null && playerCache.isCached(mediaId, dataSpec.position, requiredLength)) {
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.let { cachedStream ->
                return@Factory dataSpec
                    .withUri(cachedStream.url.toUri())
                    .withRequestHeaders(dataSpec.httpRequestHeaders + cachedStream.requestHeaders)
            }
            val cacheGeneration = songUrlCache.generation(mediaId)

            // 1. Check Community Streaming Addons for lossless download resolution
            if (com.nocturne.music.addons.AddonManager.hasEnabledAddons()) {
                val songObj = database.getSongByIdBlocking(mediaId)
                if (songObj != null) {
                    val addonStream = runBlocking(Dispatchers.IO) {
                        com.nocturne.music.addons.AddonManager.resolveLosslessStream(
                            title = songObj.song.title,
                            artist = songObj.artists.joinToString { it.name },
                            durationSec = songObj.song.duration,
                            album = songObj.song.albumName,
                        )
                    }
                    if (addonStream != null) {
                        var addonLength: Long? = null
                        try {
                            val client = OkHttpClient.Builder().build()
                            val req = okhttp3.Request.Builder()
                                .head()
                                .url(addonStream.url)
                                .apply { addonStream.headers.forEach { (k, v) -> header(k, v) } }
                                .build()
                            client.newCall(req).execute().use { resp ->
                                addonLength = resp.header("Content-Length")?.toLongOrNull()
                            }
                        } catch (_: Exception) {}

                        database.query {
                            upsert(
                                FormatEntity(
                                    id = mediaId,
                                    itag = 999,
                                    mimeType = addonStream.mimeType,
                                    codecs = addonStream.format,
                                    bitrate = addonStream.bitrate,
                                    sampleRate = addonStream.sampleRate ?: 44100,
                                    contentLength = addonLength ?: 0L,
                                    loudnessDb = null,
                                    perceptualLoudnessDb = null,
                                    playbackUrl = addonStream.url
                                )
                            )
                        }
                        val addonHeaders = dataSpec.httpRequestHeaders + addonStream.headers
                        return@Factory dataSpec
                            .withUri(addonStream.url.toUri())
                            .withRequestHeaders(addonHeaders)
                    }
                }
            }

            val playbackData = runBlocking(Dispatchers.IO) {
                val song = database.getSongByIdBlocking(mediaId)?.song
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    context = appContext,
                    contentHints = ContentHints(
                        isExplicit = song?.explicit,
                        isUploaded = song?.isUploaded,
                    ),
                )
            }.getOrThrow()
            val format = playbackData.format

            val actualContentLength = format.contentLength ?: run {
                var length: Long? = null; try {
                val client = OkHttpClient.Builder()
                    .proxy(YouTube.proxy)
                    .proxyAuthenticator { _, response ->
                        YouTube.proxyAuth?.let { auth ->
                            response.request.newBuilder()
                                .header("Proxy-Authorization", auth)
                                .build()
                        } ?: response.request
                    }
                    .build()
                val request = okhttp3.Request.Builder()
                    .head()
                    .url(playbackData.streamUrl)
                    .apply {
                        playbackData.streamHeaders.forEach { (name, value) ->
                            header(name, value)
                        }
                    }
                    .build()
                client.newCall(request).execute().use { response ->
                    length = response.header("Content-Length")?.toLongOrNull() } } catch (_: Exception) {
                }
                length ?: 0L
            }

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=").getOrNull(1)?.removeSurrounding("\"") ?: "mp4a.40.2",
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = actualContentLength,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                        playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )

                val now = LocalDateTime.now()
                val existing = getSongByIdBlocking(mediaId)?.song

                val updatedSong = if (existing != null) {
                    if (existing.dateDownload == null) {
                        existing.copy(dateDownload = now)
                    } else {
                        existing
                    }
                } else {
                    SongEntity(
                        id = mediaId,
                        title = playbackData.videoDetails?.title ?: "Unknown",
                        duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                        thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url?.resize(1200, 1200),
                        dateDownload = now,
                        isDownloaded = false
                    )
                }

                upsert(updatedSong)

                // Pre-cache the high-res thumbnail immediately when download starts
                updatedSong.thumbnailUrl?.let { url ->
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    SingletonImageLoader.get(context).enqueue(request)
                }
            }

            // For YouTube streams: append the &range= param so the download cache can
            // handle progressive HTTP range requests. For JioSaavn streams the CDN
            // doesn't need it and contentLength is null, so skip it.
            val streamUrl = if (playbackData.isSaavnStream) {
                playbackData.streamUrl
            } else {
                playbackData.streamUrl
            }

            songUrlCache.put(
                mediaId = mediaId,
                url = streamUrl,
                requestHeaders = playbackData.streamHeaders,
                clientName = playbackData.streamClient,
                expiresInSeconds = playbackData.streamExpiresInSeconds,
                expectedGeneration = cacheGeneration,
            )

            val cachedStream = songUrlCache[mediaId]
            if (cachedStream != null) {
                return@Factory dataSpec
                    .withUri(cachedStream.url.toUri())
                    .withRequestHeaders(dataSpec.httpRequestHeaders + cachedStream.requestHeaders)
            }

            dataSpec
                .withUri(streamUrl.toUri())
                .withRequestHeaders(dataSpec.httpRequestHeaders + playbackData.streamHeaders)
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }

                        scope.launch {
                            when (download.state) {
                                Download.STATE_COMPLETED -> {
                                    val songId = download.request.id
                                    database.updateDownloadedInfo(songId, true, LocalDateTime.now())
                                    try {
                                        val existingLyrics = database.lyrics(songId).firstOrNull()
                                        if (existingLyrics == null || existingLyrics.lyrics == LyricsEntity.LYRICS_NOT_FOUND) {
                                            val song = database.getSongByIdBlocking(songId)
                                            if (song != null) {
                                                val metadata = song.toMediaMetadata()
                                                val result = lyricsHelper.getLyrics(metadata)
                                                if (result.lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                                                    database.upsert(
                                                        LyricsEntity(
                                                            id = songId,
                                                            lyrics = result.lyrics,
                                                            provider = result.provider
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore lyrics background fetch failure
                                    }
                                }
                                Download.STATE_FAILED,
                                Download.STATE_STOPPED,
                                Download.STATE_REMOVING -> {
                                    database.updateDownloadedInfo(download.request.id, false, null)
                                }
                                else -> {
                                }
                            }
                        }
                    }
                }
            )
        }

    init {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    fun release() {
        scope.cancel()
    }
}
