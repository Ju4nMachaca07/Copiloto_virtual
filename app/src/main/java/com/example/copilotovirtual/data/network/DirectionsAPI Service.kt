// data/network/DirectionsApiService.kt
package com.example.copilotovirtual.data.network

import android.content.Context
import android.util.Log
import com.example.copilotovirtual.data.models.Route
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

    suspend fun getDirections(
        origin: LatLng,
        destination: LatLng,
        alternatives: Boolean = true,
        originCityId: String = "",
        destinationCityId: String = ""
    ): List<DirectionsRoute> = withContext(Dispatchers.IO) {

        // Sin internet → rutas predefinidas
        if (!NetworkUtils.isConnected(context)) {
            Log.w("DirectionsAPI", "Sin internet, usando rutas predefinidas")
            return@withContext getFallbackRoutes(originCityId, destinationCityId)
        }

        // Con internet → Google Directions API
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
                append("&key=$apiKey")
            }

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val routes = parseDirectionsResponse(response)

                // Si API no devuelve rutas, usar predefinidas
                if (routes.isEmpty()) {
                    Log.w("DirectionsAPI", "API sin resultados, usando predefinidas")
                    return@withContext getFallbackRoutes(originCityId, destinationCityId)
                }

                routes
            } else {
                Log.e("DirectionsAPI", "Error HTTP: ${connection.responseCode}")
                getFallbackRoutes(originCityId, destinationCityId)
            }
        } catch (e: Exception) {
            Log.e("DirectionsAPI", "Excepción: ${e.message}")
            getFallbackRoutes(originCityId, destinationCityId)
        }
    }

    private fun getFallbackRoutes(
        originCityId: String,
        destinationCityId: String
    ): List<DirectionsRoute> {
        Log.d("DirectionsAPI", "Cargando rutas offline: $originCityId → $destinationCityId")

        // Buscar rutas predefinidas
        val predefined = Route.getRoutesBetween(originCityId, destinationCityId)

        if (predefined.isEmpty()) {
            Log.w("DirectionsAPI", "No hay rutas predefinidas para este trayecto")
            return emptyList()
        }

        return predefined.map { route ->
            DirectionsRoute(
                distance = route.distance.toInt(),
                duration = route.estimatedTime.toInt(),
                polyline = route.waypoints,
                summary = route.description,
                isOffline = true
            )
        }
    }

    private fun parseDirectionsResponse(jsonResponse: String): List<DirectionsRoute> {
        val routes = mutableListOf<DirectionsRoute>()
        try {
            val json = JSONObject(jsonResponse)
            if (json.getString("status") != "OK") return emptyList()

            val routesArray = json.getJSONArray("routes")
            for (i in 0 until routesArray.length()) {
                val routeJson = routesArray.getJSONObject(i)
                val legs = routeJson.getJSONArray("legs").getJSONObject(0)

                routes.add(
                    DirectionsRoute(
                        distance = legs.getJSONObject("distance").getInt("value"),
                        duration = legs.getJSONObject("duration").getInt("value"),
                        polyline = decodePolyline(
                            routeJson.getJSONObject("overview_polyline").getString("points")
                        ),
                        summary = routeJson.optString("summary", "Ruta ${i + 1}"),
                        isOffline = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("DirectionsAPI", "Error parseando: ${e.message}")
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
            var result = 1; var shift = 0; var b: Int
            do { b = encoded[index++].code - 63 - 1; result += b shl shift; shift += 5 } while (b >= 0x1f)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1
            result = 1; shift = 0
            do { b = encoded[index++].code - 63 - 1; result += b shl shift; shift += 5 } while (b >= 0x1f)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1
            poly.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return poly
    }
}

data class DirectionsRoute(
    val distance: Int,
    val duration: Int,
    val polyline: List<LatLng>,
    val summary: String,
    val isOffline: Boolean = false
)