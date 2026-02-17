// screens/DownloadRoutesScreen.kt
package com.example.copilotovirtual.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.copilotovirtual.data.datasources.SavedRouteEntity
import com.example.copilotovirtual.data.models.City
import com.example.copilotovirtual.data.repositories.OfflineRouteRepository
import com.example.copilotovirtual.utils.MapUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadRoutesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { OfflineRouteRepository(context) }
    val scope = rememberCoroutineScope()

    val savedRoutes by repository.allSavedRoutes.collectAsState(initial = emptyList())

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf("") }
    var selectedOrigin by remember { mutableStateOf<City?>(null) }
    var selectedDest by remember { mutableStateOf<City?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rutas Offline")
                        Text(
                            "${savedRoutes.size} rutas guardadas",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Info
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Info, null)
                        Text(
                            "Descarga las rutas con WiFi antes de salir. " +
                                    "Funcionarán sin internet en carretera.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Selector de ciudades
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Descargar nueva ruta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CityDropdown(
                            label = "Ciudad de Origen",
                            selectedCity = selectedOrigin,
                            excludeCity = selectedDest,
                            onCitySelected = { selectedOrigin = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CityDropdown(
                            label = "Ciudad de Destino",
                            selectedCity = selectedDest,
                            excludeCity = selectedOrigin,
                            onCitySelected = { selectedDest = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mensajes
                        errorMessage?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        successMessage?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (isDownloading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(downloadProgress, style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val origin = selectedOrigin
                                    val dest = selectedDest

                                    if (origin == null || dest == null) {
                                        errorMessage = "Selecciona origen y destino"
                                        return@Button
                                    }

                                    scope.launch {
                                        isDownloading = true
                                        errorMessage = null
                                        successMessage = null

                                        repository.downloadAndSaveRoute(
                                            originCity = origin,
                                            destCity = dest,
                                            onProgress = { msg ->
                                                downloadProgress = msg
                                            }
                                        ).onSuccess { routes ->
                                            successMessage = "✓ ${routes.size} rutas guardadas"
                                            selectedOrigin = null
                                            selectedDest = null
                                        }.onFailure { error ->
                                            errorMessage = "Error: ${error.message}"
                                        }

                                        isDownloading = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedOrigin != null && selectedDest != null
                            ) {
                                Icon(Icons.Default.Download, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Descargar Ruta")
                            }
                        }
                    }
                }
            }

            // Rutas guardadas
            if (savedRoutes.isNotEmpty()) {
                item {
                    Text(
                        "Rutas guardadas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(savedRoutes) { route ->
                    SavedRouteCard(
                        route = route,
                        onDelete = {
                            scope.launch {
                                repository.deleteRoute(route)
                            }
                        }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CloudOff,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No hay rutas guardadas",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedRouteCard(
    route: SavedRouteEntity,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.OfflineBolt,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${route.startCityName} → ${route.endCityName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    route.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        MapUtils.formatDistance(route.distance),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall)
                    Text(
                        MapUtils.formatDuration(route.estimatedTime / 3600.0),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "${route.polylinePoints.size} puntos GPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar ruta") },
            text = { Text("¿Eliminar ${route.startCityName} → ${route.endCityName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}