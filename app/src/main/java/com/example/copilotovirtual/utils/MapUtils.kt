package com.example.copilotovirtual.utils

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlin.math.*

object MapUtils {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calcular distancia entre dos puntos usando fórmula de Haversine
     * @return distancia en metros
     */
    fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calcular distancia usando Location (alternativa)
     */
    fun calculateDistanceSimple(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude,
            point1.longitude,
            point2.latitude,
            point2.longitude,
            results
        )
        return results[0]
    }

    /**
     * Formatear distancia en metros a texto legible
     */
    @SuppressLint("DefaultLocale")
    fun formatDistance(meters: Double): String {
        return when {
            meters < 1000 -> "${meters.toInt()} m"
            meters < 10000 -> String.format("%.1f km", meters / 1000)
            else -> String.format("%.0f km", meters / 1000)
        }
    }

    /**
     * Formatear duración en horas a texto legible
     */
    @SuppressLint("DefaultLocale")
    fun formatDuration(hours: Double): String {
        val totalMinutes = (hours * 60).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60

        return when {
            h == 0 -> "${m} min"
            m == 0 -> "${h} h"
            else -> "${h} h ${m} min"
        }
    }

    /**
     * Calcular bearing (dirección) entre dos puntos
     * @return ángulo en grados (0-360)
     */
    fun calculateBearing(start: LatLng, end: LatLng): Double {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dLon = lon2 - lon1

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }

    /**
     * Convertir bearing a dirección cardinal
     */
    fun bearingToDirection(bearing: Double): String {
        return when {
            bearing < 22.5 || bearing >= 337.5 -> "norte"
            bearing < 67.5 -> "noreste"
            bearing < 112.5 -> "este"
            bearing < 157.5 -> "sureste"
            bearing < 202.5 -> "sur"
            bearing < 247.5 -> "suroeste"
            bearing < 292.5 -> "oeste"
            else -> "noroeste"
        }
    }

    /**
     * Calcular punto medio entre dos coordenadas
     */
    fun getMidpoint(point1: LatLng, point2: LatLng): LatLng {
        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dLon = lon2 - lon1

        val bx = cos(lat2) * cos(dLon)
        val by = cos(lat2) * sin(dLon)

        val lat3 = atan2(
            sin(lat1) + sin(lat2),
            sqrt((cos(lat1) + bx).pow(2) + by.pow(2))
        )
        val lon3 = lon1 + atan2(by, cos(lat1) + bx)

        return LatLng(Math.toDegrees(lat3), Math.toDegrees(lon3))
    }

    /**
     * Estimar tiempo de viaje
     * @param distanceMeters distancia en metros
     * @param avgSpeedKmh velocidad promedio en km/h
     * @return tiempo en horas
     */
    fun estimateTravelTime(
        distanceMeters: Double,
        avgSpeedKmh: Double = 60.0
    ): Double {
        val distanceKm = distanceMeters / 1000.0
        return distanceKm / avgSpeedKmh
    }

    /**
     * Obtener límites geográficos de una lista de puntos
     */
    fun getRouteBounds(waypoints: List<LatLng>): LatLngBounds? {
        if (waypoints.isEmpty()) return null

        val builder = LatLngBounds.Builder()
        waypoints.forEach { point ->
            builder.include(point)
        }
        return builder.build()
    }

    /**
     * Generar puntos intermedios entre dos coordenadas
     */
    fun interpolatePoints(
        start: LatLng,
        end: LatLng,
        numPoints: Int = 10
    ): List<LatLng> {
        val points = mutableListOf<LatLng>()

        for (i in 0..numPoints) {
            val fraction = i.toDouble() / numPoints
            val lat = start.latitude + (end.latitude - start.latitude) * fraction
            val lng = start.longitude + (end.longitude - start.longitude) * fraction
            points.add(LatLng(lat, lng))
        }

        return points
    }
}