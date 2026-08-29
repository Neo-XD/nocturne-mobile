package com.nocturne.music.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimedLyricWord(
    val word: String,
    @SerialName("start_ms")
    val startMs: Long,
    @SerialName("end_ms")
    val endMs: Long
)

@Serializable
data class TimedLyricLine(
    @SerialName("time_ms")
    val timeMs: Long,
    val text: String,
    val words: List<TimedLyricWord> = emptyList(),
    val translation: String? = null
)

@Serializable
data class PlainLyrics(
    val text: String,
    val footer: String? = null
)

@Serializable
data class Lyrics(
    @SerialName("video_id")
    val videoId: String,
    val timed: List<TimedLyricLine> = emptyList(),
    val plain: PlainLyrics? = null,
    val provider: String = "unknown"
) {
    val isSynced: Boolean get() = timed.isNotEmpty()
    val isWordSynced: Boolean get() = timed.any { it.words.isNotEmpty() }
}
