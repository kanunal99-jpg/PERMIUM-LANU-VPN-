package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
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

import androidx.compose.ui.res.stringResource
import com.example.R

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
          text = stringResource(R.string.settings_title),
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
          title = stringResource(R.string.auto_connect_title),
          subtitle = stringResource(R.string.auto_connect_subtitle),
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
          title = stringResource(R.string.kill_switch_title),
          subtitle = stringResource(R.string.kill_switch_subtitle),
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
          title = stringResource(R.string.auto_reconnect_title),
          subtitle = stringResource(R.string.auto_reconnect_subtitle),
          checked = autoReconnect,
          onCheckedChange = { autoReconnect = it },
          icon = Icons.Default.Sync
        )
      }
      item {
        SettingToggleItem(
          title = stringResource(R.string.dns_protection_title),
          subtitle = stringResource(R.string.dns_protection_subtitle),
          checked = dnsProtection,
          onCheckedChange = { dnsProtection = it },
          icon = Icons.Default.Public
        )
      }
      item {
        SettingToggleItem(
          title = stringResource(R.string.ipv6_protection_title),
          subtitle = stringResource(R.string.ipv6_protection_subtitle),
          checked = ipv6Protection,
          onCheckedChange = { ipv6Protection = it },
          icon = Icons.Default.Lock
        )
      }
      item {
        SettingToggleItem(
          title = stringResource(R.string.dark_mode_title),
          subtitle = stringResource(R.string.dark_mode_subtitle),
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
              Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(text = stringResource(R.string.nav_split_tunneling), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = stringResource(R.string.split_tunneling_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                Text(text = stringResource(R.string.privacy_policy_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = stringResource(R.string.privacy_policy_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
