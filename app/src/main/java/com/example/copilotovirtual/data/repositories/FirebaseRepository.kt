package com.example.copilotovirtual.data.repositories

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.copilotovirtual.data.models.AuthorizedDriver
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.copilotovirtual.data.models.User

class FirebaseRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val driversCollection = firestore.collection("authorized_drivers")
    private val usuariosCollection = firestore.collection("usuarios")

    suspend fun getUserByUsername(username: String): User? {
        return try {
            val query = usuariosCollection
                .whereEqualTo("username", username.lowercase())
                .limit(1)
                .get()
                .await()
            if (query.isEmpty) null
            else query.documents.first().toObject(User::class.java)?.copy(uid = query.documents.first().id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error get usuario: ${e.message}")
            null
        }
    }

    suspend fun createUser(
        nombre: String,
        username: String,
        passwordHash: String,
        role: String
    ): Result<User> {
        return try {
            val normalizedUsername = username.lowercase()
            // Verificar si ya existe
            val existing = usuariosCollection
                .whereEqualTo("username", normalizedUsername)
                .get()
                .await()
            if (!existing.isEmpty) {
                return Result.failure(Exception("El nombre de usuario ya existe"))
            }

            val nuevo = User(
                username = normalizedUsername,
                nombre = nombre,
                passwordHash = "",
                role = role,
                primerAcceso = true,
                activo = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val docRef = usuariosCollection.add(nuevo).await()
            Result.success(nuevo.copy(uid = docRef.id))
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error createUser: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updatePassword(uid: String, newPasswordHash: String): Result<Unit> {
        return try {
            usuariosCollection.document(uid)
                .update(
                    mapOf(
                        "passwordHash" to newPasswordHash,
                        "primerAcceso" to false,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error updatePassword: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setUsuarioActivo(uid: String, activo: Boolean): Result<Unit> {
        return try {
            usuariosCollection.document(uid)
                .update("activo", activo, "updatedAt", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error setUsuarioActivo: ${e.message}")
            Result.failure(e)
        }
    }

    fun observeConductores(): Flow<List<User>> = callbackFlow {
        val listener = usuariosCollection
            .whereEqualTo("role", "conductor")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Error observando conductores: ${error.message}")
                    return@addSnapshotListener
                }
                val conductores = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                } ?: emptyList()
                trySend(conductores)
            }
        awaitClose { listener.remove() }
    }

    fun observeDrivers(): Flow<List<AuthorizedDriver>> = callbackFlow {
        val listener = driversCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseRepo", "Error observando: ${error.message}")
                return@addSnapshotListener
            }

            val drivers = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(AuthorizedDriver::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("FirebaseRepo", "Error parseando: ${e.message}")
                    null
                }
            } ?: emptyList()

            trySend(drivers)
        }

        awaitClose { listener.remove() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getDrivers(): Result<List<AuthorizedDriver>> {
        return try {
            val snapshot = driversCollection.get().await()
            val drivers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(AuthorizedDriver::class.java)?.copy(id = doc.id)
            }
            Result.success(drivers)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error obteniendo: ${e.message}")
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun validateCredentials(
        code: String,
        username: String
    ): CredentialValidation {
        return try {
            val normalizedCode = code.trim().uppercase()
            val normalizedUsername = username.trim().lowercase()

            val snapshot = driversCollection
                .whereEqualTo("accessCode", normalizedCode)
                .whereEqualTo("username", normalizedUsername)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return CredentialValidation.Invalid("Codigo o usuario no valido")
            }

            val driver = snapshot.documents.first()
                .toObject(AuthorizedDriver::class.java)
                ?.copy(id = snapshot.documents.first().id)
                ?: return CredentialValidation.Invalid("Error al leer datos")

            when {
                !driver.isActive -> CredentialValidation.Revoked("Acceso revocado: ${driver.revokedReason}")
                driver.isRegistered -> CredentialValidation.AlreadyUsed("Codigo ya utilizado")
                else -> CredentialValidation.Valid(driver)
            }

        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error validando: ${e.message}")
            CredentialValidation.Invalid("Error de conexion: ${e.message}")
        }
    }

    suspend fun markAsRegistered(driverId: String): Result<Unit> {
        return try {
            driversCollection.document(driverId)
                .update("registeredAt", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error marcando: ${e.message}")
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun generateAutoCode(username: String): Result<AuthorizedDriver> {
        return try {
            val normalizedUsername = username.trim().lowercase()

            // Verificar que no exista
            val existing = driversCollection
                .whereEqualTo("username", normalizedUsername)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("El nombre de usuario ya existe"))
            }

            // Contar códigos existentes
            val allDrivers = driversCollection.get().await()
            val condCodes = allDrivers.documents.count {
                it.getString("accessCode")?.startsWith("COND-") == true
            }

            val nextNumber = condCodes + 1
            val newCode = "COND-${nextNumber.toString().padStart(3, '0')}"

            val newDriver = AuthorizedDriver(
                id = "", // Firebase genera el ID
                accessCode = newCode,
                username = normalizedUsername,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )

            // Agregar a Firestore
            val docRef = driversCollection.add(newDriver).await()

            Result.success(newDriver.copy(id = docRef.id))

        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error generando: ${e.message}")
            Result.failure(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createManualCode(
        customCode: String,
        username: String
    ): Result<AuthorizedDriver> {
        return try {
            val normalizedCode = customCode.trim().uppercase()
            val normalizedUsername = username.trim().lowercase()

            // Verificar código
            val existingCode = driversCollection
                .whereEqualTo("accessCode", normalizedCode)
                .get()
                .await()

            if (!existingCode.isEmpty) {
                return Result.failure(Exception("El codigo ya existe"))
            }

            // Verificar username
            val existingUser = driversCollection
                .whereEqualTo("username", normalizedUsername)
                .get()
                .await()

            if (!existingUser.isEmpty) {
                return Result.failure(Exception("El nombre de usuario ya existe"))
            }

            val newDriver = AuthorizedDriver(
                id = "",
                accessCode = normalizedCode,
                username = normalizedUsername,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )

            val docRef = driversCollection.add(newDriver).await()

            Result.success(newDriver.copy(id = docRef.id))

        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error creando: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun revokeCode(driverId: String, reason: String): Result<Unit> {
        return try {
            driversCollection.document(driverId)
                .update(
                    mapOf(
                        "isActive" to false,
                        "revokedAt" to System.currentTimeMillis(),
                        "revokedReason" to reason
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error revocando: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun reactivateCode(driverId: String): Result<Unit> {
        return try {
            driversCollection.document(driverId)
                .update(
                    mapOf(
                        "isActive" to true,
                        "revokedAt" to null,
                        "revokedReason" to null
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error reactivando: ${e.message}")
            Result.failure(e)
        }
    }

    //Eliminar código
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun deleteCode(driverId: String): Result<Unit> {
        return try {
            // Verificar que no esté registrado
            val doc = driversCollection.document(driverId).get().await()
            val driver = doc.toObject(AuthorizedDriver::class.java)

            if (driver?.isRegistered == true) {
                return Result.failure(Exception("No se puede eliminar un codigo ya registrado"))
            }

            driversCollection.document(driverId).delete().await()
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error eliminando: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun crearConductorSinPassword(nombre: String, username: String): Result<User> {
        return try {
            val normalizedUsername = username.lowercase()
            // Verificar si ya existe
            val existente = usuariosCollection
                .whereEqualTo("username", normalizedUsername)
                .get()
                .await()
            if (!existente.isEmpty) {
                return Result.failure(Exception("El nombre de usuario ya existe"))
            }

            val nuevo = User(
                username = normalizedUsername,
                nombre = nombre,
                passwordHash = "", // vacío
                role = "conductor",
                primerAcceso = true,
                activo = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val docRef = usuariosCollection.add(nuevo).await()
            Result.success(nuevo.copy(uid = docRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}