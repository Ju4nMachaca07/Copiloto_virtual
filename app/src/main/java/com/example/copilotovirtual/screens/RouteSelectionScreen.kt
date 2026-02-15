package com.example.copilotovirtual.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.copilotovirtual.data.models.City
import com.example.copilotovirtual.data.models.Route
import com.example.copilotovirtual.utils.MapUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSelectionScreen(
    onRouteSelected: (Route) -> Unit,
    onBack: () -> Unit
) {
    var selectedOriginCity by remember { mutableStateOf<City?>(null) }
    var selectedDestCity by remember { mutableStateOf<City?>(null) }
    var availableRoutes by remember { mutableStateOf<List<Route>>(emptyList()) }

    LaunchedEffect(selectedOriginCity, selectedDestCity) {
        availableRoutes = if (selectedOriginCity != null && selectedDestCity != null) {
            Route.routes.filter { route ->
                route.startCityId == selectedOriginCity!!.id &&
                        route.endCityId == selectedDestCity!!.id
            }
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Ruta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Selecciona tu origen y destino para ver las rutas disponibles",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                CityDropdown(
                    label = "Ciudad de Origen",
                    selectedCity = selectedOriginCity,
                    excludeCity = selectedDestCity,
                    onCitySelected = { selectedOriginCity = it }
                )
            }

            item {
                CityDropdown(
                    label = "Ciudad de Destino",
                    selectedCity = selectedDestCity,
                    excludeCity = selectedOriginCity,
                    onCitySelected = { selectedDestCity = it }
                )
            }

            if (availableRoutes.isEmpty() && selectedOriginCity != null && selectedDestCity != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No hay rutas disponibles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "No existe una ruta directa entre estas ciudades",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            items(availableRoutes) { route ->
                RouteCard(
                    route = route,
                    onClick = { onRouteSelected(route) }
                )
            }
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = {
                Icon(
                    if (selectedCity?.isMiningArea == true) Icons.Default.Terrain
                    else Icons.Default.LocationCity,
                    null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            cities.forEach { city ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (city.isMiningArea) Icons.Default.Terrain
                                else Icons.Default.LocationCity,
                                null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(city.name, fontWeight = FontWeight.Bold)
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
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                    "Seleccionar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(MapUtils.formatDistance(route.distance * 1000)) },
                    leadingIcon = {
                        Icon(Icons.Default.Route, null, Modifier.size(16.dp))
                    }
                )

                AssistChip(
                    onClick = { },
                    label = { Text(MapUtils.formatDuration(route.estimatedTime)) },
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, null, Modifier.size(16.dp))
                    }
                )
            }
        }
    }
}