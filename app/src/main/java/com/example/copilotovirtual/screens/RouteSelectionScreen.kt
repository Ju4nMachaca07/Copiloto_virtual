// screens/RouteSelectionScreen.kt
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
import com.example.copilotovirtual.data.models.City
import com.example.copilotovirtual.data.models.Route
import com.example.copilotovirtual.data.network.DirectionsApiService
import com.example.copilotovirtual.data.repositories.OfflineRouteRepository
import com.example.copilotovirtual.utils.MapUtils
import com.example.copilotovirtual.utils.NetworkUtils
import kotlinx.coroutines.launch
import java.util.UUID

// Fuente de donde vienen las rutas
enum class RouteSource {
    GOOGLE_API,     // Internet - rutas reales en tiempo real
    ROOM_SAVED,     // Sin internet - rutas descargadas previamente
    FALLBACK        // Sin internet NI descarga - línea recta de emergencia
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSelectionScreen(
    onRouteSelected: (Route) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val directionsService = remember { DirectionsApiService(context) }
    val offlineRepo = remember { OfflineRouteRepository(context) }

    var selectedOrigin by remember { mutableStateOf<City?>(null) }
    var selectedDest by remember { mutableStateOf<City?>(null) }
    var availableRoutes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var routeSource by remember { mutableStateOf<RouteSource?>(null) }

    LaunchedEffect(selectedOrigin, selectedDest) {
        val origin = selectedOrigin
        val dest = selectedDest

        if (origin == null || dest == null) {
            availableRoutes = emptyList()
            routeSource = null
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        availableRoutes = emptyList()

        scope.launch {
            val hasInternet = NetworkUtils.isConnected(context)

            if (hasInternet) {
                // ── ESCENARIO 1: Con internet → Google Directions API ──
                try {
                    val apiRoutes = directionsService.getDirections(
                        origin = origin.coordinates,
                        destination = dest.coordinates,
                        alternatives = true,
                        originCityId = origin.id,
                        destinationCityId = dest.id
                    )

                    availableRoutes = apiRoutes.mapIndexed { index, r ->
                        Route(
                            id = UUID.randomUUID().toString(),
                            name = if (index == 0) "Ruta Principal" else "Alternativa $index",
                            description = r.summary,
                            startCityId = origin.id,
                            endCityId = dest.id,
                            waypoints = r.polyline,
                            distance = r.distance.toDouble(),
                            estimatedTime = r.duration.toLong()
                        )
                    }
                    routeSource = RouteSource.GOOGLE_API

                } catch (e: Exception) {
                    errorMessage = "Error de conexión: ${e.message}"
                }

            } else {
                // ── ESCENARIO 2: Sin internet → Rutas guardadas en Room ──
                val saved = offlineRepo.getSavedRoutesBetween(origin.id, dest.id)

                if (saved.isNotEmpty()) {
                    availableRoutes = saved.map { entity ->
                        with(offlineRepo) { entity.toRoute() }
                    }
                    routeSource = RouteSource.ROOM_SAVED

                } else {
                    // ── ESCENARIO 3: Sin internet NI descarga → Fallback ──
                    val fallback = Route.getFallbackRoutes(origin.id, dest.id)
                    availableRoutes = fallback
                    routeSource = RouteSource.FALLBACK

                    if (fallback.isEmpty()) {
                        errorMessage = "No hay rutas disponibles.\nDescarga las rutas con WiFi."
                    }
                }
            }

            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Ruta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
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

            // Selectores de ciudad
            item {
                CityDropdown(
                    label = "Origen",
                    selectedCity = selectedOrigin,
                    excludeCity = selectedDest,
                    onCitySelected = { selectedOrigin = it }
                )
            }

            item {
                CityDropdown(
                    label = "Destino",
                    selectedCity = selectedDest,
                    excludeCity = selectedOrigin,
                    onCitySelected = { selectedDest = it }
                )
            }

            // Banner de fuente de rutas
            routeSource?.let { source ->
                item {
                    RouteSourceBanner(source = source)
                }
            }

            // Loading
            if (isLoading) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Text("Buscando rutas...")
                        }
                    }
                }
            }

            // Error
            errorMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                msg,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Lista de rutas
            items(availableRoutes) { route ->
                RouteCard(
                    route = route,
                    isFallback = routeSource == RouteSource.FALLBACK,
                    onClick = { onRouteSelected(route) }
                )
            }
        }
    }
}

@Composable
fun RouteSourceBanner(source: RouteSource) {
    val (icon, text, color) = when (source) {
        RouteSource.GOOGLE_API -> Triple(
            Icons.Default.Wifi,
            "Rutas en tiempo real de Google Maps",
            MaterialTheme.colorScheme.primaryContainer
        )
        RouteSource.ROOM_SAVED -> Triple(
            Icons.Default.OfflineBolt,
            "Rutas descargadas (sin internet)",
            MaterialTheme.colorScheme.secondaryContainer
        )
        RouteSource.FALLBACK -> Triple(
            Icons.Default.WifiOff,
            "Sin internet · Ruta aproximada · Descarga rutas en la base",
            MaterialTheme.colorScheme.errorContainer
        )
    }

    Card(colors = CardDefaults.cardColors(containerColor = color)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityDropdown(
    label: String,
    selectedCity: City?,
    excludeCity: City?,
    onCitySelected: (City) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val cities = City.cities.filter { it != excludeCity }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCity?.name ?: "",
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            leadingIcon = {
                Icon(
                    if (selectedCity?.isMiningArea == true)
                        Icons.Default.Factory
                    else
                        Icons.Default.LocationCity,
                    null
                )
            },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            cities.forEach { city ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (city.isMiningArea) Icons.Default.Factory
                                else Icons.Default.LocationCity,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = if (city.isMiningArea)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    city.name,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    city.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onCitySelected(city)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RouteCard(
    route: Route,
    isFallback: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFallback)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        route.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (route.description.isNotBlank()) {
                        Text(
                            route.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { },
                    label = { Text(MapUtils.formatDistance(route.distance)) },
                    leadingIcon = {
                        Icon(Icons.Default.Route, null, Modifier.size(14.dp))
                    }
                )
                AssistChip(
                    onClick = { },
                    label = { Text(MapUtils.formatDuration(route.estimatedTime / 3600.0)) },
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, null, Modifier.size(14.dp))
                    }
                )
                if (isFallback) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Aproximada") },
                        leadingIcon = {
                            Icon(Icons.Default.Warning, null, Modifier.size(14.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
            }
        }
    }
}