package com.um.eventosbackend.service.warmup;

import com.um.eventosbackend.repository.EventoRepository;
import com.um.eventosbackend.service.dto.BloqueoAsientosRequestDTO;
import com.um.eventosbackend.service.dto.BloqueoAsientosResponseDTO;
import com.um.eventosbackend.service.dto.MapaAsientosDTO;
import com.um.eventosbackend.service.proxy.ProxyAsientosService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Servicio para hacer warm-up de Redis al iniciar la aplicación.
 * Bloquea temporalmente 1 asiento de cada evento activo para asegurar
 * que Redis tenga datos actualizados.
 */
@Service
public class RedisWarmupService {

    private static final Logger LOG = LoggerFactory.getLogger(RedisWarmupService.class);

    private final EventoRepository eventoRepository;
    private final ProxyAsientosService proxyAsientosService;

    public RedisWarmupService(
        EventoRepository eventoRepository,
        ProxyAsientosService proxyAsientosService
    ) {
        this.eventoRepository = eventoRepository;
        this.proxyAsientosService = proxyAsientosService;
    }

    /**
     * Ejecuta el warm-up de Redis de forma asíncrona.
     * Intenta bloquear 1 asiento de cada evento activo hasta lograrlo.
     * Intenta con TODOS los asientos disponibles hasta encontrar uno que pueda bloquear.
     */
    @Async
    public void warmupRedis() {
        LOG.info("Iniciando warm-up de Redis...");
        
        try {
            // Esperar un poco para asegurar que el proxy esté listo
            // Esto es importante porque el warm-up se ejecuta al iniciar la aplicación
            try {
                Thread.sleep(5000); // Esperar 5 segundos
                LOG.debug("Espera inicial completada, el proxy debería estar listo");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Interrupción durante la espera inicial del warm-up");
                return;
            }
            
            // Obtener todos los eventos activos
            List<com.um.eventosbackend.domain.Evento> eventos = eventoRepository.findEventosActivos(Instant.now());
            LOG.info("Encontrados {} eventos activos para warm-up", eventos.size());

            int exitosos = 0;
            int fallidos = 0;

            for (com.um.eventosbackend.domain.Evento evento : eventos) {
                Long eventoId = evento.getEventoIdCatedra();
                if (eventoId == null) {
                    LOG.warn("Evento {} no tiene eventoIdCatedra, saltando", evento.getId());
                    fallidos++;
                    continue;
                }

                if (intentarBloquearAsiento(eventoId, evento)) {
                    exitosos++;
                    LOG.debug("Warm-up exitoso para eventoId: {}", eventoId);
                } else {
                    fallidos++;
                    LOG.warn("No se pudo hacer warm-up para eventoId: {} (todos los asientos pueden estar ocupados o el proxy no está disponible)", eventoId);
                }
            }

            LOG.info("Warm-up de Redis completado. Exitosos: {}, Fallidos: {}", exitosos, fallidos);
        } catch (Exception e) {
            LOG.error("Error durante el warm-up de Redis", e);
        }
    }

    /**
     * Intenta bloquear un asiento del evento hasta lograrlo o quedarse sin asientos.
     * Intenta con TODOS los asientos disponibles hasta encontrar uno que pueda bloquear.
     * 
     * @param eventoId ID del evento en la cátedra
     * @param evento Entidad Evento con información de dimensiones
     * @return true si logró bloquear un asiento, false en caso contrario
     */
    private boolean intentarBloquearAsiento(Long eventoId, com.um.eventosbackend.domain.Evento evento) {
        try {
            // Obtener mapa de asientos
            MapaAsientosDTO mapa = proxyAsientosService.obtenerMapaAsientos(eventoId);
            
            if (mapa == null) {
                LOG.warn("Mapa de asientos es null para eventoId: {}", eventoId);
                // Intentar bloquear asiento por defecto (fila 1, columna 1) para inicializar Redis
                return bloquearAsiento(eventoId, "1", 1);
            }

            List<MapaAsientosDTO.AsientoDTO> asientos = mapa.getAsientos();
            
            // Si el mapa está vacío, Redis probablemente no tiene datos aún
            // Intentar bloquear un asiento por defecto para inicializar Redis
            if (asientos == null || asientos.isEmpty()) {
                LOG.info("Mapa de asientos vacío para eventoId: {} (Redis sin inicializar), intentando bloquear asiento por defecto", eventoId);
                
                // Usar dimensiones del evento si están disponibles, sino usar valores por defecto
                Integer maxFilas = evento.getFilaAsiento();
                Integer maxColumnas = evento.getColumnAsiento();
                
                // Si no hay dimensiones, usar un rango amplio por defecto
                if (maxFilas == null || maxFilas <= 0) {
                    maxFilas = 50; // Rango amplio por defecto
                }
                if (maxColumnas == null || maxColumnas <= 0) {
                    maxColumnas = 50; // Rango amplio por defecto
                }
                
                LOG.debug("Intentando warm-up con dimensiones: {} filas x {} columnas para eventoId: {}", maxFilas, maxColumnas, eventoId);
                
                // Intentar con TODOS los asientos posibles según las dimensiones
                for (int fila = 1; fila <= maxFilas; fila++) {
                    for (int columna = 1; columna <= maxColumnas; columna++) {
                        if (bloquearAsiento(eventoId, String.valueOf(fila), columna)) {
                            LOG.info("✅ Warm-up exitoso para eventoId: {} usando asiento por defecto (fila {}, columna {})", eventoId, fila, columna);
                            return true;
                        }
                    }
                }
                LOG.warn("⚠️ No se pudo bloquear ningún asiento para eventoId: {} después de intentar {} filas x {} columnas (puede que todos estén ocupados, el evento no tenga asientos, o el proxy no esté disponible)", 
                    eventoId, maxFilas, maxColumnas);
                return false;
            }

            // Buscar el primer asiento LIBRE en el mapa - intentar con TODOS los asientos disponibles
            int intentos = 0;
            int asientosLibres = 0;
            
            for (MapaAsientosDTO.AsientoDTO asiento : asientos) {
                if (asiento.getEstado() == MapaAsientosDTO.AsientoDTO.EstadoAsiento.LIBRE && 
                    asiento.getFila() != null && 
                    asiento.getNumero() != null) {
                    
                    asientosLibres++;
                    intentos++;
                    
                    // Intentar bloquear este asiento
                    if (bloquearAsiento(eventoId, asiento.getFila(), asiento.getNumero())) {
                        LOG.info("✅ Warm-up exitoso para eventoId: {} usando asiento (fila {}, columna {}) después de {} intentos", 
                            eventoId, asiento.getFila(), asiento.getNumero(), intentos);
                        return true;
                    }
                    // Si falla, continuar con el siguiente asiento
                }
            }

            LOG.warn("No se encontró ningún asiento libre para bloquear en eventoId: {} después de intentar con {} asientos libres (total {} asientos en el mapa)", 
                eventoId, intentos, asientos.size());
            return false;
        } catch (Exception e) {
            LOG.error("Error al intentar bloquear asiento para eventoId: {}", eventoId, e);
            return false;
        }
    }

