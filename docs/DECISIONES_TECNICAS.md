# Decisiones técnicas (Backend)

## Persistencia: SQL con PostgreSQL y Flyway

- **Elección**: PostgreSQL por servicio (products_db, inventory_db).
- **Justificación**: Transacciones ACID, soporte de locking optimista (`@Version`), madurez operativa y migraciones reproducibles con Flyway. Cada microservicio tiene su propia base para cumplir con bounded context y despliegue independiente.
- **Migraciones**: Flyway en `db/migration/` (V1__*.sql) por módulo. No se usa `ddl-auto=create` en producción; `validate` para asegurar que el esquema coincide con las migraciones.

## Resiliencia (Inventory → Products)

- **Timeout**: WebClient con `Duration.ofMillis(3000)` configurable por `app.products-service.timeout-ms`.
- **Retry**: Resilience4j `@Retry(name = "products")` (3 intentos, 500 ms entre ellos).
- **Circuit breaker**: Resilience4j `@CircuitBreaker(name = "products")` (ventana 10, 50% fallos, 5 s abierto). Si Products está caído o muy lento, Inventory devuelve **503** con mensaje claro (JSON:API) en lugar de bloquear.
- **Contract**: Si Products retorna 404 → el producto no existe (Inventory responde 404). Si Products no responde o circuit abierto → 503 con código `PRODUCTS_SERVICE_UNAVAILABLE`.

## Idempotencia (compras)

- **Header**: `Idempotency-Key` en `POST /api/purchases`.
- **Comportamiento**: La primera petición con una clave se procesa y la respuesta (status + body) se guarda. Peticiones posteriores con la misma clave devuelven la respuesta almacenada sin volver a descontar stock.
- **Almacén**: Tabla `idempotency` (idempotency_key, response_status, response_body, created_at).

## Concurrencia (stock negativo)

- **Enfoque**: Locking optimista en la entidad `Inventory` con `@Version`. Dos compras simultáneas leen la misma versión; la primera hace `UPDATE ... SET available = ..., version = version + 1` y gana; la segunda falla por condición de versión y se devuelve **409 Conflict** con mensaje para reintentar.
- **Alternativa considerada**: Lock pesimista (SELECT FOR UPDATE) aumentaría contención; el optimista es suficiente para el volumen esperado y evita deadlocks.

## Seguridad

- **Entre microservicios**: Header `X-API-Key`; Inventory lo envía al llamar a Products. Products valida ese header y, si coincide con `app.internal.api-key`, considera la petición autenticada.
- **Frontend**: JWT (HS256) con secret compartido (`app.jwt.secret`). Login en Products `POST /auth/login`; el frontend envía `Authorization: Bearer <token>` a ambos servicios. Inventory valida el mismo JWT (mismo secret).
- **Rate limit**: Filtro por IP, ventana 1 minuto, máximo de peticiones configurable (`app.rate-limit.max-requests-per-minute`). Respuesta **429** en JSON:API.

## Observabilidad

- **Correlation-id**: Filtro que lee o genera `X-Correlation-ID`, lo pone en MDC y lo devuelve en la respuesta. El patrón de log incluye `%X{correlationId}`.
- **Health**: Actuator con `livenessState` y `readinessState` (Kubernetes/Docker).
- **Métricas**: Actuator expone `health`, `info`, `metrics`.
