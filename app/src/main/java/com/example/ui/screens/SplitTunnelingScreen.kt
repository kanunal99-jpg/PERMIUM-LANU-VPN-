package com.example.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.ExcludedAppEntity
import com.example.data.VpnRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.ui.res.stringResource
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelingScreen(
    onNavigateBack: () -> Unit,
    repository: VpnRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val packageManager = context.packageManager
    
    val excludedApps by repository.excludedApps.collectAsState(initial = emptyList())
    var installedApps by remember { mutableStateOf<List<ApplicationInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName == context.packageName }
                .sortedBy { it.loadLabel(packageManager).toString() }
            installedApps = apps
            isLoading = false
        }
    }

    val filteredApps = installedApps.filter {
        it.loadLabel(packageManager).toString().contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_split_tunneling)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_apps_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(filteredApps) { appInfo ->
                        val isExcluded = excludedApps.any { it.packageName == appInfo.packageName }
                        AppItem(
                            appInfo = appInfo,
                            packageManager = packageManager,
                            isExcluded = isExcluded,
                            onToggle = { checked ->
                                scope.launch {
                                    if (checked) {
                                        repository.insertExcludedApp(
                                            ExcludedAppEntity(
                                                appInfo.packageName,
                                                appInfo.loadLabel(packageManager).toString()
                                            )
                                        )
                                    } else {
                                        repository.removeExcludedApp(appInfo.packageName)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(
    appInfo: ApplicationInfo,
    packageManager: PackageManager,
    isExcluded: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val appName = appInfo.loadLabel(packageManager).toString()
    val appIcon = remember(appInfo.packageName) {
        appInfo.loadIcon(packageManager).toBitmap().asImageBitmap()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = appIcon,
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(text = appName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = appInfo.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(
            checked = isExcluded,
            onCheckedChange = onToggle
        )
    }
}
