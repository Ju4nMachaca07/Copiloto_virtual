package com.example.copilotovirtual

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.copilotovirtual.navigation.AppNavigation
import com.example.copilotovirtual.ui.theme.CopilotoVirtualTheme
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback

class MainActivity : ComponentActivity(), OnMapsSdkInitializedCallback {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)

        setContent {
            CopilotoVirtualTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> println("Maps: Renderizador más reciente")
            MapsInitializer.Renderer.LEGACY -> println("Maps: Renderizador legacy")
        }
    }
}