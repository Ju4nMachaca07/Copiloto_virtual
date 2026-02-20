package com.example.copilotovirtual.data.models

data class User(
    val uid: String = "",
    val firebaseUid: String = "",
    val username: String = "",
    val nombre: String = "",
    val passwordHash: String = "",
    val role: String = "conductor",
    val activo: Boolean = true,
    val primerAcceso: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)