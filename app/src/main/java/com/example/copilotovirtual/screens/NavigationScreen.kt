package com.example.copilotovirtual.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import com.example.copilotovirtual.components.Speedometer
import com.example.copilotovirtual.viewmodels.NavigationViewModel
import com.example.copilotovirtual.viewmodels.SharedRouteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    sharedRouteViewModel: SharedRouteViewModel,
    onBack: () -> Unit = {},
    navViewModel: NavigationViewModel = viewModel()
) {
    val context = LocalContext.current

    val route by sharedRouteViewModel.selectedRoute.collectAsState()
    val currentSpeed by navViewModel.currentSpeed.collectAsState()
    val speedLimit by navViewModel.speedLimit.collectAsState()
    val navigationProgress by navViewModel.navigationProgress.collectAsState()
    val lastInstruction by navViewModel.lastInstruction.collectAsState()
    val currentSegmentIndex by navViewModel.currentSegmentIndex.collectAsState()
    val geocercas by navViewModel.geocercas.collectAsState()  // <-- AÑADIDO

    var showStopDialog by remember { mutableStateOf(false) }

    // Cargar geocercas al entrar
    LaunchedEffect(Unit) {
        navViewModel.loadGeocercasFromKml(context)
    }

    if (route == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    LaunchedEffect(route) {
        navViewModel.initializeTTS(context)
        navViewModel.initializeLocationClient(context)
        navViewModel.startLocationUpdates(context)
        kotlinx.coroutines.delay(500)
        route?.let { navViewModel.startNavigationWithRoute(it) }
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
                        route?.let {
                            Text(
                                it.name,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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
                onRepeatInstruction = {
                    lastInstruction?.let { navViewModel.announceInstruction(it) }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MapView(
                modifier = Modifier.fillMaxSize(),
                viewModel = navViewModel,
                currentSegmentIndex = currentSegmentIndex,
                geocercas = geocercas  // <-- AÑADIDO
            )

            Speedometer(
                currentSpeed = currentSpeed,
                speedLimit = speedLimit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
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
                ) { Text("Detener") }
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
    onRepeatInstruction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Navigation,
                null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    instruction ?: "Esperando GPS...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            IconButton(
                onClick = onRepeatInstruction,
                enabled = instruction != null
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    "Repetir",
                    tint = if (instruction != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}