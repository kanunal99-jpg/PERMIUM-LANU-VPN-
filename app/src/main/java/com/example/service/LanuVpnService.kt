package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.IpApiService
import com.example.data.VpnRepository
import com.example.manager.VpnConnectionManager
import com.example.manager.VpnState
import com.example.util.WireGuardConfigBuilder
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class LanuVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var repository: VpnRepository
    private var backend: Backend? = null
    private val tunnel = WireGuardTunnel("LanuVpnTunnel")
    private val scope = CoroutineScope(Dispatchers.IO)
    private var statsJob: Job? = null

    companion object {
        const val ACTION_CONNECT = "com.example.START_VPN"
        const val ACTION_DISCONNECT = "com.example.STOP_VPN"
        private const val CHANNEL_ID = "lanu_vpn_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "LanuVpnService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        repository = VpnRepository.getInstance(applicationContext)
        try {
            backend = GoBackend(applicationContext)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize GoBackend (Native WireGuard engine).", e)
            backend = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverId = intent.getStringExtra("server_id") ?: ""
                val endpoint = intent.getStringExtra("server_endpoint") ?: ""
                val port = intent.getIntExtra("server_port", 51820)
                val publicKey = intent.getStringExtra("server_public_key") ?: ""
                val privateKey = intent.getStringExtra("client_private_key") ?: ""
                val address = intent.getStringExtra("client_address") ?: "10.0.0.2/32"

                startVpn(serverId, endpoint, port, publicKey, privateKey, address)
            }
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    override fun onRevoke() {
        Log.i(TAG, "VPN Revoked by system or user.")
        stopVpn()
        super.onRevoke()
    }

    private fun startVpn(
        serverId: String,
        endpoint: String,
        port: Int,
        publicKey: String,
        privateKey: String,
        address: String
    ) {
        Log.i(TAG, "Starting VPN to $endpoint:$port...")

        startForeground(NOTIFICATION_ID, createNotification())

        scope.launch {
            try {
                val config = WireGuardConfigBuilder.build(
                    clientPrivateKey = privateKey,
                    clientAddress = address,
                    serverPublicKey = publicKey,
                    serverEndpoint = endpoint,
                    serverPort = port
                )

                val activeBackend = backend ?: error("WireGuard backend unavailable")
                activeBackend.setState(tunnel, Tunnel.State.UP, config)

                val peerKey = com.wireguard.crypto.Key.fromBase64(publicKey)
                var handshakeVerified = false

                repeat(15) {
                    delay(1000)
                    val peerStats = activeBackend.getStatistics(tunnel).peer(peerKey)
                    if (peerStats != null && peerStats.latestHandshakeEpochMillis > 0L) {
                        handshakeVerified = true
                        return@repeat
                    }
                }

                if (!handshakeVerified) {
                    error("WireGuard handshake not received")
                }

                Log.i(TAG, "WireGuard tunnel handshake verified; validating public IP.")
                val vpnIp = IpApiService.fetchCurrentIp()
                require(vpnIp.isNotBlank() && vpnIp != "Unknown IP") {
                    "Public IP verification failed"
                }
                Log.i(TAG, "Current IP after VPN: $vpnIp")

                VpnConnectionManager.updateState(VpnState.CONNECTED, vpnIp)
                startStatsCollection()
            } catch (e: Exception) {
                Log.e(TAG, "Real WireGuard connection failed", e)
                VpnConnectionManager.updateState(VpnState.ERROR)
                stopVpn()
            }
        }
    }

    private fun startStatsCollection() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (true) {
                val stats = backend?.getStatistics(tunnel)
                if (stats != null) {
                    VpnConnectionManager.updateStats(stats.totalRx(), stats.totalTx())
                }
                delay(1000)
            }
        }
    }

    private fun stopVpn() {
        Log.i(TAG, "Stopping VPN Service...")
        statsJob?.cancel()
        try {
            backend?.setState(tunnel, Tunnel.State.DOWN, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping tunnel", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        statsJob?.cancel()
        try {
            backend?.setState(tunnel, Tunnel.State.DOWN, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping tunnel in onDestroy", e)
        }
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lanu VPN Aktif")
            .setContentText("WireGuard ile korunuyor")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Lanu VPN Servis Kanalı",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
