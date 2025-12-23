package com.um.eventosmobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.um.eventosmobile.core.error.AppError
import com.um.eventosmobile.core.error.ErrorMapper
import com.um.eventosmobile.core.session.SessionManager
import com.um.eventosmobile.shared.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de login.
 */
class LoginViewModel(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = AppError.ValidationError("Por favor complete todos los campos")
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                val token = authApi.login(username.trim(), password)
                sessionManager.setToken(token)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loginSuccess = true
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
    
    fun resetLoginSuccess() {
        _uiState.value = _uiState.value.copy(loginSuccess = false)
    }
}

/**
 * Estado de UI para la pantalla de login.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val loginSuccess: Boolean = false
)

