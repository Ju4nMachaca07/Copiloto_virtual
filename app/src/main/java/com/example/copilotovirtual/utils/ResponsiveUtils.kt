// utils/ResponsiveUtils.kt
package com.example.copilotovirtual.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Tamaños de pantalla
enum class ScreenSize { SMALL, MEDIUM, LARGE }

@Composable
fun getScreenSize(): ScreenSize {
    val width = LocalConfiguration.current.screenWidthDp.dp
    return when {
        width < 360.dp -> ScreenSize.SMALL   // Android 7 pantallas pequeñas
        width < 600.dp -> ScreenSize.MEDIUM  // Teléfonos normales
        else           -> ScreenSize.LARGE   // Tablets
    }
}

// Tamaños adaptativos
@Composable fun speedometerSize(): Dp {
    return when (getScreenSize()) {
        ScreenSize.SMALL  -> 110.dp
        ScreenSize.MEDIUM -> 150.dp
        ScreenSize.LARGE  -> 200.dp
    }
}

@Composable fun speedFontSize() = when (getScreenSize()) {
    ScreenSize.SMALL  -> 28
    ScreenSize.MEDIUM -> 42
    ScreenSize.LARGE  -> 56
}

@Composable fun buttonHeight(): Dp = when (getScreenSize()) {
    ScreenSize.SMALL  -> 48.dp
    ScreenSize.MEDIUM -> 56.dp
    ScreenSize.LARGE  -> 64.dp
}

@Composable fun contentPadding(): Dp = when (getScreenSize()) {
    ScreenSize.SMALL  -> 12.dp
    ScreenSize.MEDIUM -> 16.dp
    ScreenSize.LARGE  -> 24.dp
}