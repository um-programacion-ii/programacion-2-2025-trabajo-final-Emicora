package com.um.eventosmobile.domain.usecase

import com.um.eventosmobile.domain.mapper.EventMapper
import com.um.eventosmobile.domain.model.EventDetailDomain
import com.um.eventosmobile.shared.MobileApi

/**
 * Caso de uso para obtener el detalle de un evento.
 */
class GetEventDetailUseCase(
    private val api: MobileApi
) {
    suspend operator fun invoke(eventId: Long): EventDetailDomain {
        return EventMapper.toDomain(api.getEventDetail(eventId))
    }
}

