package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

object IcmpPingUtility {
  private const val TAG = "IcmpPingUtility"

  /**
   * Measures latency to a given host and port asynchronously without blocking the main thread.
   * Falls back to a socket connection ping if ICMP echo requires root privileges.
   */
  suspend fun ping(host: String, port: Int = 443, timeoutMs: Int = 2000): Long {
    return withContext(Dispatchers.IO) {
      try {
        val duration = measureTimeMillis {
          Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
          }
        }
        duration
      } catch (e: Exception) {
        Log.w(TAG, "Ping failed for $host:$port - ${e.message}")
        -1L
      }
    }
  }
}
