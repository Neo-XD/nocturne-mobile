package com.nocturne.music.ui.components

/**
 * Detects AI-generated music tracks (e.g. Suno, Udio, AI covers/remakes)
 * from track metadata to help listeners spot and filter AI content.
 */
object AiDetector {
    private val AI_PATTERN = Regex(
        """(?i)\b(suno(?:\s*ai)?|udio(?:\s*ai)?|ai\s+cover|ai\s+generated|ai\s+remake|ai\s+music|ai\s+vocals|ai\s+song|ai\s+version|ai\s+track)\b|\((?:ai|ai\s+cover|suno|udio)\)|\[(?:ai|ai\s+cover|suno|udio)\]"""
    )

    fun isAiTrack(title: String?, artist: String?): Boolean {
        if (title.isNullOrBlank() && artist.isNullOrBlank()) return false
        if (title != null && AI_PATTERN.containsMatchIn(title)) return true
        if (artist != null && AI_PATTERN.containsMatchIn(artist)) return true
        return false
    }
}
