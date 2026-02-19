// data/models/City.kt
package com.example.copilotovirtual.data.models

import com.google.android.gms.maps.model.LatLng

data class City(
    val id: String,
    val name: String,
    val coordinates: LatLng,  // Entrada/salida principal por carretera
    val description: String = "",
    val isMiningArea: Boolean = false
) {
    companion object {
        val cities = listOf(
            City(
                id = "arequipa",
                name = "Arequipa",
                coordinates = LatLng(-16.3988, -71.5350), // Salida norte hacia Panamericana
                description = "Salida norte hacia carretera",
                isMiningArea = false
            ),
            City(
                id = "nazca",
                name = "Nazca",
                coordinates = LatLng(-14.8389, -74.9383), // Entrada sur por Panamericana
                description = "Entrada sur desde Panamericana",
                isMiningArea = false
            ),
            City(
                id = "marcona",
                name = "Marcona",
                coordinates = LatLng(-15.3580, -75.1020), // Entrada desde Panamericana Sur
                description = "Acceso desde Panamericana Sur",
                isMiningArea = true
            ),
            City(
                id = "garita_mina_justa",
                name = "Garita Mina Justa",
                coordinates = LatLng(-15.3947, -75.1789), // Garita de control de acceso
                description = "Control de ingreso a operaciones",
                isMiningArea = true
            )
        )

        fun getCityById(id: String) = cities.find { it.id == id }
        fun getMiningCities() = cities.filter { it.isMiningArea }
    }
}