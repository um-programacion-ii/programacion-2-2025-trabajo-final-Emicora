package com.um.eventosmobile.domain.usecase

import com.um.eventosmobile.domain.mapper.EventMapper
import com.um.eventosmobile.domain.model.SeatMapDomain
import com.um.eventosmobile.shared.MobileApi

/**
 * Caso de uso para obtener el mapa de asientos de un evento.
 */
class GetSeatMapUseCase(
    private val api: MobileApi
) {
    suspend operator fun invoke(eventId: Long): SeatMapDomain {
        return EventMapper.toDomain(api.getSeatMap(eventId))
    }
}

