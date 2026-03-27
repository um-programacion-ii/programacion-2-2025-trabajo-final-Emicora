package com.um.eventosmobile.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.um.eventosmobile.core.error.AppError
import com.um.eventosmobile.core.error.ErrorMapper
import com.um.eventosmobile.domain.model.EventSummaryDomain
import com.um.eventosmobile.domain.usecase.GetEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la lista de eventos.
 */
class EventListViewModel(
    private val getEventsUseCase: GetEventsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EventListUiState())
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()
    
    init {
        loadEvents()
    }
    
    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                val events = getEventsUseCase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    events = events,
                    error = null
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
}

/**
 * Estado de UI para la lista de eventos.
 */
data class EventListUiState(
    val isLoading: Boolean = false,
    val events: List<EventSummaryDomain> = emptyList(),
    val error: AppError? = null
)

