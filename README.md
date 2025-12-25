# Sistema de Gestión de Eventos - Trabajo Final

Sistema completo de gestión de eventos con reserva de asientos, desarrollado como trabajo final para Programación 2 - 2025.

## 📋 Descripción

Sistema distribuido compuesto por tres módulos principales:
- **Backend**: API REST desarrollada con Spring Boot y JHipster
- **Proxy**: Servicio intermediario que gestiona Redis y consume mensajes de Kafka
- **Mobile**: Aplicación Android desarrollada con Kotlin Multiplatform y Jetpack Compose

## 🏗️ Arquitectura

```
┌─────────┐      ┌─────────┐      ┌─────────┐
│ Mobile  │─────▶│ Backend │─────▶│  Proxy  │
│  (KMP)  │◀─────│(Spring) │◀─────│(Spring) │
│Compose  │      │JHipster │      │         │
└─────────┘      └─────────┘      └─────────┘
                        │                 │
                        │                 │
                   ┌────▼─────┐      ┌─────▼─────┐
                   │PostgreSQL│      │  Redis    │
                   └──────────┘      └───────────┘
                                          │
                                          │
                                    ┌─────▼─────┐
                                    │  Kafka    │
                                    └─────┬─────┘
                                          │
                                    ┌─────▼─────┐
                                    │  Cátedra  │
                                    │  (Externa)│
                                    └───────────┘
```


## 🚀 Inicio Rápido

### Requisitos Previos

- **Java 17** o superior
- **Maven 3.6+**
- **PostgreSQL 12+**
- **Redis 6+** (opcional, para desarrollo local)
- **Kafka** (opcional, para desarrollo local)
- **Android Studio** (para desarrollo mobile)
- **Node.js 22+** y **npm** (para el frontend del backend)

### Configuración del Backend

1. **Configurar PostgreSQL**:
   ```bash
   # Crear base de datos
   createdb backend
   # O usar psql:
   psql -U postgres -c "CREATE DATABASE backend;"
   ```

2. **Configurar variables de entorno** (opcional):
   ```bash
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/backend
   export SPRING_DATASOURCE_USERNAME=backend
   export SPRING_DATASOURCE_PASSWORD=backend
   export CATEDRA_BASE_URL=http://192.168.194.250:8080
   export CATEDRA_AUTH_TOKEN=tu_token_aqui
   ```

3. **Instalar dependencias frontend** (requerido para desarrollo):
   ```bash
   cd backend
   ./npmw install
   ```

4. **Ejecutar el backend**:
   ```bash
   cd backend
   ./mvnw
   ```

El backend estará disponible en `http://localhost:8080`

### Configuración del Proxy

1. **Configurar variables de entorno** (opcional):
   ```bash
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
   export BACKEND_BASE_URL=http://localhost:8080
   export CATEDRA_BASE_URL=http://192.168.194.250:8080
   export CATEDRA_AUTH_TOKEN=tu_token_aqui
   ```

2. **Ejecutar el proxy**:
   ```bash
   cd proxy
   ./mvnw spring-boot:run
   ```

El proxy estará disponible en `http://localhost:8081`

### Configuración de la Aplicación Mobile

1. **Abrir el proyecto en Android Studio**:
   ```bash
   cd mobile
   # Abrir Android Studio y abrir la carpeta mobile
   ```

2. **Configurar `local.properties`** (si es necesario):
   ```properties
   sdk.dir=/ruta/a/tu/android/sdk
   ```

3. **Configurar URL del backend** (si es necesario):
   - Editar `mobile/app/src/main/java/com/um/eventosmobile/MainActivity.kt`
   - Para emulador: `http://10.0.2.2:8080`
   - Para dispositivo físico: `http://<IP_LOCAL>:8080`

4. **Compilar y ejecutar**:
   - Conectar un dispositivo Android o iniciar un emulador
   - Ejecutar desde Android Studio o usar:
     ```bash
     cd mobile
     ./gradlew installDebug
     ```

## 🧪 Pruebas de Integración

### Test End-to-End del Flujo Completo

El proyecto incluye un test de integración completo que valida el flujo completo de venta desde el login hasta la confirmación de venta.

**Ubicación:** `backend/src/test/java/com/um/eventosbackend/integration/FlujoCompletoVentaIT.java`

