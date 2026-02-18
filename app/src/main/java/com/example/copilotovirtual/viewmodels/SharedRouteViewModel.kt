// viewmodels/SharedRouteViewModel.kt
package com.example.copilotovirtual.viewmodels

import androidx.lifecycle.ViewModel
import com.example.copilotovirtual.data.models.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedRouteViewModel : ViewModel() {

    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute.asStateFlow()

    fun setRoute(route: Route) {
        _selectedRoute.value = route
    }

    fun clearRoute() {
        _selectedRoute.value = null
    }
}