package com.example.copilotovirtual.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.copilotovirtual.data.models.User
import com.example.copilotovirtual.data.repositories.FirebaseRepository
import com.example.copilotovirtual.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import at.favre.lib.crypto.bcrypt.BCrypt

class AuthViewModel(context: Context) : ViewModel() {

    private val firebaseRepo = FirebaseRepository()
    private val userRepository = UserRepository(context)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Evento para navegación (primer acceso)
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    sealed class NavigationEvent {
        object Home : NavigationEvent()
        object AdminPanel : NavigationEvent()
        object ChangePassword : NavigationEvent()
    }

    init {
        viewModelScope.launch {
            checkSavedUser()
        }
    }

    private suspend fun checkSavedUser() {
        val savedUser = userRepository.getCurrentUser()
        if (savedUser != null) {
            _currentUser.value = savedUser
            _isLoggedIn.value = true
            // Determinar a dónde navegar según el rol y primer acceso
            if (savedUser.role == "admin") {
                _navigationEvent.value = NavigationEvent.AdminPanel
            } else {
                // Para conductores, verificar si es primer acceso (necesitamos consultar Firestore para el estado actual)
                val freshUser = firebaseRepo.getUserByUsername(savedUser.username)
                if (freshUser?.primerAcceso == true) {
                    _navigationEvent.value = NavigationEvent.ChangePassword
                } else {
                    _navigationEvent.value = NavigationEvent.Home
                }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val user = firebaseRepo.getUserByUsername(username)
                if (user == null) {
                    _errorMessage.value = "Usuario no encontrado"
                    return@launch
                }
                if (user.passwordHash.isEmpty()) {
                    // Primer acceso: no se verifica contraseña
                    _currentUser.value = user
                    _isLoggedIn.value = true
                    _navigationEvent.value = NavigationEvent.ChangePassword
                    return@launch
                }
                val result = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
                if (!result.verified) {
                    _errorMessage.value = "Contraseña incorrecta"
                    return@launch
                }
                // Login exitoso
                _currentUser.value = user
                _isLoggedIn.value = true
                _navigationEvent.value = if (user.role == "admin") NavigationEvent.AdminPanel else NavigationEvent.Home
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val currentUser = _currentUser.value ?: return@launch
                val uid = currentUser.uid

                // Hashear nueva contraseña
                val hash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())

                // Actualizar en Firestore
                val result = firebaseRepo.updatePassword(uid, hash)
                if (result.isSuccess) {
                    // Actualizar también en la sesión local
                    val updatedUser = currentUser.copy(
                        passwordHash = hash,
                        primerAcceso = false
                    )
                    userRepository.saveUser(updatedUser)
                    _currentUser.value = updatedUser
                    _navigationEvent.value = NavigationEvent.Home
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al cambiar contraseña"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error al cambiar contraseña: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFirstPassword(newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val user = _currentUser.value ?: return@launch
                val hash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())
                val result = firebaseRepo.updatePassword(user.uid, hash)
                if (result.isSuccess) {
                    // Actualizar usuario local
                    _currentUser.value = user.copy(passwordHash = hash, primerAcceso = false)
                    _isLoggedIn.value = true
                    // Navegar según rol
                    _navigationEvent.value = if (user.role == "admin") NavigationEvent.AdminPanel else NavigationEvent.Home
                } else {
                    _errorMessage.value = "Error al guardar contraseña"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.clearUser()
            _currentUser.value = null
            _isLoggedIn.value = false
            _navigationEvent.value = null
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearNavigation() {
        _navigationEvent.value = null
    }
}