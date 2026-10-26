/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * BitChord-compatible Community Streaming Addons Specification
 */

package com.nocturne.music.addons

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import java.util.UUID

@Serializable
data class AddonConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val baseUrl: String = "",
    val enabled: Boolean = true,
    val allowDownloads: Boolean = true,
    val checkValidLossless: Boolean = false,
) {
    val displayName: String
        get() = name.ifBlank {
            runCatching { java.net.URI(baseUrl).host }.getOrNull() ?: baseUrl
        }
}

@Serializable
data class AddonManifest(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("version") val version: String = "",
    @SerialName("resources") val resources: List<String> = emptyList(),
    @SerialName("allowDownloads") val allowDownloads: JsonElement? = null,
    @SerialName("checkValidLossless") val checkValidLossless: JsonElement? = null,
) {
    val downloadsAllowed: Boolean
        get() = when (val elem = allowDownloads) {
            is JsonPrimitive -> elem.booleanOrNull ?: (elem.content != "0")
            else -> true
        }

    val requiresLosslessOutput: Boolean
        get() = when (val elem = checkValidLossless) {
            is JsonPrimitive -> elem.booleanOrNull ?: (elem.content == "1")
            else -> false
        }

    val isPlayable: Boolean
        get() = resources.isEmpty() || resources.any { it.equals("search", ignoreCase = true) }

    val displayName: String
        get() = name.ifBlank { id }
}

@Serializable
data class AddonSearchResponse(
    @SerialName("tracks") val tracks: List<AddonTrack> = emptyList(),
)

@Serializable
data class AddonTrack(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("artist") val artist: String = "",
    @SerialName("album") val album: String = "",
    @SerialName("duration") val duration: Double? = null,
    @SerialName("artworkURL") val artworkURL: String? = null,
    @SerialName("albumArtworkURL") val albumArtworkURL: String? = null,
    @SerialName("format") val format: String = "",
    @SerialName("audioQuality") val audioQuality: String = "",
    @SerialName("audioMode") val audioMode: String? = null,
    @SerialName("streamURL") val streamURL: String? = null,
) {
    val durationSec: Int?
        get() = duration?.takeIf { it > 0 }?.toInt()

    val artwork: String?
        get() = artworkURL?.ifBlank { null } ?: albumArtworkURL?.ifBlank { null }

    val isLossless: Boolean
        get() = format.contains("flac", ignoreCase = true) ||
            format.contains("alac", ignoreCase = true) ||
            format.contains("wav", ignoreCase = true) ||
            audioQuality.contains("lossless", ignoreCase = true) ||
            audioQuality.contains("hi_res", ignoreCase = true) ||
            audioQuality.contains("hires", ignoreCase = true)
}

@Serializable
data class AddonStream(
    @SerialName("url") val url: String = "",
    @SerialName("format") val format: String = "",
    @SerialName("quality") val quality: String = "",
    @SerialName("audioQuality") val audioQuality: String = "",
    @SerialName("codec") val codec: String? = null,
    @SerialName("sampleRate") val sampleRate: Int? = null,
    @SerialName("bitDepth") val bitDepth: Int? = null,
    @SerialName("bitrate") val bitrate: Int? = null,
    @SerialName("headers") val headers: Map<String, String> = emptyMap(),
)

data class AddonResolvedStream(
    val url: String,
    val format: String,
    val mimeType: String,
    val bitrate: Int,
    val sampleRate: Int?,
    val bitDepth: Int?,
    val headers: Map<String, String>,
    val sourceName: String,
    val trackId: String,
)
