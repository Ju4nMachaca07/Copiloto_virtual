package com.example.copilotovirtual.components

import android.Manifest
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.copilotovirtual.viewmodels.LocationPermissionState
import com.example.copilotovirtual.viewmodels.NavigationViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    viewModel: NavigationViewModel = viewModel()
) {
    val context = LocalContext.current

    val currentLocation by viewModel.currentLocation.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val mapMarkers by viewModel.mapMarkers.collectAsState()
    val isLoadingLocation by viewModel.isLoadingLocation.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val shouldCenter by viewModel.shouldCenterOnLocation.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(-15.3500, -75.1100),
            8f
        )
    }

    // Launcher de permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.startLocationUpdates(context)
            viewModel.getLastKnownLocation(context)
        }
    }

    // Inicialización
    LaunchedEffect(Unit) {
        viewModel.initializeLocationClient(context)
        if (viewModel.checkLocationPermission(context)) {
            viewModel.startLocationUpdates(context)
            viewModel.getLastKnownLocation(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Centrar en ubicación
    LaunchedEffect(shouldCenter, currentLocation) {
        if (shouldCenter && currentLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentLocation!!.latitude, currentLocation!!.longitude),
                    15f
                ),
                durationMs = 1000
            )
            viewModel.onLocationCentered()
        }
    }

    // Centrar en primera ubicación
    LaunchedEffect(currentLocation) {
        if (currentLocation != null && cameraPositionState.position.zoom < 10f) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentLocation!!.latitude, currentLocation!!.longitude),
                    12f
                ),
                durationMs = 1500
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
                        val userPosition = LatLng(location.latitude, location.longitude)

                        Circle(
                            center = userPosition,
                            radius = location.accuracy.toDouble(),
                            fillColor = Color(0x220000FF),
                            strokeColor = Color(0xFF0000FF),
                            strokeWidth = 2f
                        )

                        Marker(
                            state = MarkerState(position = userPosition),
                            title = "Tu ubicación",
                            snippet = "Precisión: ${location.accuracy.toInt()}m",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE
                            ),
                            zIndex = 10f
                        )
                    }

                    // Ruta activa
                    if (routePoints.isNotEmpty() && routePoints.size >= 2) {
                        Polyline(
                            points = routePoints,
                            color = Color(0xFF2196F3),
                            width = 12f,
                            geodesic = true,
                            zIndex = 5f
                        )

                        Marker(
                            state = MarkerState(position = routePoints.first()),
                            title = "Inicio",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN
                            ),
                            zIndex = 6f
                        )

                        Marker(
                            state = MarkerState(position = routePoints.last()),
                            title = "Destino",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            ),
                            zIndex = 6f
                        )
                    }

                    // Otros marcadores
                    mapMarkers.forEach { marker ->
                        Marker(
                            state = MarkerState(position = marker.position),
                            title = marker.title,
                            snippet = marker.snippet
                        )
                    }
                }

                // Indicador de carga
                if (isLoadingLocation) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Obteniendo ubicación GPS...")
                            }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocationOff,
            contentDescription = null,
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
            "Esta app necesita acceso a tu ubicación para mostrar rutas de navegación",
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
            Text("Conceder Permisos")
        }
    }
}