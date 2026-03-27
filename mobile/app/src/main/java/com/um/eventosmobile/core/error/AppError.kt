package com.um.eventosmobile.core.error

/**
 * Representa errores de la aplicación de forma centralizada.
 */
sealed class AppError {
    abstract val message: String
    
    data class NetworkError(override val message: String = "Error de conexión. Verifique su conexión a internet") : AppError()
    data class AuthError(override val message: String = "Usuario o contraseña incorrectos") : AppError()
    data class ServerError(override val message: String = "Error del servidor. Intente más tarde") : AppError()
    data class NotFoundError(override val message: String = "Recurso no encontrado") : AppError()
    data class ValidationError(override val message: String) : AppError()
    data class UnknownError(override val message: String = "Error desconocido") : AppError()
}

