/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * BitChord-compatible Addon Client with SharedCalls, Auth & Backoff
 */

package com.nocturne.music.addons

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Communicates with an addon server implementing the BitChord Addon protocol.
 *
 * Utilizes [SharedCalls] to deduplicate concurrent requests and cache recent manifests,
 * searches, and stream URLs. Handles 429 rate limits with exponential backoff, and
 * performs [AddonAuth] attestation handshake upon encountering 401 challenges.
 */
class AddonClient(
    rawBaseUrl: String,
    private val pickedQuality: String? = null,
) {
    val baseUrl: String = normalizeBase(rawBaseUrl)

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val calls = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val manifests = SharedCalls<AddonManifest>(MANIFEST_TTL_MS, calls, ::logReuse)
    private val searches = SharedCalls<AddonSearchResponse>(SEARCH_TTL_MS, calls, ::logReuse)
    private val streams = SharedCalls<AddonStream>(STREAM_TTL_MS, calls, ::logReuse)

    private fun logReuse(line: String) = Timber.tag(TAG).d(line)

    @Volatile
    private var quietUntilMs = 0L

    @Volatile
    private var authRequired = false

    // ── Manifest ──────────────────────────────────────────────────────────

    suspend fun manifest(): Result<AddonManifest> = manifests.get(
        key = baseUrl,
        describe = { "▶ addon manifest(${redact(baseUrl)})" },
    ) {
        val manifest = try {
            fetch<AddonManifest>(manifestUrl(baseUrl))
        } catch (failure: Throwable) {
            if (failure is AddonNotFound) throw AddonException("No manifest at that URL")
            throw failure
        }
        if (manifest.id.isBlank()) {
            throw AddonException("That URL answered, but not with an addon manifest")
        }
        if (!manifest.isPlayable) {
            val declared = manifest.resources.joinToString(", ")
            throw AddonException("This addon declares $declared — Nocturne needs search")
        }
        if (manifest.requiresBitChordAuth) authRequired = true
        manifest
    }

    suspend fun probeSearch(): Result<Int> = runCatching {
        fetch<AddonSearchResponse>(endpoint(listOf("search"), mapOf("q" to PROBE_QUERY))).tracks.size
    }

    /**
     * Verifies the addon by fetching its manifest, or falls back to probing /search
     * if the manifest is missing or non-standard.
     */
    suspend fun verifyAndGetManifest(): Result<AddonManifest> = withContext(Dispatchers.IO) {
        val manifestRes = manifest()
        if (manifestRes.isSuccess) {
            return@withContext manifestRes
        }

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
                    resources = listOf("stream", "search"),
                )
            )
        }

        manifestRes
    }

    // ── Search ────────────────────────────────────────────────────────────

    suspend fun search(query: String, tier: String = TIER_LOSSLESS): Result<List<AddonTrack>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())

        val params = settingsFor(tier)
        return searches.get(
            key = keyOf(trimmed, params.toString()),
            describe = { "▶ addon search(${redact(baseUrl)}) q=\"$trimmed\" tier=$tier" },
        ) {
            fetch<AddonSearchResponse>(endpoint(listOf("search"), params + ("q" to trimmed)))
        }.map { it.tracks }
    }

    // ── Stream ────────────────────────────────────────────────────────────

    suspend fun stream(trackId: String, tier: String = TIER_LOSSLESS, stereoOnly: Boolean = false): Result<AddonStream> {
        val params = settingsFor(tier, stereoOnly)
        return streams.get(
            key = keyOf(trackId, params.toString()),
            describe = { "▶ addon stream(${redact(baseUrl)}) id=$trackId tier=$tier" },
        ) {
            fetch<AddonStream>(endpoint(listOf("stream", trackId), params))
        }
    }

    // ── Settings passthrough ──────────────────────────────────────

    private suspend fun settingsFor(tier: String, stereoOnly: Boolean = false): Map<String, String> {
        val manifest = manifest().getOrNull()
        val declared = manifest?.settings.orEmpty()
        val params = LinkedHashMap<String, String>()
        declared.forEach { setting ->
            val key = setting.key.takeIf { it.isNotBlank() } ?: return@forEach
            setting.defaultValue?.let { params[key] = it }
        }
        if (tier.isNotBlank()) {
            val options = declared.firstOrNull { it.key == QUALITY_KEY }
                ?.options.orEmpty()
                .mapNotNull { it.stringValue }
            val chosen = when {
                tier != TIER_LOSSLESS -> matchTier(tier, options)
                manifest == null -> pickedQuality
                else -> pickedQuality?.takeIf { it in options } ?: options.firstOrNull()
            }
            params[QUALITY_KEY] = chosen ?: tier
        }
        if (stereoOnly) {
            val atmosSetting = declared.firstOrNull { it.key == ATMOS_KEY }
            if (atmosSetting == null) {
                params.remove(ATMOS_KEY)
            } else {
                params[ATMOS_KEY] = atmosSetting.options
                    .mapNotNull { it.stringValue }
                    .firstOrNull { ATMOS_OFF.matches(it) }
                    ?: ATMOS_NEVER
            }
        } else {
            params.putIfAbsent(ATMOS_KEY, ATMOS_AUTO)
        }
        return params
    }

    private fun matchTier(tier: String, options: List<String>): String? {
        if (options.isEmpty()) return null
        options.firstOrNull { it.equals(tier, ignoreCase = true) }?.let { return it }
        val wanted = if (tier.equals(TIER_LOW, ignoreCase = true)) LOW_WORDS else HIGH_WORDS
        return options.firstOrNull { option -> wanted.any { it in option.lowercase() } }
            ?: options.first()
    }

    // ── Transport ─────────────────────────────────────────────────────────

    private fun endpoint(segments: List<String>, params: Map<String, String>): String {
        val base = baseUrl.toHttpUrlOrNull()
            ?: throw AddonException("That is not a usable address")
        val builder = base.newBuilder()
        segments.forEach(builder::addPathSegment)
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private suspend inline fun <reified T> fetch(url: String): T =
        try {
            json.decodeFromString<T>(body(url))
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            if (failure !is AddonNotFound) {
                Timber.tag(TAG).w("  ✗ addon call failed ${redact(url)}: ${failure.message}")
            }
            throw failure
        }

    private suspend fun body(url: String): String = withContext(Dispatchers.IO) {
        var attempt = 0
        var authRetries = 0
        var rejectedToken: String? = null
        while (true) {
            (quietUntilMs - System.currentTimeMillis()).takeIf { it > 0 }?.let { delay(it) }

            val auth = if (authRequired) authHeaders(url, rejectedToken) else emptyMap()
            val request = Request.Builder().url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .apply { auth.forEach { (name, value) -> header(name, value) } }
                .build()

            val wait = client.newCall(request).execute().use { response ->
                val authChallenge = response.takeIf { it.code == 401 }
                    ?.header("WWW-Authenticate")
                    ?.takeIf { it.trimStart().startsWith("BitChord", ignoreCase = true) }
                when {
                    response.isSuccessful ->
                        return@withContext response.body?.string()?.takeIf { it.isNotBlank() }
                            ?: throw AddonException("Empty response")
                    authChallenge != null -> {
                        val code = AUTH_ERROR.find(authChallenge)?.groupValues?.get(1)
                        when {
                            authRetries >= 2 -> throw AddonException(AddonAuth.rejectionMessage(code))
                            auth.isEmpty() -> authRequired = true
                            code == "expired" || code == "invalid_token" -> rejectedToken = auth[AddonAuth.TOKEN_HEADER]
                            else -> throw AddonException(AddonAuth.rejectionMessage(code))
                        }
                        authRetries++
                        RETRY_NOW
                    }
                    response.code == 404 -> throw AddonNotFound()
                    response.code >= 500 -> throw AddonUnavailable("HTTP ${response.code}")
                    response.code != 429 -> throw AddonException("HTTP ${response.code}")
                    attempt >= MAX_RETRIES -> throw AddonUnavailable("This addon is rate limiting requests")
                    else -> retryAfterMs(response.header("Retry-After"), attempt)
                }
            }
            if (wait == RETRY_NOW) continue
            quietUntilMs = System.currentTimeMillis() + wait
            attempt++
        }
        @Suppress("UNREACHABLE_CODE")
        throw AddonException("unreachable")
    }

    private suspend fun authHeaders(url: String, rejectedToken: String?): Map<String, String> {
        val parsed = url.toHttpUrlOrNull() ?: throw AddonException("Invalid addon URL")
        val loopback = parsed.host == "localhost" || parsed.host == "127.0.0.1" || parsed.host == "::1"
        if (!parsed.isHttps && !loopback) {
            throw AddonException("This addon asks for verification but is not using HTTPS")
        }
        return AddonAuth.headersFor(parsed.host, rejectedToken)
    }

    suspend fun checkAuth(): Result<Unit> =
        runCatching { authHeaders(baseUrl, rejectedToken = null); Unit }
            .onFailure { if (it is CancellationException) throw it }

    private fun retryAfterMs(header: String?, attempt: Int): Long {
        val stated = header?.trim()?.toDoubleOrNull()?.times(1000)?.toLong()
        return (stated ?: (BACKOFF_BASE_MS shl attempt)).coerceIn(BACKOFF_BASE_MS, BACKOFF_CAP_MS)
    }

    fun clear() {
        manifests.clear()
        searches.clear()
        streams.clear()
        quietUntilMs = 0L
        authRequired = false
    }

    private fun keyOf(vararg parts: String) = parts.joinToString("|") { "${it.length}:$it" }

    companion object {
        private const val TAG = "AddonClient"

        const val TIER_LOSSLESS = "LOSSLESS"
        const val TIER_HIGH = "HIGH"
        const val TIER_LOW = "LOW"

        internal const val QUALITY_KEY = "quality"
        private const val ATMOS_KEY = "atmos"
        private const val ATMOS_AUTO = "auto"
        private const val ATMOS_NEVER = "0"
        private val ATMOS_OFF = Regex("0|false|off|no|none|never|stereo", RegexOption.IGNORE_CASE)

        private val HIGH_WORDS = listOf("high", "320", "normal", "standard")
        private val LOW_WORDS = listOf("low", "96", "128", "min")

        private const val MANIFEST_SUFFIX = "/manifest.json"

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

        fun manifestUrl(base: String): String = "$base$MANIFEST_SUFFIX"

        fun redact(url: String): String =
            url.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}/***" } ?: "***"

        const val MANIFEST_TTL_MS = 10 * 60 * 1000L
        const val SEARCH_TTL_MS = 10 * 60 * 1000L
        const val STREAM_TTL_MS = 5 * 60 * 1000L

        private const val MAX_RETRIES = 2
        private const val RETRY_NOW = -1L
        private val AUTH_ERROR = Regex("""error="([^"]*)"""")
        private const val BACKOFF_BASE_MS = 500L
        private const val BACKOFF_CAP_MS = 8_000L

        private val USER_AGENT = "BitChord/v${com.nocturne.music.BuildConfig.VERSION_NAME}"
        private const val PROBE_QUERY = "music"

        private val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .build()
        }
    }
}
