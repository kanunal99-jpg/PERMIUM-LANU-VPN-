package com.example.util

import android.content.Context
import com.example.data.LanuDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ServerHealthChecker {

  suspend fun checkAllServers(context: Context) {
    withContext(Dispatchers.IO) {
      try {
        val database = LanuDatabase.getDatabase(context)
        val dao = database.lanuDao()
        val servers = dao.getAllServersList()

        for (server in servers) {
          val latency = IcmpPingUtility.ping(server.endpoint)
          val status: String
          val finalLatency: Int

          if (latency >= 0) {
            finalLatency = latency.toInt()
            status = when {
              finalLatency < 120 -> "ONLINE"
              finalLatency < 300 -> "DEGRADED"
              else -> "OFFLINE"
            }
          } else {
            finalLatency = -1
            status = "OFFLINE"
          }

          val updatedServer = server.copy(
            status = status,
            latency = finalLatency
          )
          dao.updateServer(updatedServer)
        }
      } catch (e: Exception) {
        // Handle health check failure gracefully
      }
    }
  }
}
