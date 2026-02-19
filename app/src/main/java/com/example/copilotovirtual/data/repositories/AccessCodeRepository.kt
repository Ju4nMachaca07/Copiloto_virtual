package com.example.copilotovirtual.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.copilotovirtual.data.models.AuthorizedDriver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*

private val Context.driversDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "authorized_drivers"
)

class AccessCodeRepository(private val context: Context) {

    private val gson = Gson()
    private val DRIVERS_KEY = stringPreferencesKey("drivers_list")

    val authorizedDrivers: Flow<List<AuthorizedDriver>> = context.driversDataStore.data
        .map { preferences ->
            val json = preferences[DRIVERS_KEY]
            if (json != null) {
                val type = object : TypeToken<List<AuthorizedDriver>>() {}.type
                gson.fromJson<List<AuthorizedDriver>>(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        }

    private suspend fun saveDrivers(drivers: List<AuthorizedDriver>) {
        context.driversDataStore.edit { preferences ->
            preferences[DRIVERS_KEY] = gson.toJson(drivers)
        }
    }

    suspend fun validateCredentials(
        code: String,
        username: String
    ): CredentialValidation {
        val normalizedCode = code.trim().uppercase()
        val normalizedUsername = username.trim().lowercase()

        val drivers = authorizedDrivers.first()
        val driver = drivers.find {
            it.accessCode.uppercase() == normalizedCode &&
                    it.username.lowercase() == normalizedUsername
        }

        return when {
            driver == null -> CredentialValidation.Invalid("Codigo o usuario no valido")
            !driver.isActive -> CredentialValidation.Revoked("Acceso revocado")
            driver.isRegistered -> CredentialValidation.AlreadyUsed("Codigo ya utilizado")
            else -> CredentialValidation.Valid(driver)
        }
    }

    suspend fun markAsRegistered(code: String, username: String) {
        val drivers = authorizedDrivers.first()
        val updated = drivers.map { driver ->
            if (driver.accessCode.uppercase() == code.uppercase() &&
                driver.username.lowercase() == username.lowercase()) {
                driver.copy(registeredAt = System.currentTimeMillis())
            } else {
                driver
            }
        }
        saveDrivers(updated)
    }

    /**
     * Generar código automáticamente
     */
    suspend fun generateAutoCode(username: String): Result<AuthorizedDriver> {
        return try {
            val normalizedUsername = username.trim().lowercase()
            val drivers = authorizedDrivers.first()

            if (drivers.any { it.username.lowercase() == normalizedUsername }) {
                return Result.failure(Exception("El nombre de usuario ya existe"))
            }

            val nextNumber = drivers.count { it.accessCode.startsWith("COND-") } + 1
            val newCode = "COND-${nextNumber.toString().padStart(3, '0')}"

            val newDriver = AuthorizedDriver(
                id = "driver_${System.currentTimeMillis()}",
                accessCode = newCode,
                username = normalizedUsername,
                isActive = true
            )

            saveDrivers(drivers + newDriver)
            Result.success(newDriver)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crear código manual
     */
    suspend fun createManualCode(
        customCode: String,
        username: String
    ): Result<AuthorizedDriver> {
        return try {
            val normalizedCode = customCode.trim().uppercase()
            val normalizedUsername = username.trim().lowercase()
            val drivers = authorizedDrivers.first()

            if (drivers.any { it.accessCode.uppercase() == normalizedCode }) {
                return Result.failure(Exception("El codigo ya existe"))
            }

            if (drivers.any { it.username.lowercase() == normalizedUsername }) {
                return Result.failure(Exception("El nombre de usuario ya existe"))
            }

            val newDriver = AuthorizedDriver(
                id = "driver_${System.currentTimeMillis()}",
                accessCode = normalizedCode,
                username = normalizedUsername,
                isActive = true
            )

            saveDrivers(drivers + newDriver)
            Result.success(newDriver)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeCode(driverId: String, reason: String) {
        val drivers = authorizedDrivers.first()
        val updated = drivers.map { driver ->
            if (driver.id == driverId) {
                driver.copy(
                    isActive = false,
                    revokedAt = System.currentTimeMillis(),
                    revokedReason = reason
                )
            } else {
                driver
            }
        }
        saveDrivers(updated)
    }

    suspend fun reactivateCode(driverId: String) {
        val drivers = authorizedDrivers.first()
        val updated = drivers.map { driver ->
            if (driver.id == driverId && !driver.isRegistered) {
                driver.copy(
                    isActive = true,
                    revokedAt = null,
                    revokedReason = null
                )
            } else {
                driver
            }
        }
        saveDrivers(updated)
    }

    suspend fun deleteCode(driverId: String): Result<Unit> {
        val drivers = authorizedDrivers.first()
        val driver = drivers.find { it.id == driverId }

        return if (driver?.isRegistered == true) {
            Result.failure(Exception("No se puede eliminar un codigo ya registrado"))
        } else {
            val updated = drivers.filter { it.id != driverId }
            saveDrivers(updated)
            Result.success(Unit)
        }
    }

    suspend fun generateNewCode(username: String): Result<AuthorizedDriver> {
        return try {
            val normalizedUsername = username.trim().lowercase()
            val drivers = authorizedDrivers.first()

            if (drivers.any { it.username.lowercase() == normalizedUsername }) {
                return Result.failure(Exception("El nombre de usuario ya existe"))
            }

            val nextNumber = drivers.count { it.accessCode.startsWith("COND-") } + 1
            val newCode = "COND-${nextNumber.toString().padStart(3, '0')}"

            val newDriver = AuthorizedDriver(
                id = "driver_${System.currentTimeMillis()}",
                accessCode = newCode,
                username = normalizedUsername,
                isActive = true
            )

            saveDrivers(drivers + newDriver)
            Result.success(newDriver)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

sealed class CredentialValidation {
    data class Valid(val driver: AuthorizedDriver) : CredentialValidation()
    data class Invalid(val reason: String) : CredentialValidation()
    data class Revoked(val reason: String) : CredentialValidation()
    data class AlreadyUsed(val reason: String) : CredentialValidation()
}