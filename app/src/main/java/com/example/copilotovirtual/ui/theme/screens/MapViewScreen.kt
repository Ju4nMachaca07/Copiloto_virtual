package com.example.copilotovirtual.ui.theme.Screens

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.copilotovirtual.Components.OSMMapView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapViewScreen() {
    val context = LocalContext.current

    // Solicitar permiso de ubicación
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Estado para controlar si el mapa está listo
    var isMapReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Solicitar permiso al iniciar
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
        isMapReady = true
    }

    // Mostrar mapa cuando esté listo
    if (isMapReady) {
        OSMMapView(
            modifier = Modifier.fillMaxSize(),
            context = context
        )
    }
}