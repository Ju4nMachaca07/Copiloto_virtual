// data/models/City.kt
package com.example.copilotovirtual.data.models

import com.google.android.gms.maps.model.LatLng

data class City(
    val id: String,
    val name: String,
    val coordinates: LatLng,
    val description: String = "",
    val isMiningArea: Boolean = false
) {
    companion object {
        val cities = listOf(
            City(
                id = "arequipa",
                name = "Arequipa",
                coordinates = LatLng(-16.4090, -71.5375),
                description = "Ciudad principal del sur",
                isMiningArea = false
            ),
            City(
                id = "nazca",
                name = "Nazca",
                coordinates = LatLng(-14.8309, -74.9278),
                description = "Centro de servicios y logística",
                isMiningArea = false
            ),
            City(
                id = "marcona",
                name = "Marcona",
                coordinates = LatLng(-15.3500, -75.1100),
                description = "Zona de extracción de hierro",
                isMiningArea = true
            ),
            City(
                id = "garita_mina_justa",
                name = "Garita Mina Justa",
                coordinates = LatLng(-15.3947, -75.1789),
                description = "Control de ingreso Mina Justa",
                isMiningArea = true
            )
        )

        fun getCityById(id: String) = cities.find { it.id == id }
        fun getMiningCities() = cities.filter { it.isMiningArea }
    }
}