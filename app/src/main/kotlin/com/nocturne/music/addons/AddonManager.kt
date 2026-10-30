/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * Community Streaming Addons Manager
 */

package com.nocturne.music.addons

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.Locale
import kotlin.math.abs

object AddonManager {

    private const val PREFS_NAME = "streaming_addons_prefs"
    private const val KEY_ADDONS = "addons_list"
    private const val KEY_GLOBAL_ENABLED = "addons_global_enabled"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _configs = MutableStateFlow<List<AddonConfig>>(emptyList())
    val configs: StateFlow<List<AddonConfig>> = _configs.asStateFlow()

    private val _globalEnabled = MutableStateFlow(true)
    val globalEnabled: StateFlow<Boolean> = _globalEnabled.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _globalEnabled.value = prefs.getBoolean(KEY_GLOBAL_ENABLED, true)
        loadConfigs()
    }

    private fun loadConfigs() {
        val raw = prefs.getString(KEY_ADDONS, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                val list = json.decodeFromString(ListSerializer(AddonConfig.serializer()), raw)
                _configs.value = list
            }.onFailure {
                Timber.tag("AddonManager").e(it, "Failed to decode addon configs")
            }
        }
    }

    private fun saveConfigs(list: List<AddonConfig>) {
        _configs.value = list
        if (::prefs.isInitialized) {
            val raw = json.encodeToString(ListSerializer(AddonConfig.serializer()), list)
            prefs.edit().putString(KEY_ADDONS, raw).apply()
        }
    }

    fun setGlobalEnabled(enabled: Boolean) {
        _globalEnabled.value = enabled
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_GLOBAL_ENABLED, enabled).apply()
        }
    }

    fun hasEnabledAddons(): Boolean {
        return _globalEnabled.value && _configs.value.any { it.enabled }
    }

    suspend fun addAddon(rawUrl: String): Result<AddonConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = AddonClient.normalizeBase(rawUrl)
            if (_configs.value.any { it.baseUrl.equals(normalized, ignoreCase = true) }) {
                throw IllegalStateException("Addon with this URL is already added")
            }

            val client = AddonClient(normalized)
            val manifest = client.verifyAndGetManifest().getOrThrow()

            val config = AddonConfig(
                name = manifest.displayName,
                baseUrl = normalized,
                enabled = true,
                allowDownloads = manifest.downloadsAllowed,
                checkValidLossless = manifest.requiresLosslessOutput,
            )

            val updated = _configs.value + config
            saveConfigs(updated)
            config
        }
    }

    fun removeAddon(id: String) {
        val updated = _configs.value.filter { it.id != id }
        saveConfigs(updated)
    }

    fun toggleAddon(id: String, enabled: Boolean) {
        val updated = _configs.value.map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        saveConfigs(updated)
    }

    /**
     * Resolves a lossless stream for a track by querying enabled community addons.
     * Returns null if no match or error, enabling graceful fallback to default streams.
     */
    suspend fun resolveLosslessStream(
        title: String,
        artist: String,
        durationSec: Int? = null,
    ): AddonResolvedStream? = withContext(Dispatchers.IO) {
        if (!hasEnabledAddons() || title.isBlank()) return@withContext null

        val activeAddons = _configs.value.filter { it.enabled }
        val cleanTitle = cleanSearchTerm(title)
        val cleanArtist = cleanSearchTerm(artist)
        val query = if (cleanArtist.isNotBlank()) "$cleanArtist $cleanTitle" else cleanTitle

        for (addonConfig in activeAddons) {
            try {
                val client = AddonClient(addonConfig.baseUrl)
                val tracks = client.search(query).getOrNull() ?: continue
                if (tracks.isEmpty()) continue

                // Find candidate matching title and duration
                val candidate = findBestCandidate(tracks, cleanTitle, cleanArtist, durationSec) ?: continue

                // Check if candidate already contains streamURL
                val streamUrl = candidate.streamURL
                val streamInfo = if (!streamUrl.isNullOrBlank()) {
                    AddonStream(
                        url = streamUrl,
                        format = candidate.format,
                        audioQuality = candidate.audioQuality,
                    )
                } else {
                    client.stream(candidate.id).getOrNull() ?: continue
                }

                if (streamInfo.url.isBlank()) continue

                val mime = when {
                    streamInfo.format.contains("flac", ignoreCase = true) || streamInfo.url.contains(".flac", ignoreCase = true) -> "audio/flac"
                    streamInfo.format.contains("wav", ignoreCase = true) -> "audio/wav"
                    streamInfo.format.contains("alac", ignoreCase = true) -> "audio/alac"
                    streamInfo.format.contains("aac", ignoreCase = true) -> "audio/aac"
                    streamInfo.format.contains("opus", ignoreCase = true) -> "audio/opus"
                    else -> "audio/flac"
                }

                val bitrate = streamInfo.bitrate?.takeIf { it > 0 } ?: if (mime == "audio/flac") 921600 else 320000

                Timber.tag("AddonManager").i("Resolved stream for '$title' from addon '${addonConfig.displayName}': $mime, $bitrate bps")

                return@withContext AddonResolvedStream(
                    url = streamInfo.url,
                    format = streamInfo.format.ifBlank { "flac" },
                    mimeType = mime,
                    bitrate = bitrate,
                    sampleRate = streamInfo.sampleRate,
                    bitDepth = streamInfo.bitDepth,
                    headers = streamInfo.headers,
                    sourceName = addonConfig.displayName,
                    trackId = candidate.id,
                )
            } catch (e: Exception) {
                Timber.tag("AddonManager").w(e, "Error resolving from addon ${addonConfig.name}")
            }
        }

        null
    }

    private fun findBestCandidate(
        tracks: List<AddonTrack>,
        targetTitle: String,
        targetArtist: String,
        targetDurationSec: Int?,
    ): AddonTrack? {
        val normTargetTitle = targetTitle.lowercase(Locale.ROOT)
        val normTargetArtist = targetArtist.lowercase(Locale.ROOT)

        // 1. Prioritize lossless tracks with close duration
        return tracks.sortedWith(
            compareByDescending<AddonTrack> { it.isLossless }
                .thenBy { track ->
                    if (targetDurationSec != null && track.durationSec != null) {
                        abs(track.durationSec!! - targetDurationSec)
                    } else {
                        10
                    }
                }
        ).firstOrNull { track ->
            val normTrackTitle = track.title.lowercase(Locale.ROOT)
            val durationMatch = if (targetDurationSec != null && track.durationSec != null) {
                abs(track.durationSec!! - targetDurationSec) <= 8 // Within 8 seconds
            } else {
                true
            }

            val titleMatch = normTrackTitle.contains(normTargetTitle) || normTargetTitle.contains(normTrackTitle)
            durationMatch && titleMatch
        } ?: tracks.firstOrNull()
    }

    private fun cleanSearchTerm(term: String): String {
        return term
            .replace(Regex("""\(feat\..*?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(ft\..*?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\[.*?\]"""), "")
            .replace(Regex("""\(Official.*?\)""", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
