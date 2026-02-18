package com.example.copilotovirtual.data.models

import com.google.android.gms.maps.model.LatLng

data class RouteWaypoint(
    val latitude: Double,
    val longitude: Double,
    val instruction: String = "",
    val maneuver: String = "",
    val distanceToNext: Double = 0.0,
    val durationToNext: Long = 0,
    val streetName: String = ""
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}