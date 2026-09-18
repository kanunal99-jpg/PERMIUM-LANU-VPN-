package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
  @PrimaryKey val id: String,
  val country: String,
  val city: String,
  val hostname: String,
  val publicKey: String,
  val endpoint: String,
  val port: Int,
  val protocol: String, // WireGuard, OpenVPN
  val status: String, // ONLINE, DEGRADED, OFFLINE
  val latency: Int,
  val load: Int,
  val isFavorite: Boolean = false,
  val isLastUsed: Boolean = false
)
