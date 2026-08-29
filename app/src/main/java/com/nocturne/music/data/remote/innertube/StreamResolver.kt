package com.nocturne.music.data.remote.innertube

import com.nocturne.music.core.model.ResolvedStream
import kotlinx.serialization.json.*

class StreamResolver(
    private val innerTubeService: InnerTubeService
) {
    private val clientCascade = listOf(
        YouTubeClients.WEB_REMIX,
        YouTubeClients.ANDROID_VR,
        YouTubeClients.VISIONOS,
        YouTubeClients.IOS_MUSIC
    )

    suspend fun resolveStream(videoId: String): Result<ResolvedStream> {
        var lastError: Throwable? = null

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

                // Find highest quality audio format (itag 140, 251, etc. or audio/mp4 / audio/webm)
                val audioFormats = formats.mapNotNull { it.jsonObject }.filter { fmt ->
                    val mime = fmt["mimeType"]?.jsonPrimitive?.contentOrNull ?: ""
                    mime.startsWith("audio/")
                }

                val bestFormat = audioFormats.maxByOrNull { fmt ->
                    val bitrate = fmt["bitrate"]?.jsonPrimitive?.longOrNull ?: 0L
                    val itag = fmt["itag"]?.jsonPrimitive?.longOrNull ?: 0L
                    // Prefer 251 (Opus ~160k) or 140 (AAC 128k)
                    val itagBonus = if (itag == 251L) 100000L else if (itag == 140L) 50000L else 0L
                    bitrate + itagBonus
                } ?: formats.firstOrNull()?.jsonObject ?: continue

                val url = bestFormat["url"]?.jsonPrimitive?.contentOrNull
                if (url != null) {
                    val itag = bestFormat["itag"]?.jsonPrimitive?.longOrNull ?: 140L
                    val approxDurationMs = bestFormat["approxDurationMs"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
                    val loudnessDb = json["playerConfig"]
                        ?.jsonObject?.get("audioConfig")
                        ?.jsonObject?.get("loudnessDb")
                        ?.jsonPrimitive?.doubleOrNull

                    val expiresInSecs = streamingData["expiresInSeconds"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 21600L
                    val expiresAt = System.currentTimeMillis() / 1000 + expiresInSecs

                    return Result.success(
                        ResolvedStream(
                            videoId = videoId,
                            url = url,
                            itag = itag,
                            headers = mapOf("User-Agent" to client.userAgent),
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

        return Result.failure(lastError ?: IllegalStateException("All clients failed to resolve stream for $videoId"))
    }
}
