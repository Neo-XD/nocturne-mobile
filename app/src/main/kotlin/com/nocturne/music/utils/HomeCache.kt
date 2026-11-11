/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * High-performance Stale-While-Revalidate In-Memory & Session Cache for Home & Explore
 */

package com.nocturne.music.utils

import com.music.innertube.models.PlaylistItem
import com.music.innertube.pages.ExplorePage
import com.music.innertube.pages.HomePage
import com.nocturne.music.models.SimilarRecommendation
import com.nocturne.music.viewmodels.CommunityPlaylistItem
import com.nocturne.music.viewmodels.DailyDiscoverItem

object HomeCache {
    /** 20 minutes freshness lifetime */
    private const val DEFAULT_TTL_MS = 20 * 60 * 1000L

    @Volatile var homePage: HomePage? = null
    @Volatile var explorePage: ExplorePage? = null
    @Volatile var dailyDiscover: List<DailyDiscoverItem>? = null
    @Volatile var communityPlaylists: List<CommunityPlaylistItem>? = null
    @Volatile var similarRecommendations: List<SimilarRecommendation>? = null
    @Volatile var coversAndRemixes: HomePage.Section? = null
    @Volatile var accountPlaylists: List<PlaylistItem>? = null
    @Volatile var cachedAtMs: Long = 0L

    fun hasData(): Boolean = homePage != null || explorePage != null

    fun isStale(ttlMs: Long = DEFAULT_TTL_MS): Boolean {
        if (cachedAtMs == 0L) return true
        return System.currentTimeMillis() - cachedAtMs > ttlMs
    }

    fun updateTimestamp() {
        cachedAtMs = System.currentTimeMillis()
    }

    fun clear() {
        homePage = null
        explorePage = null
        dailyDiscover = null
        communityPlaylists = null
        similarRecommendations = null
        coversAndRemixes = null
        accountPlaylists = null
        cachedAtMs = 0L
    }
}
