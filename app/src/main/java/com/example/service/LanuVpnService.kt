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
import com.example.data.LanuDatabase
import com.example.data.VpnRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * LanuVpnService establishes the VPN tunnel infrastructure.
 * This skeleton implements the core VpnService lifecycle and provides 
 * the foundation for future WireGuard tunnel integration.
 */
class LanuVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerInstance: VpnTunnelWorker? = null
    private var tunnelWorkerThread: Thread? = null
    private lateinit var repository: VpnRepository

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val endpoint = intent.getStringExtra("server_endpoint") ?: "127.0.0.1"
                val port = intent.getIntExtra("server_port", 51820)
                startVpn(endpoint, port)
            }
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn(serverEndpoint: String, serverPort: Int) {
        Log.i(TAG, "Starting VPN Service to $serverEndpoint:$serverPort...")
        
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        try {
            val builder = Builder()
                .setSession("LanuVpnTunnel")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addAddress("fd00::2", 128)
                .addRoute("::", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("2606:4700:4700::1111")
                .setMtu(1420)

            runBlocking {
                val excludedApps = repository.getExcludedAppsSync()
                excludedApps.forEach {
                    try {
                        builder.addDisallowedApplication(it.packageName)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to exclude app: ${it.packageName}", e)
                    }
                }
            }
            
            val fd = builder.establish()
            vpnInterface = fd
            
            if (fd != null) {
                workerInstance = VpnTunnelWorker(this, fd, serverEndpoint, serverPort)
                tunnelWorkerThread = Thread(workerInstance)
                tunnelWorkerThread?.start()
                Log.i(TAG, "VPN Tunnel interface established and worker started.")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error establishing VPN interface", e)
            stopVpn()
        }
    }

    private fun stopVpn() {
        Log.i(TAG, "Stopping VPN Service...")
        try {
            workerInstance?.stop()
            tunnelWorkerThread?.interrupt()
            tunnelWorkerThread = null
            workerInstance = null
            
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: IOException) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lanu VPN Active")
            .setContentText("Your connection is secured")
            .setSmallIcon(android.R.drawable.ic_lock_lock) // Standard lock icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Lanu VPN Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
