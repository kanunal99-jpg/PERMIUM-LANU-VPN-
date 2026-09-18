package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.manager.VpnConnectionManager
import com.example.manager.VpnStateManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class NetworkChangeReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "NetworkChangeReceiver"
    private var lastNetworkType: Int = -1
  }

  override fun onReceive(context: Context, intent: Intent) {
    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val activeNetwork = connManager?.activeNetwork
    val capabilities = connManager?.getNetworkCapabilities(activeNetwork)

    val currentType = when {
      capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> 1
      capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> 2
      else -> 0
    }

    if (currentType == 1) {
      Log.i(TAG, "Connected to Wi-Fi network. Checking auto-connect feature.")
      
      // We need a coroutine to check the database
      val repository = com.example.data.VpnRepository.getInstance(context)
      kotlinx.coroutines.MainScope().launch {
        val settings = repository.getSettingsSync()
        if (settings?.autoConnect == true && !VpnStateManager.isConnected) {
          Log.i(TAG, "Auto-connect is enabled. Starting VPN.")
          val intentService = Intent(context, com.example.service.LanuVpnService::class.java).apply {
            action = com.example.service.LanuVpnService.ACTION_CONNECT
          }
          androidx.core.content.ContextCompat.startForegroundService(context, intentService)
        }
      }
    }

    if (lastNetworkType != -1 && lastNetworkType != currentType && currentType != 0) {
      Log.i(TAG, "Network switched between Wi-Fi and Mobile Data. Triggering VPN reconnection.")
      VpnConnectionManager.handleNetworkChange(context)
    }
    lastNetworkType = currentType
  }
}
