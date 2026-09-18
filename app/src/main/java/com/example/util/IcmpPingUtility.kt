package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

object IcmpPingUtility {
  private const val TAG = "IcmpPingUtility"

  /**
   * Measures latency to a given host using ICMP if possible, or falls back to reachability check.
   * Does NOT use TCP sockets for UDP ports like 51820.
   */
  suspend fun ping(host: String, timeoutMs: Int = 2000): Long {
    return withContext(Dispatchers.IO) {
      try {
        val duration = measureTimeMillis {
          val inet = InetAddress.getByName(host)
          if (!inet.isReachable(timeoutMs)) {
            throw Exception("Host unreachable via ICMP/Echo")
          }
        }
        duration
      } catch (e: Exception) {
        Log.w(TAG, "Ping failed for $host - ${e.message}")
        -1L
      }
    }
  }
}
