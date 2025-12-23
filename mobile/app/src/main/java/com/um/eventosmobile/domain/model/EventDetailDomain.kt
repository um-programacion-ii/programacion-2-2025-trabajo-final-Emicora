package com.um.eventosmobile.domain.model

import kotlinx.datetime.Instant

/**
 * Modelo de dominio para detalle de evento.
 */
data class EventDetailDomain(
    val id: Long,
    val eventoIdCatedra: Long?,
    val titulo: String,
    val descripcion: String?,
    val resumen: String?,
    val fecha: Instant,
    val direccion: String?,
    val imagenUrl: String?,
    val precio: Double?,
    val cancelado: Boolean,
    val tipoNombre: String?,
    val tipoDescripcion: String?,
    val filaAsientos: Int?,
    val columnAsientos: Int?,
    val integrantes: List<IntegranteDomain>
) {
    data class IntegranteDomain(
        val nombre: String?,
        val descripcion: String?,
        val imagenUrl: String?
    )
}

