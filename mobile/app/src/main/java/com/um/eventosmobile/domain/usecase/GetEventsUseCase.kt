package com.um.eventosmobile.domain.usecase

import com.um.eventosmobile.domain.mapper.EventMapper
import com.um.eventosmobile.domain.model.EventSummaryDomain
import com.um.eventosmobile.shared.MobileApi

/**
 * Caso de uso para obtener la lista de eventos.
 */
class GetEventsUseCase(
    private val api: MobileApi
) {
    suspend operator fun invoke(): List<EventSummaryDomain> {
        return api.getEvents().map { EventMapper.toDomain(it) }
    }
}

