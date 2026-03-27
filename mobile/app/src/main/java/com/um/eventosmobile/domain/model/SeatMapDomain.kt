package com.um.eventosmobile.domain.model

/**
 * Modelo de dominio para mapa de asientos.
 */
data class SeatMapDomain(
    val eventoId: Long,
    val asientos: List<SeatDomain>
) {
    data class SeatDomain(
        val fila: String,
        val numero: Int,
        val estado: SeatStatusDomain,
        val seleccionado: Boolean = false
    ) {
        enum class SeatStatusDomain {
            LIBRE,
            OCUPADO,
            BLOQUEADO
        }
    }
}

