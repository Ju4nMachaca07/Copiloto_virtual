package com.example.copilotovirtual.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "speed_limits")
data class SpeedLimitZone(
    @PrimaryKey val id: String,
    val name: String,
    val center: LatLng,
    val radius: Double, // en metros
    val speedLimit: Int, // km/h
    val zoneType: ZoneType
)

enum class ZoneType {
    URBAN,          // Zona urbana (30-50 km/h)
    RURAL,          // Carretera rural (80-90 km/h)
    HIGHWAY,        // Autopista (100-120 km/h)
    SCHOOL,         // Zona escolar (20-30 km/h)
    RESIDENTIAL     // Zona residencial (30-40 km/h)
}

// models/NavigationInstruction.kt
data class NavigationInstruction(
    val text: String,
    val spokenText: String, // Texto optimizado para TTS
    val distance: Double, // en metros
    val duration: Long, // en segundos
    val maneuver: ManeuverType,
    val location: LatLng,
    val streetName: String? = null
)

enum class ManeuverType {
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    TURN_SLIGHT_LEFT,
    TURN_SLIGHT_RIGHT,
    TURN_SHARP_LEFT,
    TURN_SHARP_RIGHT,
    UTURN_LEFT,
    UTURN_RIGHT,
    RAMP_LEFT,
    RAMP_RIGHT,
    MERGE,
    FORK_LEFT,
    FORK_RIGHT,
    ROUNDABOUT_LEFT,
    ROUNDABOUT_RIGHT,
    ARRIVE
}

// models/RouteAlternative.kt
data class RouteAlternative(
    val id: String,
    val name: String,
    val polylinePoints: List<LatLng>,
    val distance: Double,
    val duration: Long,
    val trafficInfo: TrafficInfo,
    val instructions: List<NavigationInstruction>,
    val tollRoads: Boolean = false,
    val highways: Boolean = false
)

data class TrafficInfo(
    val status: TrafficStatus,
    val delayMinutes: Long = 0
)

enum class TrafficStatus {
    CLEAR,      // Tráfico fluido
    LIGHT,      // Tráfico ligero
    MODERATE,   // Tráfico moderado
    HEAVY,      // Tráfico pesado
    SEVERE      // Tráfico muy pesado
}