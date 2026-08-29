package com.nocturne.music.data.remote.innertube

import com.nocturne.music.core.model.ResolvedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

class StreamResolver(
    private val innerTubeService: InnerTubeService
) {
    // Client cascade: ANDROID_VR & ANDROID_MUSIC provide direct GoogleVideo audio URLs without JS signature deciphering
    private val clientCascade = listOf(
        YouTubeClients.ANDROID_VR,
        YouTubeClients.ANDROID_MUSIC,
        YouTubeClients.IOS_MUSIC,
        YouTubeClients.VISIONOS,
        YouTubeClients.WEB_REMIX
    )

    suspend fun resolveStream(videoId: String): Result<ResolvedStream> = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null

        // 1. Primary: InnerTube Native Clients (ANDROID_VR / ANDROID_MUSIC / IOS_MUSIC)
        for (client in clientCascade) {
            try {
                val playerResult = innerTubeService.getPlayerResponse(client, videoId)
                if (playerResult.isFailure) {
                    lastError = playerResult.exceptionOrNull()
                    continue
                }

                val json = playerResult.getOrNull() ?: continue
                val playability = json["playabilityStatus"]?.jsonObject
                val status = playability?.get("status")?.jsonPrimitive?.contentOrNull
                if (status != "OK") {
                    val reason = playability?.get("reason")?.jsonPrimitive?.contentOrNull ?: "Status: $status"
                    lastError = IllegalStateException(reason)
                    continue
                }

                val streamingData = json["streamingData"]?.jsonObject ?: continue
                val formats = streamingData["adaptiveFormats"]?.jsonArray
                    ?: streamingData["formats"]?.jsonArray
                    ?: continue

                val audioFormats = formats.mapNotNull { it.jsonObject }.filter { fmt ->
                    val mime = fmt["mimeType"]?.jsonPrimitive?.contentOrNull ?: ""
                    mime.startsWith("audio/")
                }

                val bestFormat = audioFormats.maxByOrNull { fmt ->
                    val bitrate = fmt["bitrate"]?.jsonPrimitive?.longOrNull ?: 0L
                    val itag = fmt["itag"]?.jsonPrimitive?.longOrNull ?: 0L
                    val itagBonus = if (itag == 251L) 100000L else if (itag == 140L) 50000L else 0L
                    bitrate + itagBonus
                } ?: formats.firstOrNull()?.jsonObject ?: continue

                val streamUrl = bestFormat["url"]?.jsonPrimitive?.contentOrNull
                if (streamUrl != null) {
                    val itag = bestFormat["itag"]?.jsonPrimitive?.longOrNull ?: 140L
                    val loudnessDb = json["playerConfig"]
                        ?.jsonObject?.get("audioConfig")
                        ?.jsonObject?.get("loudnessDb")
                        ?.jsonPrimitive?.doubleOrNull

                    val expiresInSecs = streamingData["expiresInSeconds"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 21600L
                    val expiresAt = System.currentTimeMillis() / 1000 + expiresInSecs

                    return@withContext Result.success(
                        ResolvedStream(
                            videoId = videoId,
                            url = streamUrl,
                            itag = itag,
                            headers = mapOf(
                                "User-Agent" to client.userAgent,
                                "Referer" to "https://www.youtube.com/"
                            ),
                            expiresAt = expiresAt,
                            loudnessDb = loudnessDb,
                            streamClient = client.clientName
                        )
                    )
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        // 2. Secondary fallback: NewPipeExtractor
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, url)
            val audioStreams = streamInfo.audioStreams

            if (!audioStreams.isNullOrEmpty()) {
                val bestStream = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
                val streamUrl = bestStream.content

                if (!streamUrl.isNullOrBlank()) {
                    return@withContext Result.success(
                        ResolvedStream(
                            videoId = videoId,
                            url = streamUrl,
                            itag = bestStream.itag.toLong(),
                            headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                                "Referer" to "https://www.youtube.com/"
                            ),
                            expiresAt = System.currentTimeMillis() / 1000 + 21600,
                            streamClient = "NewPipeExtractor"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            lastError = e
        }

        Result.failure(lastError ?: IllegalStateException("All stream extraction methods failed for $videoId"))
    }
}
