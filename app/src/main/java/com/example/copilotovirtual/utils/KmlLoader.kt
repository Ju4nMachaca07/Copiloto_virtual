package com.example.copilotovirtual.utils

import android.content.Context
import android.util.Log
import com.example.copilotovirtual.data.models.Geocerca
import com.google.android.gms.maps.model.LatLng
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

object KmlLoader {
    private const val TAG = "KmlLoader"

    fun loadFromAssets(context: Context, filename: String = "geocercas.kml"): List<Geocerca> {
        val geocercas = mutableListOf<Geocerca>()
        var inputStream: InputStream? = null
        try {
            inputStream = context.assets.open(filename)
            Log.d(TAG, "Archivo $filename abierto correctamente")

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false // Ignorar namespaces
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var currentPlacemarkName = ""
            var currentPolygonPoints = mutableListOf<LatLng>()
            var insideCoordinates = false
            var placemarkCount = 0

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "Placemark" -> {
                                placemarkCount++
                                currentPolygonPoints.clear()
                                currentPlacemarkName = ""
                                Log.d(TAG, "Inicio Placemark #$placemarkCount")
                            }
                            "name" -> {
                                eventType = parser.next()
                                if (eventType == XmlPullParser.TEXT) {
                                    currentPlacemarkName = parser.text
                                    Log.d(TAG, "Nombre del Placemark: $currentPlacemarkName")
                                }
                            }
                            "coordinates" -> {
                                insideCoordinates = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideCoordinates) {
                            val coordsText = parser.text.trim()
                            if (coordsText.isNotEmpty()) {
                                // Dividir por espacios o saltos de línea
                                val tokens = coordsText.split("\\s+".toRegex())
                                tokens.forEach { token ->
                                    val parts = token.split(",")
                                    if (parts.size >= 2) {
                                        try {
                                            val lon = parts[0].toDouble()
                                            val lat = parts[1].toDouble()
                                            currentPolygonPoints.add(LatLng(lat, lon))
                                        } catch (_: NumberFormatException) {
                                            Log.e(TAG, "Error parseando coordenada: $token")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "coordinates" -> {
                                insideCoordinates = false
                                Log.d(TAG, "Coordenadas leídas: ${currentPolygonPoints.size} puntos")
                            }
                            "Placemark" -> {
                                if (currentPlacemarkName.isNotEmpty() && currentPolygonPoints.isNotEmpty()) {
                                    val geocerca = Geocerca(
                                        id = currentPlacemarkName.lowercase().replace(" ", "_"),
                                        name = currentPlacemarkName,
                                        polygon = currentPolygonPoints.toList()
                                    )
                                    geocercas.add(geocerca)
                                    Log.d(TAG, "Placemark agregado: ${geocerca.name}, ${geocerca.polygon.size} puntos")
                                } else {
                                    Log.w(TAG, "Placemark ignorado: nombre vacío o sin puntos")
                                }
                                currentPolygonPoints.clear()
                                currentPlacemarkName = ""
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            Log.d(TAG, "Total placemarks encontrados: $placemarkCount")
            Log.d(TAG, "Total geocercas cargadas: ${geocercas.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar KML: ${e.message}", e)
        } finally {
            inputStream?.close()
        }
        return geocercas
    }
}