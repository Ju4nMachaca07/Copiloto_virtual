package com.example.copilotovirtual.data.network

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    val routes: List<DirectionsRoute>,
    val status: String
)

data class DirectionsRoute(
    val legs: List<RouteLeg>,
    @SerializedName("overview_polyline")
    val overviewPolyline: PolylineData
)

data class RouteLeg(
    val distance: Distance,
    val duration: Duration,
    val steps: List<RouteStep>
)

data class RouteStep(
    val distance: Distance,
    val duration: Duration,
    @SerializedName("start_location")
    val startLocation: LocationPoint,
    @SerializedName("end_location")
    val endLocation: LocationPoint,
    @SerializedName("html_instructions")
    val htmlInstructions: String,
    val polyline: PolylineData
)

data class Distance(
    val text: String,
    val value: Int // en metros
)

data class Duration(
    val text: String,
    val value: Int // en segundos
)

data class LocationPoint(
    val lat: Double,
    val lng: Double
)

data class PolylineData(
    val points: String
)