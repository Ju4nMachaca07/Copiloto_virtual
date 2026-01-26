package com.example.copilotovirtual

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.copilotvirtual.ui.screens.MapViewScreen
import com.example.copilotovirtual.ui.theme.CopilotovirtualTheme  // ← Usa TU tema
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar OSMDroid
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

        enableEdgeToEdge()

        setContent {
            CopilotovirtualTheme {  // ← Usa TU tema, no Material3 directamente
                MapViewScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
    }
}