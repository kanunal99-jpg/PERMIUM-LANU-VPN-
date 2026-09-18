package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a historical VPN connection record.
 * Stores minimal metadata required for session tracking and user stats.
 */
@Entity(tableName = "connection_history")
data class ConnectionHistory(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val serverId: String,
  val serverName: String,
  val connectedAt: Long,
  val durationSeconds: Long,
  val bytesIn: Long,
  val bytesOut: Long
)
