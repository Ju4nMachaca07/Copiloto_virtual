// components/Speedometer.kt
package com.example.copilotovirtual.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import com.example.copilotovirtual.utils.ScreenSize
import com.example.copilotovirtual.utils.getScreenSize
import com.example.copilotovirtual.utils.speedFontSize
import com.example.copilotovirtual.utils.speedometerSize
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Speedometer(
    currentSpeed: Int,
    modifier: Modifier = Modifier,
    speedLimit: Int? = null

) {
    val isOverSpeed = speedLimit != null && currentSpeed > speedLimit + 5
    val animatedSpeed by animateIntAsState(
        targetValue = currentSpeed,
        animationSpec = tween(400),
        label = "speed"
    )

    val needleColor = when {
        isOverSpeed -> Color(0xFFE53935)
        currentSpeed > (speedLimit ?: 90) * 0.8 -> Color(0xFFFF9800)
        else -> Color(0xFF43A047)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Gauge reducido a 90dp
            Canvas(modifier = Modifier.size(90.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val radius = size.minDimension / 2 - 6f

                // Fondo
                drawArc(
                    color = Color(0xFFE0E0E0),
                    startAngle = 135f, sweepAngle = 270f, useCenter = false,
                    style = Stroke(10f, cap = StrokeCap.Round),
                    topLeft = Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )

                // Verde zona segura
                val safeAngle = ((speedLimit ?: 90).toFloat() / 140f) * 270f
                drawArc(
                    color = Color(0xFF43A047),
                    startAngle = 135f,
                    sweepAngle = safeAngle.coerceAtMost(270f),
                    useCenter = false,
                    style = Stroke(10f, cap = StrokeCap.Round),
                    topLeft = Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )

                // Rojo zona peligrosa
                if (speedLimit != null && 270f - safeAngle > 0) {
                    drawArc(
                        color = Color(0xFFE53935).copy(alpha = 0.3f),
                        startAngle = 135f + safeAngle,
                        sweepAngle = 270f - safeAngle,
                        useCenter = false,
                        style = Stroke(10f, cap = StrokeCap.Round),
                        topLeft = Offset(cx - radius, cy - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }

                // Aguja
                val needleAngle = Math.toRadians(
                    (135.0 + (animatedSpeed.toFloat() / 140f) * 270f)
                )
                val needleLen = radius * 0.68f
                drawLine(
                    color = needleColor,
                    start = Offset(cx, cy),
                    end = Offset(
                        cx + (needleLen * cos(needleAngle)).toFloat(),
                        cy + (needleLen * sin(needleAngle)).toFloat()
                    ),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = needleColor, radius = 5f, center = Offset(cx, cy))
                drawCircle(color = Color.White, radius = 2.5f, center = Offset(cx, cy))
            }

            // Número centrado
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Text(
                    "$animatedSpeed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = needleColor
                )
                Text("km/h", fontSize = 7.sp, color = Color.Gray)
            }
        }

        // Límite
        speedLimit?.let { limit ->
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isOverSpeed) Color(0xFFE53935) else Color(0xFF1565C0),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "$limit",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                if (isOverSpeed) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}