# Cumplimiento Prueba Técnica – Backend (solo microservicios)

## Estructura del repositorio y `src`

- **No hay `src` en la raíz del proyecto.** El POM raíz (`ecommerce-app-parent`) es solo agregador: `packaging=pom` y declara los módulos `products-service` e `inventory-service`.
- Cada **microservicio** es un **módulo Maven** con su propio `src/main/java` y `src/test/java`. Así se cumple el requisito de **2 microservicios** (2 aplicaciones independientes, 2 JARs, Docker Compose con 2 servicios).
- El código de cada servicio vive únicamente en:
  - `products-service/src/main/java`
  - `inventory-service/src/main/java`

---

## Checklist requisitos transversales (Backend)

| Requisito | Estado | Dónde |
|-----------|--------|-------|
| Java 17+ / Spring Boot 3+ | ✅ | Parent POM, Spring Boot 3.3.5 |
| Maven | ✅ | `pom.xml`, `mvnw` |
| Docker + Docker Compose | ✅ | `compose.yaml`, Dockerfiles por servicio |
| API JSON:API | ✅ | Respuestas y errores en `application/vnd.api+json` |
| Paginación, filtros, orden documentados | ✅ | Swagger/OpenAPI por servicio |
| Persistencia SQL + Flyway | ✅ | PostgreSQL, migraciones en `db/migration/` |
| Justificación persistencia | ✅ | `docs/DECISIONES_TECNICAS.md` |
| Comunicación entre servicios con API Key | ✅ | Header `X-API-Key`; Inventory lo envía; Products lo valida |
| Endpoints al frontend con JWT | ✅ | Filtro JWT en `/api/**` (opcional por perfil) |
| Rate limit básico | ✅ | Filtro por IP (configurable) |
| Timeouts + retries + circuit breaker | ✅ | Resilience4j en Inventory (cliente a Products) |
| Error claro si Inventory/Products no responde | ✅ | 503 + JSON:API error |
| Logs con correlation-id | ✅ | Filtro + MDC; patrón de log incluye `%X{correlationId}` |
| Health liveness/readiness | ✅ | Actuator `livenessState`, `readinessState` |
| Métricas (Actuator) | ✅ | `management.endpoints.web.exposure.include=health,info,metrics` |
| Bean Validation, DTOs, capas | ✅ | Arquitectura hexagonal por servicio |
| Errores 404, 409, 422, 500 | ✅ | Controladores y manejadores de excepción |

---

## Microservicio A: Products Service

| Requisito | Estado |
|-----------|--------|
| id (UUID), sku único, name, price ≥ 0, status ACTIVE/INACTIVE, timestamps | ✅ |
| CRUD productos | ✅ |
| Listado: paginación, filtro status, búsqueda sku/name, orden price/createdAt | ✅ |
| 409 Conflict si SKU duplicado | ✅ |

---

## Microservicio B: Inventory Service

| Requisito | Estado |
|-----------|--------|
| productId, available, reserved, version (optimistic lock) | ✅ |
| Consultar inventario validando producto en Products | ✅ |
| POST /purchases (productId, quantity), idempotencia Idempotency-Key | ✅ |
| Validar producto existe, stock suficiente, descontar stock | ✅ |
| Evento InventoryChanged (log estructurado) | ✅ |
| Concurrencia: no stock negativo (optimistic locking) | ✅ |
| Cliente HTTP a Products: timeout, retry, circuit breaker | ✅ |
| Contract claro si Products caído o 404 | ✅ (503 / 404 en JSON:API) |

---

## Pruebas Backend

| Requisito | Estado |
|-----------|--------|
| Unitarias (servicios, validaciones) | ✅ |
| Integración con Testcontainers (DB) | ✅ |
| Comunicación Products ↔ Inventory (happy path y fallos) | ✅ |
| Cobertura 75–80% módulos críticos | Objetivo en servicios/controladores |

---

## Entrega

| Requisito | Estado |
|-----------|--------|
| README: cómo correr, docker compose, variables de entorno | ✅ |
| Colección Postman o ejemplos curl | ✅ (README + `docs/curl-examples.md`) |
| docs/: diagrama C4, decisiones técnicas | ✅ |
| Swagger/OpenAPI por servicio | ✅ |
| Evidencia pruebas (comando/salida) | Incluido en README o CI |
