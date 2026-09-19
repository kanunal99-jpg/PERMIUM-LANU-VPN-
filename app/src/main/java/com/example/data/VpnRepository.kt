package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class VpnRepository(private val dao: LanuDao) {
  val allServers: Flow<List<ServerEntity>> = dao.getAllServers()
  val favoriteServers: Flow<List<ServerEntity>> = dao.getFavoriteServers()
  val settings: Flow<SettingsEntity?> = dao.getSettings()
  val history: Flow<List<ConnectionHistory>> = dao.getHistory()
  val excludedApps: Flow<List<ExcludedAppEntity>> = dao.getExcludedApps()

  /**
   * No fake/default WireGuard credentials are shipped.
   * A server is usable only after real endpoint, peer public key and client
   * private key are provisioned. This prevents a successful-looking fake VPN.
   */
  suspend fun initializeDefaultServers() {
    val defaults = listOf(
      ServerEntity(
        id = "lanu_primary",
        country = "Germany",
        city = "Frankfurt",
        hostname = "de1.lanuvpn.net",
        publicKey = "",
        privateKey = "",
        address = "10.8.0.2/32",
        endpoint = "",
        port = 51820,
        protocol = "WireGuard",
        status = "CONFIG_REQUIRED",
        latency = -1,
        load = 0,
        isFavorite = true,
        isLastUsed = true
      )
    )
    dao.insertServers(defaults)
    dao.saveSettings(SettingsEntity())
  }

  suspend fun getLastUsedServerSync(): ServerEntity? = dao.getLastUsedServer()

  suspend fun updateServer(server: ServerEntity) = dao.updateServer(server)
  suspend fun setLastUsedServer(serverId: String) {
    dao.clearLastUsed()
    dao.setLastUsed(serverId)
  }
  suspend fun insertHistory(history: ConnectionHistory) = dao.insertHistory(history)
  suspend fun saveSettings(settings: SettingsEntity) = dao.saveSettings(settings)
  suspend fun getSettingsSync(): SettingsEntity? = dao.getSettingsOneShot()
  suspend fun toggleFavorite(serverId: String) = dao.toggleFavorite(serverId)
  suspend fun insertExcludedApp(app: ExcludedAppEntity) = dao.insertExcludedApp(app)
  suspend fun removeExcludedApp(packageName: String) = dao.removeExcludedApp(packageName)
  suspend fun getExcludedAppsSync(): List<ExcludedAppEntity> = dao.getExcludedAppsList()

  companion object {
    @Volatile private var INSTANCE: VpnRepository? = null
    fun getInstance(context: Context): VpnRepository {
      return INSTANCE ?: synchronized(this) {
        val database = LanuDatabase.getDatabase(context)
        VpnRepository(database.lanuDao()).also { INSTANCE = it }
      }
    }
  }
}
