package com.example.copilotovirtual.ui.theme.Screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.copilotovirtual.Components.OSMMapView
import androidx.compose.ui.platform.LocalContext
import com.example.copilotovirtual.Components.LocationManager
import kotlin.text.*

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen() {
    val context = LocalContext.current
    var selectedPoint by remember { mutableStateOf<LocationManager.LocationPoint?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Puntos de Interés - Perú") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mapa (50% de la pantalla)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                OSMMapView(
                    context = context,
                    showPoints = true,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Información (50% de la pantalla)
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Información general
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Área Minera - Ica, Perú",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Distancia Marcona → Mina Justa: ${String.format("%.1f", LocationManager.distanciaMarconaMinaJusta)} km")
                                Text("• Distancia Mina Justa → Nazca: ${String.format("%.1f", LocationManager.distanciaMinaJustaNazca)} km")
                                Text("• Distancia total: ${String.format("%.1f", LocationManager.distanciaTotal)} km")
                            }
                        }
                    }

                    // Lista de puntos
                    items(LocationManager.ALL_POINTS) { point ->
                        LocationCard(
                            point = point,
                            isSelected = selectedPoint == point,
                            onClick = { selectedPoint = point }
                        )
                    }

                    // Información adicional
                    item {
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Información del mapa offline",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Esta área está incluida en tu mapa descargado")
                                Text("• Zoom disponible: 12-14 (de MOBAC)")
                                Text("• Área cubierta: ~150 km de costa")
                                Text("• Funciona 100% sin internet")
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationCard(
    point: LocationManager.LocationPoint,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    point.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(point.description)
                Text(
                    "Lat: ${String.format("%.4f", point.latitude)}, " +
                            "Lng: ${String.format("%.4f", point.longitude)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}