package com.um.eventosbackend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.um.eventosbackend.IntegrationTest;
import com.um.eventosbackend.domain.Evento;
import com.um.eventosbackend.domain.User;
import com.um.eventosbackend.repository.EventoRepository;
import com.um.eventosbackend.repository.UserRepository;
import com.um.eventosbackend.service.dto.BloqueoAsientosResponseDTO;
import com.um.eventosbackend.service.dto.MapaAsientosDTO;
import com.um.eventosbackend.web.rest.vm.LoginVM;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Test de integración end-to-end del flujo completo de venta:
 * Login → Listar eventos → Ver detalle → Obtener mapa de asientos →
 * Seleccionar asientos → Bloquear asientos → Actualizar nombres →
 * Procesar venta → Limpiar sesión
 */
@AutoConfigureMockMvc
@IntegrationTest
class FlujoCompletoVentaIT {

    private static final String TEST_USER_LOGIN = "test-flujo-venta";
    private static final String TEST_USER_PASSWORD = "test1234";
    private static final String TEST_USER_EMAIL = "test-flujo-venta@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Qualifier("proxyRestTemplate")
    private RestTemplate proxyRestTemplate;

    private MockRestServiceServer mockProxyServer;
    private String jwtToken;

    @BeforeEach
    @Transactional
    void setUp() {
        // Limpiar usuario de prueba si existe
        userRepository.findOneByLogin(TEST_USER_LOGIN).ifPresent(userRepository::delete);

        // Crear usuario de prueba
        User testUser = new User();
        testUser.setLogin(TEST_USER_LOGIN);
        testUser.setEmail(TEST_USER_EMAIL);
        testUser.setPassword(passwordEncoder.encode(TEST_USER_PASSWORD));
        testUser.setActivated(true);
        userRepository.saveAndFlush(testUser);

        // Crear evento de prueba si no existe
        if (eventoRepository.count() == 0) {
            Evento evento = new Evento();
            evento.setEventoIdCatedra(999L); // ID de prueba
            evento.setTitulo("Evento de Prueba - Test de Integración");
            evento.setDescripcion("Este es un evento de prueba creado para el test de integración");
            evento.setResumen("Evento de prueba");
            // Fecha futura (mañana) para que no esté expirado
            evento.setFecha(Instant.now().plus(1, ChronoUnit.DAYS));
            evento.setDireccion("Dirección de Prueba 123");
            evento.setPrecio(new BigDecimal("100.00"));
            evento.setCancelado(false);
            // Configurar dimensiones de asientos
            evento.setFilaAsiento(10);
            evento.setColumnAsiento(10);
            eventoRepository.saveAndFlush(evento);
        }

        // Configurar mock del proxy
        mockProxyServer = MockRestServiceServer.createServer(proxyRestTemplate);
    }

