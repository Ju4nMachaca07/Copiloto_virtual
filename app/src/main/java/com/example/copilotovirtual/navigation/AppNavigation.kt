package com.example.copilotovirtual.navigation

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.copilotovirtual.screens.*
import com.example.copilotovirtual.viewmodels.AuthViewModel
import com.example.copilotovirtual.viewmodels.SharedRouteViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val context = LocalContext.current // <-- 1. Obtener contexto
    val sharedRouteViewModel: SharedRouteViewModel = viewModel()
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(context))

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { isAdmin, primerAcceso ->
                    when {
                        isAdmin -> navController.navigate("adminPanel") {
                            popUpTo("login") { inclusive = true }
                        }
                        primerAcceso -> navController.navigate("changePassword") {
                            popUpTo("login") { inclusive = true }
                        }
                        else -> navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // Ruta unificada para cambiar contraseña (primer acceso)
        composable("changePassword") {
            ChangePasswordScreen(
                authViewModel = authViewModel,
                onPasswordChanged = {
                    // Después de cambiar la contraseña, vamos a la pantalla principal según el rol
                    val currentUser = authViewModel.currentUser.value
                    if (currentUser?.role == "admin") {
                        navController.navigate("adminPanel") {
                            popUpTo("changePassword") { inclusive = true }
                        }
                    } else {
                        navController.navigate("home") {
                            popUpTo("changePassword") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable("map") {
            MapScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable("routeSelection") {
            RouteSelectionScreen(
                onRouteSelected = { route ->
                    sharedRouteViewModel.setRoute(route)
                    navController.navigate("navigation")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("navigation") {
            NavigationScreen(
                sharedRouteViewModel = sharedRouteViewModel,
                onBack = {
                    sharedRouteViewModel.clearRoute()
                    navController.popBackStack()
                }
            )
        }

        composable("adminPanel") {
            AdminPanelScreen(
                onBack = {
                    navController.navigate("home") {
                        popUpTo("adminPanel") { inclusive = true }
                    }
                }
            )
        }

        composable("downloadRoutes") {
            DownloadRoutesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

class AuthViewModelFactory(private val context: Context) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}