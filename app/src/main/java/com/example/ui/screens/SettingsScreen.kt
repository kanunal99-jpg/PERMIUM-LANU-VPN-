package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.manager.AutoConnectManager
import com.example.manager.KillSwitchManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  onNavigateToPrivacy: () -> Unit,
  onNavigateToSplitTunneling: () -> Unit,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  
  val autoConnectFlow = remember { AutoConnectManager.isAutoConnectEnabled(context) }
  val autoConnect by autoConnectFlow.collectAsState(initial = false)

  val killSwitchFlow = remember { KillSwitchManager.isKillSwitchEnabled(context) }
  val killSwitch by killSwitchFlow.collectAsState(initial = true)

  var autoReconnect by remember { mutableStateOf(true) }
  var dnsProtection by remember { mutableStateOf(true) }
  var ipv6Protection by remember { mutableStateOf(true) }
  var darkMode by remember { mutableStateOf(true) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(20.dp)
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Settings & Security",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      item {
        SettingToggleItem(
          title = "Auto-Connect on Launch",
          subtitle = "Automatically establish secure tunnel on app startup",
          checked = autoConnect,
          onCheckedChange = { enabled ->
            scope.launch {
              AutoConnectManager.setAutoConnectEnabled(context, enabled)
            }
          },
          icon = Icons.Default.FlashOn
        )
      }
      item {
        SettingToggleItem(
          title = "Kill Switch",
          subtitle = "Block internet when VPN disconnects",
          checked = killSwitch,
          onCheckedChange = { enabled ->
            scope.launch {
              KillSwitchManager.setKillSwitchEnabled(context, enabled)
            }
          },
          icon = Icons.Default.Shield
        )
      }
      item {
        SettingToggleItem(
          title = "Auto Reconnect",
          subtitle = "Automatically reconnect on network changes",
          checked = autoReconnect,
          onCheckedChange = { autoReconnect = it },
          icon = Icons.Default.Sync
        )
      }
      item {
        SettingToggleItem(
          title = "DNS Leak Protection",
          subtitle = "Route all DNS queries through secure resolvers",
          checked = dnsProtection,
          onCheckedChange = { dnsProtection = it },
          icon = Icons.Default.Public
        )
      }
      item {
        SettingToggleItem(
          title = "IPv6 Leak Protection",
          subtitle = "Prevent IPv6 traffic leakage outside tunnel",
          checked = ipv6Protection,
          onCheckedChange = { ipv6Protection = it },
          icon = Icons.Default.Lock
        )
      }
      item {
        SettingToggleItem(
          title = "Dark Mode",
          subtitle = "Enable luxury dark cyber-secure theme",
          checked = darkMode,
          onCheckedChange = { darkMode = it },
          icon = Icons.Default.DarkMode
        )
      }
      item {
        Card(
          modifier = Modifier.fillMaxWidth().clickable { onNavigateToSplitTunneling() },
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(text = "Split Tunneling", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Exclude apps from the VPN tunnel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
              }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
          }
        }
      }
      item {
        Card(
          modifier = Modifier.fillMaxWidth().clickable { onNavigateToPrivacy() },
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(text = "Zero-Log Privacy Policy", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Read our strict non-logging commitment", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
              }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
          }
        }
      }
    }
  }
}

@Composable
fun SettingToggleItem(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  icon: androidx.compose.ui.graphics.vector.ImageVector
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier.padding(16.dp).fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
          Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
          Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
      }
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
      )
    }
  }
}
