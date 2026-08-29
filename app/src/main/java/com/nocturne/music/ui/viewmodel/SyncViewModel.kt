package com.nocturne.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.nocturne.music.sync.SyncConnectionStatus
import com.nocturne.music.sync.SyncManager
import kotlinx.coroutines.flow.StateFlow

class SyncViewModel(
    private val syncManager: SyncManager
) : ViewModel() {

    val status: StateFlow<SyncConnectionStatus> = syncManager.status
    val errorMessage: StateFlow<String?> = syncManager.errorMessage
    val connectedHost: StateFlow<String?> = syncManager.connectedHost

    fun connect(ipOrHost: String, pin: String) {
        syncManager.connectToHost(ipOrHost = ipOrHost, pin = pin)
    }

    fun disconnect() {
        syncManager.disconnect()
    }
}
