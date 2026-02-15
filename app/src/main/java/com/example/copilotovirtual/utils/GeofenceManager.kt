package com.example.copilotovirtual.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlin.math.abs

/**
 * Segmento de ruta con geocerca
 */
data class RouteSegment(
    val id: String,
    val name: String,
    val startPoint: LatLng,
    val endPoint: LatLng,
    val radiusMeters: Double = 100.0,
    val instruction: String,
    val speedLimit: Int? = null,
    val warningDistance: Double = 500.0 // Distancia para alertas previas
)

/**
 * Manager para geocercas y navegación guiada por voz
 */
class GeofenceManager {

    private var currentSegmentIndex = 0
    private var hasEnteredSegment = false
    private var lastAnnouncementTime = 0L
    private val announcementCooldown = 5000L // 5 segundos entre anuncios

    // Distancias para anuncios progresivos
    private val announcementDistances = listOf(500.0, 300.0, 100.0)
    private val announcedDistances = mutableSetOf<Double>()

    /**
     * Generar segmentos desde waypoints
     */
    fun generateSegments(
        waypoints: List<LatLng>,
        routeName: String
    ): List<RouteSegment> {
        if (waypoints.size < 2) return emptyList()

        val segments = mutableListOf<RouteSegment>()

        for (i in 0 until waypoints.size - 1) {
            val bearing = MapUtils.calculateBearing(waypoints[i], waypoints[i + 1])
            val direction = MapUtils.bearingToDirection(bearing)
            val distance = MapUtils.calculateDistance(waypoints[i], waypoints[i + 1])

            val instruction = when {
                i == 0 -> "Iniciando ruta hacia $routeName. Continúe hacia el $direction"
                i == waypoints.size - 2 -> "Aproximándose al destino final en ${MapUtils.formatDistance(distance)}"
                else -> "Continúe hacia el $direction durante ${MapUtils.formatDistance(distance)}"
            }

            // Determinar límite de velocidad según el tipo de segmento
            val speedLimit = when {
                distance < 5000 -> 40 // Zona urbana/minera
                distance < 20000 -> 60 // Carretera secundaria
                else -> 80 // Carretera principal
            }

            segments.add(
                RouteSegment(
                    id = "segment_$i",
                    name = "Tramo ${i + 1}",
                    startPoint = waypoints[i],
                    endPoint = waypoints[i + 1],
                    radiusMeters = 100.0,
                    instruction = instruction,
                    speedLimit = speedLimit
                )
            )
        }

        return segments
    }

