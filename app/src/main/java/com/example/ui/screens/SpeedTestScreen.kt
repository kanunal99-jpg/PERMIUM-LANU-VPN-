package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.example.R

@Composable
fun SpeedTestScreen(onBack: () -> Unit) {
  var isTesting by remember { mutableStateOf(false) }
  var downloadSpeed by remember { mutableStateOf(0.0) }
  var uploadSpeed by remember { mutableStateOf(0.0) }
  var pingMs by remember { mutableStateOf(0) }
  val scope = rememberCoroutineScope()

  fun startTest() {
    if (isTesting) return
    isTesting = true
    downloadSpeed = 0.0
    uploadSpeed = 0.0
    pingMs = 0

    scope.launch {
      pingMs = 24
      delay(400)
      for (i in 1..20) {
        downloadSpeed = i * 4.8
        delay(60)
      }
      for (i in 1..15) {
        uploadSpeed = i * 2.2
        delay(60)
      }
      isTesting = false
    }
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Top bar
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
          text = stringResource(R.string.speed_test_title),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Gauge Card
    Card(
      modifier = Modifier.fillMaxWidth().height(220.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = if (isTesting) stringResource(R.string.measuring_bandwidth) else "${String.format("%.1f", downloadSpeed)} Mbps",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = stringResource(R.string.download_speed_label),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Details Row
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Text(stringResource(R.string.upload_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${String.format("%.1f", uploadSpeed)} Mbps",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
      Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Text(stringResource(R.string.ping_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "$pingMs ms",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Button(
      onClick = { startTest() },
      enabled = !isTesting,
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
      Text(
        text = if (isTesting) stringResource(R.string.testing_label) else stringResource(R.string.start_speed_test),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.background
      )
    }
  }
}
