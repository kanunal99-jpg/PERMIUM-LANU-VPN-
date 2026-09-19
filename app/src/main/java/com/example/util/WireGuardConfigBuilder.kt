package com.example.util

import com.wireguard.config.Config
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface
import com.wireguard.config.Peer

object WireGuardConfigBuilder {
  fun build(
    clientPrivateKey: String,
    clientAddress: String,
    serverPublicKey: String,
    serverEndpoint: String,
    serverPort: Int,
    dns: String = "1.1.1.1",
    mtu: Int = 1280,
    enableIpv6: Boolean = false
  ): Config {
    require(clientPrivateKey.isNotBlank() && !clientPrivateKey.contains("PASTE_")) { "Real client private key required" }
    require(serverPublicKey.isNotBlank() && !serverPublicKey.contains("PASTE_")) { "Real server public key required" }
    require(serverEndpoint.isNotBlank() && !serverEndpoint.contains("1.2.3.4")) { "Real WireGuard endpoint required" }
    require(serverPort in 1..65535) { "Invalid WireGuard port" }

    val interfaceBuilder = Interface.Builder()
      .parsePrivateKey(clientPrivateKey)
      .addAddress(InetNetwork.parse(clientAddress))
      .setMtu(mtu)
      .parseDnsServers(dns)

    val peerBuilder = Peer.Builder()
      .parsePublicKey(serverPublicKey)
      .parseEndpoint("$serverEndpoint:$serverPort")
      .addAllowedIp(InetNetwork.parse("0.0.0.0/0"))

    if (enableIpv6) peerBuilder.addAllowedIp(InetNetwork.parse("::/0"))

    return Config.Builder()
      .setInterface(interfaceBuilder.build())
      .addPeer(peerBuilder.build())
      .build()
  }
}
