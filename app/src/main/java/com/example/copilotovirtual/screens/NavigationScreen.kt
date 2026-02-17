// screens/NavigationScreen.kt
package com.example.copilotovirtual.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.copilotovirtual.components.MapView
import com.example.copilotovirtual.components.Speedometer
import com.example.copilotovirtual.utils.MapUtils
import com.example.copilotovirtual.viewmodels.NavigationViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    routeId: String,
    onBack: () -> Unit = {},
    navViewModel: NavigationViewModel = viewModel()
) {
    val context = LocalContext.current

    val currentLocation by navViewModel.currentLocation.collectAsState()
    val navigationProgress by navViewModel.navigationProgress.collectAsState()
    val lastInstruction by navViewModel.lastInstruction.collectAsState()
    val currentSegments by navViewModel.currentSegments.collectAsState()
    val currentSpeed by navViewModel.currentSpeed.collectAsState()
    val speedLimit by navViewModel.speedLimit.collectAsState()

    var showStopDialog by remember { mutableStateOf(false) }
    var showSpeedometerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        navViewModel.initializeTTS(context)
        navViewModel.initializeLocationClient(context)
        navViewModel.startLocationUpdates(context)
        kotlinx.coroutines.delay(500)
        navViewModel.startNavigation(routeId)
    }

    DisposableEffect(Unit) {
        onDispose {
            navViewModel.stopNavigation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "NAVEGANDO",
                            style = MaterialTheme.typography.titleMedium
                        )
                        LinearProgressIndicator(
                            progress = { navigationProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showStopDialog = true }) {
                        Icon(
                            Icons.Default.Close,
                            "Detener",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationInfoBar(
                instruction = lastInstruction,
                currentSegment = if (currentSegments.isNotEmpty()) {
                    val index = (navigationProgress * currentSegments.size).toInt()
                        .coerceIn(0, currentSegments.size - 1)
                    currentSegments[index]
                } else null,
                onRepeatInstruction = {
                    lastInstruction?.let { navViewModel.announceInstruction(it) }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            MapView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                viewModel = navViewModel
            )

            Speedometer(
                currentSpeed = currentSpeed,
                speedLimit = speedLimit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = paddingValues.calculateTopPadding() + 8.dp, end = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(12.dp)
            )

            // Velocímetro flotante
            SpeedometerWidget(
                currentSpeed = currentSpeed,
                speedLimit = speedLimit,
                isExpanded = showSpeedometerExpanded,
                onToggleExpand = { showSpeedometerExpanded = !showSpeedometerExpanded },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .padding(top = paddingValues.calculateTopPadding())
            )
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Detener Navegación") },
            text = { Text("¿Estás seguro que deseas detener la navegación?") },
            confirmButton = {
                Button(
                    onClick = {
                        navViewModel.stopNavigation()
                        showStopDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Detener")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Continuar")
                }
            }
        )
    }
}

@Composable
fun SpeedometerWidget(
    currentSpeed: Int,
    speedLimit: Int?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverSpeed = speedLimit != null && currentSpeed > speedLimit + 5

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = when {
            isOverSpeed -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        tonalElevation = 8.dp,
        onClick = onToggleExpand
    ) {
        if (isExpanded) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Velocímetro circular
                SpeedGauge(
                    speed = currentSpeed,
                    maxSpeed = 120,
                    modifier = Modifier.size(150.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Velocidad actual
                Text(
                    "$currentSpeed",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverSpeed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
                Text(
                    "km/h",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                speedLimit?.let { limit ->
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isOverSpeed) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Límite: $limit km/h",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverSpeed) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isOverSpeed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "¡REDUCIR VELOCIDAD!",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    Icons.Default.UnfoldLess,
                    "Minimizar",
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            // Vista compacta
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$currentSpeed",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverSpeed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
                Text(
                    "km/h",
                    style = MaterialTheme.typography.labelSmall
                )

                if (isOverSpeed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        Icons.Default.Warning,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedGauge(
    speed: Int,
    maxSpeed: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2

        // Arco de fondo
        drawArc(
            color = Color.LightGray,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 20f, cap = StrokeCap.Round),
            topLeft = Offset(centerX - radius, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )

        // Arco de velocidad
        val speedAngle = (speed.toFloat() / maxSpeed) * 270f
        val speedColor = when {
            speed > maxSpeed * 0.8 -> Color.Red
            speed > maxSpeed * 0.6 -> Color(0xFFFFA500) // Orange
            else -> Color.Green
        }

        drawArc(
            color = speedColor,
            startAngle = 135f,
            sweepAngle = speedAngle,
            useCenter = false,
            style = Stroke(width = 20f, cap = StrokeCap.Round),
            topLeft = Offset(centerX - radius, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )

        // Aguja
        val needleAngle = Math.toRadians((135 + speedAngle).toDouble())
        val needleLength = radius * 0.7f
        val needleEndX = centerX + (needleLength * cos(needleAngle)).toFloat()
        val needleEndY = centerY + (needleLength * sin(needleAngle)).toFloat()

        drawLine(
            color = Color.Black,
            start = Offset(centerX, centerY),
            end = Offset(needleEndX, needleEndY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Centro
        drawCircle(
            color = Color.Black,
            radius = 10f,
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
fun NavigationInfoBar(
    instruction: String?,
    currentSegment: com.example.copilotovirtual.utils.RouteSegment?,
    onRepeatInstruction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        instruction ?: "Esperando señal GPS...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    currentSegment?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            it.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onRepeatInstruction,
                    enabled = instruction != null
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        "Repetir instrucción",
                        tint = if (instruction != null)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            currentSegment?.speedLimit?.let { limit ->
                Spacer(modifier = Modifier.height(12.dp))
                AssistChip(
                    onClick = { },
                    label = { Text("Límite: $limit km/h") },
                    leadingIcon = {
                        Icon(Icons.Default.Speed, null, Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }
        }
    }
}