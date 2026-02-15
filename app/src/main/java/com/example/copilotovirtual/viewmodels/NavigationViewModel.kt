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
import com.google.android.gms.location.FusedLocationProviderClient
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
            5000L
        ).apply {
            setMinUpdateIntervalMillis(2000L)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _currentLocation.value = location
                    _isLoadingLocation.value = false

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
            Log.e("NavViewModel", "Error obteniendo ubicacion: ${e.message}")
        }
    }

    fun centerOnCurrentLocation() {
        _shouldCenterOnLocation.value = true
    }

    fun onLocationCentered() {
        _shouldCenterOnLocation.value = false
    }

    /**
     * INICIAR NAVEGACIÓN - CORREGIDO PARA USAR WAYPOINTS DETALLADOS
     */
    fun startNavigation(routeId: String) {
        val route = Route.getRouteById(routeId)
        if (route == null) {
            Log.e("NavViewModel", "Ruta no encontrada: $routeId")
            return
        }

        Log.d("NavViewModel", "Iniciando navegacion: ${route.name}")
        Log.d("NavViewModel", "Waypoints en ruta: ${route.waypoints.size}")

        _isNavigating.value = true

        // USAR LOS WAYPOINTS DETALLADOS DE LA RUTA
        _routePoints.value = route.waypoints

        // Generar segmentos con geocercas
        val segments = geofenceManager.generateSegments(route.waypoints, route.name)
        _currentSegments.value = segments

        Log.d("NavViewModel", "Segmentos generados: ${segments.size}")

        geofenceManager.reset()

        val instruction = "Iniciando navegacion hacia ${route.name}"
        _lastInstruction.value = instruction
        announceInstruction(instruction)
    }

    fun stopNavigation() {
        Log.d("NavViewModel", "Deteniendo navegacion")

        _isNavigating.value = false
        _routePoints.value = emptyList()
        _currentSegments.value = emptyList()
        _navigationProgress.value = 0f
        _lastInstruction.value = null

        geofenceManager.reset()
        announceInstruction("Navegacion detenida")
    }

    fun checkNavigationProgress(location: Location) {
        if (_currentSegments.value.isEmpty()) return

        geofenceManager.checkGeofence(
            currentLocation = location,
            segments = _currentSegments.value,
            onEnterSegment = { segment, instruction ->
                Log.d("NavViewModel", "Geocerca alcanzada: $instruction")
                _lastInstruction.value = instruction
                _navigationProgress.value = geofenceManager.getProgress(_currentSegments.value.size)
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