    /**
     * Bloquea un asiento específico.
     * 
     * @param eventoId ID del evento
     * @param fila Fila del asiento (String, puede ser número o letra)
     * @param numero Número del asiento
     * @return true si el bloqueo fue exitoso, false en caso contrario
     */
    private boolean bloquearAsiento(Long eventoId, String fila, Integer numero) {
        try {
            // Convertir fila de String a Integer
            Integer filaInt = convertirFilaAInteger(fila);
            if (filaInt == null) {
                LOG.warn("No se pudo convertir fila '{}' a Integer", fila);
                return false;
            }

            // Crear request de bloqueo
            BloqueoAsientosRequestDTO request = new BloqueoAsientosRequestDTO();
            request.setEventoId(eventoId);
            
            BloqueoAsientosRequestDTO.AsientoBloqueoDTO asientoDto = new BloqueoAsientosRequestDTO.AsientoBloqueoDTO();
            asientoDto.setFila(filaInt);
            asientoDto.setColumna(numero);
            
            request.setAsientos(List.of(asientoDto));

            // Intentar bloquear
            BloqueoAsientosResponseDTO respuesta = proxyAsientosService.bloquearAsientos(request);
            
            if (respuesta != null && Boolean.TRUE.equals(respuesta.getExitoso())) {
                LOG.info("✅ Asiento bloqueado exitosamente para warm-up: eventoId={}, fila={}, numero={}", eventoId, fila, numero);
                return true;
            } else {
                String mensaje = respuesta != null ? respuesta.getMensaje() : "Respuesta nula";
                
                // Verificar si es un error de conexión (proxy no disponible)
                if (mensaje != null && (mensaje.contains("I/O error") || mensaje.contains("Connection refused") || 
                    mensaje.contains("connect timed out") || mensaje.contains("No route to host"))) {
                    LOG.warn("⚠️ Proxy no disponible para warm-up: eventoId={}, fila={}, numero={}, error={}", 
                        eventoId, fila, numero, mensaje);
                    // No es un error crítico, el warm-up puede fallar si el proxy no está listo
                    return false;
                }
                
                LOG.debug("❌ No se pudo bloquear asiento: eventoId={}, fila={}, numero={}, motivo={}", 
                    eventoId, fila, numero, mensaje);
                // Si la respuesta indica que el asiento no está disponible, es normal (puede estar ocupado)
                if (mensaje != null && (mensaje.contains("no disponible") || mensaje.contains("ocupado") || mensaje.contains("bloqueado"))) {
                    LOG.debug("Asiento no disponible (esperado): eventoId={}, fila={}, numero={}", eventoId, fila, numero);
                }
                return false;
            }
        } catch (Exception e) {
            LOG.warn("Excepción al bloquear asiento para warm-up: eventoId={}, fila={}, numero={}, error={}", 
                eventoId, fila, numero, e.getMessage());
            return false;
        }
    }

    /**
     * Convierte una fila de String a Integer.
     * Si es una letra (A-Z), la convierte a número (A=1, B=2, etc.).
     * Si es un número, lo parsea directamente.
     */
    private Integer convertirFilaAInteger(String fila) {
        if (fila == null || fila.isEmpty()) {
            return null;
        }

        String filaUpper = fila.trim().toUpperCase();
        
        // Si es una letra (A-Z)
        if (filaUpper.length() == 1 && filaUpper.charAt(0) >= 'A' && filaUpper.charAt(0) <= 'Z') {
            return filaUpper.charAt(0) - 'A' + 1; // A=1, B=2, C=3, etc.
        }
        
        // Si es un número, intentar parsearlo
        try {
            return Integer.parseInt(filaUpper);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

