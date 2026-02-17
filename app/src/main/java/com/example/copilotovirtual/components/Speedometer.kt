// components/Speedometer.kt
package com.example.copilotovirtual.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Speedometer(
    currentSpeed: Int,
    speedLimit: Int? = null,
    modifier: Modifier = Modifier
) {
    val isOverSpeed = speedLimit != null && currentSpeed > speedLimit + 5

    // Animación suave del velocímetro
    val animatedSpeed by animateIntAsState(
        targetValue = currentSpeed,
        animationSpec = tween(durationMillis = 500),
        label = "speed"
    )

    // Color que cambia según velocidad
    val needleColor = when {
        isOverSpeed -> Color(0xFFE53935)          // Rojo
        currentSpeed > (speedLimit ?: 90) * 0.8 -> Color(0xFFFF9800) // Naranja
        else -> Color(0xFF43A047)                  // Verde
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gauge circular
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val radius = size.minDimension / 2 - 10f
                val strokeWidth = 18f

                // Fondo gris del arco
                drawArc(
                    color = Color(0xFFE0E0E0),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )

                // Arco verde (zona segura)
                val safeLimit = speedLimit ?: 90
                val safeAngle = (safeLimit.toFloat() / 140f) * 270f
                drawArc(
                    color = Color(0xFF43A047),
                    startAngle = 135f,
                    sweepAngle = safeAngle.coerceAtMost(270f),
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )

                // Arco rojo (zona peligrosa)
                if (speedLimit != null) {
                    val dangerStart = 135f + safeAngle
                    val dangerSweep = 270f - safeAngle
                    if (dangerSweep > 0) {
                        drawArc(
                            color = Color(0xFFE53935).copy(alpha = 0.3f),
                            startAngle = dangerStart,
                            sweepAngle = dangerSweep,
                            useCenter = false,
                            style = Stroke(strokeWidth, cap = StrokeCap.Round),
                            topLeft = Offset(cx - radius, cy - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        )
                    }
                }

                // Aguja de velocidad actual
                val speedAngle = (animatedSpeed.toFloat() / 140f) * 270f
                val needleAngleRad = Math.toRadians((135.0 + speedAngle))
                val needleLength = radius * 0.72f

                drawLine(
                    color = needleColor,
                    start = Offset(cx, cy),
                    end = Offset(
                        cx + (needleLength * cos(needleAngleRad)).toFloat(),
                        cy + (needleLength * sin(needleAngleRad)).toFloat()
                    ),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )

                // Centro del velocímetro
                drawCircle(
                    color = needleColor,
                    radius = 10f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = Offset(cx, cy)
                )
            }

            // Velocidad en el centro
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    "$currentSpeed",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = needleColor
                )
                Text(
                    "km/h",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // Límite de velocidad
        speedLimit?.let { limit ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = CircleShape,
                color = if (isOverSpeed) Color(0xFFE53935)
                else Color(0xFF1565C0),
                modifier = Modifier.size(56.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "$limit",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "km/h",
                        fontSize = 8.sp,
                        color = Color.White
                    )
                }
            }

            if (isOverSpeed) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFE53935),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "¡VELOCIDAD EXCEDIDA!",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}