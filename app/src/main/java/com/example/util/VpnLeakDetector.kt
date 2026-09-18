package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface

object VpnLeakDetector {
  private const val TAG = "VpnLeakDetector"

  data class LeakTestResult(
    val isDnsSecure: Boolean,
    val isIpv6Protected: Boolean,
    val activeInterface: String,
    val dnsServers: List<String>,
    val details: String
  )

  suspend fun performLeakTest(context: Context): LeakTestResult = withContext(Dispatchers.IO) {
    var dnsSecure = true
    var ipv6Protected = true
    val dnsList = mutableListOf<String>()
    var activeIface = "Unknown"

    try {
      val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      val activeNetwork = connManager?.activeNetwork
      val caps = connManager?.getNetworkCapabilities(activeNetwork)

      if (caps != null) {
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
          activeIface = "VPN Tunnel"
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
          activeIface = "Wi-Fi (Warning: VPN might be disconnected)"
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
          activeIface = "Cellular (Warning: VPN might be disconnected)"
        }
      }

      // Check network interfaces for IPv6 addresses
      val interfaces = NetworkInterface.getNetworkInterfaces()
      var foundPublicIpv6 = false
      while (interfaces.hasMoreElements()) {
        val iface = interfaces.nextElement()
        if (iface.isUp && !iface.isLoopback) {
          val addrs = iface.inetAddresses
          while (addrs.hasMoreElements()) {
            val addr = addrs.nextElement()
            if (addr is Inet6Address && !addr.isSiteLocalAddress && !addr.isLoopbackAddress) {
              // Check if it's not a tun interface
              if (!iface.name.startsWith("tun") && !iface.name.startsWith("ppp")) {
                foundPublicIpv6 = true
                Log.w(TAG, "Potential IPv6 leak detected on interface ${iface.name}: ${addr.hostAddress}")
              }
            }
          }
        }
      }

      if (foundPublicIpv6) {
        ipv6Protected = false
      }

      // Verify DNS servers
      val linkProperties = connManager?.getLinkProperties(activeNetwork)
      linkProperties?.dnsServers?.forEach { dns ->
        dnsList.add(dns.hostAddress ?: "")
        // Check if DNS is leaking to public resolvers outside VPN subnet (e.g. 8.8.8.8 over cellular/wifi when VPN should handle all DNS)
        if (dns.hostAddress?.startsWith("8.8.8.8") == true || dns.hostAddress?.startsWith("1.1.1.1") == true) {
          dnsSecure = false
        }
      }

    } catch (e: Exception) {
      Log.e(TAG, "Error performing leak test", e)
      dnsSecure = false
    }

    val details = buildString {
      append("Active Interface: $activeIface\n")
      append("DNS Servers: ${dnsList.joinToString(", ")}\n")
      append(if (dnsSecure) "✓ DNS Leak Test Passed\n" else "✗ Potential DNS Leak Detected\n")
      append(if (ipv6Protected) "✓ IPv6 Leak Protection Active\n" else "✗ IPv6 Leak Detected\n")
    }

    LeakTestResult(
      isDnsSecure = dnsSecure,
      isIpv6Protected = ipv6Protected,
      activeInterface = activeIface,
      dnsServers = dnsList,
      details = details
    )
  }
}
