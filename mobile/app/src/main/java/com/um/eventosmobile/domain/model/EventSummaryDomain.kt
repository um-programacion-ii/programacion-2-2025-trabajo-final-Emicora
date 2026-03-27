package com.um.eventosmobile.domain.model

import kotlinx.datetime.Instant

/**
 * Modelo de dominio para resumen de evento.
 */
data class EventSummaryDomain(
    val id: Long,
    val titulo: String,
    val resumen: String?,
    val fecha: Instant,
    val direccion: String?,
    val precio: Double?,
    val cancelado: Boolean,
    val tipoNombre: String?,
    val tipoDescripcion: String?
)

