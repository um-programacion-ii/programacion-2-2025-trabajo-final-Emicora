package com.um.eventosmobile.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.um.eventosmobile.core.error.AppError
import com.um.eventosmobile.core.error.ErrorMapper
import com.um.eventosmobile.shared.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de registro.
 */
class RegisterViewModel(
    private val authApi: AuthApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    
    fun register(
        login: String,
        password: String,
        confirmPassword: String,
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null
    ) {
        // Validaciones
        if (login.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = AppError.ValidationError("El nombre de usuario es requerido")
            )
            return
        }
        
        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = AppError.ValidationError("La contraseña es requerida")
            )
            return
        }
        
        if (password.length < 4) {
            _uiState.value = _uiState.value.copy(
                error = AppError.ValidationError("La contraseña debe tener al menos 4 caracteres")
            )
            return
        }
        
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                error = AppError.ValidationError("Las contraseñas no coinciden")
            )
            return
        }
        
        if (email != null && email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(
                error = AppError.ValidationError("El email no es válido")
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                authApi.register(
                    login = login.trim(),
                    password = password,
                    firstName = firstName?.takeIf { it.isNotBlank() },
                    lastName = lastName?.takeIf { it.isNotBlank() },
                    email = email?.takeIf { it.isNotBlank() }
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registerSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorMapper.mapException(e)
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun resetRegisterSuccess() {
        _uiState.value = _uiState.value.copy(registerSuccess = false)
    }
}

/**
 * Estado de UI para la pantalla de registro.
 */
data class RegisterUiState(
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val registerSuccess: Boolean = false
)

