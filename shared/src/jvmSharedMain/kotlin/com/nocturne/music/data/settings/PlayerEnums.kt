package com.nocturne.music.data.settings

/**
 * Stream bitrate ceiling on the YouTube fallback path — MEDIUM, HIGH and
 * LOSSLESS all mean "whatever the best available Opus format is" there; what
 * actually tells them apart is which other sources are allowed to answer
 * *before* YouTube gets asked. That part is [permits], and the rungs read:
 *
 * - [LOSSLESS] — the user's own addons and JioSaavn both asked.
 * - [HIGH] — the addons skipped, JioSaavn asked.
 * - [MEDIUM] and [LOW] — both skipped; YouTube's own Opus ladder is all there
 *   is, capped at [maxKbps].
 *
 * [hourly] is what the ceiling costs in data over an hour of listening, which
 * is the only part of this a user actually cares about on a metered plan.
 */
enum class AudioQuality(
    val maxKbps: Int,
    val label: String,
    val detail: String,
    val hourly: String,
) {
    LOW(64, "Low", "~64 kbps · smallest download", "29 MB/hr"),
    MEDIUM(Int.MAX_VALUE, "Medium", "Best available · ~171 kbps Opus", "77 MB/hr"),
    HIGH(Int.MAX_VALUE, "High", "JioSaavn up to 320kbps, YouTube fallback", "144 MB/hr"),
    LOSSLESS(Int.MAX_VALUE, "Lossless", "Your addons + JioSaavn, bit-exact where available", "300+ MB/hr"),
    ;
}

/** What the phone's main player stands on, below the artwork. */
enum class PlayerTheme {
    /** The artwork's own colours hung off its bottom edge, as a mesh. */
    DEFAULT,

    /** The cover blurred across the whole screen, as the lyrics and queue are. */
    SIMPLE,

    /** Material 3 Expressive tonal backdrop derived from the artwork palette. */
    M3E,
}

/** Visual layout style for the expanded player screen. */
enum class PlayerStyle {
    /** Nocturne player layout with glass controls, top source bar, and bottom panel pill. */
    SQUARE,

    /** Classic player layout. */
    CLASSIC,
}

/** Overall app design language. */
enum class UiStyle {
    /** Default Liquid Glass / Apple-inspired chrome. */
    DEFAULT,

    /** Material 3 Expressive (M3E) dynamic tonal surfaces, shapes, and navigation. */
    M3E,
}

/** Layout style for the Home tab. */
enum class HomeStyle {
    /** Default Listen Now feed. */
    DEFAULT,

    /** Nocturne Music (LiMusic-inspired) greeting, mood chips, hero carousel, and shortcuts. */
    NOCTURNE_MUSIC,
}

/** The surface that was last open inside the expanded player. */
enum class LastPlayerScreen {
    MAIN,
    LYRICS,
    QUEUE,
}
