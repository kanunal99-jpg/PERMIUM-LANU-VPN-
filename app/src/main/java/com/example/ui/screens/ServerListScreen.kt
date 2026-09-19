package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServerEntity

import androidx.compose.ui.res.stringResource
import com.example.R

@Composable
fun ServerListScreen(
  servers: List<ServerEntity>,
  selectedServer: ServerEntity?,
  onServerSelected: (ServerEntity) -> Unit,
  onToggleFavorite: (ServerEntity) -> Unit,
  onRefreshLatency: () -> Unit,
  onBack: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var filterFavorite by remember { mutableStateOf(false) }
  var isPinging by remember { mutableStateOf(false) }

  val filteredServers = servers.filter {
    (it.country.contains(searchQuery, ignoreCase = true) || it.city.contains(searchQuery, ignoreCase = true)) &&
        (!filterFavorite || it.isFavorite)
  }

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
          text = stringResource(R.string.servers_title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
          isPinging = true
          onRefreshLatency()
        }) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.refresh_ping),
            tint = MaterialTheme.colorScheme.primary
          )
        }
        IconButton(onClick = { filterFavorite = !filterFavorite }) {
          Icon(
            imageVector = if (filterFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Favorites",
            tint = if (filterFavorite) Color.Red else MaterialTheme.colorScheme.onBackground
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Search bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text(stringResource(R.string.search_placeholder)) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      shape = RoundedCornerShape(12.dp),
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
      )
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Fastest Server Button
    Button(
      onClick = {
        val fastest = servers.minByOrNull { it.latency }
        if (fastest != null) {
          onServerSelected(fastest)
          onBack()
        }
      },
      modifier = Modifier.fillMaxWidth().height(50.dp),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
      Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Spacer(modifier = Modifier.width(8.dp))
      Text(stringResource(R.string.auto_connect_fastest), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Server list
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      items(filteredServers) { server ->
        val isSelected = selectedServer?.id == server.id
        Card(
          modifier =
            Modifier.fillMaxWidth().clickable {
              onServerSelected(server)
              onBack()
            },
          shape = RoundedCornerShape(16.dp),
          colors =
            CardDefaults.cardColors(
              containerColor =
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface
            ),
          border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
        ) {
          Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier =
                  Modifier.size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = when (server.country) {
                    "Germany" -> "🇩🇪"
                    "Netherlands" -> "🇳🇱"
                    "Turkey" -> "🇹🇷"
                    "United States" -> "🇺🇸"
                    "United Kingdom" -> "🇬🇧"
                    else -> "🌐"
                  },
                  fontSize = 22.sp
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = server.country,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${server.city} • ${server.protocol}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.outline
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Column(horizontalAlignment = Alignment.End) {
                val statusColor = when (server.status.uppercase()) {
                  "ONLINE" -> Color(0xFF00C853)
                  "DEGRADED" -> Color(0xFFFFAB00)
                  else -> Color(0xFFD32F2F)
                }

                val statusText = when (server.status.uppercase()) {
                  "ONLINE" -> stringResource(R.string.status_online)
                  "DEGRADED" -> stringResource(R.string.status_degraded)
                  else -> stringResource(R.string.status_offline)
                }

                Text(
                  text = "${server.latency} ms",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = statusColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = statusColor.copy(alpha = 0.15f)
                ) {
                  Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              IconButton(onClick = { onToggleFavorite(server) }) {
                Icon(
                  imageVector = if (server.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                  contentDescription = "Favorite",
                  tint = if (server.isFavorite) Color.Red else MaterialTheme.colorScheme.outline
                )
              }
            }
          }
        }
      }
    }
  }
}
