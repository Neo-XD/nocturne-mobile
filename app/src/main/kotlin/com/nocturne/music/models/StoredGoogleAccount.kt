/**
 * Nocturne Music Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nocturne.music.models

import kotlinx.serialization.Serializable

@Serializable
data class StoredGoogleAccount(
    val id: String,
    val name: String,
    val email: String = "",
    val channelHandle: String = "",
    val cookie: String = "",
    val visitorData: String = "",
    val dataSyncId: String = "",
    val isActive: Boolean = false,
)
