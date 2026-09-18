package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LanuDao {
  @Query("SELECT * FROM servers")
  fun getAllServers(): Flow<List<ServerEntity>>

  @Query("SELECT * FROM servers")
  suspend fun getAllServersList(): List<ServerEntity>

  @Query("SELECT * FROM servers WHERE isFavorite = 1")
  fun getFavoriteServers(): Flow<List<ServerEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertServers(servers: List<ServerEntity>)

  @Update
  suspend fun updateServer(server: ServerEntity)

  @Query("UPDATE servers SET isLastUsed = 0")
  suspend fun clearLastUsed()

  @Query("UPDATE servers SET isLastUsed = 1 WHERE id = :serverId")
  suspend fun setLastUsed(serverId: String)

  @Query("SELECT * FROM servers WHERE isLastUsed = 1 LIMIT 1")
  suspend fun getLastUsedServer(): ServerEntity?

  @Query("SELECT * FROM settings WHERE id = 1")
  fun getSettings(): Flow<SettingsEntity?>

  @Query("SELECT * FROM settings WHERE id = 1")
  suspend fun getSettingsOneShot(): SettingsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveSettings(settings: SettingsEntity)

  @Query("UPDATE servers SET isFavorite = NOT isFavorite WHERE id = :serverId")
  suspend fun toggleFavorite(serverId: String)

  @Query("SELECT * FROM connection_history ORDER BY connectedAt DESC LIMIT 20")
  fun getHistory(): Flow<List<ConnectionHistory>>

  @Insert
  suspend fun insertHistory(history: ConnectionHistory)

  @Query("SELECT * FROM favorite_servers")
  fun getFavorites(): Flow<List<FavoriteServer>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addFavorite(favorite: FavoriteServer)

  @Query("DELETE FROM favorite_servers WHERE serverId = :serverId")
  suspend fun removeFavorite(serverId: String)

  @Query("SELECT * FROM excluded_apps")
  fun getExcludedApps(): Flow<List<ExcludedAppEntity>>

  @Query("SELECT * FROM excluded_apps")
  suspend fun getExcludedAppsList(): List<ExcludedAppEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExcludedApp(app: ExcludedAppEntity)

  @Query("DELETE FROM excluded_apps WHERE packageName = :packageName")
  suspend fun removeExcludedApp(packageName: String)
}
