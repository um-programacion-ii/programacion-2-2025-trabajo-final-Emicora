package com.um.eventosmobile.domain.mapper

import com.um.eventosmobile.shared.EventDetail
import com.um.eventosmobile.shared.EventSummary
import com.um.eventosmobile.shared.SeatMap
import com.um.eventosmobile.domain.model.EventDetailDomain
import com.um.eventosmobile.domain.model.EventSummaryDomain
import com.um.eventosmobile.domain.model.SeatMapDomain

/**
 * Mapea modelos del módulo shared a modelos de dominio.
 */
object EventMapper {
    
    fun toDomain(summary: EventSummary): EventSummaryDomain {
        return EventSummaryDomain(
            id = summary.id,
            titulo = summary.titulo,
            resumen = summary.resumen,
            fecha = summary.fecha,
            direccion = summary.direccion,
            precio = summary.precio,
            cancelado = summary.cancelado,
            tipoNombre = summary.tipoNombre,
            tipoDescripcion = summary.tipoDescripcion
        )
    }
    
    fun toDomain(detail: EventDetail): EventDetailDomain {
        return EventDetailDomain(
            id = detail.id,
            eventoIdCatedra = detail.eventoIdCatedra,
            titulo = detail.titulo,
            descripcion = detail.descripcion,
            resumen = detail.resumen,
            fecha = detail.fecha,
            direccion = detail.direccion,
            imagenUrl = detail.imagenUrl,
            precio = detail.precio,
            cancelado = detail.cancelado,
            tipoNombre = detail.tipoNombre,
            tipoDescripcion = detail.tipoDescripcion,
            filaAsientos = detail.filaAsientos,
            columnAsientos = detail.columnAsientos,
            integrantes = detail.integrantes.map { integrante ->
                EventDetailDomain.IntegranteDomain(
                    nombre = integrante.nombre,
                    descripcion = integrante.descripcion,
                    imagenUrl = integrante.imagenUrl
                )
            }
        )
    }
    
    fun toDomain(seatMap: SeatMap): SeatMapDomain {
        return SeatMapDomain(
            eventoId = seatMap.eventoId,
            asientos = seatMap.asientos.map { seat ->
                SeatMapDomain.SeatDomain(
                    fila = seat.fila,
                    numero = seat.numero,
                    estado = when (seat.estado) {
                        com.um.eventosmobile.shared.SeatStatus.LIBRE -> SeatMapDomain.SeatDomain.SeatStatusDomain.LIBRE
                        com.um.eventosmobile.shared.SeatStatus.OCUPADO -> SeatMapDomain.SeatDomain.SeatStatusDomain.OCUPADO
                        com.um.eventosmobile.shared.SeatStatus.BLOQUEADO -> SeatMapDomain.SeatDomain.SeatStatusDomain.BLOQUEADO
                    },
                    seleccionado = seat.seleccionado
                )
            }
        )
    }
}

