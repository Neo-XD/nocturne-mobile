/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * BitChord-compatible Addon Client
 */

package com.nocturne.music.addons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class AddonClient(rawBaseUrl: String) {

    val baseUrl: String = normalizeBase(rawBaseUrl)

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    suspend fun manifest(): Result<AddonManifest> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "$baseUrl/manifest.json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Nocturne/${com.nocturne.music.BuildConfig.VERSION_NAME} (Android)")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Addon server responded with HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw IllegalStateException("Empty manifest response")
                val manifest = json.decodeFromString<AddonManifest>(body)
                if (manifest.id.isBlank()) {
                    throw IllegalStateException("URL responded, but not with a valid addon manifest")
                }
                manifest
            }
        }
    }

    /**
     * BitChord verification strategy: asks /manifest.json first, and if unavailable/custom,
     * probes /search as a second opinion to verify if the addon functions correctly.
     */
    suspend fun verifyAndGetManifest(): Result<AddonManifest> = withContext(Dispatchers.IO) {
        val manifestRes = manifest()
        if (manifestRes.isSuccess) {
            return@withContext manifestRes
        }

        // Second pass: Probe /search (BitChord SourceFormats pattern)
        val probeRes = probeSearch()
        if (probeRes.isSuccess) {
            val hostName = try {
                java.net.URI(baseUrl).host ?: "Community Addon"
            } catch (_: Exception) {
                "Community Addon"
            }
            return@withContext Result.success(
                AddonManifest(
                    id = baseUrl,
                    name = hostName,
                    version = "1.0.0",
                    resources = listOf("stream", "search")
                )
            )
        }

        // Neither manifest nor search answered properly
        manifestRes
    }

    suspend fun probeSearch(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "$baseUrl/search?q=test"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Nocturne/${com.nocturne.music.BuildConfig.VERSION_NAME} (Android)")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                val body = response.body?.string() ?: return@use 0
                val searchResponse = json.decodeFromString<AddonSearchResponse>(body)
                searchResponse.tracks.size
            }
        }
    }

    suspend fun search(query: String): Result<List<AddonTrack>> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$baseUrl/search?q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Nocturne/${com.nocturne.music.BuildConfig.VERSION_NAME} (Android)")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Search failed with HTTP ${response.code}")
                }
                val body = response.body?.string() ?: return@use emptyList()
                val searchResponse = json.decodeFromString<AddonSearchResponse>(body)
                searchResponse.tracks
            }
        }
    }

    suspend fun stream(trackId: String): Result<AddonStream> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedId = URLEncoder.encode(trackId, "UTF-8")
            val url = "$baseUrl/stream/$encodedId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Nocturne/${com.nocturne.music.BuildConfig.VERSION_NAME} (Android)")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Stream lookup failed with HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw IllegalStateException("Empty stream response")
                json.decodeFromString<AddonStream>(body)
            }
        }
    }

    companion object {
        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        fun normalizeBase(rawUrl: String): String {
            var trimmed = rawUrl.trim().trimEnd('/')
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                trimmed = "https://$trimmed"
            }
            val lastSegment = trimmed.substringAfterLast('/', "")
            return if (lastSegment.endsWith(".json", ignoreCase = true) && trimmed.contains("://")) {
                trimmed.dropLast(lastSegment.length).trimEnd('/')
            } else {
                trimmed
            }
        }
    }
}
