package com.example.copilotovirtual.viewmodels

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.copilotovirtual.data.models.User
import com.example.copilotovirtual.data.repositories.CredentialValidation
import com.example.copilotovirtual.data.repositories.FirebaseRepository
import com.example.copilotovirtual.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
        }
    }

    /**
     * ACCESO TRABAJADOR: Solo código + username
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun workerLogin(accessCode: String, username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                if (accessCode.isBlank() || username.isBlank()) {
                    _errorMessage.value = "Codigo y usuario son requeridos"
                    return@launch
                }

                Log.d("AuthViewModel", "Validando: $accessCode / $username")

                val validation = firebaseRepo.validateCredentials(accessCode, username)

                when (validation) {
                    is CredentialValidation.Valid -> {
                        val user = User(
                            id = validation.driver.id,
                            username = username,
                            accessCode = accessCode,
                            isOwner = false
                        )

                        userRepository.saveUser(user)
                        firebaseRepo.markAsRegistered(validation.driver.id)

                        _currentUser.value = user
                        _isLoggedIn.value = true
                        _errorMessage.value = null

                        Log.d("AuthViewModel", "Login exitoso")
                    }
                    is CredentialValidation.Invalid -> {
                        _errorMessage.value = validation.reason
                    }
                    is CredentialValidation.Revoked -> {
                        _errorMessage.value = validation.reason
                    }
                    is CredentialValidation.AlreadyUsed -> {
                        val existingUser = userRepository.findByUsername(username)
                        if (existingUser != null && existingUser.accessCode == accessCode) {
                            _currentUser.value = existingUser
                            _isLoggedIn.value = true
                            _errorMessage.value = null
                        } else {
                            _errorMessage.value = "Codigo ya utilizado por otro usuario"
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error: ${e.message}")
                _errorMessage.value = "Error de conexion: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * LOGIN ADMIN: Usuario + contraseña
     */
    fun adminLogin(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                if (username.isBlank() || password.isBlank()) {
                    _errorMessage.value = "Usuario y contraseña son requeridos"
                    return@launch
                }

                if (username.lowercase() == "admin" && password == "admin123") {
                    val adminUser = User(
                        id = "admin_001",
                        username = "admin",
                        accessCode = "OWNER-MASTER",
                        isOwner = true
                    )

                    userRepository.saveUser(adminUser)
                    _currentUser.value = adminUser
                    _isLoggedIn.value = true
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Credenciales de administrador incorrectas"
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
            try {
                _isLoggedIn.value = false
                _currentUser.value = null
                _errorMessage.value = null
                userRepository.clearUser()
            } catch (e: Exception) {
                _errorMessage.value = "Error al cerrar sesion: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}