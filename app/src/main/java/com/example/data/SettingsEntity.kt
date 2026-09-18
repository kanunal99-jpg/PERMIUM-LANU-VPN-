package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
  @PrimaryKey val id: Int = 1,
  val vpnProtocol: String = "WireGuard",
  val killSwitchEnabled: Boolean = true,
  val autoConnect: Boolean = false,
  val autoReconnect: Boolean = true,
  val dnsProtection: Boolean = true,
  val ipv6Protection: Boolean = true,
  val darkMode: Boolean = true
)
