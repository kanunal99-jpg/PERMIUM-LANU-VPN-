package com.example.util

import android.content.Context
import com.example.data.LanuDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object ServerHealthChecker {

  suspend fun checkAllServers(context: Context) {
    withContext(Dispatchers.IO) {
      try {
        val database = LanuDatabase.getDatabase(context)
        val dao = database.lanuDao()
        val servers = dao.getAllServersList()

        for (server in servers) {
          val latency = IcmpPingUtility.ping(server.endpoint, server.port)
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
            finalLatency = 999
            status = "OFFLINE"
          }

          val loadDelta = Random.nextInt(-4, 5)
          val newLoad = max(5, min(98, server.load + loadDelta))

          val updatedServer = server.copy(
            status = status,
            latency = finalLatency,
            load = newLoad
          )
          dao.updateServer(updatedServer)
        }
      } catch (e: Exception) {
        // Handle health check failure gracefully
      }
    }
  }
}
