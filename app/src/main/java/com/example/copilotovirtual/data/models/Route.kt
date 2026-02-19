// data/models/Route.kt
package com.example.copilotovirtual.data.models

import android.util.Log
import com.google.android.gms.maps.model.LatLng

data class Route(
    val id: String,
    val name: String,
    val description: String = "",
    val startCityId: String,
    val endCityId: String,
    val waypoints: List<LatLng>,
    val distance: Double,
    val estimatedTime: Long,
    val tollRoads: Boolean = false,
    val highways: Boolean = true
) {
    companion object {
        // Fallback routes solo para cuando no hay internet ni descarga
        val fallbackRoutes = listOf(

            // ==========================================
            // MARCONA ↔ NAZCA
            // ==========================================
            Route(
                id = "fallback_marcona_nazca",
                name = "Marcona → Nazca",
                description = "Ruta aproximada (sin internet)",
                startCityId = "marcona",
                endCityId = "nazca",
                waypoints = listOf(
                    LatLng(-15.3500, -75.1100),
                    LatLng(-14.8309, -74.9278)
                ),
                distance = 135000.0,
                estimatedTime = 6300
            ),
            Route(
                id = "fallback_nazca_marcona",
                name = "Nazca → Marcona",
                description = "Ruta aproximada (sin internet)",
                startCityId = "nazca",
                endCityId = "marcona",
                waypoints = listOf(
                    LatLng(-14.8309, -74.9278),
                    LatLng(-15.3500, -75.1100)
                ),
                distance = 135000.0,
                estimatedTime = 6300
            ),

            // ==========================================
            // MARCONA ↔ AREQUIPA
            // ==========================================
            Route(
                id = "fallback_marcona_arequipa",
                name = "Marcona → Arequipa",
                description = "Ruta aproximada (sin internet)",
                startCityId = "marcona",
                endCityId = "arequipa",
                waypoints = listOf(
                    LatLng(-15.3500, -75.1100),
                    LatLng(-16.4090, -71.5375)
                ),
                distance = 620000.0,
                estimatedTime = 27000
            ),
            Route(
                id = "fallback_arequipa_marcona",
                name = "Arequipa → Marcona",
                description = "Ruta aproximada (sin internet)",
                startCityId = "arequipa",
                endCityId = "marcona",
                waypoints = listOf(
                    LatLng(-16.4090, -71.5375),
                    LatLng(-15.3500, -75.1100)
                ),
                distance = 620000.0,
                estimatedTime = 27000
            ),

            // ==========================================
            // MARCONA ↔ GARITA MINA JUSTA
            // ==========================================
            Route(
                id = "fallback_marcona_garita",
                name = "Marcona → Garita Mina Justa",
                description = "Ruta aproximada (sin internet)",
                startCityId = "marcona",
                endCityId = "garita_mina_justa",
                waypoints = listOf(
                    LatLng(-15.3500, -75.1100),
                    LatLng(-15.3947, -75.1789)
                ),
                distance = 12000.0,
                estimatedTime = 900
            ),
            Route(
                id = "fallback_garita_marcona",
                name = "Garita Mina Justa → Marcona",
                description = "Ruta aproximada (sin internet)",
                startCityId = "garita_mina_justa",
                endCityId = "marcona",
                waypoints = listOf(
                    LatLng(-15.3947, -75.1789),
                    LatLng(-15.3500, -75.1100)
                ),
                distance = 12000.0,
                estimatedTime = 900
            ),

            // ==========================================
            // NAZCA ↔ AREQUIPA
            // ==========================================
            Route(
                id = "fallback_nazca_arequipa",
                name = "Nazca → Arequipa",
                description = "Ruta aproximada (sin internet)",
                startCityId = "nazca",
                endCityId = "arequipa",
                waypoints = listOf(
                    LatLng(-14.8309, -74.9278),
                    LatLng(-16.4090, -71.5375)
                ),
                distance = 565000.0,
                estimatedTime = 25200
            ),
            Route(
                id = "fallback_arequipa_nazca",
                name = "Arequipa → Nazca",
                description = "Ruta aproximada (sin internet)",
                startCityId = "arequipa",
                endCityId = "nazca",
                waypoints = listOf(
                    LatLng(-16.4090, -71.5375),
                    LatLng(-14.8309, -74.9278)
                ),
                distance = 565000.0,
                estimatedTime = 25200
            ),

            // ==========================================
            // NAZCA ↔ GARITA MINA JUSTA
            // ==========================================
            Route(
                id = "fallback_nazca_garita",
                name = "Nazca → Garita Mina Justa",
                description = "Ruta aproximada (sin internet)",
                startCityId = "nazca",
                endCityId = "garita_mina_justa",
                waypoints = listOf(
                    LatLng(-14.8309, -74.9278),
                    LatLng(-15.3947, -75.1789)
                ),
                distance = 148000.0,
                estimatedTime = 7200
            ),
            Route(
                id = "fallback_garita_nazca",
                name = "Garita Mina Justa → Nazca",
                description = "Ruta aproximada (sin internet)",
                startCityId = "garita_mina_justa",
                endCityId = "nazca",
                waypoints = listOf(
                    LatLng(-15.3947, -75.1789),
                    LatLng(-14.8309, -74.9278)
                ),
                distance = 148000.0,
                estimatedTime = 7200
            ),

            // ==========================================
            // AREQUIPA ↔ GARITA MINA JUSTA
            // ==========================================
            Route(
                id = "fallback_arequipa_garita",
                name = "Arequipa → Garita Mina Justa",
                description = "Ruta aproximada (sin internet)",
                startCityId = "arequipa",
                endCityId = "garita_mina_justa",
                waypoints = listOf(
                    LatLng(-16.4090, -71.5375),
                    LatLng(-15.3947, -75.1789)
                ),
                distance = 632000.0,
                estimatedTime = 28800
            ),
            Route(
                id = "fallback_garita_arequipa",
                name = "Garita Mina Justa → Arequipa",
                description = "Ruta aproximada (sin internet)",
                startCityId = "garita_mina_justa",
                endCityId = "arequipa",
                waypoints = listOf(
                    LatLng(-15.3947, -75.1789),
                    LatLng(-16.4090, -71.5375)
                ),
                distance = 632000.0,
                estimatedTime = 28800
            )
        )

        fun getRouteById(id: String): Route? =
            fallbackRoutes.find { it.id == id }

        fun getFallbackRoutes(
            originCityId: String,
            destCityId: String
        ): List<Route> = fallbackRoutes.filter {
            it.startCityId == originCityId &&
                    it.endCityId == destCityId
        }

        fun getRoutesBetween(originId: String, destId: String): List<Route> {
            return emptyList()
        }
    }
}