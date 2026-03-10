# E-commerce Backend (Prueba Técnica)

Backend de dos **microservicios** (Products e Inventory) con Java 17, Spring Boot 3.3, API JSON:API, arquitectura hexagonal y persistencia PostgreSQL.

---

## Índice

- [Arquitectura del proyecto](#arquitectura-del-proyecto)
- [Requisitos](#requisitos)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Despliegue local con Docker](#despliegue-local-con-docker)
- [Despliegue en producción](#despliegue-en-producción)
- [Ejecución sin Docker](#ejecución-sin-docker)
- [Seguridad y endpoints](#seguridad-y-endpoints)
- [Pruebas](#pruebas)
- [Documentación adicional](#documentación-adicional)
- [Tecnologías](#tecnologías)

---

## Arquitectura del proyecto

### Visión general (C4 – contexto y contenedores)

El sistema expone dos microservicios que pueden consumirse desde un frontend (Vue) o clientes HTTP:

```
+------------------------+                    +------------------------+
|  products-service      |                    |  inventory-service      |
|  (Spring Boot, :8080)  |                    |  (Spring Boot, :8081)  |
|  CRUD productos       |   HTTP + API Key   |  Inventario por producto |
|  Login JWT             |<-------------------|  POST /purchases        |
|  PostgreSQL            |   (WebClient)      |  Idempotencia, lock     |
|  (products_db)          |                    |  PostgreSQL (inventory_db)|
+------------------------+                    +------------------------+
```

- **products-service**: Catálogo (CRUD), listado con paginación/filtros/orden, login y emisión de JWT. Acepta `X-API-Key` (llamadas entre servicios) o JWT (frontend).
- **inventory-service**: Inventario por producto, compras con idempotencia (`Idempotency-Key`) y optimistic locking. Llama a products-service para validar productos; usa Resilience4j (retry + circuit breaker).

Diagramas C4 completos: [docs/ARQUITECTURA_C4.md](docs/ARQUITECTURA_C4.md).

### Arquitectura hexagonal (por microservicio)

Cada servicio está organizado en capas bajo `src/main/java`:

| Capa | Descripción |
|------|-------------|
| **domain** | Entidades, value objects y excepciones de dominio. Sin dependencias de framework. |
| **application.port.in** | Casos de uso (interfaces que expone la aplicación). |
| **application.port.out** | Puertos de salida (repositorios, cliente HTTP a Products). |
| **application.model** | Comandos, filtros y resultados de casos de uso. |
| **application.service** | Implementación de los casos de uso. |
| **infrastructure.persistence** | Adaptadores JPA (Spring Data). |
| **infrastructure.web** | Controladores REST y DTOs JSON:API. |
| **infrastructure.client** | (solo inventory) Cliente WebClient a Products. |
| **infrastructure.config** | Seguridad, filtros (correlation-id, rate limit, JWT, API Key). |

Las dependencias apuntan hacia el dominio: la infraestructura implementa los puertos; la aplicación orquesta dominio y puertos.

---

## Requisitos

- **Java 17+**
- **Maven 3.9+** (o usar `./mvnw` incluido)
- **Docker y Docker Compose** (para despliegue con bases de datos o stack completo)

---

## Estructura del repositorio

```
ecommerceApp/
├── pom.xml                    # POM padre (solo módulos, no código)
├── compose.yaml               # Docker Compose: DBs + ambos servicios
├── mvnw, .mvnw/               # Maven Wrapper
├── products-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/...      # Código Products (hexagonal)
├── inventory-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/...      # Código Inventory (hexagonal)
└── docs/
    ├── ARQUITECTURA_C4.md
    ├── DECISIONES_TECNICAS.md
    ├── COMPLIANCE_BACKEND.md
    └── curl-examples.md
```

No hay `src` en la raíz: el padre es solo agregador; cada microservicio es un módulo Maven independiente con su propio JAR.

---

## Despliegue local con Docker

### Opción A: Stack completo (recomendado para desarrollo local)

Levanta bases de datos PostgreSQL y ambos servicios en contenedores:

```bash
# Compilar JARs y construir imágenes
./mvnw clean package -DskipTests -pl products-service,inventory-service -am
docker compose up --build
```

- **Products**: http://localhost:8080  
- **Inventory**: http://localhost:8081  
- **Products DB**: localhost:5432 (usuario/contraseña: `products`/`products`)  
- **Inventory DB**: localhost:5433 (usuario/contraseña: `inventory`/`inventory`)

El `compose.yaml` ya inyecta en **inventory-service** la URL de Products (`http://products-service:8080`) y la API Key. No hace falta configurar nada más para probar el flujo completo.

### Opción B: Solo bases de datos con Docker

Si prefieres ejecutar las aplicaciones con Maven en tu máquina:

```bash
# Solo PostgreSQL
docker compose up -d products-db inventory-db

# Terminal 1 – Products (puerto 8080)
cd products-service && ../mvnw spring-boot:run

# Terminal 2 – Inventory (puerto 8081)
cd inventory-service && ../mvnw spring-boot:run
```

En este caso, Inventory usa por defecto `http://localhost:8080` para Products. Si cambias el puerto de Products, configura las variables indicadas más abajo.

### Variables de entorno (Docker / local)

Para **products-service** (producción o override local):

| Variable | Descripción | Por defecto (local) |
|----------|-------------|----------------------|
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://localhost:5432/products_db` |
| `SPRING_DATASOURCE_USERNAME` | Usuario DB | `products` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña DB | `products` |
| `INTERNAL_API_KEY` / `app.internal.api-key` | API Key para llamadas entre servicios | `internal-api-key-change-in-prod` |
| `JWT_SECRET` / `app.jwt.secret` | Secret para firmar/validar JWT | (valor por defecto en código; **cambiar en producción**) |
| `app.rate-limit.max-requests-per-minute` | Rate limit por IP | `100` |

Para **inventory-service**:

| Variable | Descripción | Por defecto (local) |
|----------|-------------|----------------------|
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL | `jdbc:postgresql://localhost:5433/inventory_db` |
| `APP_PRODUCTS_SERVICE_URL` | URL del Products Service | `http://localhost:8080` (en Docker: `http://products-service:8080`) |
| `APP_PRODUCTS_SERVICE_API_KEY` | API Key para llamar a Products | `internal-api-key-change-in-prod` |
| `APP_PRODUCTS_SERVICE_TIMEOUT_MS` | Timeout HTTP a Products (ms) | `3000` |
| `JWT_SECRET` | Mismo secret que Products (validar JWT) | (mismo que Products) |

---

## Despliegue en producción

### Imágenes Docker

Cada servicio tiene su propio **Dockerfile** (multi-stage: build con JDK, runtime con JRE):

- `products-service/Dockerfile` → imagen que expone el puerto **8080**.
- `inventory-service/Dockerfile` → imagen que expone el puerto **8081**.

Ejemplo de construcción y etiquetado para un registro:

```bash
./mvnw clean package -DskipTests -pl products-service,inventory-service -am

docker build -f products-service/Dockerfile -t mi-registry/products-service:1.0 .
docker build -f inventory-service/Dockerfile -t mi-registry/inventory-service:1.0 .
docker push mi-registry/products-service:1.0
docker push mi-registry/inventory-service:1.0
```

El contexto de build es la **raíz del proyecto** (donde está el POM padre y `mvnw`), ya que los Dockerfiles ejecutan Maven desde ahí.

### Recomendaciones de producción

1. **Secrets**: No usar valores por defecto de `app.jwt.secret` ni `app.internal.api-key`. Inyectar secretos vía variables de entorno o un gestor de secretos (por ejemplo, AWS Secrets Manager, Vault).
2. **Bases de datos**: Usar PostgreSQL gestionado o instancias dedicadas; no compartir credenciales entre entornos. Las migraciones se ejecutan con Flyway al arrancar (`spring.flyway.enabled=true`).
3. **Red**: En Kubernetes o ECS, configurar `APP_PRODUCTS_SERVICE_URL` con el nombre de servicio interno (por ejemplo `http://products-service:8080`).
4. **Health checks**: Los endpoints de Actuator `liveness` y `readiness` están habilitados; usarlos para probes en el orquestador.
5. **Observabilidad**: Los logs incluyen `correlationId` (header `X-Correlation-ID`). Exponer métricas (Actuator) e integrar con tu sistema de monitoreo.
6. **Rate limit**: Ajustar `app.rate-limit.max-requests-per-minute` según capacidad y política de uso.

### Orden de arranque

En producción, **products-service** debe estar disponible antes de que **inventory-service** reciba tráfico real, ya que Inventory llama a Products para validar productos. Usar health checks y políticas de reintento en el orquestador para el arranque en paralelo.

---

## Ejecución sin Docker

Solo compilar y ejecutar tests:

```bash
./mvnw clean package -pl products-service,inventory-service -am
```

Ejecutar un servicio (con DBs ya levantadas, por ejemplo con Docker):

```bash
cd products-service && ../mvnw spring-boot:run
# o
cd inventory-service && ../mvnw spring-boot:run
```

Los tests de integración usan **H2** en perfil `test` (no requieren Docker). Para ejecutar solo los tests de un módulo:

```bash
./mvnw test -pl products-service
./mvnw test -pl inventory-service
```

---

## Seguridad y endpoints

- **Entre servicios**: Header `X-API-Key` con el valor configurado en `app.internal.api-key` (Products valida; Inventory lo envía en las llamadas a Products).
- **Frontend / usuarios**: Login en Products (`POST /auth/login`) devuelve un JWT. Enviar `Authorization: Bearer <token>` en las peticiones a `/api/**` de ambos servicios.
- **Rate limit**: Por IP, configurable; respuestas 429 cuando se supera el límite.
- **Content-Type**: API en formato JSON:API (`Content-Type` y `Accept: application/vnd.api+json`).

### Endpoints principales

| Servicio | Método | Ruta | Descripción |
|----------|--------|------|-------------|
| Products | POST | `/auth/login` | Login (body: `username`, `password`) → JWT |
| Products | GET | `/api/products` | Listado (paginación, filtros, orden) |
| Products | GET | `/api/products/{id}` | Producto por UUID |
| Products | POST | `/api/products` | Crear producto |
| Products | PATCH | `/api/products/{id}` | Actualizar |
| Products | DELETE | `/api/products/{id}` | Eliminar |
| Inventory | GET | `/api/inventory/{productId}` | Consultar inventario |
| Inventory | PUT | `/api/inventory/{productId}` | Crear/actualizar stock |
| Inventory | POST | `/api/purchases` | Compra (header opcional: `Idempotency-Key`) |

**Documentación interactiva de la API (Swagger UI / OpenAPI 3):**

- **Products:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) — especificación: `/v3/api-docs`
- **Inventory:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) — especificación: `/v3/api-docs`

En Swagger UI puedes probar todos los endpoints, ver parámetros y respuestas, y autorizar con JWT o API Key. Generado con [springdoc-openapi](https://springdoc.org/).

Health: `http://localhost:8080/actuator/health` y `http://localhost:8081/actuator/health`.  
Ejemplos curl: [docs/curl-examples.md](docs/curl-examples.md).

---

## Pruebas

- **Unitarias**: Servicios de aplicación (casos de uso) con Mockito.
- **Integración**: Controladores con MockMvc y perfil `test` (H2 en memoria).

Ejecutar todos los tests:

```bash
./mvnw test -pl products-service,inventory-service -am
```

Cobertura objetivo en módulos críticos: 75–80%. Checklist de cumplimiento de la prueba técnica: [docs/COMPLIANCE_BACKEND.md](docs/COMPLIANCE_BACKEND.md).

---

## Documentación adicional

| Documento | Contenido |
|-----------|-----------|
| **Swagger UI** (por servicio) | **Documentación de la API**: endpoints, parámetros, esquemas JSON:API, respuestas y autenticación. Products: `/swagger-ui.html` (8080); Inventory: `/swagger-ui.html` (8081). |
| [docs/ARQUITECTURA_C4.md](docs/ARQUITECTURA_C4.md) | Diagramas C4 (contexto y contenedores). |
| [docs/DIAGRAMAS_UML.md](docs/DIAGRAMAS_UML.md) | Diagramas UML: casos de uso, clases (dominio) y despliegue. |
| [docs/DECISIONES_TECNICAS.md](docs/DECISIONES_TECNICAS.md) | Persistencia, resiliencia, idempotencia, concurrencia, seguridad. |
| [docs/COMPLIANCE_BACKEND.md](docs/COMPLIANCE_BACKEND.md) | Checklist de requisitos de la prueba técnica (backend). |
| [docs/curl-examples.md](docs/curl-examples.md) | Ejemplos curl (login, productos, inventario, compras, health, Swagger). |

---

## Tecnologías

- **Java 17**, **Spring Boot 3.3**
- **Spring Web**, **Spring Data JPA**, **Flyway**, **Validation**
- **PostgreSQL** (producción/Docker); **H2** (tests)
- **Resilience4j**: retry y circuit breaker en el cliente Inventory → Products
- **Spring Security**: API Key, JWT (jjwt), rate limit por IP
- **Actuator**: health (liveness/readiness), info, metrics
- **Springdoc OpenAPI**: Swagger UI y `/v3/api-docs` en cada servicio
- **JUnit 5**, **Mockito**, **MockMvc** para pruebas
