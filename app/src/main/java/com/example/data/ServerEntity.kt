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
  val privateKey: String = "", // Client private key
  val address: String = "10.0.0.2/32", // Client address
  val endpoint: String,
  val port: Int,
  val protocol: String, // WireGuard
  val status: String,
  val latency: Int,
  val load: Int,
  val dns: String = "1.1.1.1",
  val mtu: Int = 1280,
  val isFavorite: Boolean = false,
  val isLastUsed: Boolean = false
)
