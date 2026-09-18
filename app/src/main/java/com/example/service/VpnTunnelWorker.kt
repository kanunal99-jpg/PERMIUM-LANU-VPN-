package com.example.service

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 * VpnTunnelWorker handles the packet loop for the VPN tunnel.
 * It reads IP packets from the TUN interface and forwards them to the remote VPN server via UDP.
 * It also receives UDP packets from the server and writes them back to the TUN interface.
 */
class VpnTunnelWorker(
    private val vpnService: VpnService,
    private val tunnelInterface: ParcelFileDescriptor,
    private val serverAddress: String,
    private val serverPort: Int
) : Runnable {

    companion object {
        private const val TAG = "VpnTunnelWorker"
        private const val MAX_PACKET_SIZE = 32767
    }

    @Volatile
    private var isRunning = true

    override fun run() {
        Log.i(TAG, "Starting tunnel worker to $serverAddress:$serverPort")
        
        var tunnel: DatagramChannel? = null
        try {
            tunnel = DatagramChannel.open()
            if (!vpnService.protect(tunnel.socket())) {
                throw IllegalStateException("Cannot protect tunnel socket")
            }

            tunnel.connect(InetSocketAddress(serverAddress, serverPort))
            tunnel.configureBlocking(true)

            val inputStream = FileInputStream(tunnelInterface.fileDescriptor)
            val outputStream = FileOutputStream(tunnelInterface.fileDescriptor)

            // Start Ingress thread (Server -> Device)
            val ingressThread = Thread {
                val packet = ByteBuffer.allocate(MAX_PACKET_SIZE)
                var lastKeepAlive = System.currentTimeMillis()
                try {
                    while (isRunning) {
                        packet.clear()
                        val readFromServer = tunnel.read(packet)
                        if (readFromServer > 0) {
                            outputStream.write(packet.array(), 0, readFromServer)
                        }

                        // Send NAT Keep-alive every 20 seconds
                        val now = System.currentTimeMillis()
                        if (now - lastKeepAlive > 20000) {
                            val keepAlive = ByteBuffer.allocate(1)
                            keepAlive.put(0x00.toByte())
                            keepAlive.flip()
                            tunnel.write(keepAlive)
                            lastKeepAlive = now
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning) Log.e(TAG, "Ingress error, attempting to stay alive", e)
                }
            }
            ingressThread.start()

            // Handle Egress in the main worker thread (Device -> Server)
            val packet = ByteBuffer.allocate(MAX_PACKET_SIZE)
            try {
                while (isRunning) {
                    packet.clear()
                    val length = inputStream.read(packet.array())
                    if (length > 0) {
                        packet.limit(length)
                        tunnel.write(packet)
                    } else if (length == -1) {
                        break // End of stream
                    }
                }
            } catch (e: Exception) {
                if (isRunning) Log.e(TAG, "Egress error", e)
            }

        } catch (e: Exception) {
            if (isRunning) Log.e(TAG, "Tunnel worker error", e)
        } finally {
            stop()
            try {
                tunnel?.close()
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun stop() {
        isRunning = false
        try {
            tunnelInterface.close()
        } catch (e: Exception) { /* ignore */ }
    }
}
