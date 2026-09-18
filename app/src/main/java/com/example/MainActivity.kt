package com.example

import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.LanuDatabase
import com.example.data.VpnRepository
import com.example.manager.AutoConnectManager
import com.example.manager.VpnConnectionManager
import com.example.manager.VpnStateManager
import com.example.ui.screens.LanuApp
import com.example.ui.theme.LanuVpnTheme
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.worker.ServerPingWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private lateinit var repository: VpnRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = LanuDatabase.getDatabase(applicationContext)
    repository = VpnRepository.getInstance(applicationContext)

    val pingWorkRequest = PeriodicWorkRequestBuilder<ServerPingWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
      "ServerPingPeriodicWork",
      ExistingPeriodicWorkPolicy.KEEP,
      pingWorkRequest
    )

    val connectivityManager = getSystemService(ConnectivityManager::class.java)
    connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
      private var isFirstCallback = true
      override fun onAvailable(network: Network) {
        super.onAvailable(network)
        if (!isFirstCallback && VpnStateManager.isConnected) {
          VpnConnectionManager.handleNetworkChange(applicationContext)
        }
        isFirstCallback = false
      }
    })

    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
      repository.initializeDefaultServers()

      // Check auto-connect on launch
      val autoConnect = AutoConnectManager.isAutoConnectEnabled(applicationContext).first()
      if (autoConnect && !VpnStateManager.isConnected) {
        val servers = repository.allServers.first()
        val targetServer = servers.firstOrNull { it.isLastUsed } ?: servers.firstOrNull()
        targetServer?.let {
          VpnConnectionManager.connect(applicationContext, it)
        }
      }
    }

    setContent {
      LanuVpnTheme(darkTheme = true) {
        val servers by repository.allServers.collectAsState(initial = emptyList())
        val selectedServer = servers.firstOrNull { it.isLastUsed } ?: servers.firstOrNull()

        LanuApp(
          repository = repository,
          servers = servers,
          selectedServer = selectedServer,
          onServerSelected = { server ->
            scope.launch {
              repository.setLastUsedServer(server.id)
            }
          },
          onToggleFavorite = { server ->
            scope.launch {
              repository.updateServer(server.copy(isFavorite = !server.isFavorite))
            }
          },
          onRefreshLatency = {
            scope.launch {
              repository.refreshServerHealth(applicationContext)
            }
          }
        )
      }
    }
  }
}
