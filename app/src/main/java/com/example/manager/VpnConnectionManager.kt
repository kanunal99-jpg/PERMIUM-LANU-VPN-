package com.example.manager

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.ServerEntity
import com.example.service.LanuVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class VpnState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, DISCONNECTING, ERROR }

data class ConnectionStats(
  val durationSeconds: Long = 0,
  val bytesIn: Long = 0,
  val bytesOut: Long = 0,
  val currentIp: String = "Unknown",
  val serverIp: String = "Unknown"
)

object VpnConnectionManager {
  private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
  val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()
  private val _isConnected = MutableStateFlow(false)
  val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
  private val _currentServer = MutableStateFlow<ServerEntity?>(null)
  val currentServer: StateFlow<ServerEntity?> = _currentServer.asStateFlow()
  private val _stats = MutableStateFlow(ConnectionStats())
  val stats: StateFlow<ConnectionStats> = _stats.asStateFlow()
  private var timerJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.IO)

  private fun updateInternalState(state: VpnState) {
    _vpnState.value = state
    VpnStateManager.updateStatus(
      when (state) {
        VpnState.CONNECTING, VpnState.RECONNECTING -> VpnStateManager.VpnStatus.CONNECTING
        VpnState.CONNECTED -> VpnStateManager.VpnStatus.CONNECTED
        VpnState.DISCONNECTING, VpnState.DISCONNECTED -> VpnStateManager.VpnStatus.DISCONNECTED
        VpnState.ERROR -> VpnStateManager.VpnStatus.ERROR
      }
    )
  }

  fun handleNetworkChange(context: Context) {
    if (!_isConnected.value) return
    _currentServer.value?.let { connect(context, it) }
  }

  fun toggleConnection(context: Context, server: ServerEntity?) {
    if (_isConnected.value) disconnect(context) else server?.let { connect(context, it) }
  }

  fun updateState(state: VpnState, vpnIp: String? = null) {
    updateInternalState(state)
    when (state) {
      VpnState.CONNECTED -> {
        _isConnected.value = true
        vpnIp?.let { _stats.value = _stats.value.copy(currentIp = it) }
        startTimer()
      }
      VpnState.DISCONNECTED, VpnState.ERROR -> {
        _isConnected.value = false
        stopTimer()
      }
      else -> Unit
    }
  }

  fun updateStats(bytesIn: Long, bytesOut: Long) {
    _stats.value = _stats.value.copy(bytesIn = bytesIn, bytesOut = bytesOut)
  }

  fun connect(context: Context, server: ServerEntity) {
    if (_vpnState.value == VpnState.CONNECTED || _vpnState.value == VpnState.CONNECTING) return
    if (!server.hasUsableWireGuardConfig()) {
      updateInternalState(VpnState.ERROR)
      return
    }
    updateInternalState(VpnState.CONNECTING)
    _currentServer.value = server
    _stats.value = _stats.value.copy(serverIp = server.endpoint)

    scope.launch {
      try {
        val intent = Intent(context, LanuVpnService::class.java).apply {
          action = LanuVpnService.ACTION_CONNECT
          putExtra("server_id", server.id)
          putExtra("server_endpoint", server.endpoint)
          putExtra("server_port", server.port)
          putExtra("server_public_key", server.publicKey)
          putExtra("client_private_key", server.privateKey)
          putExtra("client_address", server.address)
        }
        ContextCompat.startForegroundService(context, intent)
      } catch (e: Exception) {
        updateInternalState(VpnState.ERROR)
        _isConnected.value = false
      }
    }
  }

  fun disconnect(context: Context) {
    if (_vpnState.value == VpnState.DISCONNECTED) return
    updateInternalState(VpnState.DISCONNECTING)
    scope.launch {
      context.startService(Intent(context, LanuVpnService::class.java).apply {
        action = LanuVpnService.ACTION_DISCONNECT
      })
      stopTimer()
      updateInternalState(VpnState.DISCONNECTED)
      _isConnected.value = false
      _stats.value = _stats.value.copy(currentIp = "Unknown", durationSeconds = 0)
    }
  }

  private fun startTimer() {
    timerJob?.cancel()
    val startTime = System.currentTimeMillis()
    timerJob = scope.launch {
      while (isActive && _vpnState.value == VpnState.CONNECTED) {
        delay(1000)
        _stats.value = _stats.value.copy(durationSeconds = (System.currentTimeMillis() - startTime) / 1000)
      }
    }
  }

  private fun stopTimer() {
    timerJob?.cancel()
    timerJob = null
  }
}

private fun ServerEntity.hasUsableWireGuardConfig(): Boolean =
  endpoint.isNotBlank() &&
  port in 1..65535 &&
  publicKey.isNotBlank() &&
  privateKey.isNotBlank() &&
  !publicKey.contains("PASTE_", ignoreCase = true) &&
  !privateKey.contains("PASTE_", ignoreCase = true) &&
  address.isNotBlank()
