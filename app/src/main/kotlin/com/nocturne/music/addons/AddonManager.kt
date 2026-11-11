/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * Community Streaming Addons Manager with BitChord TrackMatcher & Parallel Resolution
 */

package com.nocturne.music.addons

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.Collections
import java.util.LinkedHashMap

object AddonManager {

    private const val PREFS_NAME = "streaming_addons_prefs"
    private const val KEY_ADDONS = "addons_list"
    private const val KEY_GLOBAL_ENABLED = "addons_global_enabled"
    private const val QUERY_TIMEOUT_MS = 4500L
    private const val MAX_CACHED_RESOLUTIONS = 100

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _configs = MutableStateFlow<List<AddonConfig>>(emptyList())
    val configs: StateFlow<List<AddonConfig>> = _configs.asStateFlow()

    private val _globalEnabled = MutableStateFlow(true)
    val globalEnabled: StateFlow<Boolean> = _globalEnabled.asStateFlow()

    /** Fast in-memory cache of resolved stream objects to eliminate network lookups on repeats */
    private val resolutionCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, AddonResolvedStream>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, AddonResolvedStream>) = size > MAX_CACHED_RESOLUTIONS
        }
    )

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        val appCtx = context.applicationContext
        AddonAuth.init(appCtx)
        prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
     * Uses BitChord's [TrackMatcher] target scoring and queries addons in parallel.
     */
    suspend fun resolveLosslessStream(
        title: String,
        artist: String,
        durationSec: Int? = null,
        album: String? = null,
    ): AddonResolvedStream? = withContext(Dispatchers.IO) {
        if (!hasEnabledAddons() || title.isBlank()) return@withContext null

        val cacheKey = "$title|$artist|${durationSec ?: 0}|$album"
        resolutionCache[cacheKey]?.let { cached ->
            Timber.tag("AddonManager").d("Reusing in-memory resolved addon stream for '$title'")
            return@withContext cached
        }

        val target = TrackMatcher.Target(
            title = title,
            artist = artist,
            durationSec = durationSec,
            album = album,
        )

        val activeAddons = _configs.value.filter { it.enabled }
        if (activeAddons.isEmpty()) return@withContext null

        val queries = TrackMatcher.queries(target)
        if (queries.isEmpty()) return@withContext null

        coroutineScope {
            // Query each enabled addon concurrently
            val deferreds = activeAddons.map { addonConfig ->
                async(Dispatchers.IO) {
                    withTimeoutOrNull(QUERY_TIMEOUT_MS) {
                        try {
                            val client = AddonClient(addonConfig.baseUrl, addonConfig.pickedQuality)
                            for (q in queries) {
                                val tracks = client.search(q).getOrNull().orEmpty()
                                if (tracks.isEmpty()) continue

                                val bestCandidate = TrackMatcher.best(tracks, target) ?: continue

                                val streamUrl = bestCandidate.streamURL
                                val streamInfo = if (!streamUrl.isNullOrBlank()) {
                                    AddonStream(
                                        url = streamUrl,
                                        format = bestCandidate.format,
                                        audioQuality = bestCandidate.audioQuality,
                                    )
                                } else {
                                    client.stream(bestCandidate.id).getOrNull()
                                } ?: continue

                                if (streamInfo.url.isBlank()) continue

                                val mime = when {
                                    streamInfo.format.contains("flac", ignoreCase = true) || streamInfo.url.contains(".flac", ignoreCase = true) -> "audio/flac"
                                    streamInfo.format.contains("wav", ignoreCase = true) -> "audio/wav"
                                    streamInfo.format.contains("alac", ignoreCase = true) -> "audio/alac"
                                    streamInfo.format.contains("aac", ignoreCase = true) -> "audio/aac"
                                    streamInfo.format.contains("opus", ignoreCase = true) -> "audio/opus"
                                    else -> "audio/flac"
                                }

                                val bitrate = streamInfo.bitrate?.toInt()?.takeIf { it > 0 } ?: if (mime == "audio/flac") 921600 else 320000

                                val resolved = AddonResolvedStream(
                                    url = streamInfo.url,
                                    format = streamInfo.format.ifBlank { "flac" },
                                    mimeType = mime,
                                    bitrate = bitrate,
                                    sampleRate = streamInfo.sampleRate?.toInt(),
                                    bitDepth = streamInfo.bitDepth?.toInt(),
                                    headers = streamInfo.headers,
                                    sourceName = addonConfig.displayName,
                                    trackId = bestCandidate.id,
                                )
                                Timber.tag("AddonManager").i("Resolved lossless stream for '${target.title}' from addon '${addonConfig.displayName}'")
                                return@withTimeoutOrNull resolved
                            }
                            null
                        } catch (e: Exception) {
                            Timber.tag("AddonManager").w(e, "Error resolving from addon ${addonConfig.name}")
                            null
                        }
                    }
                }
            }

            // Return first successful match
            for (deferred in deferreds) {
                val result = deferred.await()
                if (result != null) {
                    resolutionCache[cacheKey] = result
                    return@coroutineScope result
                }
            }
            null
        }
    }

    fun clearCache() {
        resolutionCache.clear()
    }
}
