package com.nocturne.music.data.remote.lyrics

import com.nocturne.music.core.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.*

class LyricsService(
    private val httpClient: HttpClient,
    private val json: Json
) {
    suspend fun getLyrics(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Long? = null
    ): Lyrics {
        // 1. Try Boidu for word-by-word synced lyrics
        try {
            val boiduResult = fetchBoiduLyrics(title, artist, durationSeconds)
            if (boiduResult != null && boiduResult.timed.isNotEmpty()) {
                return boiduResult.copy(videoId = videoId, provider = "boidu")
            }
        } catch (_: Exception) {}

        // 2. Try LRCLIB for line-by-line synced lyrics
        try {
            val lrcResult = fetchLrcLibLyrics(title, artist, durationSeconds)
            if (lrcResult != null && lrcResult.timed.isNotEmpty()) {
                return lrcResult.copy(videoId = videoId, provider = "lrclib")
            }
        } catch (_: Exception) {}

        return Lyrics(videoId = videoId, provider = "none")
    }

    private suspend fun fetchBoiduLyrics(
        title: String,
        artist: String,
        duration: Long?
    ): Lyrics? {
        val cleanTitle = cleanSearchTerm(title)
        val cleanArtist = cleanSearchTerm(artist)

        val response = httpClient.get("https://api.boidu.dev/lyrics") {
            parameter("title", cleanTitle)
            parameter("artist", cleanArtist)
            duration?.let { parameter("duration", it) }
        }

        val body = response.body<String>()
        val jsonElement = json.parseToJsonElement(body).jsonObject
        val linesArray = jsonElement["lines"]?.jsonArray ?: return null

        val timedLines = mutableListOf<TimedLyricLine>()
        for (line in linesArray) {
            val lineObj = line.jsonObject
            val timeMs = lineObj["time"]?.jsonPrimitive?.longOrNull ?: 0L
            val text = lineObj["text"]?.jsonPrimitive?.contentOrNull ?: ""
            val wordsArray = lineObj["words"]?.jsonArray

            val words = mutableListOf<TimedLyricWord>()
            wordsArray?.forEach { w ->
                val wObj = w.jsonObject
                val word = wObj["word"]?.jsonPrimitive?.contentOrNull ?: ""
                val start = wObj["start"]?.jsonPrimitive?.longOrNull ?: 0L
                val end = wObj["end"]?.jsonPrimitive?.longOrNull ?: 0L
                words.add(TimedLyricWord(word = word, startMs = start, endMs = end))
            }

            timedLines.add(
                TimedLyricLine(
                    timeMs = timeMs,
                    text = text,
                    words = words
                )
            )
        }

        return Lyrics(videoId = "", timed = timedLines, provider = "boidu")
    }

    private suspend fun fetchLrcLibLyrics(
        title: String,
        artist: String,
        duration: Long?
    ): Lyrics? {
        val cleanTitle = cleanSearchTerm(title)
        val cleanArtist = cleanSearchTerm(artist)

        val response = httpClient.get("https://lrclib.net/api/get") {
            parameter("track_name", cleanTitle)
            parameter("artist_name", cleanArtist)
            duration?.let { parameter("duration", it) }
        }

        val body = response.body<String>()
        val jsonElement = json.parseToJsonElement(body).jsonObject

        val syncedLyrics = jsonElement["syncedLyrics"]?.jsonPrimitive?.contentOrNull
        val plainLyrics = jsonElement["plainLyrics"]?.jsonPrimitive?.contentOrNull

        if (syncedLyrics != null) {
            val parsedLines = parseLrcString(syncedLyrics)
            return Lyrics(
                videoId = "",
                timed = parsedLines,
                plain = plainLyrics?.let { PlainLyrics(text = it) },
                provider = "lrclib"
            )
        }

        if (plainLyrics != null) {
            return Lyrics(
                videoId = "",
                plain = PlainLyrics(text = plainLyrics),
                provider = "lrclib"
            )
        }

        return null
    }

    private fun parseLrcString(lrc: String): List<TimedLyricLine> {
        val lines = mutableListOf<TimedLyricLine>()
        val lrcRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

        for (rawLine in lrc.lines()) {
            val match = lrcRegex.find(rawLine) ?: continue
            val (min, sec, frac, text) = match.destructured
            val minutes = min.toLongOrNull() ?: 0L
            val seconds = sec.toLongOrNull() ?: 0L
            val millis = if (frac.length == 2) (frac.toLongOrNull() ?: 0L) * 10 else frac.toLongOrNull() ?: 0L

            val totalMs = (minutes * 60 + seconds) * 1000 + millis
            lines.add(TimedLyricLine(timeMs = totalMs, text = text.trim()))
        }

        return lines.sortedBy { it.timeMs }
    }

    private fun cleanSearchTerm(term: String): String {
        return term.replace(Regex("""\(.*?\)|\[.*?\]|feat\..*|ft\..*""", RegexOption.IGNORE_CASE), "").trim()
    }
}
