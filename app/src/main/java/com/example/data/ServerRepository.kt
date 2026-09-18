package com.example.data

import kotlinx.coroutines.flow.Flow

class ServerRepository(private val dao: LanuDao) {
  val allServers: Flow<List<ServerEntity>> = dao.getAllServers()
  val favoriteServers: Flow<List<ServerEntity>> = dao.getFavoriteServers()
  val settings: Flow<SettingsEntity?> = dao.getSettings()
  val history: Flow<List<ConnectionHistory>> = dao.getHistory()
  val excludedApps: Flow<List<ExcludedAppEntity>> = dao.getExcludedApps()

  suspend fun initializeDefaultServers() {
    val defaults =
      listOf(
        ServerEntity(
          id = "de_frankfurt_1",
          country = "Germany",
          city = "Frankfurt",
          hostname = "de1.lanuvpn.net",
          publicKey = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v=",
          endpoint = "185.220.101.5",
          port = 51820,
          protocol = "WireGuard",
          status = "ONLINE",
          latency = 24,
          load = 32,
          isFavorite = true,
          isLastUsed = true
        ),
        ServerEntity(
          id = "nl_amsterdam_1",
          country = "Netherlands",
          city = "Amsterdam",
          hostname = "nl1.lanuvpn.net",
          publicKey = "b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2a=",
          endpoint = "194.154.20.18",
          port = 51820,
          protocol = "WireGuard",
          status = "ONLINE",
          latency = 31,
          load = 45,
          isFavorite = false,
          isLastUsed = false
        ),
        ServerEntity(
          id = "tr_istanbul_1",
          country = "Turkey",
          city = "Istanbul",
          hostname = "tr1.lanuvpn.net",
          publicKey = "c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2a3b=",
          endpoint = "185.130.44.12",
          port = 51820,
          protocol = "WireGuard",
          status = "ONLINE",
          latency = 48,
          load = 28,
          isFavorite = true,
          isLastUsed = false
        ),
        ServerEntity(
          id = "us_ny_1",
          country = "United States",
          city = "New York",
          hostname = "us1.lanuvpn.net",
          publicKey = "d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2a3b4c=",
          endpoint = "45.33.32.156",
          port = 51820,
          protocol = "WireGuard",
          status = "ONLINE",
          latency = 95,
          load = 60,
          isFavorite = false,
          isLastUsed = false
        ),
        ServerEntity(
          id = "uk_london_1",
          country = "United Kingdom",
          city = "London",
          hostname = "uk1.lanuvpn.net",
          publicKey = "e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2a3b4c5d=",
          endpoint = "178.62.204.99",
          port = 51820,
          protocol = "OpenVPN",
          status = "ONLINE",
          latency = 38,
          load = 40,
          isFavorite = false,
          isLastUsed = false
        )
      )
    dao.insertServers(defaults)
    dao.saveSettings(SettingsEntity())
  }

  suspend fun updateServer(server: ServerEntity) {
    dao.updateServer(server)
  }

  suspend fun setLastUsedServer(serverId: String) {
    dao.clearLastUsed()
    dao.setLastUsed(serverId)
  }

  suspend fun saveSettings(settings: SettingsEntity) {
    dao.saveSettings(settings)
  }

  suspend fun logConnection(history: ConnectionHistory) {
    dao.insertHistory(history)
  }

  suspend fun insertExcludedApp(app: ExcludedAppEntity) {
    dao.insertExcludedApp(app)
  }

  suspend fun removeExcludedApp(packageName: String) {
    dao.removeExcludedApp(packageName)
  }
}
