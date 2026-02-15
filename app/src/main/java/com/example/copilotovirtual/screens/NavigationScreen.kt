package com.example.copilotovirtual.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.copilotovirtual.components.MapView
import com.example.copilotovirtual.data.models.Route
import com.example.copilotovirtual.utils.MapUtils
import com.example.copilotovirtual.viewmodels.NavigationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    routeId: String,
    onBack: () -> Unit = {},
    navViewModel: NavigationViewModel = viewModel()
) {
    val context = LocalContext.current

    // Obtener ruta directamente
    val selectedRoute = remember { Route.getRouteById(routeId) }

    val currentLocation by navViewModel.currentLocation.collectAsState()
    val navigationProgress by navViewModel.navigationProgress.collectAsState()
    val lastInstruction by navViewModel.lastInstruction.collectAsState()
    val currentSegments by navViewModel.currentSegments.collectAsState()

    var showStopDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        navViewModel.initializeTTS(context)
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
                            selectedRoute?.name ?: "Navegando...",
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
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationInfoBar(
                instruction = lastInstruction,
                route = selectedRoute,
                currentSegment = if (currentSegments.isNotEmpty()) {
                    val index = (navigationProgress * currentSegments.size).toInt()
                        .coerceIn(0, currentSegments.size - 1)
                    currentSegments[index]
                } else null,
                totalSegments = currentSegments.size,
                onRepeatInstruction = {
                    lastInstruction?.let { navViewModel.announceInstruction(it) }
                }
            )
        }
    ) { paddingValues ->
        MapView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            viewModel = navViewModel
        )
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Detener Navegacion") },
            text = { Text("Estas seguro que deseas detener la navegacion?") },
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
fun NavigationInfoBar(
    instruction: String?,
    route: Route?,
    currentSegment: com.example.copilotovirtual.utils.RouteSegment?,
    totalSegments: Int,
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

                    route?.let {
                        Text(
                            "${MapUtils.formatDistance(it.distance * 1000)} - " +
                                    "${MapUtils.formatDuration(it.estimatedTime)}",
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
                        "Repetir instruccion",
                        tint = if (instruction != null)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (currentSegment != null && totalSegments > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentIndex = (currentSegment.id.substringAfter("_").toIntOrNull() ?: 0) + 1

                    AssistChip(
                        onClick = { },
                        label = { Text("Tramo $currentIndex/$totalSegments") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Flag,
                                null,
                                Modifier.size(16.dp)
                            )
                        }
                    )

                    currentSegment.speedLimit?.let { limit ->
                        AssistChip(
                            onClick = { },
                            label = { Text("Limite: $limit km/h") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Speed,
                                    null,
                                    Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}