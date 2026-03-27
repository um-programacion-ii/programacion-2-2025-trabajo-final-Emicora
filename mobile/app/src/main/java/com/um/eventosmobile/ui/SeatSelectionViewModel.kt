package com.um.eventosmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.um.eventosmobile.core.error.AppError
import com.um.eventosmobile.core.error.ErrorMapper
import com.um.eventosmobile.domain.model.SeatMapDomain
import com.um.eventosmobile.domain.model.EventDetailDomain
import com.um.eventosmobile.domain.usecase.GetEventDetailUseCase
import com.um.eventosmobile.domain.usecase.GetSeatMapUseCase
import com.um.eventosmobile.shared.AsientoSeleccionadoDto
import com.um.eventosmobile.shared.MobileApi
import com.um.eventosmobile.shared.Seat
import com.um.eventosmobile.shared.SeatStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SeatSelectionUiState(
    val seatMap: SeatMapDomain? = null,
    val eventDetail: EventDetailDomain? = null,
    val isLoading: Boolean = false,
    val isBlocking: Boolean = false,
    val error: AppError? = null,
    val selectedSeats: Set<Pair<String, Int>> = emptySet(),
    val expiresAt: String? = null
)

class SeatSelectionViewModel(
    val api: MobileApi,
    private val eventId: Long,
    private val getEventDetailUseCase: GetEventDetailUseCase,
    private val getSeatMapUseCase: GetSeatMapUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeatSelectionUiState(isLoading = true))
    val uiState: StateFlow<SeatSelectionUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val detail = getEventDetailUseCase(eventId)
                val mapDto = getSeatMapUseCase(eventId)
                val completeMap = buildCompleteSeatMap(mapDto, detail.filaAsientos, detail.columnAsientos)

                // Traer selección activa si existe
                val selection = runCatching { api.getCurrentSelection(eventId) }.getOrNull()
                val activeSeats = selection?.asientos?.mapNotNull { s ->
                    s.numero?.let { n -> s.fila to n }
                }?.toSet().orEmpty()
                val expiresAt = selection?.expiracion?.toString()

                _uiState.value = _uiState.value.copy(
                    seatMap = completeMap,
                    eventDetail = detail,
                    isLoading = false,
                    error = if (completeMap.asientos.isEmpty()) AppError.ValidationError("No hay asientos disponibles para este evento") else null,
                    selectedSeats = activeSeats,
                    expiresAt = expiresAt
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorMapper.mapException(e)
                )
            }
        }
    }

    fun toggleSeat(fila: String, numero: Int, estado: SeatMapDomain.SeatDomain.SeatStatusDomain) {
        // Permitir toggle solo si estaba seleccionado o es LIBRE
        val isSelected = _uiState.value.selectedSeats.contains(fila to numero)
        if (!isSelected && estado != SeatMapDomain.SeatDomain.SeatStatusDomain.LIBRE) return

        val updated = if (isSelected) {
            _uiState.value.selectedSeats - (fila to numero)
        } else {
            if (_uiState.value.selectedSeats.size < 4) _uiState.value.selectedSeats + (fila to numero)
            else _uiState.value.selectedSeats
        }
        _uiState.value = _uiState.value.copy(selectedSeats = updated, error = null)
    }

    fun blockAndContinue(onSuccess: (List<Pair<String, Int>>, String?) -> Unit, onError: (String) -> Unit) {
        val seats = _uiState.value.selectedSeats
        if (seats.isEmpty()) {
            onError("Debe seleccionar al menos un asiento")
            return
        }
        if (seats.size > 4) {
            onError("Puede seleccionar máximo 4 asientos")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isBlocking = true, error = null)

                // Guardar selección en sesión
                api.updateSelectedEvent(eventId)
                val asientosDto = seats.map { (fila, numero) ->
                    AsientoSeleccionadoDto(
                        fila = fila,
                        numero = numero,
                        nombrePersona = null,
                        apellidoPersona = null
                    )
                }
                api.updateSelectedSeats(asientosDto)

                // Bloquear
                val blockResponse = api.blockSeats(eventId)
                if (blockResponse.exitoso == true) {
                    val selection = api.getCurrentSelection(eventId)
                    val expires = selection?.expiracion?.toString()
                    _uiState.value = _uiState.value.copy(
                        isBlocking = false,
                        error = null,
                        selectedSeats = seats,
                        expiresAt = expires
                    )
                    onSuccess(seats.toList(), expires)
                } else {
                    val msg = blockResponse.mensaje ?: "No todos los asientos pueden ser bloqueados"
                    _uiState.value = _uiState.value.copy(isBlocking = false, error = AppError.ValidationError(msg))
                    onError(msg)
                }
            } catch (e: Exception) {
                val error = ErrorMapper.mapException(e)
                _uiState.value = _uiState.value.copy(isBlocking = false, error = error)
                onError(error.message)
            }
        }
    }

    private fun buildCompleteSeatMap(
        map: SeatMapDomain,
        filas: Int?,
        columnas: Int?
    ): SeatMapDomain {
        val totalEsperado = (filas ?: 0) * (columnas ?: 0)

        if (totalEsperado > 0 && map.asientos.size >= totalEsperado) {
            return map
        }
        if (filas == null || columnas == null) return map

        val asientosByKey = map.asientos.associateBy { "${it.fila}-${it.numero}" }
        val allSeats = mutableListOf<SeatMapDomain.SeatDomain>()

        for (filaNum in 1..filas) {
            val filaLabel = filaNum.toString()
            for (columna in 1..columnas) {
                val key = "$filaLabel-$columna"
                val existing = asientosByKey[key]
                if (existing != null) {
                    allSeats.add(existing)
                } else {
                    allSeats.add(
                        SeatMapDomain.SeatDomain(
                            fila = filaLabel,
                            numero = columna,
                            estado = SeatMapDomain.SeatDomain.SeatStatusDomain.LIBRE,
                            seleccionado = false
                        )
                    )
                }
            }
        }
        return SeatMapDomain(eventoId = map.eventoId, asientos = allSeats)
    }

}

