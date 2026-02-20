package com.example.copilotovirtual.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.copilotovirtual.data.models.User
import com.example.copilotovirtual.data.repositories.FirebaseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onBack: () -> Unit) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    // Observar conductores (role = "conductor")
    val conductores by repository.observeConductores().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedConductor by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    BackHandler {
        Log.d("AdminPanel", "BackHandler triggered")
        onBack()
    }

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
                        Log.d("AdminPanel", "Navigation icon clicked")
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
                Icon(Icons.Default.Add, "Agregar conductor")
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
                            Text("${conductores.count { it.primerAcceso && it.activo }}")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Registrados") },
                    icon = {
                        Badge {
                            Text("${conductores.count { !it.primerAcceso && it.activo }}")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Revocados") },
                    icon = {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("${conductores.count { !it.activo }}")
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> PendientesList(
                    conductores = conductores.filter { it.primerAcceso && it.activo },
                    onRevoke = { conductor -> selectedConductor = conductor }
                )
                1 -> RegistradosList(
                    conductores = conductores.filter { !it.primerAcceso && it.activo },
                    onRevoke = { conductor -> selectedConductor = conductor }
                )
                2 -> RevocadosList(
                    conductores = conductores.filter { !it.activo },
                    onReactivate = { conductor ->
                        scope.launch {
                            isLoading = true
                            repository.setUsuarioActivo(conductor.uid, true)
                            isLoading = false
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddConductorDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { nombre, username ->
                scope.launch {
                    isLoading = true
                    val result = repository.createUser(
                        nombre = nombre,
                        username = username,
                        passwordHash = "",
                        role = "conductor"
                    )
                    if (result.isSuccess) {
                        showAddDialog = false
                    }
                    isLoading = false
                }
            }
        )
    }

    selectedConductor?.let { conductor ->
        RevokeConductorDialog(
            conductor = conductor,
            onDismiss = { selectedConductor = null },
            onConfirm = { reason ->
                scope.launch {
                    isLoading = true
                    // Aquí podrías agregar un campo de motivo en la colección de usuarios si lo deseas
                    repository.setUsuarioActivo(conductor.uid, false)
                    selectedConductor = null
                    isLoading = false
                }
            }
        )
    }
}

@Composable
fun PendientesList(
    conductores: List<User>,
    onRevoke: (User) -> Unit
) {
    if (conductores.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Schedule,
            message = "No hay conductores pendientes"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conductores) { conductor ->
                ConductorCard(
                    conductor = conductor,
                    onRevoke = { onRevoke(conductor) }
                )
            }
        }
    }
}

@Composable
fun RegistradosList(
    conductores: List<User>,
    onRevoke: (User) -> Unit
) {
    if (conductores.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Group,
            message = "No hay conductores registrados"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conductores) { conductor ->
                ConductorCard(
                    conductor = conductor,
                    onRevoke = { onRevoke(conductor) },
                    isRegistered = true
                )
            }
        }
    }
}

@Composable
fun RevocadosList(
    conductores: List<User>,
    onReactivate: (User) -> Unit
) {
    if (conductores.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Block,
            message = "No hay conductores revocados"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conductores) { conductor ->
                RevocadoCard(
                    conductor = conductor,
                    onReactivate = { onReactivate(conductor) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorCard(
    conductor: User,
    onRevoke: () -> Unit,
    isRegistered: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isRegistered) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        conductor.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Usuario: ${conductor.username}",
                        style = MaterialTheme.typography.bodyMedium
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { },
                    label = { Text(if (isRegistered) "Registrado" else "Pendiente") },
                    leadingIcon = {
                        Icon(
                            if (isRegistered) Icons.Default.CheckCircle else Icons.Default.Schedule,
                            null,
                            Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isRegistered)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Creado: ${formatDate(conductor.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevocadoCard(
    conductor: User,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        conductor.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Usuario: ${conductor.username}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(
                    onClick = onReactivate,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Restore, "Reactivar")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Creado: ${formatDate(conductor.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConductorDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, username: String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PersonAdd, null) },
        title = { Text("Agregar Nuevo Conductor") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase() },
                    label = { Text("Nombre de usuario") },
                    placeholder = { Text("ej: juan123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(nombre, username)
                },
                enabled = nombre.isNotBlank() && username.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Crear")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevokeConductorDialog(
    conductor: User,
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
                Text("¿Revocar acceso a ${conductor.nombre}?")
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

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}