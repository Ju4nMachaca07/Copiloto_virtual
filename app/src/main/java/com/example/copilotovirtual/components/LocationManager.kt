package com.example.copilotovirtual.Components

import org.osmdroid.util.GeoPoint
import kotlin.math.pow

object LocationManager {

    // Coordenadas exactas
    data class LocationPoint(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val description: String = ""
    )

    // Tus puntos específicos
    val MARCONA = LocationPoint(
        name = "Marcona",
        latitude = -15.3619,
        longitude = -75.1653,
        description = "Ciudad y puerto minero"
    )

    val MINA_JUSTA = LocationPoint(
        name = "Mina Justa",
        latitude = -15.2833,
        longitude = -75.1000,
        description = "Proyecto minero de cobre"
    )

    val NAZCA = LocationPoint(
        name = "Nazca",
        latitude = -14.8350,
        longitude = -74.9386,
        description = "Ciudad famosa por las Líneas"
    )

    // Centro del área (para vista general)
    val AREA_CENTER = GeoPoint(
        (-15.3619 + -14.8350) / 2,  // Latitud promedio
        (-75.1653 + -74.9386) / 2   // Longitud promedio
    )

    // Todos los puntos en una lista
    val ALL_POINTS = listOf(MARCONA, MINA_JUSTA, NAZCA)

    // Distancia aproximada entre puntos (en km)
    fun calculateDistance(point1: LocationPoint, point2: LocationPoint): Double {
        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dlon = lon2 - lon1
        val dlat = lat2 - lat1

        val a = Math.sin(dlat / 2).pow(2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dlon / 2).pow(2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return 6371 * c  // Radio de la Tierra en km
    }

    // Distancias entre tus puntos
    val distanciaMarconaMinaJusta = calculateDistance(MARCONA, MINA_JUSTA)
    val distanciaMinaJustaNazca = calculateDistance(MINA_JUSTA, NAZCA)
    val distanciaTotal = calculateDistance(MARCONA, NAZCA)
}