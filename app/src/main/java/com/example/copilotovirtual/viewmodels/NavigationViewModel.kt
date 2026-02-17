// viewmodels/NavigationViewModel.kt
package com.example.copilotovirtual.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.copilotovirtual.data.models.Route
import com.example.copilotovirtual.utils.GeofenceManager
import com.example.copilotovirtual.utils.RouteSegment
import com.example.copilotovirtual.utils.TextToSpeechHelper
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LocationPermissionState {
    object Granted : LocationPermissionState()
    object Denied : LocationPermissionState()
    object Unknown : LocationPermissionState()
}

data class MapMarkerData(
    val position: LatLng,
    val title: String,
    val snippet: String? = null
)

class NavigationViewModel : ViewModel() {

    // Location
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isLoadingLocation = MutableStateFlow(false)
    val isLoadingLocation: StateFlow<Boolean> = _isLoadingLocation.asStateFlow()

    private val _permissionState = MutableStateFlow<LocationPermissionState>(LocationPermissionState.Unknown)
    val permissionState: StateFlow<LocationPermissionState> = _permissionState.asStateFlow()

    private val _shouldCenterOnLocation = MutableStateFlow(false)
    val shouldCenterOnLocation: StateFlow<Boolean> = _shouldCenterOnLocation.asStateFlow()

    // Navigation
    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints.asStateFlow()

    private val _currentSegments = MutableStateFlow<List<RouteSegment>>(emptyList())
    val currentSegments: StateFlow<List<RouteSegment>> = _currentSegments.asStateFlow()

    private val _navigationProgress = MutableStateFlow(0f)
    val navigationProgress: StateFlow<Float> = _navigationProgress.asStateFlow()

    private val _lastInstruction = MutableStateFlow<String?>(null)
    val lastInstruction: StateFlow<String?> = _lastInstruction.asStateFlow()

    private val _mapMarkers = MutableStateFlow<List<MapMarkerData>>(emptyList())
    val mapMarkers: StateFlow<List<MapMarkerData>> = _mapMarkers.asStateFlow()

    // Speed monitoring
    private val _currentSpeed = MutableStateFlow(0)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    private val _speedLimit = MutableStateFlow<Int?>(null)
    val speedLimit: StateFlow<Int?> = _speedLimit.asStateFlow()

    private var lastSpeedWarning = 0L
    private val SPEED_WARNING_COOLDOWN = 30000L // 30 segundos

    // Managers
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val geofenceManager = GeofenceManager()
    private var ttsHelper: TextToSpeechHelper? = null

    fun initializeLocationClient(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
    }

    fun initializeTTS(context: Context) {
        if (ttsHelper == null) {
            Log.d("NavViewModel", "Inicializando TTS")
            ttsHelper = TextToSpeechHelper(context)
        }
    }

    fun checkLocationPermission(context: Context): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        _permissionState.value = if (granted) {
            LocationPermissionState.Granted
        } else {
            LocationPermissionState.Denied
        }

        return granted
    }

    fun startLocationUpdates(context: Context) {
        if (!checkLocationPermission(context)) {
            _permissionState.value = LocationPermissionState.Denied
            return
        }

        _isLoadingLocation.value = true

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L // Actualización cada 2 segundos
        ).apply {
            setMinUpdateIntervalMillis(1000L)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _currentLocation.value = location
                    _isLoadingLocation.value = false

                    // Actualizar velocidad
                    updateSpeed(location)

                    if (_isNavigating.value) {
                        checkNavigationProgress(location)
                    }
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("NavViewModel", "Error permisos: ${e.message}")
            _permissionState.value = LocationPermissionState.Denied
            _isLoadingLocation.value = false
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        _isLoadingLocation.value = false
    }

    fun getLastKnownLocation(context: Context) {
        if (!checkLocationPermission(context)) return

        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                location?.let {
                    _currentLocation.value = it
                    _isLoadingLocation.value = false
                }
            }
        } catch (e: SecurityException) {
            Log.e("NavViewModel", "Error obteniendo ubicación: ${e.message}")
        }
    }

    fun centerOnCurrentLocation() {
        _shouldCenterOnLocation.value = true
    }

    fun onLocationCentered() {
        _shouldCenterOnLocation.value = false
    }

    private fun updateSpeed(location: Location) {
        // Convertir m/s a km/h
        val speedKmh = (location.speed * 3.6).toInt()
        _currentSpeed.value = speedKmh.coerceAtLeast(0)

        // Verificar exceso de velocidad
        _speedLimit.value?.let { limit ->
            if (speedKmh > limit + 5) { // 5 km/h de tolerancia
                val now = System.currentTimeMillis()
                if (now - lastSpeedWarning > SPEED_WARNING_COOLDOWN) {
                    announceSpeedWarning(speedKmh, limit)
                    lastSpeedWarning = now
                }
            }
        }
    }

    private fun announceSpeedWarning(currentSpeed: Int, limit: Int) {
        val warning = "Atención. Velocidad excedida. " +
                "Límite: $limit kilómetros por hora. " +
                "Tu velocidad: $currentSpeed kilómetros por hora. " +
                "Por favor, reduce la velocidad."
        announceInstruction(warning)
    }

    fun startNavigation(routeId: String) {
        val route = Route.getRouteById(routeId)
        if (route == null) {
            Log.e("NavViewModel", "Ruta no encontrada: $routeId")
            return
        }

        Log.d("NavViewModel", "Iniciando navegación: ${route.name}")
        Log.d("NavViewModel", "Waypoints en ruta: ${route.waypoints.size}")

        _isNavigating.value = true
        _routePoints.value = route.waypoints

        // Generar segmentos con geocercas
        val segments = geofenceManager.generateSegments(route.waypoints, route.name)
        _currentSegments.value = segments

        Log.d("NavViewModel", "Segmentos generados: ${segments.size}")

        // Establecer límite de velocidad inicial
        if (segments.isNotEmpty()) {
            _speedLimit.value = segments.first().speedLimit
        }

        geofenceManager.reset()

        val instruction = "Iniciando navegación hacia ${route.name}. " +
                "Distancia total: ${(route.distance / 1000).toInt()} kilómetros."
        _lastInstruction.value = instruction
        announceInstruction(instruction)
    }

    fun stopNavigation() {
        Log.d("NavViewModel", "Deteniendo navegación")

        _isNavigating.value = false
        _routePoints.value = emptyList()
        _currentSegments.value = emptyList()
        _navigationProgress.value = 0f
        _lastInstruction.value = null
        _speedLimit.value = null
        _currentSpeed.value = 0

        geofenceManager.reset()
        announceInstruction("Navegación detenida")
    }

    fun checkNavigationProgress(location: Location) {
        if (_currentSegments.value.isEmpty()) return

        geofenceManager.checkGeofence(
            currentLocation = location,
            segments = _currentSegments.value,
            onEnterSegment = { segment, instruction ->
                Log.d("NavViewModel", "Geocerca: $instruction")
                _lastInstruction.value = instruction
                _navigationProgress.value = geofenceManager.getProgress(_currentSegments.value.size)
                _speedLimit.value = segment.speedLimit
                announceInstruction(instruction)
            }
        )
    }

    fun announceInstruction(instruction: String) {
        Log.d("NavViewModel", "Anunciando: $instruction")
        ttsHelper?.speak(instruction)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        ttsHelper?.shutdown()
    }
}