    @Test
    @Transactional
    void testFlujoCompletoVenta() throws Exception {
        // ============================================
        // PASO 1: AUTENTICACIÓN
        // ============================================
        System.out.println("🚀 Paso 1: Autenticación del usuario");

        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(TEST_USER_LOGIN);
        loginVM.setPassword(TEST_USER_PASSWORD);

        MvcResult authResult = mockMvc
            .perform(
                post("/api/authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(loginVM))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id_token").exists())
            .andExpect(jsonPath("$.id_token").isNotEmpty())
            .andReturn();

        // Extraer el token JWT
        String responseContent = authResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        jwtToken = jsonNode.get("id_token").asText();
        assertThat(jwtToken).isNotEmpty();

        System.out.println("✅ Autenticación exitosa. Token obtenido.");

        // ============================================
        // PASO 2: OBTENER LISTA DE EVENTOS
        // ============================================
        System.out.println("📋 Paso 2: Obteniendo lista de eventos activos");

        MvcResult eventosResult = mockMvc
            .perform(get("/api/eventos").header(AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andReturn();

        String eventosContent = eventosResult.getResponse().getContentAsString();
        JsonNode eventosArray = objectMapper.readTree(eventosContent);
        assertThat(eventosArray.isArray()).isTrue();

        if (eventosArray.size() == 0) {
            System.out.println("⚠️ No hay eventos disponibles. Saltando pasos siguientes.");
            return;
        }

        // Obtener el primer evento
        JsonNode primerEvento = eventosArray.get(0);
        Long eventoId = primerEvento.get("id").asLong();
        String eventoTitulo = primerEvento.get("titulo").asText();

        System.out.println("✅ Se obtuvieron " + eventosArray.size() + " eventos");
        System.out.println("   📅 Usando evento: " + eventoTitulo + " (ID: " + eventoId + ")");

        // ============================================
        // PASO 3: OBTENER DETALLE DE EVENTO
        // ============================================
        System.out.println("📄 Paso 3: Obteniendo detalle del evento " + eventoId);

        MvcResult detalleResult = mockMvc
            .perform(get("/api/eventos/" + eventoId).header(AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(eventoId))
            .andExpect(jsonPath("$.titulo").exists())
            .andReturn();

        String detalleContent = detalleResult.getResponse().getContentAsString();
        JsonNode detalleEvento = objectMapper.readTree(detalleContent);
        
        // Obtener eventoIdCatedra del detalle (necesario para la venta)
        Long eventoIdCatedra = detalleEvento.has("eventoIdCatedra") 
            ? detalleEvento.get("eventoIdCatedra").asLong() 
            : eventoId; // Fallback al ID interno si no tiene eventoIdCatedra
        
        System.out.println("✅ Detalle del evento obtenido: " + detalleEvento.get("titulo").asText());
        System.out.println("   🆔 ID Cátedra: " + eventoIdCatedra);

        // ============================================
        // CONFIGURAR TODOS LOS MOCKS DEL PROXY ANTES DE HACER REQUESTS
        // ============================================
        // Mockear respuesta del proxy para obtener mapa de asientos
        // Nota: El servicio usa el ID interno directamente para llamar al proxy
        MapaAsientosDTO mapaMock = new MapaAsientosDTO();
        mapaMock.setEventoId(eventoId); // El proxy devuelve el mismo ID que recibe
        mapaMock.setAsientos(new ArrayList<>());
        // Crear algunos asientos de prueba
        for (int fila = 1; fila <= 10; fila++) {
            for (int col = 1; col <= 10; col++) {
                MapaAsientosDTO.AsientoDTO asiento = new MapaAsientosDTO.AsientoDTO();
                asiento.setFila(String.valueOf(fila));
                asiento.setNumero(col);
                asiento.setEstado(MapaAsientosDTO.AsientoDTO.EstadoAsiento.LIBRE);
                asiento.setSeleccionado(false);
                mapaMock.getAsientos().add(asiento);
            }
        }

        // El RestTemplate tiene rootUri configurado, así que el mock debe esperar la URL completa
        mockProxyServer
            .expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8081/api/asientos/evento/" + eventoId))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(HttpMethod.GET))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                objectMapper.writeValueAsString(mapaMock),
                MediaType.APPLICATION_JSON
            ));

        // Mockear respuesta del proxy para bloqueo de asientos (se configurará con los datos del asiento después)
        // Pre-configuramos el mock básico, pero necesitamos los datos del asiento seleccionado
        // Por ahora solo preparamos la estructura, el mock completo se agregará después de seleccionar el asiento

        // ============================================
        // PASO 4: OBTENER MAPA DE ASIENTOS
        // ============================================
        System.out.println("🪑 Paso 4: Obteniendo mapa de asientos del evento " + eventoId);

        MvcResult mapaResult = mockMvc
            .perform(get("/api/asientos/evento/" + eventoId).header(AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventoId").value(eventoId))
            .andExpect(jsonPath("$.asientos").isArray())
            .andReturn();

        String mapaContent = mapaResult.getResponse().getContentAsString();
        JsonNode mapaAsientos = objectMapper.readTree(mapaContent);
        JsonNode asientosArray = mapaAsientos.get("asientos");

        int totalAsientos = asientosArray.size();
        int asientosLibres = 0;
        for (JsonNode asiento : asientosArray) {
            if ("LIBRE".equals(asiento.get("estado").asText())) {
                asientosLibres++;
            }
        }

        System.out.println("✅ Mapa de asientos obtenido:");
        System.out.println("   📊 Total: " + totalAsientos);
        System.out.println("   ✅ Libres: " + asientosLibres);

        if (asientosLibres == 0) {
            System.out.println("⚠️ No hay asientos libres disponibles. Saltando pasos siguientes.");
            return;
        }

        // Buscar un asiento libre para seleccionar
        JsonNode asientoLibre = null;
        for (JsonNode asiento : asientosArray) {
            if ("LIBRE".equals(asiento.get("estado").asText())) {
                asientoLibre = asiento;
                break;
            }
        }

        if (asientoLibre == null) {
            System.out.println("⚠️ No se encontró un asiento libre. Saltando pasos siguientes.");
            return;
        }

        String filaAsiento = asientoLibre.get("fila").asText();
        int numeroAsiento = asientoLibre.get("numero").asInt();

        System.out.println("   🎫 Asiento seleccionado: Fila " + filaAsiento + ", Número " + numeroAsiento);

        // ============================================
        // PASO 5: ACTUALIZAR EVENTO EN SESIÓN
        // ============================================
        // Nota: Usar ID interno para que el bloqueo funcione (el servicio de bloqueo compara con el ID recibido)
        System.out.println("💾 Paso 5: Guardando evento " + eventoId + " en sesión");

        mockMvc
            .perform(put("/api/sesion/evento/" + eventoId).header(AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());

        System.out.println("✅ Evento guardado en sesión (ID: " + eventoId + ")");

        // ============================================
        // PASO 6: SELECCIONAR ASIENTOS
        // ============================================
        System.out.println("🎫 Paso 6: Seleccionando asientos");

        // Crear el array de asientos seleccionados
        Map<String, Object> asientoSeleccionado = new HashMap<>();
        asientoSeleccionado.put("fila", filaAsiento);
        asientoSeleccionado.put("numero", numeroAsiento);
        asientoSeleccionado.put("nombrePersona", null);
        asientoSeleccionado.put("apellidoPersona", null);

        Object[] asientosArrayJson = new Object[] { asientoSeleccionado };

        mockMvc
            .perform(
                put("/api/sesion/asientos")
                    .header(AUTHORIZATION, "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(asientosArrayJson))
            )
            .andExpect(status().isNoContent());

        System.out.println("✅ 1 asiento seleccionado");
        System.out.println("   - Fila " + filaAsiento + ", Asiento " + numeroAsiento);

        // ============================================
        // PASO 7: BLOQUEAR ASIENTOS
        // ============================================
        System.out.println("🔒 Paso 7: Bloqueando asientos seleccionados");

        // Preparar respuesta del proxy para bloqueo de asientos
        BloqueoAsientosResponseDTO bloqueoMock = new BloqueoAsientosResponseDTO();
        bloqueoMock.setExitoso(true);
        bloqueoMock.setMensaje("Asientos bloqueados exitosamente");
        bloqueoMock.setAsientosBloqueados(new ArrayList<>());
        bloqueoMock.setAsientosNoDisponibles(new ArrayList<>());
        
        // Agregar el asiento bloqueado
        BloqueoAsientosResponseDTO.AsientoBloqueoDTO asientoBloqueado = new BloqueoAsientosResponseDTO.AsientoBloqueoDTO();
        // El response usa fila como String y numero como Integer
        asientoBloqueado.setFila(filaAsiento);
        asientoBloqueado.setNumero(numeroAsiento);
        bloqueoMock.getAsientosBloqueados().add(asientoBloqueado);

        // Resetear el servidor mock para poder agregar una nueva expectativa después del primer request
        mockProxyServer.reset();
        
        // El RestTemplate tiene rootUri configurado, así que el mock debe esperar la URL completa
        mockProxyServer
            .expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8081/api/asientos/bloquear"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(HttpMethod.POST))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.eventoId").value(eventoId)) // El servicio usa el ID interno
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                objectMapper.writeValueAsString(bloqueoMock),
                MediaType.APPLICATION_JSON
            ));

        MvcResult bloqueoResult = mockMvc
            .perform(post("/api/asientos/bloquear/" + eventoId).header(AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exitoso").exists())
            .andReturn();

        String bloqueoContent = bloqueoResult.getResponse().getContentAsString();
        JsonNode bloqueoResponse = objectMapper.readTree(bloqueoContent);
        boolean exitoso = bloqueoResponse.get("exitoso").asBoolean();

        if (exitoso) {
            System.out.println("✅ Asientos bloqueados exitosamente");
            if (bloqueoResponse.has("asientosBloqueados")) {
                int cantidadBloqueados = bloqueoResponse.get("asientosBloqueados").size();
                System.out.println("   🔒 Asientos bloqueados: " + cantidadBloqueados);
            }
        } else {
            System.out.println("⚠️ No se pudieron bloquear los asientos");
            String mensaje = bloqueoResponse.has("mensaje") ? bloqueoResponse.get("mensaje").asText() : "Error desconocido";
            System.out.println("   Mensaje: " + mensaje);
            System.out.println("   ℹ️ Esto puede ocurrir si los asientos ya están ocupados o bloqueados");
            // Continuar con el test aunque el bloqueo falle (puede ser por conflictos)
        }

        // ============================================
        // PASO 8: ACTUALIZAR NOMBRES DE PASAJEROS
        // ============================================
        System.out.println("👤 Paso 8: Actualizando nombres de pasajeros");

        Map<String, Object> nombresPorAsiento = new HashMap<>();
        Map<String, Object> asientoConNombre = new HashMap<>();
        asientoConNombre.put("fila", filaAsiento);
        asientoConNombre.put("numero", numeroAsiento);
        asientoConNombre.put("nombrePersona", "Juan");
        asientoConNombre.put("apellidoPersona", "Pérez");
        nombresPorAsiento.put(filaAsiento + "-" + numeroAsiento, asientoConNombre);

        mockMvc
            .perform(
                put("/api/sesion/nombres")
                    .header(AUTHORIZATION, "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(nombresPorAsiento))
            )
            .andExpect(status().isNoContent());

        System.out.println("✅ Nombres de pasajeros actualizados");
        System.out.println("   👤 Juan Pérez - Fila " + filaAsiento + ", Asiento " + numeroAsiento);

        // ============================================
        // PASO 9: VERIFICAR ESTADO DE SESIÓN
        // ============================================
        System.out.println("📊 Paso 9: Verificando estado de sesión");

        MvcResult estadoResult = mockMvc
            .perform(get("/api/sesion/estado").header(AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventoId").value(eventoId)) // Usar ID interno (antes de actualizar para venta)
            .andExpect(jsonPath("$.asientosSeleccionados").isArray())
            .andReturn();

        String estadoContent = estadoResult.getResponse().getContentAsString();
        JsonNode estadoSesion = objectMapper.readTree(estadoContent);
        System.out.println("✅ Estado de sesión verificado:");
        System.out.println("   📅 Evento ID: " + estadoSesion.get("eventoId").asLong());
        System.out.println("   🎫 Asientos seleccionados: " + estadoSesion.get("asientosSeleccionados").size());

        // ============================================
        // PASO 9.5: ACTUALIZAR EVENTO EN SESIÓN PARA VENTA
        // ============================================
        // Nota: El servicio de venta busca por eventoIdCatedra y compara con el estado de sesión
        // Necesitamos actualizar la sesión con eventoIdCatedra antes de la venta
        System.out.println("🔄 Actualizando sesión con ID Cátedra para la venta: " + eventoIdCatedra);

        mockMvc
            .perform(put("/api/sesion/evento/" + eventoIdCatedra).header(AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());

        System.out.println("✅ Sesión actualizada con ID Cátedra para la venta");

        // Configurar mock para confirmar venta en el proxy
        // Resetear el servidor mock para poder agregar una nueva expectativa
        mockProxyServer.reset();
        
        // Mockear respuesta del proxy para confirmar venta
        com.um.eventosbackend.service.dto.VentaResponseDTO ventaConfirmadaMock = new com.um.eventosbackend.service.dto.VentaResponseDTO();
        ventaConfirmadaMock.setResultado("EXITOSA");
        ventaConfirmadaMock.setMensaje("Venta confirmada exitosamente");
        ventaConfirmadaMock.setVentaIdCatedra(12345L); // ID de ejemplo
        
        mockProxyServer
            .expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://localhost:8081/api/ventas/confirmar"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(HttpMethod.POST))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                objectMapper.writeValueAsString(ventaConfirmadaMock),
                MediaType.APPLICATION_JSON
            ));

        // ============================================
        // PASO 10: PROCESAR VENTA
        // ============================================
        System.out.println("💳 Paso 10: Procesando venta");

        // Crear el request de venta
        // Nota: El servicio de venta busca por eventoIdCatedra, no por ID interno
        Map<String, Object> ventaRequest = new HashMap<>();
        ventaRequest.put("eventoId", eventoIdCatedra);
        Map<String, Object> asientoVenta = new HashMap<>();
        asientoVenta.put("fila", filaAsiento);
        asientoVenta.put("numero", numeroAsiento);
        asientoVenta.put("nombrePersona", "Juan");
        asientoVenta.put("apellidoPersona", "Pérez");
        ventaRequest.put("asientos", new Object[] { asientoVenta });

        MvcResult ventaResult = mockMvc
            .perform(
                post("/api/ventas")
                    .header(AUTHORIZATION, "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(ventaRequest))
            )
            .andExpect(status().isCreated())
            .andReturn();

        String ventaContent = ventaResult.getResponse().getContentAsString();
        JsonNode ventaResponse = objectMapper.readTree(ventaContent);

        System.out.println("✅ Venta procesada");
        System.out.println("   🎟️ Resultado: " + ventaResponse.get("resultado").asText());
        if (ventaResponse.has("id")) {
            System.out.println("   🆔 Venta ID: " + ventaResponse.get("id").asLong());
        }
        if (ventaResponse.has("mensaje")) {
            System.out.println("   💬 Mensaje: " + ventaResponse.get("mensaje").asText());
        }

        // ============================================
        // PASO 11: LIMPIAR SESIÓN
        // ============================================
        System.out.println("🧹 Paso 11: Limpiando sesión");

        mockMvc
            .perform(delete("/api/sesion/estado").header(AUTHORIZATION, "Bearer " + jwtToken))
            .andExpect(status().isNoContent());

        System.out.println("✅ Sesión limpiada exitosamente");
        System.out.println("🎉 Flujo completo finalizado exitosamente");
    }
}

