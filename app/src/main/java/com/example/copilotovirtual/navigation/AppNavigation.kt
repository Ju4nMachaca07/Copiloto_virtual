// navigation/AppNavigation.kt
package com.example.copilotovirtual.navigation

import android.content.Context
import android.os.Build
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
    val navController = rememberNavController()
    val context = LocalContext.current

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context)
    )

    //ViewModel compartido entre pantallas
    val sharedRouteViewModel: SharedRouteViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
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
                    // Guardar ruta en ViewModel compartido
                    sharedRouteViewModel.setRoute(route)
                    // Navegar solo con señal
                    navController.navigate("navigation")
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        //Ruta simple sin argumentos
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
                onBack = { navController.popBackStack() }
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