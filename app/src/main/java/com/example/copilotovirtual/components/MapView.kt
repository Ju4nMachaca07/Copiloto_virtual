package com.example.copilotovirtual.components

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.copilotovirtual.data.models.Geocerca
import com.example.copilotovirtual.utils.SpeedLimitZones
import com.example.copilotovirtual.viewmodels.LocationPermissionState
import com.example.copilotovirtual.viewmodels.NavigationViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.math.abs

fun createPolygonAroundSegment(start: LatLng, end: LatLng, widthMeters: Double): List<LatLng> {
    val midLat = (start.latitude + end.latitude) / 2
    val midLng = (start.longitude + end.longitude) / 2
    val offset = 0.001 // ~111 metros
    return listOf(
        LatLng(midLat - offset, midLng - offset),
        LatLng(midLat - offset, midLng + offset),
        LatLng(midLat + offset, midLng + offset),
        LatLng(midLat + offset, midLng - offset)
    )
}

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    viewModel: NavigationViewModel,
    currentSegmentIndex: Int,
    geocercas: List<Geocerca>
) {
    val context = LocalContext.current
    val currentLocation by viewModel.currentLocation.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val currentSegments by viewModel.currentSegments.collectAsState()
    val gpsStatus by viewModel.gpsStatus.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.startLocationUpdates(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeLocationClient(context)
        if (viewModel.checkLocationPermission(context)) {
            viewModel.startLocationUpdates(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-16.4, -71.5), 10f)
    }

    Box(modifier = modifier) {
        when (permissionState) {
            is LocationPermissionState.Denied -> {
                PermissionDeniedScreen(
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
            else -> {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        compassEnabled = true
                    )
                ) {
                    // Ubicación actual
                    currentLocation?.let { location ->
                        val userPos = LatLng(location.latitude, location.longitude)
                        Marker(
                            state = MarkerState(position = userPos),
                            title = "Tu ubicación",
                            snippet = "Precisión: ${location.accuracy.toInt()}m",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            zIndex = 10f
                        )
                    }

                    // Geocercas del JSON
                    val colores = listOf(
                        Color(0xFFE53935), // rojo
                        Color(0xFFFF9800), // naranja
                        Color(0xFFFFEB3B), // amarillo
                        Color(0xFF4CAF50), // verde
                        Color(0xFF2196F3), // azul
                        Color(0xFF9C27B0), // morado
                        Color(0xFFFF4081), // rosa
                        Color(0xFF00BCD4), // cian
                        Color(0xFFFF6F00), // ámbar
                        Color(0xFF8BC34A), // verde claro
                        Color(0xFF673AB7), // púrpura oscuro
                        Color(0xFFFF5722), // naranja oscuro
                        Color(0xFF795548), // marrón
                        Color(0xFF607D8B), // azul grisáceo
                        Color(0xFFE91E63)  // rosa intenso
                    )

                    geocercas.forEach { geocerca ->
                        // Generar un índice basado en el hash del id para que cada geocerca tenga un color consistente
                        val colorIndex = abs(geocerca.id.hashCode()) % colores.size
                        val fillColor = colores[colorIndex].copy(alpha = 0.7f) // 70% opacidad para ver el mapa debajo

                        Polygon(
                            points = geocerca.polygon,
                            fillColor = fillColor,
                            strokeColor = Color.Black,      // borde negro para destacar
                            strokeWidth = 4f,
                            zIndex = 10f,
                            clickable = false
                        )
                    }

                    // Ruta azul
                    if (routePoints.isNotEmpty()) {
                        Polyline(
                            points = routePoints,
                            color = Color(0xFF2196F3),
                            width = 12f,
                            geodesic = true,
                            zIndex = 4f
                        )
                    }

                    // Polígonos de velocidad (segmentos de navegación)
                    if (currentSegments.isNotEmpty()) {
                        val maxSegmentsToShow = 20
                        val startIdx = (currentSegmentIndex - 2).coerceAtLeast(0)
                        val endIdx = (currentSegmentIndex + maxSegmentsToShow).coerceAtMost(currentSegments.size)
                        val segmentsToShow = currentSegments.subList(startIdx, endIdx)

                        segmentsToShow.forEachIndexed { idx, segment ->
                            val fillColor = when (segment.speedLimit) {
                                in 0..40 -> Color(0x66FF4444)
                                in 41..70 -> Color(0x66FFAA00)
                                else -> Color(0x6600FF00)
                            }
                            val polygonPoints = createPolygonAroundSegment(
                                start = segment.startPoint,
                                end = segment.endPoint,
                                widthMeters = 30.0
                            )
                            Polygon(
                                points = polygonPoints,
                                fillColor = fillColor,
                                strokeColor = fillColor.copy(alpha = 1f),
                                strokeWidth = 2f,
                                zIndex = 5f,
                                clickable = false
                            )
                            if (idx + startIdx == currentSegmentIndex) {
                                Polygon(
                                    points = polygonPoints,
                                    fillColor = Color.Transparent,
                                    strokeColor = Color.Black,
                                    strokeWidth = 4f,
                                    zIndex = 6f,
                                    clickable = false
                                )
                            }
                        }
                    }

                    // Zonas de velocidad fijas (opcional)
                    SpeedLimitZones.zones.forEach { zone ->
                        Marker(
                            state = MarkerState(position = zone.center),
                            title = zone.name,
                            snippet = "Límite: ${zone.speedLimit} km/h",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                        )
                    }
                }

                // Texto superpuesto con el contador de geocercas (FUERA del GoogleMap)
                Text(
                    text = "Geocercas: ${geocercas.size}",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .background(Color.White)
                        .padding(8.dp)
                )

                // Loading cuando no hay ubicación
                if (currentLocation == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                Text(
                                    "Conectando GPS",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    gpsStatus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "Consejos:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("• Sal al exterior", style = MaterialTheme.typography.bodySmall)
                                        Text("• Verifica que el GPS esté activado", style = MaterialTheme.typography.bodySmall)
                                        Text("• Espera 30-60 segundos", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Banner GPS activo
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.GpsFixed,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF43A047)
                            )
                            Text(
                                gpsStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocationOff, null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Permisos de ubicación requeridos",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Esta app necesita GPS para la navegación",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LocationOn, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Conceder Permisos GPS")
        }
    }
}