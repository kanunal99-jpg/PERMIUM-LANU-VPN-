package com.example.util

import android.content.Context
import com.example.data.LanuDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ICMP is not a WireGuard health signal. Server status is therefore based
 * only on whether a complete real peer configuration exists. Actual
 * availability is established by the tunnel's latest handshake.
 */
object ServerHealthChecker {
  suspend fun checkAllServers(context: Context) = withContext(Dispatchers.IO) {
    val dao = LanuDatabase.getDatabase(context).lanuDao()
    dao.getAllServersList().forEach { server ->
      val ready = server.endpoint.isNotBlank() &&
        server.publicKey.isNotBlank() &&
        server.privateKey.isNotBlank() &&
        !server.publicKey.contains("PASTE_") &&
        !server.privateKey.contains("PASTE_")
      dao.updateServer(server.copy(
        status = if (ready) "READY_FOR_WIREGUARD" else "CONFIG_REQUIRED",
        latency = -1
      ))
    }
  }
}
