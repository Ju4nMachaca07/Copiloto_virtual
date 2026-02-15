package com.example.copilotovirtual.data.models

data class AuthorizedDriver(
    val id: String = "",
    val accessCode: String = "",
    val username: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val registeredAt: Long? = null,
    val revokedAt: Long? = null,
    val revokedReason: String? = null
) {
    // Constructor vacío para Firebase
    constructor() : this(
        id = "",
        accessCode = "",
        username = "",
        isActive = true,
        createdAt = 0L,
        registeredAt = null,
        revokedAt = null,
        revokedReason = null
    )

    val isRegistered: Boolean
        get() = registeredAt != null

    val canRegister: Boolean
        get() = isActive && !isRegistered
}