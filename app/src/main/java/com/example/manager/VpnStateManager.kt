package com.example.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton responsible for maintaining the single source of truth for VPN connection status.
 */
object VpnStateManager {
    enum class VpnStatus {
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        ERROR
    }

    private val _status = MutableStateFlow(VpnStatus.DISCONNECTED)
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateStatus(newStatus: VpnStatus, error: String? = null) {
        _status.value = newStatus
        _errorMessage.value = error
    }

    val isConnected: Boolean
        get() = _status.value == VpnStatus.CONNECTED
}
