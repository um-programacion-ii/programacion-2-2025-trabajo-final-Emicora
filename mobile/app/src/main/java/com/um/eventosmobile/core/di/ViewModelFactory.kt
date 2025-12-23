package com.um.eventosmobile.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.um.eventosmobile.core.session.SessionManager
import com.um.eventosmobile.domain.usecase.GetEventDetailUseCase
import com.um.eventosmobile.domain.usecase.GetEventsUseCase
import com.um.eventosmobile.domain.usecase.GetSeatMapUseCase
import com.um.eventosmobile.shared.AuthApi
import com.um.eventosmobile.shared.MobileApi
import com.um.eventosmobile.ui.events.EventListViewModel
import com.um.eventosmobile.ui.login.LoginViewModel
import com.um.eventosmobile.ui.SeatSelectionViewModel

/**
 * Factory para crear ViewModels con sus dependencias.
 */
class ViewModelFactory(
    private val authApi: AuthApi,
    private val mobileApi: MobileApi,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    
    private val getEventsUseCase = GetEventsUseCase(mobileApi)
    private val getEventDetailUseCase = GetEventDetailUseCase(mobileApi)
    private val getSeatMapUseCase = GetSeatMapUseCase(mobileApi)
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(authApi, sessionManager) as T
            }
            modelClass.isAssignableFrom(EventListViewModel::class.java) -> {
                EventListViewModel(getEventsUseCase) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    
    /**
     * Factory específico para SeatSelectionViewModel que requiere eventId.
     */
    fun createSeatSelectionViewModel(eventId: Long): SeatSelectionViewModel {
        return SeatSelectionViewModel(
            api = mobileApi,
            eventId = eventId,
            getEventDetailUseCase = getEventDetailUseCase,
            getSeatMapUseCase = getSeatMapUseCase
        )
    }
}

