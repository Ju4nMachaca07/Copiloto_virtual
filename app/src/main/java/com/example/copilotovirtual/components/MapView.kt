// components/MapView.kt
package com.example.copilotovirtual.components

import android.Manifest
import android.content.Context
import android.graphics.Paint
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.copilotovirtual.utils.NetworkUtils
import com.example.copilotovirtual.viewmodels.LocationPermissionState
import com.example.copilotovirtual.viewmodels.NavigationViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OsmMapView
import org.osmdroid.views.overlay.Polyline as OsmPolyline
import org.osmdroid.views.overlay.Marker as OsmMarker

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    viewModel: NavigationViewModel
) {
    val context = LocalContext.current

    val currentLocation by viewModel.currentLocation.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val shouldCenter by viewModel.shouldCenterOnLocation.collectAsState()
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
                // Mapa
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(
                            currentLocation?.let {
                                LatLng(it.latitude, it.longitude)
                            } ?: LatLng(-15.3500, -75.1100),
                            if (currentLocation != null) 15f else 8f
                        )
                    },
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
                    // Ubicación actual del usuario
                    currentLocation?.let { location ->
                        val userPos = LatLng(location.latitude, location.longitude)

                        // Círculo de precisión
                        Circle(
                            center = userPos,
                            radius = location.accuracy.toDouble(),
                            fillColor = Color(0x220000FF),
                            strokeColor = Color(0xFF0000FF),
                            strokeWidth = 2f
                        )

                        // Marcador de ubicación
                        Marker(
                            state = MarkerState(position = userPos),
                            title = "Tu ubicación",
                            snippet = "Precisión: ${location.accuracy.toInt()}m",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE
                            ),
                            zIndex = 10f
                        )
                    }

                    // FIX: Dibujar la ruta si existe
                    if (routePoints.isNotEmpty() && routePoints.size >= 2) {
                        Log.d("MapView", "Dibujando ruta con ${routePoints.size} puntos")

                        Polyline(
                            points = routePoints,
                            color = Color(0xFF2196F3),
                            width = 12f,
                            geodesic = true,
                            zIndex = 5f
                        )

                        // Marcador de inicio
                        Marker(
                            state = MarkerState(position = routePoints.first()),
                            title = "Inicio",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN
                            ),
                            zIndex = 6f
                        )

                        // Marcador de destino
                        Marker(
                            state = MarkerState(position = routePoints.last()),
                            title = "Destino",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            ),
                            zIndex = 6f
                        )
                    } else if (routePoints.isNotEmpty()) {
                        Log.w("MapView", "Ruta con solo ${routePoints.size} puntos - necesita al menos 2")
                    }
                }

                // FIX: Loading SOLO cuando no hay ubicación
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
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                        Text("• Sal al exterior",
                                            style = MaterialTheme.typography.bodySmall)
                                        Text("• Verifica que el GPS esté activado",
                                            style = MaterialTheme.typography.bodySmall)
                                        Text("• Espera 30-60 segundos",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Banner GPS activo (cuando ya tiene ubicación)
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

// MAPA ONLINE CON GOOGLE MAPS
@Composable
fun OnlineMapView(
    currentLocation: android.location.Location?,
    routePoints: List<com.google.android.gms.maps.model.LatLng>,
    shouldCenter: Boolean,
    onCentered: () -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(-15.3500, -75.1100), 8f
        )
    }

    LaunchedEffect(shouldCenter, currentLocation) {
        if (shouldCenter && currentLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentLocation.latitude, currentLocation.longitude), 15f
                ), 1000
            )
            onCentered()
        }
    }

    LaunchedEffect(currentLocation) {
        if (currentLocation != null && cameraPositionState.position.zoom < 10f) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentLocation.latitude, currentLocation.longitude), 12f
                ), 1500
            )
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true
        )
    ) {
        currentLocation?.let { location ->
            val userPos = LatLng(location.latitude, location.longitude)
            Circle(
                center = userPos,
                radius = location.accuracy.toDouble(),
                fillColor = Color(0x220000FF),
                strokeColor = Color(0xFF0000FF),
                strokeWidth = 2f
            )
            Marker(
                state = MarkerState(position = userPos),
                title = "Tu ubicación",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )
        }

        if (routePoints.size >= 2) {
            Polyline(
                points = routePoints,
                color = Color(0xFF2196F3),
                width = 12f,
                geodesic = true
            )
            Marker(
                state = MarkerState(position = routePoints.first()),
                title = "Inicio",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )
            Marker(
                state = MarkerState(position = routePoints.last()),
                title = "Destino",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
        }
    }
}

// MAPA OFFLINE CON OSMDROID
@Composable
fun OfflineMapView(
    context: Context,
    currentLocation: android.location.Location?,
    routePoints: List<com.google.android.gms.maps.model.LatLng>,
    shouldCenter: Boolean
) {
    // Configurar OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.getExternalFilesDir(null)
            osmdroidTileCache = context.getExternalFilesDir("tiles")
        }
    }

    AndroidView(
        factory = { ctx ->
            OsmMapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(12.0)

                // Centro inicial en Marcona
                controller.setCenter(GeoPoint(-15.3500, -75.1100))
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            // Ubicación actual
            currentLocation?.let { location ->
                val marker = OsmMarker(mapView).apply {
                    position = GeoPoint(location.latitude, location.longitude)
                    title = "Tu ubicación"
                    setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)

                if (shouldCenter) {
                    mapView.controller.animateTo(
                        GeoPoint(location.latitude, location.longitude)
                    )
                    mapView.controller.setZoom(15.0)
                }
            }

            // Ruta
            if (routePoints.size >= 2) {
                val polyline = OsmPolyline().apply {
                    setPoints(routePoints.map { GeoPoint(it.latitude, it.longitude) })
                    outlinePaint.color = android.graphics.Color.BLUE
                    outlinePaint.strokeWidth = 12f
                    outlinePaint.style = Paint.Style.STROKE
                }
                mapView.overlays.add(polyline)

                // Marcador inicio
                val startMarker = OsmMarker(mapView).apply {
                    position = GeoPoint(
                        routePoints.first().latitude,
                        routePoints.first().longitude
                    )
                    title = "Inicio"
                }
                mapView.overlays.add(startMarker)

                // Marcador fin
                val endMarker = OsmMarker(mapView).apply {
                    position = GeoPoint(
                        routePoints.last().latitude,
                        routePoints.last().longitude
                    )
                    title = "Destino"
                }
                mapView.overlays.add(endMarker)
            }

            mapView.invalidate()
        }
    )
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
        Text("Permisos de ubicación requeridos",
            style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Esta app necesita GPS para la navegación",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.LocationOn, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Conceder Permisos GPS")
        }
    }
}