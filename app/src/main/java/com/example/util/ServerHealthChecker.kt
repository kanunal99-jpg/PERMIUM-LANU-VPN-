package com.example.util

import android.content.Context
import com.example.data.LanuDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ICMP is deliberately not used as a WireGuard health signal.
 * A server can block ICMP while accepting UDP/51820, and vice versa.
 * The authoritative connection signal is the WireGuard peer handshake
 * observed after the tunnel is brought UP.
 */
object ServerHealthChecker {
  suspend fun checkAllServers(context: Context) = withContext(Dispatchers.IO) {
    val dao = LanuDatabase.getDatabase(context).lanuDao()
    dao.getAllServersList().forEach { server ->
      val usable = server.endpoint.isNotBlank() &&
        server.publicKey.isNotBlank() &&
        server.privateKey.isNotBlank() &&
        !server.publicKey.contains("PASTE_") &&
        !server.privateKey.contains("PASTE_")
      dao.updateServer(server.copy(
        status = if (usable) "READY_FOR_WIREGUARD" else "CONFIG_REQUIRED",
        latency = -1
      ))
    }
  }
}
