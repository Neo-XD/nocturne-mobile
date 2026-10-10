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

    private const val PREFIX_PRESETS = "presets_"
    private const val PREFIX_ACTIVE_PRESET = "active_preset_"
    const val UNSAVED_CUSTOM_PROFILE = "Custom (unsaved)"

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
                .putString(PREFIX_ACTIVE_PRESET + playlistId, UNSAVED_CUSTOM_PROFILE)
                .apply()
            Timber.d("PlaylistOrderCache: Saved order with ${songIds.size} songs for playlist $playlistId (profile: $UNSAVED_CUSTOM_PROFILE)")
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
     * Save an arrangement preset with a specific name.
     */
    fun saveArrangementPreset(context: Context, playlistId: String, presetName: String, songIds: List<String>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val allPresetsJson = prefs.getString(PREFIX_PRESETS + playlistId, null)
            val presetsObj = if (allPresetsJson != null) org.json.JSONObject(allPresetsJson) else org.json.JSONObject()
            
            val jsonArray = JSONArray()
            songIds.forEach { jsonArray.put(it) }
            presetsObj.put(presetName, jsonArray)

            prefs.edit()
                .putString(PREFIX_PRESETS + playlistId, presetsObj.toString())
                .putString(PREFIX_ACTIVE_PRESET + playlistId, presetName)
                .putString(PREFIX_PENDING + playlistId, jsonArray.toString())
                .apply()
            Timber.d("PlaylistOrderCache: Saved preset '$presetName' for playlist $playlistId")
        } catch (e: Exception) {
            Timber.e(e, "PlaylistOrderCache: Failed to save arrangement preset")
        }
    }

    /**
     * Get all arrangement presets for a playlist (Map of Name -> List of Song IDs).
     */
    fun getArrangementPresets(context: Context, playlistId: String): Map<String, List<String>> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val allPresetsJson = prefs.getString(PREFIX_PRESETS + playlistId, null) ?: return emptyMap()
            val presetsObj = org.json.JSONObject(allPresetsJson)
            val result = mutableMapOf<String, List<String>>()
            val keys = presetsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val array = presetsObj.getJSONArray(key)
                val list = ArrayList<String>(array.length())
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                result[key] = list
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "PlaylistOrderCache: Failed to get presets")
            emptyMap()
        }
    }

    /**
     * Get active arrangement preset name (e.g. "Custom (unsaved)" or preset name).
     */
    fun getActivePresetName(context: Context, playlistId: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREFIX_ACTIVE_PRESET + playlistId, UNSAVED_CUSTOM_PROFILE) ?: UNSAVED_CUSTOM_PROFILE
    }

    /**
     * Set active arrangement preset name.
     */
    fun setActivePresetName(context: Context, playlistId: String, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREFIX_ACTIVE_PRESET + playlistId, name).apply()
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
