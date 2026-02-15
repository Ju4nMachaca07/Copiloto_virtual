package com.example.copilotovirtual.data.models

data class User(
    val id: String,
    val username: String,
    val accessCode: String,
    val isOwner: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)