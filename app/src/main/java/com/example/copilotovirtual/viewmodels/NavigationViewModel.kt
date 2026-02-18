// viewmodels/NavigationViewModel.kt
package com.example.copilotovirtual.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isLoadingLocation = MutableStateFlow(true)
    val isLoadingLocation: StateFlow<Boolean> = _isLoadingLocation.asStateFlow()

    private val _permissionState = MutableStateFlow<LocationPermissionState>(
        LocationPermissionState.Unknown
    )
    val permissionState: StateFlow<LocationPermissionState> = _permissionState.asStateFlow()

    private val _shouldCenterOnLocation = MutableStateFlow(false)
    val shouldCenterOnLocation: StateFlow<Boolean> = _shouldCenterOnLocation.asStateFlow()

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

    private val _currentSpeed = MutableStateFlow(0)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    private val _speedLimit = MutableStateFlow<Int?>(null)
    val speedLimit: StateFlow<Int?> = _speedLimit.asStateFlow()

    // Estado GPS para mostrar en UI
    private val _gpsStatus = MutableStateFlow("Iniciando GPS...")
    val gpsStatus: StateFlow<String> = _gpsStatus.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val geofenceManager = GeofenceManager()
    private var ttsHelper: TextToSpeechHelper? = null
    private var lastSpeedWarning = 0L
    private val SPEED_WARNING_COOLDOWN = 30000L

    fun initializeLocationClient(context: Context) {
        if (fusedLocationClient == null) {
            Log.d("GPS", "Inicializando FusedLocationClient")
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
    }

    fun initializeTTS(context: Context) {
        if (ttsHelper == null) {
            ttsHelper = TextToSpeechHelper(context)
        }
    }

    fun checkLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val granted = fine || coarse
        Log.d("GPS", "Permiso fino: $fine, grueso: $coarse")

        _permissionState.value = if (granted)
            LocationPermissionState.Granted
        else
            LocationPermissionState.Denied

        return granted
    }

    // Verificar si el GPS del dispositivo está activado
    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
                as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Log.d("GPS", "GPS activado: $gpsEnabled, Red activada: $networkEnabled")
        return gpsEnabled || networkEnabled
    }

    fun startLocationUpdates(context: Context) {
        Log.d("GPS", "startLocationUpdates llamado")

        if (!checkLocationPermission(context)) {
            Log.e("GPS", "Sin permisos de ubicación")
            _permissionState.value = LocationPermissionState.Denied
            _isLoadingLocation.value = false
            return
        }

        if (!isGpsEnabled(context)) {
            Log.e("GPS", "GPS desactivado en el dispositivo")
            _gpsStatus.value = "GPS desactivado. Actívalo en Configuración."
            _isLoadingLocation.value = false
            return
        }

        _isLoadingLocation.value = true
        _gpsStatus.value = "Buscando señal GPS..."

        // LocationRequest menos estricto para Android 7+
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L // cada 3 segundos
        ).apply {
            setMinUpdateIntervalMillis(1000L)
            setMaxUpdateDelayMillis(5000L)
            // SIN setWaitForAccurateLocation - esto bloqueaba en Android 7
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                Log.d("GPS", "Ubicación recibida: ${location.latitude}, ${location.longitude}, " +
                        "precisión: ${location.accuracy}m")

                _currentLocation.value = location
                _isLoadingLocation.value = false
                _gpsStatus.value = "GPS conectado ✓ (${location.accuracy.toInt()}m)"

                updateSpeed(location)

                if (_isNavigating.value) {
                    checkNavigationProgress(location)
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d("GPS", "GPS disponible: ${availability.isLocationAvailable}")
                if (!availability.isLocationAvailable) {
                    _gpsStatus.value = "Buscando señal GPS..."
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.d("GPS", "requestLocationUpdates registrado correctamente")

            // Intentar obtener última ubicación conocida inmediatamente
            getLastKnownLocation(context)

        } catch (e: SecurityException) {
            Log.e("GPS", "SecurityException: ${e.message}")
            _permissionState.value = LocationPermissionState.Denied
            _isLoadingLocation.value = false
        }

    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
            Log.d("GPS", "Location updates detenidas")
        }
        locationCallback = null
    }

    fun getLastKnownLocation(context: Context) {
        if (!checkLocationPermission(context)) return

        try {
            fusedLocationClient?.lastLocation
                ?.addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d("GPS", "Última ubicación conocida: " +
                                "${location.latitude}, ${location.longitude}")
                        _currentLocation.value = location
                        _isLoadingLocation.value = false
                        _gpsStatus.value = "GPS conectado ✓"
                    } else {
                        Log.w("GPS", "lastLocation es null - esperando nueva ubicación")
                        _gpsStatus.value = "Buscando señal GPS... (sal al exterior)"
                    }
                }
                ?.addOnFailureListener { e ->
                    Log.e("GPS", "Error obteniendo lastLocation: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.e("GPS", "SecurityException en lastLocation: ${e.message}")
        }
    }

    fun centerOnCurrentLocation() {
        _shouldCenterOnLocation.value = true
    }

    fun onLocationCentered() {
        _shouldCenterOnLocation.value = false
    }

    private fun updateSpeed(location: Location) {
        val speedKmh = if (location.hasSpeed()) {
            (location.speed * 3.6).toInt().coerceAtLeast(0)
        } else {
            0
        }
        _currentSpeed.value = speedKmh

        _speedLimit.value?.let { limit ->
            if (speedKmh > limit + 5) {
                val now = System.currentTimeMillis()
                if (now - lastSpeedWarning > SPEED_WARNING_COOLDOWN) {
                    announceInstruction(
                        "Atención. Velocidad excedida. " +
                                "Límite $limit kilómetros por hora. " +
                                "Velocidad actual $speedKmh kilómetros por hora."
                    )
                    lastSpeedWarning = now
                }
            }
        }
    }

    fun startNavigationWithRoute(route: Route) {
        Log.d("NavViewModel", "Iniciando navegación: ${route.name}")
        Log.d("NavViewModel", "Waypoints en ruta: ${route.waypoints.size}")

        // Mostrar primeros 3 puntos para debug
        route.waypoints.take(3).forEachIndexed { index, point ->
            Log.d("NavViewModel", "  Punto $index: ${point.latitude}, ${point.longitude}")
        }

        _isNavigating.value = true
        _routePoints.value = route.waypoints

        Log.d("NavViewModel", "routePoints actualizados con ${_routePoints.value.size} puntos")

        val segments = geofenceManager.generateSegments(route.waypoints, route.name)
        _currentSegments.value = segments

        Log.d("NavViewModel", "Segmentos creados: ${segments.size}")

        if (segments.isNotEmpty()) {
            _speedLimit.value = segments.first().speedLimit
            Log.d("NavViewModel", "Límite inicial: ${segments.first().speedLimit} km/h")
        }

        geofenceManager.reset()

        val instruction = "Iniciando navegación hacia ${route.name}. " +
                "Distancia total: ${(route.distance / 1000).toInt()} kilómetros."
        _lastInstruction.value = instruction
        announceInstruction(instruction)
    }

    // Mantener la función anterior por si acaso, pero ya no se usará
    fun startNavigation(routeId: String) {
        val route = Route.getRouteById(routeId)
        if (route == null) {
            Log.e("NavViewModel", "Ruta no encontrada: $routeId")
            return
        }
        startNavigationWithRoute(route)
    }

    fun stopNavigation() {
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
                _lastInstruction.value = instruction
                _navigationProgress.value =
                    geofenceManager.getProgress(_currentSegments.value.size)
                _speedLimit.value = segment.speedLimit
                announceInstruction(instruction)
            }
        )
    }

    fun announceInstruction(instruction: String) {
        Log.d("TTS", "Anunciando: $instruction")
        ttsHelper?.speak(instruction)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        ttsHelper?.shutdown()
    }
}