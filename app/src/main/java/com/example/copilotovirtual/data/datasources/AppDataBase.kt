// data/datasources/AppDataBase.kt
package com.example.copilotovirtual.data.datasources

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==================== ENTIDADES ====================

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val accessCode: String,
    val isOwner: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_routes")
data class CachedRoute(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val startCityId: String,
    val endCityId: String,
    val waypointsJson: String, // JSON serializado
    val distance: Double,
    val estimatedTime: Long,
    val tollRoads: Boolean = false,
    val highways: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

// ==================== DAOs ====================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface RouteDao {
    @Query("SELECT * FROM cached_routes ORDER BY lastUpdated DESC")
    fun getAllRoutes(): Flow<List<CachedRoute>>

    @Query("SELECT * FROM cached_routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: String): CachedRoute?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: CachedRoute)

    @Update
    suspend fun updateRoute(route: CachedRoute)

    @Delete
    suspend fun deleteRoute(route: CachedRoute)

    @Query("DELETE FROM cached_routes")
    suspend fun deleteAllRoutes()
}

// ==================== DATABASE ====================

@Database(
    entities = [
        UserEntity::class,
        CachedRoute::class,
        SavedRouteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDataBase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun routeDao(): RouteDao
    abstract fun savedRouteDao(): SavedRouteDao  // ← AGREGAR

    companion object {
        @Volatile
        private var INSTANCE: AppDataBase? = null

        fun getDatabase(context: Context): AppDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDataBase::class.java,
                    "copiloto_virtual_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}