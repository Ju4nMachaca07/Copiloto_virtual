// data/network/DirectionsApiService.kt
package com.example.copilotovirtual.data.network

import android.content.Context
import android.util.Log
import com.example.copilotovirtual.data.models.RouteWaypoint
import com.example.copilotovirtual.utils.NetworkUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DirectionsApiService(private val context: Context) {

    private val apiKey = "AIzaSyDOGsRxVnHG5oWJGZMbsabAWreCA3Pebqg"

    suspend fun getDirectionsWithInstructions(
        origin: LatLng,
        destination: LatLng,
        alternatives: Boolean = true
    ): List<DirectionsRouteWithInstructions> = withContext(Dispatchers.IO) {

        if (!NetworkUtils.isConnected(context)) {
            return@withContext emptyList()
        }

        try {
            val originStr = "${origin.latitude},${origin.longitude}"
            val destStr = "${destination.latitude},${destination.longitude}"

            val urlString = buildString {
                append("https://maps.googleapis.com/maps/api/directions/json?")
                append("origin=${URLEncoder.encode(originStr, "UTF-8")}")
                append("&destination=${URLEncoder.encode(destStr, "UTF-8")}")
                append("&alternatives=$alternatives")
                append("&mode=driving")
                append("&language=es")
                append("&units=metric")
                append("&key=$apiKey")
            }

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseDirectionsWithInstructions(response)
            } else {
                Log.e("DirectionsAPI", "Error HTTP: ${connection.responseCode}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("DirectionsAPI", "Error: ${e.message}")
            emptyList()
        }
    }

    private fun parseDirectionsWithInstructions(json: String): List<DirectionsRouteWithInstructions> {
        val routes = mutableListOf<DirectionsRouteWithInstructions>()

        try {
            val jsonObj = JSONObject(json)
            if (jsonObj.getString("status") != "OK") {
                Log.w("DirectionsAPI", "Status: ${jsonObj.getString("status")}")
                return emptyList()
            }

            val routesArray = jsonObj.getJSONArray("routes")

            for (i in 0 until routesArray.length()) {
                val routeJson = routesArray.getJSONObject(i)
                val legs = routeJson.getJSONArray("legs").getJSONObject(0)

                val distance = legs.getJSONObject("distance").getInt("value")
                val duration = legs.getJSONObject("duration").getInt("value")
                val summary = routeJson.optString("summary", "Ruta $i")

                // ✅ Extraer waypoints CON instrucciones de cada step
                val waypoints = mutableListOf<RouteWaypoint>()
                val stepsArray = legs.getJSONArray("steps")

                for (j in 0 until stepsArray.length()) {
                    val step = stepsArray.getJSONObject(j)

                    val startLoc = step.getJSONObject("start_location")
                    val endLoc = step.getJSONObject("end_location")

                    val instruction = step.getString("html_instructions")
                        .replace("<[^>]*>".toRegex(), "") // Quitar HTML tags
                        .replace("&nbsp;", " ")

                    val maneuver = step.optString("maneuver", "continue")
                    val stepDistance = step.getJSONObject("distance").getInt("value")
                    val stepDuration = step.getJSONObject("duration").getInt("value")

                    // Waypoint de inicio del step
                    waypoints.add(
                        RouteWaypoint(
                            latitude = startLoc.getDouble("lat"),
                            longitude = startLoc.getDouble("lng"),
                            instruction = instruction,
                            maneuver = maneuver,
                            distanceToNext = stepDistance.toDouble(),
                            durationToNext = stepDuration.toLong(),
                            streetName = summary
                        )
                    )

                    // Decodificar polyline del step para puntos intermedios
                    val polyline = step.getJSONObject("polyline").getString("points")
                    val decoded = decodePolyline(polyline)

                    // Agregar puntos intermedios (sin instrucción, solo para seguimiento)
                    decoded.drop(1).dropLast(1).forEach { point ->
                        waypoints.add(
                            RouteWaypoint(
                                latitude = point.latitude,
                                longitude = point.longitude,
                                instruction = "", // Punto intermedio
                                maneuver = "continue",
                                distanceToNext = 0.0,
                                durationToNext = 0
                            )
                        )
                    }
                }

                // Último waypoint (destino)
                val endLocation = legs.getJSONObject("end_location")
                waypoints.add(
                    RouteWaypoint(
                        latitude = endLocation.getDouble("lat"),
                        longitude = endLocation.getDouble("lng"),
                        instruction = "Has llegado a tu destino",
                        maneuver = "arrive",
                        distanceToNext = 0.0,
                        durationToNext = 0
                    )
                )

                routes.add(
                    DirectionsRouteWithInstructions(
                        distance = distance,
                        duration = duration,
                        summary = summary,
                        waypoints = waypoints
                    )
                )

                Log.d("DirectionsAPI", "Ruta $i: ${waypoints.size} waypoints, " +
                        "${waypoints.count { it.instruction.isNotEmpty() }} con instrucciones")
            }

        } catch (e: Exception) {
            Log.e("DirectionsAPI", "Error parseando: ${e.message}", e)
        }

        return routes
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var result = 1
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            result = 1
            shift = 0
            do {
                b = encoded[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            poly.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return poly
    }
}

data class DirectionsRouteWithInstructions(
    val distance: Int,
    val duration: Int,
    val summary: String,
    val waypoints: List<RouteWaypoint>
)