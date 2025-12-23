package com.um.eventosmobile.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor centralizado de sesión de usuario.
 * Maneja el token de autenticación y su estado.
 */
class SessionManager {
    
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    /**
     * Obtiene el token actual de forma síncrona (para providers).
     */
    fun getCurrentToken(): String? = _token.value
    
    /**
     * Establece el token y actualiza el estado de autenticación.
     */
    fun setToken(token: String?) {
        _token.value = token
        _isAuthenticated.value = token != null
    }
    
    /**
     * Limpia la sesión.
     */
    fun clearSession() {
        _token.value = null
        _isAuthenticated.value = false
    }
}

