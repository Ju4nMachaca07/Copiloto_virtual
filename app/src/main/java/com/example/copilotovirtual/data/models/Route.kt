package com.example.copilotovirtual.data.models

import com.google.android.gms.maps.model.LatLng

data class Route(
    val id: String,
    val name: String,
    val startCityId: String,
    val endCityId: String,
    val waypoints: List<LatLng>,
    val distance: Double,
    val estimatedTime: Double,
    val description: String = ""
) {
    companion object {
        private val cityCoordinates = mapOf(
            "marcona" to LatLng(-15.3500, -75.1100),
            "mina_justa" to LatLng(-15.4000, -75.2000),
            "nazca" to LatLng(-14.8309, -74.9278),
            "arequipa" to LatLng(-16.4090, -71.5375)
        )

        val routes = listOf(
            // ========== MARCONA → MINA JUSTA (15.5 km) ==========
            Route(
                id = "marcona_mina_justa",
                name = "Marcona → Mina Justa",
                startCityId = "marcona",
                endCityId = "mina_justa",
                waypoints = listOf(
                    LatLng(-15.3500, -75.1100), // Marcona
                    LatLng(-15.3600, -75.1300), // Km 2
                    LatLng(-15.3700, -75.1500), // Km 5
                    LatLng(-15.3800, -75.1700), // Km 8
                    LatLng(-15.3900, -75.1900), // Km 12
                    LatLng(-15.4000, -75.2000)  // Mina Justa
                ),
                distance = 15.5,
                estimatedTime = 0.5,
                description = "Ruta minera directa"
            ),

            // ========== MARCONA → NAZCA (85 km - Panamericana Sur) ==========
            Route(
                id = "marcona_nazca",
                name = "Marcona → Nazca",
                startCityId = "marcona",
                endCityId = "nazca",
                waypoints = listOf(
                    LatLng(-15.3500, -75.1100), // Marcona - Puerto
                    LatLng(-15.3400, -75.0900), // Salida hacia carretera
                    LatLng(-15.3200, -75.0600), // Km 5 - Empalme
                    LatLng(-15.2900, -75.0400), // Km 10
                    LatLng(-15.2600, -75.0200), // Km 15
                    LatLng(-15.2300, -75.0100), // Km 20
                    LatLng(-15.2000, -75.0000), // Km 25
                    LatLng(-15.1700, -74.9850), // Km 30
                    LatLng(-15.1400, -74.9750), // Km 35
                    LatLng(-15.1100, -74.9650), // Km 40
                    LatLng(-15.0800, -74.9550), // Km 45
                    LatLng(-15.0500, -74.9500), // Km 50
                    LatLng(-15.0200, -74.9450), // Km 55
                    LatLng(-14.9900, -74.9400), // Km 60
                    LatLng(-14.9600, -74.9350), // Km 65
                    LatLng(-14.9300, -74.9320), // Km 70
                    LatLng(-14.9000, -74.9300), // Km 75
                    LatLng(-14.8700, -74.9290), // Km 80 - Entrada Nazca
                    LatLng(-14.8500, -74.9285), // Av. Principal
                    LatLng(-14.8309, -74.9278)  // Nazca - Centro
                ),
                distance = 85.0,
                estimatedTime = 1.5,
                description = "Por Panamericana Sur"
            ),

            // ========== MARCONA → AREQUIPA (320 km) ==========
            Route(
                id = "marcona_arequipa",
                name = "Marcona → Arequipa",
                startCityId = "marcona",
                endCityId = "arequipa",
                waypoints = listOf(
                    LatLng(-15.3500, -75.1100), // Marcona
                    LatLng(-15.4000, -74.9000), // Km 20 - Interior
                    LatLng(-15.4500, -74.7000), // Km 40
                    LatLng(-15.5000, -74.5000), // Km 60
                    LatLng(-15.5500, -74.3000), // Km 80
                    LatLng(-15.6000, -74.1000), // Km 100
                    LatLng(-15.6500, -73.9000), // Km 120
                    LatLng(-15.7000, -73.7000), // Km 140
                    LatLng(-15.7500, -73.5000), // Km 160
                    LatLng(-15.8000, -73.3000), // Km 180
                    LatLng(-15.8500, -73.1000), // Km 200
                    LatLng(-15.9000, -72.9000), // Km 220
                    LatLng(-15.9500, -72.7000), // Km 240
                    LatLng(-16.0000, -72.5000), // Km 260
                    LatLng(-16.0500, -72.3000), // Km 280
                    LatLng(-16.1500, -72.0000), // Km 300
                    LatLng(-16.2500, -71.8000), // Km 310
                    LatLng(-16.3500, -71.6500), // Entrada Arequipa
                    LatLng(-16.4090, -71.5375)  // Arequipa - Centro
                ),
                distance = 320.0,
                estimatedTime = 5.0,
                description = "Ruta de montaña"
            ),

            // ========== MINA JUSTA → MARCONA ==========
            Route(
                id = "mina_justa_marcona",
                name = "Mina Justa → Marcona",
                startCityId = "mina_justa",
                endCityId = "marcona",
                waypoints = listOf(
                    LatLng(-15.4000, -75.2000), // Mina Justa
                    LatLng(-15.3900, -75.1900),
                    LatLng(-15.3800, -75.1700),
                    LatLng(-15.3700, -75.1500),
                    LatLng(-15.3600, -75.1300),
                    LatLng(-15.3500, -75.1100)  // Marcona
                ),
                distance = 15.5,
                estimatedTime = 0.5,
                description = "Retorno a Marcona"
            ),

            // ========== MINA JUSTA → NAZCA ==========
            Route(
                id = "mina_justa_nazca",
                name = "Mina Justa → Nazca",
                startCityId = "mina_justa",
                endCityId = "nazca",
                waypoints = listOf(
                    LatLng(-15.4000, -75.2000), // Mina Justa
                    LatLng(-15.3700, -75.1500), // Km 5
                    LatLng(-15.3400, -75.1000), // Km 10
                    LatLng(-15.3000, -75.0600), // Km 15
                    LatLng(-15.2600, -75.0300), // Km 20
                    LatLng(-15.2200, -75.0100), // Km 25
                    LatLng(-15.1800, -74.9900), // Km 30
                    LatLng(-15.1400, -74.9750), // Km 35
                    LatLng(-15.1000, -74.9650), // Km 40
                    LatLng(-15.0600, -74.9550), // Km 45
                    LatLng(-15.0200, -74.9450), // Km 50
                    LatLng(-14.9800, -74.9380), // Km 55
                    LatLng(-14.9400, -74.9330), // Km 60
                    LatLng(-14.9000, -74.9300), // Km 65
                    LatLng(-14.8600, -74.9290), // Km 70
                    LatLng(-14.8309, -74.9278)  // Nazca
                ),
                distance = 95.0,
                estimatedTime = 1.7,
                description = "Ruta alternativa"
            ),

            // ========== MINA JUSTA → AREQUIPA ==========
            Route(
                id = "mina_justa_arequipa",
                name = "Mina Justa → Arequipa",
                startCityId = "mina_justa",
                endCityId = "arequipa",
                waypoints = listOf(
                    LatLng(-15.4000, -75.2000), // Mina Justa
                    LatLng(-15.4500, -74.9500), // Km 20
                    LatLng(-15.5000, -74.7000), // Km 40
                    LatLng(-15.5500, -74.5000), // Km 60
                    LatLng(-15.6000, -74.3000), // Km 80
                    LatLng(-15.6500, -74.1000), // Km 100
                    LatLng(-15.7000, -73.9000), // Km 120
                    LatLng(-15.7500, -73.7000), // Km 140
                    LatLng(-15.8000, -73.5000), // Km 160
                    LatLng(-15.8500, -73.3000), // Km 180
                    LatLng(-15.9000, -73.1000), // Km 200
                    LatLng(-15.9500, -72.9000), // Km 220
                    LatLng(-16.0000, -72.7000), // Km 240
                    LatLng(-16.1000, -72.4000), // Km 260
                    LatLng(-16.2000, -72.1000), // Km 280
                    LatLng(-16.3000, -71.8000), // Km 300
                    LatLng(-16.3700, -71.6500), // Km 320
                    LatLng(-16.4090, -71.5375)  // Arequipa
                ),
                distance = 330.0,
                estimatedTime = 5.2,
                description = "Via Marcona"
            ),

            // ========== NAZCA → MARCONA ==========
            Route(
                id = "nazca_marcona",
                name = "Nazca → Marcona",
                startCityId = "nazca",
                endCityId = "marcona",
                waypoints = listOf(
                    LatLng(-14.8309, -74.9278), // Nazca
                    LatLng(-14.8500, -74.9285),
                    LatLng(-14.8700, -74.9290),
                    LatLng(-14.9000, -74.9300),
                    LatLng(-14.9300, -74.9320),
                    LatLng(-14.9600, -74.9350),
                    LatLng(-14.9900, -74.9400),
                    LatLng(-15.0200, -74.9450),
                    LatLng(-15.0500, -74.9500),
                    LatLng(-15.0800, -74.9550),
                    LatLng(-15.1100, -74.9650),
                    LatLng(-15.1400, -74.9750),
                    LatLng(-15.1700, -74.9850),
                    LatLng(-15.2000, -75.0000),
                    LatLng(-15.2300, -75.0100),
                    LatLng(-15.2600, -75.0200),
                    LatLng(-15.2900, -75.0400),
                    LatLng(-15.3200, -75.0600),
                    LatLng(-15.3400, -75.0900),
                    LatLng(-15.3500, -75.1100)  // Marcona
                ),
                distance = 85.0,
                estimatedTime = 1.5,
                description = "Por Panamericana Sur"
            ),

            // ========== NAZCA → MINA JUSTA ==========
            Route(
                id = "nazca_mina_justa",
                name = "Nazca → Mina Justa",
                startCityId = "nazca",
                endCityId = "mina_justa",
                waypoints = listOf(
                    LatLng(-14.8309, -74.9278), // Nazca
                    LatLng(-14.8600, -74.9290),
                    LatLng(-14.9000, -74.9300),
                    LatLng(-14.9400, -74.9330),
                    LatLng(-14.9800, -74.9380),
                    LatLng(-15.0200, -74.9450),
                    LatLng(-15.0600, -74.9550),
                    LatLng(-15.1000, -74.9650),
                    LatLng(-15.1400, -74.9750),
                    LatLng(-15.1800, -74.9900),
                    LatLng(-15.2200, -75.0100),
                    LatLng(-15.2600, -75.0300),
                    LatLng(-15.3000, -75.0600),
                    LatLng(-15.3400, -75.1000),
                    LatLng(-15.3700, -75.1500),
                    LatLng(-15.4000, -75.2000)  // Mina Justa
                ),
                distance = 95.0,
                estimatedTime = 1.7,
                description = "Directo a la mina"
            ),

            // ========== NAZCA → AREQUIPA ==========
            Route(
                id = "nazca_arequipa",
                name = "Nazca → Arequipa",
                startCityId = "nazca",
                endCityId = "arequipa",
                waypoints = listOf(
                    LatLng(-14.8309, -74.9278), // Nazca
                    LatLng(-14.9000, -74.8500), // Km 20
                    LatLng(-15.0000, -74.7000), // Km 40
                    LatLng(-15.1000, -74.6000), // Km 60
                    LatLng(-15.2000, -74.5000), // Km 80
                    LatLng(-15.3000, -74.4000), // Km 100
                    LatLng(-15.4000, -74.2000), // Km 120
                    LatLng(-15.5000, -74.0000), // Km 140
                    LatLng(-15.6000, -73.8000), // Km 160
                    LatLng(-15.7000, -73.5000), // Km 180
                    LatLng(-15.8000, -73.2000), // Km 200
                    LatLng(-15.9000, -72.9000), // Km 220
                    LatLng(-16.0000, -72.6000), // Km 240
                    LatLng(-16.1000, -72.3000), // Km 260
                    LatLng(-16.2000, -72.0000), // Km 280
                    LatLng(-16.3000, -71.7500), // Km 300
                    LatLng(-16.4090, -71.5375)  // Arequipa
                ),
                distance = 450.0,
                estimatedTime = 6.0,
                description = "Ruta larga"
            ),

            // ========== AREQUIPA → MARCONA ==========
            Route(
                id = "arequipa_marcona",
                name = "Arequipa → Marcona",
                startCityId = "arequipa",
                endCityId = "marcona",
                waypoints = listOf(
                    LatLng(-16.4090, -71.5375), // Arequipa
                    LatLng(-16.3500, -71.6500),
                    LatLng(-16.2500, -71.8000),
                    LatLng(-16.1500, -72.0000),
                    LatLng(-16.0500, -72.3000),
                    LatLng(-16.0000, -72.5000),
                    LatLng(-15.9500, -72.7000),
                    LatLng(-15.9000, -72.9000),
                    LatLng(-15.8500, -73.1000),
                    LatLng(-15.8000, -73.3000),
                    LatLng(-15.7500, -73.5000),
                    LatLng(-15.7000, -73.7000),
                    LatLng(-15.6500, -73.9000),
                    LatLng(-15.6000, -74.1000),
                    LatLng(-15.5500, -74.3000),
                    LatLng(-15.5000, -74.5000),
                    LatLng(-15.4500, -74.7000),
                    LatLng(-15.4000, -74.9000),
                    LatLng(-15.3500, -75.1100)  // Marcona
                ),
                distance = 320.0,
                estimatedTime = 5.0,
                description = "Hacia la costa"
            ),

            // ========== AREQUIPA → MINA JUSTA ==========
            Route(
                id = "arequipa_mina_justa",
                name = "Arequipa → Mina Justa",
                startCityId = "arequipa",
                endCityId = "mina_justa",
                waypoints = listOf(
                    LatLng(-16.4090, -71.5375), // Arequipa
                    LatLng(-16.3700, -71.6500),
                    LatLng(-16.3000, -71.8000),
                    LatLng(-16.2000, -72.1000),
                    LatLng(-16.1000, -72.4000),
                    LatLng(-16.0000, -72.7000),
                    LatLng(-15.9500, -72.9000),
                    LatLng(-15.9000, -73.1000),
                    LatLng(-15.8500, -73.3000),
                    LatLng(-15.8000, -73.5000),
                    LatLng(-15.7500, -73.7000),
                    LatLng(-15.7000, -73.9000),
                    LatLng(-15.6500, -74.1000),
                    LatLng(-15.6000, -74.3000),
                    LatLng(-15.5500, -74.5000),
                    LatLng(-15.5000, -74.7000),
                    LatLng(-15.4500, -74.9500),
                    LatLng(-15.4000, -75.2000)  // Mina Justa
                ),
                distance = 330.0,
                estimatedTime = 5.2,
                description = "Via Marcona"
            ),

            // ========== AREQUIPA → NAZCA ==========
            Route(
                id = "arequipa_nazca",
                name = "Arequipa → Nazca",
                startCityId = "arequipa",
                endCityId = "nazca",
                waypoints = listOf(
                    LatLng(-16.4090, -71.5375), // Arequipa
                    LatLng(-16.3000, -71.7500),
                    LatLng(-16.2000, -72.0000),
                    LatLng(-16.1000, -72.3000),
                    LatLng(-16.0000, -72.6000),
                    LatLng(-15.9000, -72.9000),
                    LatLng(-15.8000, -73.2000),
                    LatLng(-15.7000, -73.5000),
                    LatLng(-15.6000, -73.8000),
                    LatLng(-15.5000, -74.0000),
                    LatLng(-15.4000, -74.2000),
                    LatLng(-15.3000, -74.4000),
                    LatLng(-15.2000, -74.5000),
                    LatLng(-15.1000, -74.6000),
                    LatLng(-15.0000, -74.7000),
                    LatLng(-14.9000, -74.8500),
                    LatLng(-14.8309, -74.9278)  // Nazca
                ),
                distance = 450.0,
                estimatedTime = 6.0,
                description = "Ruta panoramica"
            )
        )

        fun getRouteById(id: String): Route? = routes.find { it.id == id }

        fun getRoutesFromCity(cityId: String): List<Route> =
            routes.filter { it.startCityId == cityId }

        fun getCityCoordinates(cityId: String): LatLng? = cityCoordinates[cityId]
    }
}