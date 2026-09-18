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

    override fun run() {
        Log.i(TAG, "Starting tunnel worker to $serverAddress:$serverPort")
        
        var tunnel: DatagramChannel? = null
        try {
            // 1. Initialize the UDP socket and connect to the server
            tunnel = DatagramChannel.open()
            
            // Protect the socket from being routed back into the VPN tunnel itself
            if (!vpnService.protect(tunnel.socket())) {
                throw IllegalStateException("Cannot protect tunnel socket")
            }

            tunnel.connect(InetSocketAddress(serverAddress, serverPort))
            tunnel.configureBlocking(true)

            // 2. Set up streams for the TUN interface
            val inputStream = FileInputStream(tunnelInterface.fileDescriptor)
            val outputStream = FileOutputStream(tunnelInterface.fileDescriptor)

            val packet = ByteBuffer.allocate(MAX_PACKET_SIZE)

            // 3. Packet Loop
            while (true) {
                // Read from TUN interface (Packets outgoing from the device)
                packet.clear()
                val length = inputStream.read(packet.array())
                if (length > 0) {
                    packet.limit(length)
                    // Write to UDP socket (Forward to VPN server)
                    tunnel.write(packet)
                }

                // Read from UDP socket (Packets incoming from the VPN server)
                packet.clear()
                val readFromServer = tunnel.read(packet)
                if (readFromServer > 0) {
                    // Write back to TUN interface (Deliver to the device)
                    outputStream.write(packet.array(), 0, readFromServer)
                }
                
                // Sleep slightly if no data to prevent CPU pinning if non-blocking
                // Though we are in blocking mode here.
                if (length <= 0 && readFromServer <= 0) {
                    Thread.sleep(10)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tunnel worker error", e)
        } finally {
            try {
                tunnel?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
