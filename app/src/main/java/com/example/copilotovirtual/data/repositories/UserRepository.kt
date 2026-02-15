package com.example.copilotovirtual.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.copilotovirtual.data.models.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class UserRepository(private val context: Context) {

    private val gson = Gson()
    private val CURRENT_USER_KEY = stringPreferencesKey("current_user")
    private val ALL_USERS_KEY = stringPreferencesKey("all_users")

    suspend fun saveUser(user: User) {
        context.userDataStore.edit { preferences ->
            // Guardar usuario actual
            preferences[CURRENT_USER_KEY] = gson.toJson(user)

            // Agregar a lista de todos los usuarios
            val allUsers = getAllUsers().toMutableList()
            allUsers.removeAll { it.username == user.username }
            allUsers.add(user)
            preferences[ALL_USERS_KEY] = gson.toJson(allUsers)
        }
    }

    suspend fun getCurrentUser(): User? {
        return context.userDataStore.data.map { preferences ->
            preferences[CURRENT_USER_KEY]?.let { json ->
                gson.fromJson(json, User::class.java)
            }
        }.first()
    }

    suspend fun findByUsername(username: String): User? {
        val allUsers = getAllUsers()
        return allUsers.find { it.username.lowercase() == username.trim().lowercase() }
    }

    private suspend fun getAllUsers(): List<User> {
        return context.userDataStore.data.map { preferences ->
            preferences[ALL_USERS_KEY]?.let { json ->
                val type = object : TypeToken<List<User>>() {}.type
                gson.fromJson<List<User>>(json, type) ?: emptyList()
            } ?: emptyList()
        }.first()
    }

    suspend fun clearUser() {
        context.userDataStore.edit { preferences ->
            preferences.remove(CURRENT_USER_KEY)
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
    }
}