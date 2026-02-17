// data/datasources/SavedRouteEntity.kt
package com.example.copilotovirtual.data.datasources

import androidx.room.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

// Conversor de LatLng para Room
class LatLngConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromLatLngList(value: List<LatLng>): String = gson.toJson(value)

    @TypeConverter
    fun toLatLngList(value: String): List<LatLng> {
        val type = object : TypeToken<List<LatLng>>() {}.type
        return gson.fromJson(value, type)
    }
}

// Entidad - cada punto de la ruta real de Google
@Entity(tableName = "saved_routes")
@TypeConverters(LatLngConverter::class)
data class SavedRouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val startCityId: String,
    val endCityId: String,
    val startCityName: String,
    val endCityName: String,
    val polylinePoints: List<LatLng>, // Puntos REALES de Google Directions
    val distance: Double,             // metros
    val estimatedTime: Long,          // segundos
    val savedAt: Long = System.currentTimeMillis(),
    val isDownloaded: Boolean = true
)

// DAO
@Dao
interface SavedRouteDao {
    @Query("SELECT * FROM saved_routes ORDER BY savedAt DESC")
    fun getAllSavedRoutes(): Flow<List<SavedRouteEntity>>

    @Query("""
        SELECT * FROM saved_routes 
        WHERE startCityId = :originId AND endCityId = :destId
        ORDER BY savedAt DESC
    """)
    suspend fun getRoutesBetween(
        originId: String,
        destId: String
    ): List<SavedRouteEntity>

    @Query("SELECT * FROM saved_routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: String): SavedRouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRouteEntity)

    @Delete
    suspend fun deleteRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes WHERE startCityId = :originId AND endCityId = :destId")
    suspend fun deleteRoutesBetween(originId: String, destId: String)

    @Query("SELECT COUNT(*) FROM saved_routes")
    suspend fun getRouteCount(): Int
}