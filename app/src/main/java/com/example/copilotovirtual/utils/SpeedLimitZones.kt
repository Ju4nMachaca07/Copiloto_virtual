// utils/SpeedLimitZones.kt
package com.example.copilotovirtual.utils

import com.google.android.gms.maps.model.LatLng

data class SpeedLimitZone(
    val id: String,
    val name: String,
    val center: LatLng,
    val radius: Double, // metros
    val speedLimit: Int // km/h
)

object SpeedLimitZones {

    val zones = listOf(
        // Marcona - Zona urbana
        SpeedLimitZone(
            id = "marcona_urban",
            name = "Zona Urbana Marcona",
            center = LatLng(-15.3500, -75.1100),
            radius = 3000.0,
            speedLimit = 40
        ),

        // Nazca - Zona urbana
        SpeedLimitZone(
            id = "nazca_urban",
            name = "Zona Urbana Nazca",
            center = LatLng(-14.8309, -74.9278),
            radius = 5000.0,
            speedLimit = 40
        ),

        // Mina Justa - Zona industrial
        SpeedLimitZone(
            id = "mina_justa_industrial",
            name = "Zona Industrial Mina Justa",
            center = LatLng(-15.4000, -75.2000),
            radius = 2000.0,
            speedLimit = 30
        ),

        // Panamericana Sur - Tramo 1
        SpeedLimitZone(
            id = "panamericana_1",
            name = "Panamericana Sur - Tramo 1",
            center = LatLng(-15.2000, -75.0500),
            radius = 10000.0,
            speedLimit = 90
        ),

        // Panamericana Sur - Tramo 2
        SpeedLimitZone(
            id = "panamericana_2",
            name = "Panamericana Sur - Tramo 2",
            center = LatLng(-15.0000, -74.9500),
            radius = 10000.0,
            speedLimit = 90
        )
    )

    fun getZoneAt(location: LatLng): SpeedLimitZone? {
        return zones.firstOrNull { zone ->
            MapUtils.calculateDistance(location, zone.center) <= zone.radius
        }
    }
}