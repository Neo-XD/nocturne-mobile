package com.nocturne.music.ui.components

import com.nocturne.music.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** What [AiDetector] found for a track: AI-made music, and whether its artwork is AI-made too. */
data class AiVerdict(val music: Boolean, val art: Boolean) {
    companion object {
        val NONE = AiVerdict(music = false, art = false)
    }
}

/**
 * Tags AI-generated music from the SoulOverAI dataset of reviewed AI artists, plus explicit
 * markers in a title ("AI cover", "Suno", "(AI)").
 *
 * An artist is matched by YouTube channel id first, then by exact name. A name match is
 * refused when the dataset pins that name to a different channel, so a real artist who
 * shares a name with an AI act is never tagged. The dataset's `ai-visuals` marker drives
 * [AiVerdict.art].
 */
object AiDetector {
    private const val DATASET_URL =
        "https://raw.githubusercontent.com/xoundbyte/soul-over-ai/main/dist/artists.json"
    private const val CACHE_FILE = "soul_over_ai_artists.json"

    // The dataset is under 1 MB; anything far larger is not it.
    private const val MAX_DATASET_BYTES = 8L * 1024 * 1024
    private const val AI_VISUALS = "ai-visuals"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val started = AtomicBoolean(false)

    private val TITLE_MARKERS = Regex(
        """(?i)\b(suno(?:\s*ai)?|udio\s*ai|ai\s+cover|ai\s+generated|ai\s+remake|ai\s+vocals|ai\s+version)\b|[(\[](?:ai|ai\s+cover|suno|udio)[)\]]""",
    )
    private val CREDIT_SPLIT = Regex("""\s*(?:,|&|\bx\b|\bfeat\.?|\bft\.?|\bwith\b)\s*""", RegexOption.IGNORE_CASE)

    private class Entry(val channelId: String?, val markers: Set<String>)

    private class Index(val byChannel: Map<String, Entry>, val byName: Map<String, Entry>)

    @Volatile
    private var index = Index(emptyMap(), emptyMap())

    /** Where the last good copy of the dataset is kept, so tags survive being offline. Set at startup. */
    @Volatile
    var cacheDir: File? = null

    private val _revision = MutableStateFlow(0)

    /** Bumps whenever the dataset changes, so remembered verdicts can be recomputed. */
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun verdict(song: Song): AiVerdict {
        ensureStarted()
        var music = TITLE_MARKERS.containsMatchIn(song.title)
        var art = false
        for ((name, channelId) in credits(song)) {
            val entry = lookup(name, channelId) ?: continue
            music = true
            if (AI_VISUALS in entry.markers) art = true
        }
        return if (music || art) AiVerdict(music, art) else AiVerdict.NONE
    }

    private fun credits(song: Song): List<Pair<String, String?>> {
        if (song.artists.isNotEmpty()) return song.artists.map { it.name to it.browseId }
        val whole = song.artist.trim()
        if (whole.isEmpty()) return emptyList()
        val parts = whole.split(CREDIT_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }
        // The lead's id belongs to the first credit only; the whole byline is tried as one name too.
        return listOf(whole to song.artistId) + parts.mapIndexed { i, part -> part to song.artistId.takeIf { i == 0 } }
    }

    private fun lookup(name: String, channelId: String?): Entry? {
        val current = index
        if (channelId != null) current.byChannel[channelId]?.let { return it }
        val entry = current.byName[normalize(name)] ?: return null
        if (entry.channelId != null && channelId != null && channelId != entry.channelId) return null
        return entry
    }

    private fun normalize(name: String) = name.trim().lowercase()

    private fun ensureStarted() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            cacheFile()?.takeIf { it.isFile && it.length() in 1..MAX_DATASET_BYTES }?.let { file ->
                runCatching { install(file.readText()) }
            }
            refresh()
        }
    }

    private fun refresh() {
        runCatching {
            val req = okhttp3.Request.Builder()
                .url(DATASET_URL)
                .header("User-Agent", "NocturneMusic")
                .build()
            com.nocturne.music.data.Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return
                val body = resp.body ?: return
                if (body.contentLength() > MAX_DATASET_BYTES) return
                val text = body.string()
                if (text.length > MAX_DATASET_BYTES) return
                install(text)
                cacheFile()?.let { file ->
                    val tmp = File(file.parentFile, "$CACHE_FILE.tmp")
                    tmp.writeText(text)
                    tmp.renameTo(file)
                }
            }
        }
    }

    private fun install(text: String) {
        val array = json.parseToJsonElement(text) as? JsonArray ?: return
        val byChannel = HashMap<String, Entry>()
        val byName = HashMap<String, Entry>()
        for (element in array) {
            val obj = runCatching { element.jsonObject }.getOrNull() ?: continue
            if (obj["removed"]?.jsonPrimitive?.booleanOrNull == true) continue
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: continue
            val channelId = obj["youtube"]?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("UC") }
            val markers = obj["markers"]?.let { runCatching { it.jsonArray }.getOrNull() }
                ?.mapNotNull { runCatching { it.jsonPrimitive.contentOrNull?.trim() }.getOrNull() }
                ?.toSet()
                .orEmpty()
            val entry = Entry(channelId, markers)
            channelId?.let { byChannel[it] = entry }
            byName[normalize(name)] = entry
        }
        if (byName.isEmpty()) return
        index = Index(byChannel, byName)
        _revision.value++
    }

    private fun cacheFile(): File? = cacheDir?.let { File(it, CACHE_FILE) }
}
