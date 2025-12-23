package com.um.eventosmobile.core.error

import com.um.eventosmobile.shared.AuthException

/**
 * Mapea excepciones a errores de aplicación amigables para el usuario.
 */
object ErrorMapper {
    
    fun mapException(exception: Throwable): AppError {
        return when (exception) {
            is AuthException -> {
                when (exception.statusCode) {
                    401, 403 -> AppError.AuthError(exception.message ?: "Usuario o contraseña incorrectos")
                    400 -> AppError.ValidationError(exception.message ?: "Datos inválidos")
                    else -> AppError.AuthError(exception.message ?: "Error de autenticación")
                }
            }
            is java.net.UnknownHostException,
            is java.net.ConnectException,
            is java.net.SocketTimeoutException -> {
                AppError.NetworkError()
            }
            is kotlinx.serialization.SerializationException -> {
                AppError.ServerError("Error al procesar la respuesta del servidor")
            }
            else -> {
                val message = exception.message ?: "Error desconocido"
                when {
                    message.contains("401", ignoreCase = true) -> AppError.AuthError("Sesión expirada")
                    message.contains("403", ignoreCase = true) -> AppError.AuthError("Acceso denegado")
                    message.contains("404", ignoreCase = true) -> AppError.NotFoundError()
                    message.contains("500", ignoreCase = true) -> AppError.ServerError()
                    message.contains("Network", ignoreCase = true) ||
                    message.contains("conexión", ignoreCase = true) ||
                    message.contains("conexion", ignoreCase = true) ||
                    message.contains("internet", ignoreCase = true) ||
                    message.contains("timeout", ignoreCase = true) -> AppError.NetworkError()
                    else -> AppError.UnknownError(message)
                }
            }
        }
    }
}

