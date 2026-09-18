package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ServerEntity
import com.example.data.ServerRepository

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  object Home : Screen("home", "Home", Icons.Default.VpnKey)
  object Servers : Screen("servers", "Servers", Icons.Default.Public)
  object SpeedTest : Screen("speed_test", "Speed", Icons.Default.Speed)
  object Settings : Screen("settings", "Settings", Icons.Default.Settings)
  object Privacy : Screen("privacy", "Privacy", Icons.Default.Security)
  object SplitTunneling : Screen("split_tunneling", "Split Tunneling", Icons.Default.AltRoute)
}

@Composable
fun LanuApp(
  repository: ServerRepository,
  servers: List<ServerEntity>,
  selectedServer: ServerEntity?,
  onServerSelected: (ServerEntity) -> Unit,
  onToggleFavorite: (ServerEntity) -> Unit,
  onRefreshLatency: () -> Unit
) {
  val navController = rememberNavController()
  val items = listOf(Screen.Home, Screen.Servers, Screen.SpeedTest, Screen.Settings)

  Scaffold(
    bottomBar = {
      val navBackStackEntry by navController.currentBackStackEntryAsState()
      val currentRoute = navBackStackEntry?.destination?.route
      if (currentRoute in items.map { it.route }) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp
        ) {
          items.forEach { screen ->
            NavigationBarItem(
              icon = { Icon(screen.icon, contentDescription = screen.title) },
              label = { Text(screen.title) },
              selected = currentRoute == screen.route,
              onClick = {
                if (currentRoute != screen.route) {
                  navController.navigate(screen.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                }
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.outline
              )
            )
          }
        }
      }
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = Screen.Home.route,
      modifier = Modifier.padding(innerPadding)
    ) {
      composable(Screen.Home.route) {
        MainScreen(
          selectedServer = selectedServer,
          onNavigateToServers = { navController.navigate(Screen.Servers.route) },
          onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
        )
      }
      composable(Screen.Servers.route) {
        ServerListScreen(
          servers = servers,
          selectedServer = selectedServer,
          onServerSelected = onServerSelected,
          onToggleFavorite = onToggleFavorite,
          onRefreshLatency = onRefreshLatency,
          onBack = { navController.popBackStack() }
        )
      }
      composable(Screen.SpeedTest.route) {
        SpeedTestScreen(onBack = { navController.popBackStack() })
      }
      composable(Screen.Settings.route) {
        SettingsScreen(
          onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
          onNavigateToSplitTunneling = { navController.navigate(Screen.SplitTunneling.route) },
          onBack = { navController.popBackStack() }
        )
      }
      composable(Screen.Privacy.route) {
        PrivacyScreen(onBack = { navController.popBackStack() })
      }
      composable(Screen.SplitTunneling.route) {
        SplitTunnelingScreen(
          onNavigateBack = { navController.popBackStack() },
          repository = repository
        )
      }
    }
  }
}
