// data/repositories/OfflineRouteRepository.kt
package com.example.copilotovirtual.data.repositories

import android.content.Context
import android.util.Log
import com.example.copilotovirtual.data.datasources.AppDataBase
import com.example.copilotovirtual.data.datasources.SavedRouteEntity
import com.example.copilotovirtual.data.models.City
import com.example.copilotovirtual.data.models.Route
import com.example.copilotovirtual.data.network.DirectionsApiService
import com.example.copilotovirtual.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class OfflineRouteRepository(private val context: Context) {

    private val dao = AppDataBase.getDatabase(context).savedRouteDao()
    private val directionsApi = DirectionsApiService(context)

    val allSavedRoutes: Flow<List<SavedRouteEntity>> = dao.getAllSavedRoutes()

    // Descargar y guardar ruta real de Google
    suspend fun downloadAndSaveRoute(
        originCity: City,
        destCity: City,
        onProgress: (String) -> Unit = {}
    ): Result<List<SavedRouteEntity>> {
        return try {
            if (!NetworkUtils.isConnected(context)) {
                return Result.failure(Exception("Sin conexión a internet"))
            }

            onProgress("Consultando Google Maps...")

            val directions = directionsApi.getDirections(
                origin = originCity.coordinates,
                destination = destCity.coordinates,
                alternatives = true,
                originCityId = originCity.id,
                destinationCityId = destCity.id
            )

            if (directions.isEmpty()) {
                return Result.failure(Exception("No se encontraron rutas"))
            }

            onProgress("Guardando ${directions.size} rutas...")

            // Borrar rutas anteriores entre estas ciudades
            dao.deleteRoutesBetween(originCity.id, destCity.id)

            val savedRoutes = directions.mapIndexed { index, route ->
                val entity = SavedRouteEntity(
                    id = UUID.randomUUID().toString(),
                    name = if (index == 0) "Ruta Principal" else "Alternativa $index",
                    description = route.summary.ifBlank {
                        "${originCity.name} → ${destCity.name}"
                    },
                    startCityId = originCity.id,
                    endCityId = destCity.id,
                    startCityName = originCity.name,
                    endCityName = destCity.name,
                    polylinePoints = route.polyline,
                    distance = route.distance.toDouble(),
                    estimatedTime = route.duration.toLong()
                )
                dao.insertRoute(entity)
                Log.d("OfflineRoute", "Guardada: ${entity.name} con ${route.polyline.size} puntos")
                entity
            }

            onProgress("¡Listo! ${savedRoutes.size} rutas guardadas")
            Result.success(savedRoutes)

        } catch (e: Exception) {
            Log.e("OfflineRoute", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    // Obtener rutas guardadas (sin internet)
    suspend fun getSavedRoutesBetween(
        originCityId: String,
        destCityId: String
    ): List<SavedRouteEntity> {
        return dao.getRoutesBetween(originCityId, destCityId)
    }

    // Verificar si hay rutas guardadas
    suspend fun hasRoutesFor(
        originCityId: String,
        destCityId: String
    ): Boolean {
        return dao.getRoutesBetween(originCityId, destCityId).isNotEmpty()
    }

    suspend fun deleteRoute(route: SavedRouteEntity) {
        dao.deleteRoute(route)
    }

    // Convertir entidad guardada a Route del modelo
    fun SavedRouteEntity.toRoute(): Route {
        return Route(
            id = id,
            name = name,
            description = description,
            startCityId = startCityId,
            endCityId = endCityId,
            waypoints = polylinePoints,
            distance = distance,
            estimatedTime = estimatedTime
        )
    }
}