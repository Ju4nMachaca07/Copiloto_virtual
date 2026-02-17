// data/network/DirectionsModels.kt
package com.example.copilotovirtual.data.network

import com.google.gson.annotations.SerializedName

// Modelos para la respuesta de Google Directions API
data class DirectionsResponse(
    @SerializedName("routes") val routes: List<DirectionRoute>,
    @SerializedName("status") val status: String
)

data class DirectionRoute(
    @SerializedName("summary") val summary: String,
    @SerializedName("legs") val legs: List<RouteLeg>,
    @SerializedName("overview_polyline") val overviewPolyline: Polyline,
    @SerializedName("warnings") val warnings: List<String> = emptyList(),
    @SerializedName("waypoint_order") val waypointOrder: List<Int> = emptyList()
)

data class RouteLeg(
    @SerializedName("distance") val distance: Distance,
    @SerializedName("duration") val duration: Duration,
    @SerializedName("start_address") val startAddress: String,
    @SerializedName("end_address") val endAddress: String,
    @SerializedName("start_location") val startLocation: Location,
    @SerializedName("end_location") val endLocation: Location,
    @SerializedName("steps") val steps: List<RouteStep>
)

data class RouteStep(
    @SerializedName("distance") val distance: Distance,
    @SerializedName("duration") val duration: Duration,
    @SerializedName("start_location") val startLocation: Location,
    @SerializedName("end_location") val endLocation: Location,
    @SerializedName("html_instructions") val htmlInstructions: String,
    @SerializedName("maneuver") val maneuver: String? = null,
    @SerializedName("polyline") val polyline: Polyline
)

data class Distance(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int // en metros
)

data class Duration(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int // en segundos
)

data class Location(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class Polyline(
    @SerializedName("points") val points: String
)