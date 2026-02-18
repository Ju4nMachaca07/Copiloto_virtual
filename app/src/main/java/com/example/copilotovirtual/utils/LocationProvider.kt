package com.example.copilotovirtual.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*

class LocationProvider(private val context: Context) {

    // Detectar automáticamente qué usar
    private val useGoogleServices: Boolean by lazy {
        val result = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context)
        val available = result == ConnectionResult.SUCCESS
        Log.d(
            "LocationProvider",
            if (available) "Google Play Services disponible → usando FusedLocation"
            else "Google Play Services NO disponible → usando GPS nativo"
        )
        available
    }

    // Google (Samsung, Motorola, Xiaomi, etc.)
    private var fusedClient: FusedLocationProviderClient? = null
    private var fusedCallback: LocationCallback? = null

    // Nativo (Huawei/Honor sin GMS)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var nativeListener: LocationListener? = null

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    fun isGpsEnabled(): Boolean {
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val net = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Log.d("LocationProvider", "GPS=$gps, Network=$net")
        return gps || net
    }

    fun startUpdates(
        onLocation: (Location) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (!hasPermission()) {
            onError("Sin permisos de ubicación"); return
        }
        if (!isGpsEnabled()) {
            onError("GPS desactivado. Actívalo en Configuración → Ubicación"); return
        }

        if (useGoogleServices) {
            startWithGoogle(onLocation, onError)
        } else {
            startWithNative(onLocation, onError)
        }
    }

    // ── Google Play Services (Samsung, Motorola, etc.) ──
    private fun startWithGoogle(
        onLocation: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            fusedClient = LocationServices.getFusedLocationProviderClient(context)

            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 3000L
            ).apply {
                setMinUpdateIntervalMillis(1000L)
                setMaxUpdateDelayMillis(5000L)
            }.build()

            fusedCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        Log.d(
                            "LocationProvider",
                            "Google GPS: ${location.latitude}, ${location.longitude}"
                        )
                        onLocation(location)
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        onError("Buscando señal GPS...")
                    }
                }
            }

            fusedClient?.requestLocationUpdates(
                request, fusedCallback!!, Looper.getMainLooper()
            )

            // Última ubicación conocida inmediata
            fusedClient?.lastLocation?.addOnSuccessListener { location ->
                location?.let { onLocation(it) }
            }

            Log.d("LocationProvider", "FusedLocation iniciado")

        } catch (e: SecurityException) {
            Log.e("LocationProvider", "Google GPS error: ${e.message}")
            // Fallback a GPS nativo si falla Google
            Log.w("LocationProvider", "Fallback a GPS nativo...")
            startWithNative(onLocation, onError)
        }
    }

    // ── GPS Nativo Android (Huawei/Honor sin GMS) ──
    private fun startWithNative(
        onLocation: (Location) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            nativeListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    Log.d(
                        "LocationProvider",
                        "Nativo GPS: ${location.latitude}, ${location.longitude}"
                    )
                    onLocation(location)
                }

                override fun onProviderEnabled(provider: String) {
                    Log.d("LocationProvider", "Provider activado: $provider")
                }

                override fun onProviderDisabled(provider: String) {
                    onError("GPS desactivado: $provider")
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(
                    provider: String?, status: Int, extras: Bundle?
                ) {
                }
            }

            var registered = 0

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L, 1f, nativeListener!!
                )
                registered++
                Log.d("LocationProvider", "GPS_PROVIDER registrado")
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L, 5f, nativeListener!!
                )
                registered++
                Log.d("LocationProvider", "NETWORK_PROVIDER registrado")
            }

            if (registered == 0) {
                onError("No hay proveedores GPS disponibles")
                return
            }

            // Última ubicación conocida inmediata
            getLastKnownLocation()?.let { onLocation(it) }

            Log.d("LocationProvider", "$registered providers nativos registrados")

        } catch (e: SecurityException) {
            Log.e("LocationProvider", "Native GPS error: ${e.message}")
            onError("Error GPS: ${e.message}")
        }
    }

    fun stopUpdates() {
        // Detener Google
        fusedCallback?.let {
            fusedClient?.removeLocationUpdates(it)
            Log.d("LocationProvider", "FusedLocation detenido")
        }
        fusedCallback = null
        fusedClient = null

        // Detener nativo
        nativeListener?.let {
            try {
                locationManager.removeUpdates(it)
                Log.d("LocationProvider", "GPS nativo detenido")
            } catch (e: Exception) {
                Log.e("LocationProvider", "Error deteniendo nativo: ${e.message}")
            }
        }
        nativeListener = null
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getLastKnownLocation(): Location? {
        if (!hasPermission()) return null

        var best: Location? = null

        // Intentar Google primero
        if (useGoogleServices) {
            try {
                fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient?.lastLocation?.addOnSuccessListener { loc ->
                    loc?.let {
                        Log.d("LocationProvider", "LastKnown Google: ${it.latitude}")
                    }
                }
            } catch (e: Exception) {
                Log.w("LocationProvider", "LastKnown Google falló, usando nativo")
            }
        }

        // Nativo como respaldo
        try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            for (provider in providers) {
                if (!locationManager.isProviderEnabled(provider)) continue
                val loc = locationManager.getLastKnownLocation(provider)
                if (loc != null) {
                    if (best == null || loc.accuracy < best.accuracy) {
                        best = loc
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("LocationProvider", "Error lastKnown nativo: ${e.message}")
        }

        return best
    }
}