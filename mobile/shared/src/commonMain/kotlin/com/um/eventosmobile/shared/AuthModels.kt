package com.um.eventosmobile.shared

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequestDto(
    val username: String,
    val password: String,
    val rememberMe: Boolean = true
)

@Serializable
data class AuthResponseDto(
    val id_token: String
)

@Serializable
data class RegisterRequestDto(
    val login: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val langKey: String? = "es"
)

