package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a user's favorite VPN server.
 * Adheres to minimal data collection by only storing the unique identifier of the server.
 */
@Entity(tableName = "favorite_servers")
data class FavoriteServer(
  @PrimaryKey val serverId: String
)