**Flujo que cubre:**
1. ✅ Autenticación de usuario
2. ✅ Obtención de lista de eventos activos
3. ✅ Visualización de detalle de evento
4. ✅ Obtención de mapa de asientos
5. ✅ Selección de asientos
6. ✅ Bloqueo de asientos
7. ✅ Actualización de nombres de pasajeros
8. ✅ Verificación de estado de sesión
9. ✅ Procesamiento de venta
10. ✅ Limpieza de sesión

### Ejecutar el Test

```bash
cd backend
./mvnw test -Dtest=FlujoCompletoVentaIT
```

O ejecutar todos los tests de integración:

```bash
./mvnw test
```

El test utiliza:
- **MockMvc** para simular requests HTTP
- **JWT** para autenticación
- **@Transactional** para limpieza automática de datos
- **@IntegrationTest** para configuración completa del contexto Spring

### Ver la Salida del Test

El test incluye mensajes informativos en consola que muestran el progreso de cada paso:

```
🚀 Paso 1: Autenticación del usuario
✅ Autenticación exitosa. Token obtenido.
📋 Paso 2: Obteniendo lista de eventos activos
✅ Se obtuvieron X eventos
...
🎉 Flujo completo finalizado exitosamente
```

## 📚 Documentación de Endpoints

### Autenticación

- `POST /api/authenticate` - Autenticación de usuario
  ```json
  {
    "username": "admin",
    "password": "admin"
  }
  ```
  Respuesta:
  ```json
  {
    "id_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```

### Eventos

- `GET /api/eventos` - Lista de eventos activos (requiere autenticación)
  - Solo devuelve eventos no cancelados y no expirados
  - Respuesta: Array de `EventoResumenDTO`

- `GET /api/eventos/{id}` - Detalle de un evento (requiere autenticación)
  - Respuesta: `EventoDetalleDTO`

### Asientos

- `GET /api/asientos/evento/{eventoId}` - Mapa de asientos (requiere autenticación)
  - Respuesta: `MapaAsientosDTO`

- `POST /api/asientos/bloquear/{eventoId}` - Bloquear asientos seleccionados (requiere autenticación)
  - Bloquea los asientos seleccionados en la sesión del usuario
  - Respuesta: `BloqueoAsientosResponseDTO`
  - Maneja conflictos automáticamente si los asientos ya están ocupados

### Sesión

- `GET /api/sesion/estado` - Estado actual de la sesión (requiere autenticación)
  - Respuesta: `EstadoSeleccionDTO`

- `PUT /api/sesion/evento/{eventoId}` - Actualizar evento seleccionado (requiere autenticación)

- `PUT /api/sesion/asientos` - Actualizar asientos seleccionados (requiere autenticación)
  ```json
  [
    {
      "fila": "1",
      "numero": 1,
      "nombrePersona": null,
      "apellidoPersona": null
    }
  ]
  ```

- `PUT /api/sesion/nombres` - Actualizar nombres de pasajeros (requiere autenticación)
  ```json
  {
    "1-1": {
      "fila": "1",
      "numero": 1,
      "nombrePersona": "Juan",
      "apellidoPersona": "Pérez"
    }
  }
  ```

- `DELETE /api/sesion/estado` - Limpiar estado de sesión (requiere autenticación)

### Ventas

- `POST /api/ventas` - Procesar una venta (requiere autenticación)
  ```json
  {
    "eventoId": 1
  }
  ```
  - Usa los asientos y nombres guardados en la sesión del usuario

### Administración

- `POST /api/admin/eventos/sincronizar` - Sincronizar eventos con cátedra (requiere rol ADMIN)
- `GET /api/admin/catedra-token` - Estado del token de cátedra (requiere rol ADMIN)
- `PUT /api/admin/catedra-token` - Actualizar token de cátedra (requiere rol ADMIN)

## 🔧 Configuración Avanzada

### Variables de Entorno del Backend

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `SPRING_DATASOURCE_URL` | URL de PostgreSQL | `jdbc:postgresql://localhost:5432/backend` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de PostgreSQL | `backend` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de PostgreSQL | `backend` |
| `CATEDRA_AUTH_TOKEN` | Token para API de cátedra | - |
| `CATEDRA_BASE_URL` | URL base de la API de cátedra | `http://192.168.194.250:8080` |

### Variables de Entorno del Proxy

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `REDIS_HOST` | Host de Redis | `192.168.194.250` |
| `REDIS_PORT` | Puerto de Redis | `6379` |
| `KAFKA_BOOTSTRAP_SERVERS` | Servidores de Kafka | `192.168.194.250:9092` |
| `KAFKA_CONSUMER_GROUP_ID` | ID del grupo de consumidores | `emicoratolo-2025` |
| `BACKEND_BASE_URL` | URL del backend | `http://localhost:8080` |
| `CATEDRA_BASE_URL` | URL de la API de cátedra | `http://192.168.194.250:8080` |
| `CATEDRA_AUTH_TOKEN` | Token para API de cátedra | - |

