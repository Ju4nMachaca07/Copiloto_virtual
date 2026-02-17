package com.example.copilotovirtual.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.copilotovirtual.data.models.AuthorizedDriver
import com.example.copilotovirtual.data.repositories.FirebaseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onBack: () -> Unit) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    // Observar cambios en tiempo real
    val drivers by repository.observeDrivers().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDriver by remember { mutableStateOf<AuthorizedDriver?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panel de Administracion")
                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Agregar codigo")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pendientes") },
                    icon = {
                        Badge {
                            Text("${drivers.count { it.canRegister }}")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Registrados") },
                    icon = {
                        Badge {
                            Text("${drivers.count { it.isRegistered }}")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Revocados") },
                    icon = {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("${drivers.count { !it.isActive }}")
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> PendingDriversList(
                    drivers = drivers.filter { it.canRegister },
                    onRevoke = { driver -> selectedDriver = driver },
                    onDelete = { driver ->
                        scope.launch {
                            isLoading = true
                            repository.deleteCode(driver.id)
                            isLoading = false
                        }
                    }
                )
                1 -> RegisteredDriversList(
                    drivers = drivers.filter { it.isRegistered },
                    onRevoke = { driver -> selectedDriver = driver }
                )
                2 -> RevokedDriversList(
                    drivers = drivers.filter { !it.isActive },
                    onReactivate = { driver ->
                        scope.launch {
                            isLoading = true
                            repository.reactivateCode(driver.id)
                            isLoading = false
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddDriverDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { mode, customCode, username ->
                scope.launch {
                    isLoading = true
                    val result = if (mode == "auto") {
                        repository.generateAutoCode(username)
                    } else {
                        repository.createManualCode(customCode, username)
                    }

                    result.onSuccess {
                        showAddDialog = false
                    }

                    isLoading = false
                }
            },
            repository = repository
        )
    }

    selectedDriver?.let { driver ->
        RevokeDriverDialog(
            driver = driver,
            onDismiss = { selectedDriver = null },
            onConfirm = { reason ->
                scope.launch {
                    isLoading = true
                    repository.revokeCode(driver.id, reason)
                    selectedDriver = null
                    isLoading = false
                }
            }
        )
    }
}

@Composable
fun PendingDriversList(
    drivers: List<AuthorizedDriver>,
    onRevoke: (AuthorizedDriver) -> Unit,
    onDelete: (AuthorizedDriver) -> Unit
) {
    if (drivers.isEmpty()) {
        EmptyState(
            icon = Icons.Default.CheckCircle,
            message = "No hay codigos pendientes"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(drivers) { driver ->
                PendingDriverCard(
                    driver = driver,
                    onRevoke = { onRevoke(driver) },
                    onDelete = { onDelete(driver) }
                )
            }
        }
    }
}

@Composable
fun RegisteredDriversList(
    drivers: List<AuthorizedDriver>,
    onRevoke: (AuthorizedDriver) -> Unit
) {
    if (drivers.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Group,
            message = "No hay conductores registrados"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(drivers) { driver ->
                RegisteredDriverCard(
                    driver = driver,
                    onRevoke = { onRevoke(driver) }
                )
            }
        }
    }
}

@Composable
fun RevokedDriversList(
    drivers: List<AuthorizedDriver>,
    onReactivate: (AuthorizedDriver) -> Unit
) {
    if (drivers.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Block,
            message = "No hay codigos revocados"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(drivers) { driver ->
                RevokedDriverCard(
                    driver = driver,
                    onReactivate = {
                        if (!driver.isRegistered) {
                            onReactivate(driver)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingDriverCard(
    driver: AuthorizedDriver,
    onRevoke: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        driver.accessCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Usuario: ${driver.username}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Opciones")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Revocar") },
                            onClick = {
                                showMenu = false
                                onRevoke()
                            },
                            leadingIcon = { Icon(Icons.Default.Block, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { },
                    label = { Text("Pendiente") },
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, null, Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )

                Text(
                    "Creado: ${formatDate(driver.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisteredDriverCard(
    driver: AuthorizedDriver,
    onRevoke: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        driver.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Codigo: ${driver.accessCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRevoke,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Block, "Revocar acceso")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AssistChip(
                onClick = { },
                label = { Text("Registrado: ${formatDate(driver.registeredAt!!)}") },
                leadingIcon = {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevokedDriverCard(
    driver: AuthorizedDriver,
    onReactivate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        driver.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        driver.accessCode,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    driver.revokedReason?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Motivo: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (!driver.isRegistered) {
                    IconButton(onClick = onReactivate) {
                        Icon(Icons.Default.Restore, "Reactivar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Revocado: ${formatDate(driver.revokedAt!!)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDriverDialog(
    onDismiss: () -> Unit,
    onConfirm: (mode: String, customCode: String, username: String) -> Unit,
    repository: FirebaseRepository
) {
    var mode by remember { mutableStateOf("auto") }
    var customCode by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PersonAdd, null) },
        title = { Text("Agregar Nuevo Conductor") },
        text = {
            Column {
                if (generatedCode == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilterChip(
                            selected = mode == "auto",
                            onClick = { mode = "auto" },
                            label = { Text("Auto") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = mode == "manual",
                            onClick = { mode = "manual" },
                            label = { Text("Manual") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (mode == "manual") {
                        OutlinedTextField(
                            value = customCode,
                            onValueChange = { customCode = it.uppercase() },
                            label = { Text("Codigo Personalizado") },
                            placeholder = { Text("TRUCK-05") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.lowercase() },
                        label = { Text("Nombre de usuario") },
                        placeholder = { Text("carlos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    errorMsg?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Codigo creado exitosamente",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("CODIGO:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                generatedCode!!,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("USUARIO:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                username,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Comparte esta informacion con el conductor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (generatedCode == null) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMsg = null

                            val result = if (mode == "auto") {
                                repository.generateAutoCode(username)
                            } else {
                                repository.createManualCode(customCode, username)
                            }

                            result.onSuccess { driver ->
                                generatedCode = driver.accessCode
                            }.onFailure { error ->
                                errorMsg = error.message
                            }

                            isLoading = false
                        }
                    },
                    enabled = !isLoading && username.isNotBlank() && (mode == "auto" || customCode.isNotBlank())
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (mode == "auto") "Generar" else "Crear")
                    }
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        },
        dismissButton = {
            if (generatedCode == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevokeDriverDialog(
    driver: AuthorizedDriver,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Block, null) },
        title = { Text("Revocar Acceso") },
        text = {
            Column {
                Text("Revocar acceso a ${driver.username}?")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo (opcional)") },
                    placeholder = { Text("ej: Fin de contrato") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.ifBlank { "Sin especificar" }) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Revocar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
