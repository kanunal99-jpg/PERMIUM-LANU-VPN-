package com.example.util

import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.config.InetNetwork
import com.wireguard.crypto.Key
import java.net.InetAddress

object WireGuardConfigBuilder {
    fun build(
        clientPrivateKey: String,
        clientAddress: String,
        serverPublicKey: String,
        serverEndpoint: String,
        serverPort: Int,
        dns: String = "1.1.1.1",
        mtu: Int = 1280
    ): Config {
        val interfaceBuilder = Interface.Builder()
            .parsePrivateKey(clientPrivateKey)
            .addAddress(InetNetwork.parse(clientAddress))
            .setMtu(mtu)
            .parseDnsServers(dns)

        val peerBuilder = Peer.Builder()
            .parsePublicKey(serverPublicKey)
            .parseEndpoint("$serverEndpoint:$serverPort")
            .addAllowedIp(InetNetwork.parse("0.0.0.0/0"))
            .addAllowedIp(InetNetwork.parse("::/0"))

        return Config.Builder()
            .setInterface(interfaceBuilder.build())
            .addPeer(peerBuilder.build())
            .build()
    }
}
