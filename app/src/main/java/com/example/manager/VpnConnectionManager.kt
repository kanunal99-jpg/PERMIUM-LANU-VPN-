package com.example.manager

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.LanuDatabase
import com.example.data.IpApiService
import com.example.data.ServerEntity
import com.example.service.LanuVpnService
import com.example.util.IcmpPingUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class VpnState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  RECONNECTING,
  DISCONNECTING,
  ERROR
}

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

  private fun updateInternalState(state: VpnState) {
    _vpnState.value = state
    val status = when (state) {
      VpnState.CONNECTING, VpnState.RECONNECTING -> VpnStateManager.VpnStatus.CONNECTING
      VpnState.CONNECTED -> VpnStateManager.VpnStatus.CONNECTED
      VpnState.DISCONNECTING, VpnState.DISCONNECTED -> VpnStateManager.VpnStatus.DISCONNECTED
      VpnState.ERROR -> VpnStateManager.VpnStatus.ERROR
    }
    VpnStateManager.updateStatus(status)
  }

  private val _isConnected = MutableStateFlow(false)
  val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

  private val _currentServer = MutableStateFlow<ServerEntity?>(null)
  val currentServer: StateFlow<ServerEntity?> = _currentServer.asStateFlow()

  private val _stats = MutableStateFlow(ConnectionStats())
  val stats: StateFlow<ConnectionStats> = _stats.asStateFlow()

  private var timerJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.IO)

  fun handleNetworkChange(context: Context) {
    if (_isConnected.value) {
      val server = _currentServer.value
      if (server != null) {
        scope.launch {
          updateInternalState(VpnState.RECONNECTING)
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
          // CONNECTED will be set by the service after verification
        }
      }
    }
  }

  fun toggleConnection(context: Context, server: ServerEntity?) {
    if (_isConnected.value) {
      disconnect(context)
    } else {
      server?.let { connect(context, it) }
    }
  }

  fun updateState(state: VpnState, vpnIp: String? = null) {
    updateInternalState(state)
    if (state == VpnState.CONNECTED) {
        _isConnected.value = true
        vpnIp?.let { _stats.value = _stats.value.copy(currentIp = it) }
        startTimer()
    } else if (state == VpnState.DISCONNECTED || state == VpnState.ERROR) {
        _isConnected.value = false
        stopTimer()
    }
  }

  fun updateStats(bytesIn: Long, bytesOut: Long) {
    _stats.value = _stats.value.copy(
        bytesIn = bytesIn,
        bytesOut = bytesOut
    )
  }

  fun connect(context: Context, server: ServerEntity) {
    if (_vpnState.value == VpnState.CONNECTED || _vpnState.value == VpnState.CONNECTING) return
    
    updateInternalState(VpnState.CONNECTING)
    _currentServer.value = server
    _stats.value = _stats.value.copy(serverIp = server.endpoint)
    
    scope.launch {
      try {
        // Measure real-time latency before establishing tunnel
        val pingResult = IcmpPingUtility.ping(server.endpoint)
        if (pingResult < 0) {
          updateInternalState(VpnState.ERROR)
          return@launch
        }
        
        // Update latency in local DB for UI persistence
        val dao = LanuDatabase.getDatabase(context).lanuDao()
        dao.updateServer(server.copy(latency = pingResult.toInt()))

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
        
        // Note: updateInternalState(VpnState.CONNECTED) will be called by the service 
        // after successful handshake verification.
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
      val intent = Intent(context, LanuVpnService::class.java).apply {
        action = LanuVpnService.ACTION_DISCONNECT
      }
      context.startService(intent)
      
      stopTimer()
      updateInternalState(VpnState.DISCONNECTED)
      _isConnected.value = false
      scope.launch {
        val currentIp = IpApiService.fetchCurrentIp()
        _stats.value = _stats.value.copy(currentIp = currentIp, durationSeconds = 0)
      }
    }
  }

  private fun startTimer() {
    timerJob?.cancel()
    val startTime = System.currentTimeMillis()
    timerJob = scope.launch {
      while (isActive && _vpnState.value == VpnState.CONNECTED) {
        delay(1000)
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        // TODO: Get real traffic stats from VpnService
        _stats.value = _stats.value.copy(
          durationSeconds = elapsed
        )
      }
    }
  }

  private fun stopTimer() {
    timerJob?.cancel()
    timerJob = null
  }
}