    /**
     * Verificar entrada a geocerca y generar alertas
     */
    fun checkGeofence(
        currentLocation: Location,
        segments: List<RouteSegment>,
        onEnterSegment: (RouteSegment, String) -> Unit
    ) {
        if (currentSegmentIndex >= segments.size) {
            // Navegación completada
            return
        }

        val currentSegment = segments[currentSegmentIndex]
        val currentPosition = LatLng(currentLocation.latitude, currentLocation.longitude)
        val distanceToEnd = MapUtils.calculateDistance(currentPosition, currentSegment.endPoint)

        val currentTime = System.currentTimeMillis()

        // ==================== ENTRÓ A LA GEOCERCA ====================
        if (distanceToEnd <= currentSegment.radiusMeters && !hasEnteredSegment) {
            hasEnteredSegment = true

            if (currentTime - lastAnnouncementTime > announcementCooldown) {
                onEnterSegment(currentSegment, "Ha llegado al ${currentSegment.name}")
                lastAnnouncementTime = currentTime
            }

            // Avanzar al siguiente segmento
            currentSegmentIndex++
            announcedDistances.clear()

            // Anunciar siguiente instrucción
            if (currentSegmentIndex < segments.size) {
                val nextSegment = segments[currentSegmentIndex]
                onEnterSegment(nextSegment, nextSegment.instruction)
                hasEnteredSegment = false
            } else {
                onEnterSegment(currentSegment, "Ha llegado a su destino. Navegación completada.")
            }
        }
        // ==================== ALERTAS DE PROXIMIDAD ====================
        else if (!hasEnteredSegment && currentTime - lastAnnouncementTime > announcementCooldown) {
            // Anuncios progresivos
            for (announcementDistance in announcementDistances) {
                if (distanceToEnd <= announcementDistance &&
                    !announcedDistances.contains(announcementDistance)) {

                    val message = when {
                        announcementDistance >= 500 ->
                            "En ${announcementDistance.toInt()} metros, ${currentSegment.instruction}"
                        announcementDistance >= 100 ->
                            "En ${announcementDistance.toInt()} metros"
                        else ->
                            "Próximo punto de control en ${announcementDistance.toInt()} metros"
                    }

                    onEnterSegment(currentSegment, message)
                    announcedDistances.add(announcementDistance)
                    lastAnnouncementTime = currentTime
                    break
                }
            }
        }

        // ==================== ALERTA DE VELOCIDAD ====================
        if (currentTime - lastAnnouncementTime > announcementCooldown * 2) {
            currentSegment.speedLimit?.let { limit ->
                val speedKmh = (currentLocation.speed * 3.6).toInt()

                if (speedKmh > limit + 10) { // Margen de 10 km/h
                    onEnterSegment(
                        currentSegment,
                        "Atención. Reducir velocidad. Límite ${limit} kilómetros por hora. " +
                                "Velocidad actual ${speedKmh} kilómetros por hora"
                    )
                    lastAnnouncementTime = currentTime
                }
            }
        }

        // ==================== ALERTA DE DESVÍO ====================
        // Verificar si se está desviando de la ruta
        val distanceToStart = MapUtils.calculateDistance(currentPosition, currentSegment.startPoint)
        val totalSegmentDistance = MapUtils.calculateDistance(
            currentSegment.startPoint,
            currentSegment.endPoint
        )

        // Si está muy lejos del segmento actual
        if (distanceToEnd > totalSegmentDistance * 1.5 &&
            distanceToStart > totalSegmentDistance * 0.5 &&
            currentTime - lastAnnouncementTime > announcementCooldown * 3) {

            onEnterSegment(
                currentSegment,
                "Posible desvío de ruta detectado. Recalculando."
            )
            lastAnnouncementTime = currentTime
        }
    }

    /**
     * Reiniciar progreso
     */
    fun reset() {
        currentSegmentIndex = 0
        hasEnteredSegment = false
        lastAnnouncementTime = 0L
        announcedDistances.clear()
    }

    /**
     * Obtener progreso actual (0.0 a 1.0)
     */
    fun getProgress(totalSegments: Int): Float {
        if (totalSegments == 0) return 0f
        return currentSegmentIndex.toFloat() / totalSegments.toFloat().coerceAtLeast(1f)
    }

    /**
     * Obtener segmento actual
     */
    fun getCurrentSegmentIndex(): Int = currentSegmentIndex

    /**
     * Obtener tiempo estimado restante
     */
    fun getEstimatedTimeRemaining(
        currentLocation: Location,
        segments: List<RouteSegment>,
        avgSpeedKmh: Double = 60.0
    ): Double {
        if (currentSegmentIndex >= segments.size) return 0.0

        var totalDistance = 0.0
        val currentPosition = LatLng(currentLocation.latitude, currentLocation.longitude)

        // Distancia al final del segmento actual
        totalDistance += MapUtils.calculateDistance(
            currentPosition,
            segments[currentSegmentIndex].endPoint
        )

        // Distancia de segmentos restantes
        for (i in currentSegmentIndex + 1 until segments.size) {
            totalDistance += MapUtils.calculateDistance(
                segments[i].startPoint,
                segments[i].endPoint
            )
        }

        return MapUtils.estimateTravelTime(totalDistance, avgSpeedKmh)
    }
}