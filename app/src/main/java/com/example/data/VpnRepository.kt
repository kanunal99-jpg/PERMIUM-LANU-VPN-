package com.example.data

import android.content.Context
import com.example.util.ServerHealthChecker
import kotlinx.coroutines.flow.Flow

class VpnRepository(private val dao: LanuDao) {
  val allServers: Flow<List<ServerEntity>> = dao.getAllServers()
  val favoriteServers: Flow<List<ServerEntity>> = dao.getFavoriteServers()
  val settings: Flow<SettingsEntity?> = dao.getSettings()
  val history: Flow<List<ConnectionHistory>> = dao.getHistory()
  val excludedApps: Flow<List<ExcludedAppEntity>> = dao.getExcludedApps()

  suspend fun initializeDefaultServers() {
    val defaults = listOf(
      ServerEntity(
        id = "de_frankfurt_1",
        country = "Germany",
        city = "Frankfurt",
        hostname = "de1.lanuvpn.net",
        publicKey = "PASTE_YOUR_SERVER_PUBLIC_KEY_HERE",
        privateKey = "PASTE_YOUR_CLIENT_PRIVATE_KEY_HERE",
        address = "10.0.0.2/32",
        endpoint = "1.2.3.4",
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

  suspend fun getLastUsedServerSync(): ServerEntity? {
    return dao.getLastUsedServer()
  }

  suspend fun updateServer(server: ServerEntity) {
    dao.updateServer(server)
  }

  suspend fun setLastUsedServer(serverId: String) {
    dao.clearLastUsed()
    dao.setLastUsed(serverId)
  }

  suspend fun insertHistory(history: ConnectionHistory) {
    dao.insertHistory(history)
  }

  suspend fun saveSettings(settings: SettingsEntity) {
    dao.saveSettings(settings)
  }

  suspend fun getSettingsSync(): SettingsEntity? {
    return dao.getSettingsOneShot()
  }

  suspend fun toggleFavorite(serverId: String) {
    dao.toggleFavorite(serverId)
  }

  suspend fun insertExcludedApp(app: ExcludedAppEntity) {
    dao.insertExcludedApp(app)
  }

  suspend fun removeExcludedApp(packageName: String) {
    dao.removeExcludedApp(packageName)
  }

  suspend fun getExcludedAppsSync(): List<ExcludedAppEntity> {
    return dao.getExcludedAppsList()
  }

  suspend fun refreshServerHealth(context: Context) {
    ServerHealthChecker.checkAllServers(context)
  }

  companion object {
    @Volatile
    private var INSTANCE: VpnRepository? = null

    fun getInstance(context: Context): VpnRepository {
      return INSTANCE ?: synchronized(this) {
        val database = LanuDatabase.getDatabase(context)
        val instance = VpnRepository(database.lanuDao())
        INSTANCE = instance
        instance
      }
    }
  }
}
