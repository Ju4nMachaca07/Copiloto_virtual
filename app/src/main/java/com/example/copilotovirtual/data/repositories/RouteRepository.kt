// data/repositories/RouteRepository.kt
package com.example.copilotovirtual.data.repositories

import android.content.Context
import com.example.copilotovirtual.data.datasources.AppDataBase
import com.example.copilotovirtual.data.datasources.CachedRoute
import com.example.copilotovirtual.data.models.Route
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RouteRepository(context: Context) {
    private val routeDao = AppDataBase.getDatabase(context).routeDao()
    private val gson = Gson()

    fun getAllRoutes(): Flow<List<Route>> {
        return routeDao.getAllRoutes().map { cachedRoutes ->
            cachedRoutes.map { it.toRoute() }
        }
    }

    suspend fun getRouteById(routeId: String): Route? {
        return routeDao.getRouteById(routeId)?.toRoute()
    }

    suspend fun saveRoute(route: Route) {
        val cachedRoute = route.toCachedRoute()
        routeDao.insertRoute(cachedRoute)
    }

    suspend fun deleteRoute(routeId: String) {
        val cached = routeDao.getRouteById(routeId)
        cached?.let { routeDao.deleteRoute(it) }
    }

    // Conversiones
    private fun CachedRoute.toRoute(): Route {
        val waypointsList = gson.fromJson<List<LatLng>>(
            waypointsJson,
            object : TypeToken<List<LatLng>>() {}.type
        )

        return Route(
            id = id,
            name = name,
            description = description,
            startCityId = startCityId,
            endCityId = endCityId,
            waypoints = waypointsList,
            distance = distance,
            estimatedTime = estimatedTime,
            tollRoads = tollRoads,
            highways = highways
        )
    }

    private fun Route.toCachedRoute(): CachedRoute {
        return CachedRoute(
            id = id,
            name = name,
            description = description,
            startCityId = startCityId,
            endCityId = endCityId,
            waypointsJson = gson.toJson(waypoints),
            distance = distance,
            estimatedTime = estimatedTime,
            tollRoads = tollRoads,
            highways = highways
        )
    }
}