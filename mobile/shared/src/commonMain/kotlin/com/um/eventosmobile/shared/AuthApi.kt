package com.um.eventosmobile.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json

/**
 * Excepción personalizada para errores de autenticación
 */
class AuthException(message: String, val statusCode: Int? = null) : Exception(message)

class AuthApi(private val backendUrl: String) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }

    suspend fun login(username: String, password: String): String {
        try {
            val response: HttpResponse = try {
                client.post("$backendUrl/api/authenticate") {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequestDto(username = username, password = password, rememberMe = true))
                }
            } catch (e: io.ktor.client.plugins.ClientRequestException) {
                // Manejar errores HTTP 4xx antes de deserializar
                when (e.response.status.value) {
                    401 -> throw AuthException("Usuario o contraseña incorrectos", 401)
                    403 -> throw AuthException("Acceso denegado", 403)
                    400 -> throw AuthException("Datos de inicio de sesión inválidos", 400)
                    else -> throw AuthException("Error al iniciar sesión", e.response.status.value)
                }
            } catch (e: io.ktor.client.plugins.ServerResponseException) {
                // Manejar errores HTTP 5xx
                throw AuthException("Error del servidor. Intente más tarde", e.response.status.value)
            }
            
            // Verificar código de estado HTTP antes de deserializar
            val statusCode = response.status.value
            when {
                statusCode == 401 -> {
                    throw AuthException("Usuario o contraseña incorrectos", 401)
                }
                statusCode == 403 -> {
                    throw AuthException("Acceso denegado", 403)
                }
                statusCode == 400 -> {
                    throw AuthException("Datos de inicio de sesión inválidos", 400)
                }
                statusCode >= 500 -> {
                    throw AuthException("Error del servidor. Intente más tarde", statusCode)
                }
                statusCode < 200 || statusCode >= 300 -> {
                    throw AuthException("Error al iniciar sesión", statusCode)
                }
            }
            
            // Intentar deserializar la respuesta - capturar TODOS los errores de deserialización
            val resp: AuthResponseDto = try {
                response.body()
            } catch (e: SerializationException) {
                // Error de deserialización (ej: falta id_token) = credenciales incorrectas
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("id_token", ignoreCase = true) ||
                    errorMsg.contains("required", ignoreCase = true) ||
                    errorMsg.contains("missing", ignoreCase = true) ||
                    errorMsg.contains("Illegal input", ignoreCase = true)) {
                    throw AuthException("Usuario o contraseña incorrectos", statusCode)
                }
                throw AuthException("Usuario o contraseña incorrectos", statusCode)
            } catch (e: Exception) {
                // Cualquier otro error de deserialización o excepción
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("id_token", ignoreCase = true) ||
                    errorMsg.contains("required", ignoreCase = true) ||
                    errorMsg.contains("missing", ignoreCase = true) ||
                    errorMsg.contains("Illegal input", ignoreCase = true) ||
                    errorMsg.contains("SerializationException", ignoreCase = true)) {
                    throw AuthException("Usuario o contraseña incorrectos", statusCode)
                }
                throw AuthException("Error al procesar la respuesta del servidor", statusCode)
            }
            
            // Verificar que el token existe y no está vacío
            if (resp.id_token.isBlank()) {
                throw AuthException("Usuario o contraseña incorrectos", 401)
            }
            
            return resp.id_token
            
        } catch (e: AuthException) {
            // Re-lanzar excepciones de autenticación personalizadas
            throw e
        } catch (e: SerializationException) {
            // Capturar errores de serialización que pueden ocurrir en cualquier punto
            val message = e.message ?: ""
            if (message.contains("id_token", ignoreCase = true) ||
                message.contains("required", ignoreCase = true) ||
                message.contains("missing", ignoreCase = true) ||
                message.contains("Illegal input", ignoreCase = true)) {
                throw AuthException("Usuario o contraseña incorrectos", 401)
            }
            throw AuthException("Usuario o contraseña incorrectos", 401)
        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            // Ktor lanza esta excepción para códigos HTTP 4xx
            // También puede contener errores de deserialización en el cuerpo
            val message = e.message ?: ""
            when {
                message.contains("id_token", ignoreCase = true) ||
                message.contains("required", ignoreCase = true) ||
                message.contains("missing", ignoreCase = true) ||
                message.contains("Illegal input", ignoreCase = true) -> {
                    throw AuthException("Usuario o contraseña incorrectos", 401)
                }
                e.response.status.value == 401 -> {
                    throw AuthException("Usuario o contraseña incorrectos", 401)
                }
                e.response.status.value == 403 -> {
                    throw AuthException("Acceso denegado", 403)
                }
                e.response.status.value == 400 -> {
                    throw AuthException("Datos de inicio de sesión inválidos", 400)
                }
                else -> {
                    throw AuthException("Error al iniciar sesión", e.response.status.value)
                }
            }
        } catch (e: io.ktor.client.plugins.ServerResponseException) {
            // Ktor lanza esta excepción para códigos HTTP 5xx
            throw AuthException("Error del servidor. Intente más tarde", e.response.status.value)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Re-lanzar cancelaciones
            throw e
        } catch (e: Exception) {
            // Capturar otros errores (red, timeout, deserialización, etc.)
            val message = e.message ?: "Error desconocido"
            when {
                message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("Failed to connect", ignoreCase = true) ||
                message.contains("Network", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) ||
                message.contains("Connection", ignoreCase = true) -> {
                    throw AuthException("Error de conexión. Verifique su conexión a internet", null)
                }
                message.contains("id_token", ignoreCase = true) ||
                message.contains("required", ignoreCase = true) ||
                message.contains("missing", ignoreCase = true) ||
                message.contains("Illegal input", ignoreCase = true) ||
                message.contains("SerializationException", ignoreCase = true) ||
                e is SerializationException -> {
                    // Error de deserialización por falta de id_token = credenciales incorrectas
                    throw AuthException("Usuario o contraseña incorrectos", 401)
                }
                else -> {
                    throw AuthException("Error al iniciar sesión. Intente nuevamente", null)
                }
            }
        }
    }
}

