/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.utils

import android.content.Context
import org.json.JSONArray
import timber.log.Timber

/**
 * Persists manual playlist song order to disk while synchronization with YouTube Music is pending.
 * Prevents background sync operations from reverting local reordering before remote updates take effect.
 */
object PlaylistOrderCache {
    private const val PREFS_NAME = "nocturne_playlist_order_cache"
    private const val PREFIX_PENDING = "pending_order_"

    /**
     * Cache the custom order of song IDs to disk for a playlist.
     */
    fun saveCustomOrder(context: Context, playlistId: String, songIds: List<String>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            songIds.forEach { jsonArray.put(it) }
            prefs.edit()
                .putString(PREFIX_PENDING + playlistId, jsonArray.toString())
                .apply()
            Timber.d("PlaylistOrderCache: Saved order with ${songIds.size} songs for playlist $playlistId")
        } catch (e: Exception) {
            Timber.e(e, "PlaylistOrderCache: Failed to save custom order")
        }
    }

    /**
     * Retrieve the cached custom order of song IDs if present.
     */
    fun getCustomOrder(context: Context, playlistId: String): List<String>? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(PREFIX_PENDING + playlistId, null) ?: return null
            val jsonArray = JSONArray(jsonStr)
            val list = ArrayList<String>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            Timber.e(e, "PlaylistOrderCache: Failed to get custom order")
            null
        }
    }

    /**
     * Check if a playlist has an unconfirmed pending order sync.
     */
    fun hasPendingOrder(context: Context, playlistId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(PREFIX_PENDING + playlistId)
    }

    /**
     * Clear the cache once remote is confirmed to be in sync.
     */
    fun clearOrder(context: Context, playlistId: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(PREFIX_PENDING + playlistId)
                .apply()
            Timber.d("PlaylistOrderCache: Cleared custom order for playlist $playlistId")
        } catch (e: Exception) {
            Timber.e(e, "PlaylistOrderCache: Failed to clear custom order")
        }
    }
}