## 📱 Flujo de Usuario en la Aplicación Mobile

1. **Login/Registro**: El usuario ingresa sus credenciales o se registra
2. **Lista de Eventos**: Se muestran todos los eventos activos (no cancelados, no expirados)
3. **Detalle del Evento**: El usuario selecciona un evento para ver detalles
4. **Selección de Asientos**: El usuario selecciona hasta 4 asientos
5. **Bloqueo de Asientos**: Los asientos se bloquean por 2.5 minutos
6. **Ingreso de Nombres**: El usuario ingresa nombres y apellidos de los pasajeros
7. **Confirmación**: El usuario revisa los detalles de la compra
8. **Procesamiento**: Se procesa la venta
9. **Resultado**: Se muestra el resultado de la transacción

## 🔐 Seguridad

- **Autenticación**: JWT (JSON Web Tokens)
- **Almacenamiento de Tokens**: Encriptado usando Android Security Crypto
- **Roles**: `ROLE_USER`, `ROLE_ADMIN`
- **CORS**: Configurado para permitir requests desde la aplicación mobile

## 🐛 Manejo de Errores

### Eventos Cancelados y Expirados

- Los eventos cancelados se marcan con `cancelado = true` en la base de datos
- Los eventos expirados se detectan comparando la fecha con la fecha actual
- Ambos tipos de eventos se filtran automáticamente de los listados
- Las sesiones asociadas a eventos cancelados/expirados se limpian automáticamente

### Conflictos en Ventas Concurrentes

- Cuando múltiples usuarios intentan bloquear el mismo asiento, el sistema detecta el conflicto
- El usuario recibe un mensaje claro indicando que el asiento ya no está disponible
- La selección del usuario se limpia automáticamente para permitir seleccionar otros asientos
- Los conflictos se registran en los logs para análisis

## 📊 Características Implementadas

✅ Autenticación JWT  
✅ Registro de usuarios  
✅ Gestión de eventos (CRUD)  
✅ Selección y bloqueo de asientos  
✅ Gestión de sesiones de usuario  
✅ Procesamiento de ventas  
✅ Integración con Redis (a través del proxy)  
✅ Consumo de mensajes Kafka  
✅ Sincronización de eventos con API externa  
✅ Warm-up de Redis al iniciar  
✅ Manejo de eventos cancelados y expirados  
✅ Manejo de conflictos en ventas concurrentes  
✅ Test de integración end-to-end del flujo completo  
✅ Arquitectura MVVM en Mobile  
✅ UI moderna y profesional con Jetpack Compose  
✅ Arquitectura Hexagonal (Backend)  

## 🚧 Desarrollo

### Estructura del Proyecto

```
.
├── backend/          # Backend Spring Boot + JHipster
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/        # Código Java
│   │   │   ├── resources/   # Configuración
│   │   │   └── webapp/     # Frontend Angular (JHipster)
│   │   └── test/           # Tests de integración
│   └── pom.xml
├── proxy/            # Proxy Spring Boot
│   ├── src/
│   │   └── main/
│   │       ├── java/        # Servicios de Kafka y Redis
│   │       └── resources/   # Configuración
│   └── pom.xml
├── mobile/           # Aplicación Android
│   ├── app/          # Módulo Android (UI con Compose)
│   ├── shared/       # Módulo compartido KMP (lógica de negocio)
│   └── build.gradle.kts
├── scripts/          # Scripts de utilidad
└── README.md
```

### Tecnologías Utilizadas

**Backend:**
- Spring Boot 3.4.5
- JHipster 8.11.0
- PostgreSQL
- Liquibase
- Spring Security
- Spring Kafka
- JUnit 5 (para pruebas de integración)

**Proxy:**
- Spring Boot 3.4.5
- Spring Kafka
- Lettuce (Redis)
- JWT

**Mobile:**
- Kotlin Multiplatform
- Jetpack Compose
- Ktor Client
- Kotlinx Serialization
- Navigation Compose
- MVVM Architecture

## 📝 Licencia

Este proyecto fue desarrollado como trabajo final para Programación 2 - 2025.

## 👤 Autor

Emicoratolo - 2025

---

Para más información sobre configuración específica, consulta los READMEs individuales en cada módulo.
