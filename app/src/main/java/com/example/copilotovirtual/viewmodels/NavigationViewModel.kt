package com.example.copilotovirtual.viewmodels

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.copilotovirtual.data.models.Geocerca
import com.example.copilotovirtual.data.models.Route
import com.example.copilotovirtual.utils.GeofenceManager
import com.example.copilotovirtual.utils.KmlLoader
import com.example.copilotovirtual.utils.RouteSegment
import com.example.copilotovirtual.utils.TextToSpeechHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LocationPermissionState {
    object Granted : LocationPermissionState()
    object Denied : LocationPermissionState()
    object Unknown : LocationPermissionState()
}

class NavigationViewModel(application: Application) : AndroidViewModel(application) {

    private val _geocercas = MutableStateFlow<List<Geocerca>>(emptyList())
    val geocercas: StateFlow<List<Geocerca>> = _geocercas.asStateFlow()

    private val _currentGeocercaId = MutableStateFlow<String?>(null)
    val currentGeocercaId: StateFlow<String?> = _currentGeocercaId.asStateFlow()

    fun loadGeocercasFromKml(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val lista = KmlLoader.loadFromAssets(context)
            Log.d("NavigationViewModel", "Geocercas cargadas: ${lista.size}")
            _geocercas.value = lista
        }
    }

    private val _currentSegmentIndex = MutableStateFlow(0)
    val currentSegmentIndex: StateFlow<Int> = _currentSegmentIndex.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _permissionState = MutableStateFlow<LocationPermissionState>(LocationPermissionState.Unknown)
    val permissionState: StateFlow<LocationPermissionState> = _permissionState.asStateFlow()

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

    private val _currentSpeed = MutableStateFlow(0)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    private val _speedLimit = MutableStateFlow<Int?>(null)
    val speedLimit: StateFlow<Int?> = _speedLimit.asStateFlow()

    private val _gpsStatus = MutableStateFlow("Iniciando GPS...")
    val gpsStatus: StateFlow<String> = _gpsStatus.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val geofenceManager = GeofenceManager()
    private var ttsHelper: TextToSpeechHelper? = null
    private var lastSpeedWarning = 0L
    private val SPEED_WARNING_COOLDOWN = 30000L

    // Verifica si la ubicación actual está dentro de alguna geocerca y actualiza el límite
    private fun checkGeocercas(location: Location, geocercas: List<Geocerca>) {
        val point = LatLng(location.latitude, location.longitude)
        val geocercaEncontrada = geocercas.firstOrNull { geocerca ->
            PolyUtil.containsLocation(point, geocerca.polygon, true)
        }
        val nuevaId = geocercaEncontrada?.id
        if (nuevaId != _currentGeocercaId.value) {
            _currentGeocercaId.value = nuevaId
            if (nuevaId != null) {
                // Como no tenemos límites en el KML, asignamos un valor por defecto (60)
                // Si después tienes límites, puedes agregar un campo a Geocerca
                _speedLimit.value = 60
                announceInstruction("Ingresando a ${geocercaEncontrada.name}. Velocidad máxima 60 km/h")
            } else {
                _speedLimit.value = null
                announceInstruction("Saliendo de zona de velocidad controlada")
            }
        }
    }

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
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val granted = fine || coarse
        Log.d("GPS", "Permiso fino: $fine, grueso: $coarse")

        _permissionState.value = if (granted) LocationPermissionState.Granted else LocationPermissionState.Denied
        return granted
    }

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
            return
        }

        if (!isGpsEnabled(context)) {
            Log.e("GPS", "GPS desactivado en el dispositivo")
            _gpsStatus.value = "GPS desactivado. Actívalo en Configuración."
            return
        }

        _gpsStatus.value = "Buscando señal GPS..."

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).apply {
            setMinUpdateIntervalMillis(1000L)
            setMaxUpdateDelayMillis(5000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                Log.d("GPS", "Ubicación recibida: ${location.latitude}, ${location.longitude}, precisión: ${location.accuracy}m")

                _currentLocation.value = location
                _gpsStatus.value = "GPS conectado ✓ (${location.accuracy.toInt()}m)"

                updateSpeed(location)

                if (_isNavigating.value) {
                    checkNavigationProgress(location)
                }
                // Verificar geocercas con la lista actual
                checkGeocercas(location, _geocercas.value)
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d("GPS", "GPS disponible: ${availability.isLocationAvailable}")
                if (!availability.isLocationAvailable) {
                    _gpsStatus.value = "Buscando señal GPS..."
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
            Log.d("GPS", "requestLocationUpdates registrado correctamente")
            getLastKnownLocation(context)
        } catch (e: SecurityException) {
            Log.e("GPS", "SecurityException: ${e.message}")
            _permissionState.value = LocationPermissionState.Denied
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
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("GPS", "Última ubicación conocida: ${location.latitude}, ${location.longitude}")
                    _currentLocation.value = location
                    _gpsStatus.value = "GPS conectado ✓"
                } else {
                    Log.w("GPS", "lastLocation es null - esperando nueva ubicación")
                    _gpsStatus.value = "Buscando señal GPS... (sal al exterior)"
                }
            }?.addOnFailureListener { e ->
                Log.e("GPS", "Error obteniendo lastLocation: ${e.message}")
            }
        } catch (e: SecurityException) {
            Log.e("GPS", "SecurityException en lastLocation: ${e.message}")
        }
    }

    fun centerOnCurrentLocation() {
        // Implementar si se necesita
    }

    private fun updateSpeed(location: Location) {
        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6).toInt().coerceAtLeast(0) else 0
        _currentSpeed.value = speedKmh

        _speedLimit.value?.let { limit ->
            if (speedKmh > limit + 5) {
                val now = System.currentTimeMillis()
                if (now - lastSpeedWarning > SPEED_WARNING_COOLDOWN) {
                    announceInstruction("Atención. Velocidad excedida. Límite $limit km/h. Velocidad actual $speedKmh km/h.")
                    lastSpeedWarning = now
                }
            }
        }
    }

    fun startNavigationWithRoute(route: Route) {
        Log.d("NavViewModel", "Iniciando navegación: ${route.name}, waypoints: ${route.waypoints.size}")
        _isNavigating.value = true
        _routePoints.value = route.waypoints

        val segments = geofenceManager.generateSegments(route.waypoints, route.name)
        _currentSegments.value = segments
        _currentSegmentIndex.value = 0

        if (segments.isNotEmpty()) {
            _speedLimit.value = segments.first().speedLimit
        }

        geofenceManager.reset()

        val instruction = "Iniciando navegación hacia ${route.name}. Distancia total: ${(route.distance / 1000).toInt()} kilómetros."
        _lastInstruction.value = instruction
        announceInstruction(instruction)
    }

    fun stopNavigation() {
        _isNavigating.value = false
        _routePoints.value = emptyList()
        _currentSegments.value = emptyList()
        _navigationProgress.value = 0f
        _lastInstruction.value = null
        _speedLimit.value = null
        _currentSpeed.value = 0
        _currentGeocercaId.value = null
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
                _navigationProgress.value = geofenceManager.getProgress(_currentSegments.value.size)
                _speedLimit.value = segment.speedLimit
                _currentSegmentIndex.value = geofenceManager.getCurrentSegmentIndex()
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