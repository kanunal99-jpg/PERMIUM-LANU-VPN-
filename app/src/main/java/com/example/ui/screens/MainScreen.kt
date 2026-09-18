package com.example.ui.screens

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServerEntity
import com.example.manager.ConnectionStats
import com.example.manager.VpnConnectionManager
import com.example.manager.VpnStateManager
import androidx.compose.runtime.collectAsState
import com.example.manager.VpnStateManager.VpnStatus

@Composable
fun MainScreen(
  selectedServer: ServerEntity?,
  onNavigateToServers: () -> Unit,
  onNavigateToSettings: () -> Unit
) {
  val context = LocalContext.current
  val vpnStatus by VpnStateManager.status.collectAsState()
  val stats by VpnConnectionManager.stats.collectAsState()

  val vpnPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == android.app.Activity.RESULT_OK && selectedServer != null) {
        VpnConnectionManager.connect(context, selectedServer)
      }
    }

  val pulseAnim = animateFloatAsState(
    targetValue = if (vpnStatus == VpnStatus.CONNECTED) 1.05f else 1.0f,
    animationSpec = infiniteRepeatable(tween(1000), androidx.compose.animation.core.RepeatMode.Reverse)
  )

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Top Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.Security,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "LANU VPN",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
      IconButton(onClick = onNavigateToSettings) {
        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Current Server Card
    Card(
      modifier = Modifier.fillMaxWidth().clickable { onNavigateToServers() },
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier.size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = selectedServer?.let { "${it.country} (${it.city})" } ?: "Germany (Frankfurt)",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Latency: ${selectedServer?.latency ?: 24} ms • ${selectedServer?.protocol ?: "WireGuard"}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
      }
    }

    Spacer(modifier = Modifier.height(36.dp))

    // Main VPN Status Ring & Connect Button
    Box(
      modifier =
        Modifier.size(220.dp)
          .scale(if (vpnStatus == VpnStatus.CONNECTED) pulseAnim.value else 1.0f)
          .background(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    if (vpnStatus == VpnStatus.CONNECTED) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surface
                  )
              ),
            shape = CircleShape
          )
          .border(
            width = 4.dp,
            color =
              if (vpnStatus == VpnStatus.CONNECTED) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape
          )
          .clickable {
            if (vpnStatus == VpnStatus.CONNECTED) {
              VpnConnectionManager.disconnect(context)
            } else {
              val intent = VpnService.prepare(context)
              if (intent != null) {
                vpnPermissionLauncher.launch(intent)
              } else {
                selectedServer?.let { VpnConnectionManager.connect(context, it) }
              }
            }
          },
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
          imageVector = if (vpnStatus == VpnStatus.CONNECTED) Icons.Default.Lock else Icons.Default.LockOpen,
          contentDescription = null,
          tint = if (vpnStatus == VpnStatus.CONNECTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text =
            when (vpnStatus) {
              VpnStatus.CONNECTED -> "CONNECTED"
              VpnStatus.CONNECTING -> "CONNECTING..."
              VpnStatus.ERROR -> "ERROR"
              else -> "TAP TO CONNECT"
            },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        if (vpnStatus == VpnStatus.CONNECTED) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = formatDuration(stats.durationSeconds),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(40.dp))

    // Info Stats Grid
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      StatCard(
        modifier = Modifier.weight(1f),
        title = "PUBLIC IP",
        value = stats.currentIp,
        icon = Icons.Default.NetworkCheck
      )
      StatCard(
        modifier = Modifier.weight(1f),
        title = "ZERO-LOGS",
        value = "Protected",
        icon = Icons.Default.VerifiedUser
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    // Connect / Disconnect Action Button
    Button(
      onClick = {
        if (vpnStatus == VpnStatus.CONNECTED) {
          VpnConnectionManager.disconnect(context)
        } else {
          val intent = VpnService.prepare(context)
          if (intent != null) {
            vpnPermissionLauncher.launch(intent)
          } else {
            selectedServer?.let { VpnConnectionManager.connect(context, it) }
          }
        }
      },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(16.dp),
      colors =
        ButtonDefaults.buttonColors(
          containerColor =
            if (vpnStatus == VpnStatus.CONNECTED) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )
    ) {
      Text(
        text = if (vpnStatus == VpnStatus.CONNECTED) "DISCONNECT" else "CONNECT NOW",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (vpnStatus == VpnStatus.CONNECTED) Color.White else MaterialTheme.colorScheme.background
      )
    }
  }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

private fun formatDuration(seconds: Long): String {
  val hours = seconds / 3600
  val minutes = (seconds % 3600) / 60
  val secs = seconds % 60
  return String.format("%02d:%02d:%02d", hours, minutes, secs)
}
