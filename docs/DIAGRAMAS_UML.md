# Diagramas UML – E-commerce Backend

Diagramas en **Mermaid** (render en GitHub, GitLab y editores Markdown): casos de uso, clases de dominio y despliegue.

---

## 1. Diagrama de casos de uso

Actores y casos de uso del sistema (backend). El **Usuario** actúa a través del **Frontend**; el **Inventory Service** es actor secundario que consume el Products Service.

```mermaid
flowchart LR
    subgraph actors["Actores"]
        U(("Usuario"))
        FE[Frontend\nVue / API]
        INV[Inventory Service]
    end

    subgraph products["Sistema: Products Service"]
        UC1(Login)
        UC2(Listar productos)
        UC3(Crear producto)
        UC4(Actualizar producto)
        UC5(Eliminar producto)
    end

    subgraph inventory["Sistema: Inventory Service"]
        UC6(Consultar inventario)
        UC7(Fijar inventario)
        UC8(Registrar compra)
    end

    U --> FE
    FE --> UC1
    FE --> UC2
    FE --> UC3
    FE --> UC4
    FE --> UC5
    FE --> UC6
    FE --> UC7
    FE --> UC8
    INV --> UC2
```

| Actor | Descripción |
|-------|-------------|
| **Usuario** | Persona que usa la aplicación. |
| **Frontend** | Cliente (Vue, Postman, etc.) que consume la API con JWT. |
| **Inventory Service** | Microservicio que consulta productos (p. ej. validar que existan). |

| Caso de uso | Servicio | Endpoint / descripción |
|-------------|----------|------------------------|
| Login | Products | `POST /auth/login` → JWT |
| Listar productos | Products | `GET /api/products` (paginación, filtros, orden) |
| Crear producto | Products | `POST /api/products` |
| Actualizar producto | Products | `PATCH /api/products/{id}` |
| Eliminar producto | Products | `DELETE /api/products/{id}` |
| Consultar inventario | Inventory | `GET /api/inventory/{productId}` |
| Fijar inventario | Inventory | `PUT /api/inventory/{productId}` |
| Registrar compra | Inventory | `POST /api/purchases` (idempotente con `Idempotency-Key`) |

---

## 2. Diagrama de clases (dominio)

Entidades y enums del dominio de ambos microservicios. Relaciones entre sí y con excepciones de dominio.

### 2.1 Products Service (dominio)

```mermaid
classDiagram
    class Product {
        -UUID id
        -String sku
        -String name
        -BigDecimal price
        -ProductStatus status
        -Instant createdAt
        -Instant updatedAt
        +getId() UUID
        +getSku() String
        +getName() String
        +getPrice() BigDecimal
        +getStatus() ProductStatus
        +getCreatedAt() Instant
        +getUpdatedAt() Instant
    }
    class ProductStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
    }
    class SkuAlreadyExistsException {
        <<exception>>
        +SkuAlreadyExistsException(String sku)
    }
    Product --> ProductStatus : status
    SkuAlreadyExistsException ..> Product : sku duplicado
```

### 2.2 Inventory Service (dominio)

```mermaid
classDiagram
    class Inventory {
        -UUID productId
        -int available
        -int reserved
        -long version
        +getProductId() UUID
        +getAvailable() int
        +getReserved() int
        +getVersion() long
    }
    class Purchase {
        -UUID id
        -UUID productId
        -int quantity
        -String idempotencyKey
        -Instant processedAt
        +getId() UUID
        +getProductId() UUID
        +getQuantity() int
        +getIdempotencyKey() String
        +getProcessedAt() Instant
    }
    class IdempotencyRecord {
        -String idempotencyKey
        -int responseStatus
        -String responseBody
        -Instant createdAt
        +getIdempotencyKey() String
        +getResponseStatus() int
        +getResponseBody() String
    }
    Purchase --> Inventory : productId referencia
    IdempotencyRecord ..> Purchase : cachea respuesta compra
```

- **Inventory**: identificado por `productId` (PK); `version` para control optimista.
- **Purchase**: registro de compra; `idempotencyKey` para idempotencia.
- **IdempotencyRecord**: guarda la respuesta de una compra ya procesada para repetir la misma petición sin efecto.

---

## 3. Diagrama de despliegue

Nodos de ejecución, artefactos y conexiones en un despliegue tipo Docker Compose.

```mermaid
flowchart TB
    subgraph host["Nodo: Host (máquina / Docker Compose)"]
        subgraph app["Contenedores de aplicación"]
            PS["«artifact»\nproducts-service\nJAR Spring Boot\n:8080"]
            IS["«artifact»\ninventory-service\nJAR Spring Boot\n:8081"]
        end
        subgraph db["Contenedores de persistencia"]
            PG1["«database»\nproducts-db\nPostgreSQL 16\n:5432"]
            PG2["«database»\ninventory-db\nPostgreSQL 16\n:5433"]
        end
    end

    PS -->|"JDBC"| PG1
    IS -->|"JDBC"| PG2
    IS -->|"HTTP + X-API-Key\n:8080"| PS
```

### Vista de componentes en nodo (despliegue simplificado)

```mermaid
flowchart LR
    subgraph docker["Entorno Docker"]
        subgraph c1["products-service"]
            P[ProductController\nAuthController]
            S[ProductApplicationService]
            R[ProductRepository]
            P --> S --> R
        end
        subgraph c2["inventory-service"]
            I[InventoryController]
            IS[InventoryApplicationService]
            IR[InventoryRepository\nPurchaseRepository]
            IC[ProductsServiceClient]
            I --> IS --> IR
            IS --> IC
        end
        subgraph dbs["Bases de datos"]
            D1[(products_db)]
            D2[(inventory_db)]
        end
    end

    R --> D1
    IR --> D2
    IC -->|"HTTP"| c1
```

| Elemento | Tipo | Descripción |
|----------|------|-------------|
| **products-service** | Contenedor / artefacto | JAR Spring Boot, puerto 8080. |
| **inventory-service** | Contenedor / artefacto | JAR Spring Boot, puerto 8081. |
| **products-db** | Base de datos | PostgreSQL, puerto 5432 (interno 5432 en red Docker). |
| **inventory-db** | Base de datos | PostgreSQL, puerto 5433 (interno 5432 en red Docker). |

---

## 4. Diagrama de secuencia (interacción entre microservicios)

Flujos en los que **Inventory Service** llama a **Products Service** para validar que el producto exista en el catálogo. Comunicación HTTP con header `X-API-Key` (WebClient + Resilience4j).

### 4.1 Consultar inventario (GET /api/inventory/{productId})

El cliente pide el inventario de un producto. Inventory valida primero en Products que el producto exista; si existe, consulta su propia base de datos.

```mermaid
sequenceDiagram
    participant C as Cliente\n(Frontend / JWT)
    participant IC as Inventory\nController
    participant AS as Inventory\nApplication Service
    participant PSP as ProductsService\nPort (Adapter)
    participant PS as Products Service\n:8080
    participant IR as Inventory\nRepository
    participant DB_I as inventory_db

    C->>IC: GET /api/inventory/{productId}
    IC->>AS: getInventoryValidatingProduct(productId)
    AS->>PSP: getProductExists(productId)
    PSP->>PS: GET /api/products/{id}\n(X-API-Key)
    alt Producto no existe
        PS-->>PSP: 404
        PSP-->>AS: Mono.empty()
        AS-->>IC: GetInventoryResult.productNotFound()
        IC-->>C: 404 Producto no encontrado
    else Producto existe
        PS-->>PSP: 200 + JSON:API product
        PSP-->>AS: Mono.just(productId)
        AS->>IR: findById(productId)
        IR->>DB_I: SELECT
        alt Sin registro de inventario
            DB_I-->>IR: vacío
            IR-->>AS: Optional.empty()
            AS-->>IC: inventoryNotFound()
            IC-->>C: 404 Inventario no encontrado
        else Inventario existe
            DB_I-->>IR: Inventory
            IR-->>AS: Optional.of(inventory)
            AS-->>IC: success(inventory)
            IC-->>C: 200 + JSON:API inventory
        end
    end
```

### 4.2 Registrar compra (POST /api/purchases)

El cliente registra una compra (descuento de stock). Inventory comprueba idempotencia opcional, valida el producto en Products, bloquea la fila de inventario (SELECT FOR UPDATE), descuenta stock y persiste la compra.

```mermaid
sequenceDiagram
    participant C as Cliente\n(Frontend / JWT)
    participant IC as Inventory\nController
    participant AS as Inventory\nApplication Service
    participant IDP as Idempotency\nRecord Port
    participant PSP as ProductsService\nPort (Adapter)
    participant PS as Products Service\n:8080
    participant IR as Inventory\nRepository
    participant PR as Purchase\nRepository
    participant DB_I as inventory_db

    C->>IC: POST /api/purchases\nIdempotency-Key? body: productId, quantity
    IC->>AS: processPurchase(productId, quantity, idempotencyKey)

    alt Idempotency-Key presente y ya procesada
        AS->>IDP: findByIdempotencyKey(key)
        IDP-->>AS: Optional.of(record)
        AS-->>IC: PurchaseResult.fromIdempotency (cached)
        IC-->>C: 201 + respuesta cacheada
    else Primera vez o sin clave idempotente
        AS->>PSP: getProductExists(productId)
        PSP->>PS: GET /api/products/{id}\n(X-API-Key)
        alt Producto no existe
            PS-->>PSP: 404
            PSP-->>AS: Mono.empty()
            AS-->>IC: productNotFound()
            IC-->>C: 404 Producto no encontrado
        else Producto existe
            PS-->>PSP: 200
            PSP-->>AS: Mono.just(productId)
            AS->>IR: findByProductIdForUpdate(productId)
            IR->>DB_I: SELECT ... FOR UPDATE
            alt Sin inventario / stock insuficiente / lock
                DB_I-->>IR: vacío o available < quantity
                IR-->>AS: empty / inv
                AS-->>IC: inventoryNotFound() / insufficientStock() / conflict()
                IC-->>C: 404 / 422 / 409
            else Stock suficiente
                DB_I-->>IR: Inventory (locked)
                IR-->>AS: Optional.of(inv)
                AS->>AS: setAvailable(available - quantity)
                AS->>IR: saveAndFlush(inv)
                IR->>DB_I: UPDATE inventory
                AS->>PR: save(Purchase)
                PR->>DB_I: INSERT purchase
                AS-->>IC: PurchaseResult.success(purchase)
                IC->>IDP: save(IdempotencyRecord) si Idempotency-Key
                IC-->>C: 201 + JSON:API purchase
            end
        end
    end
```

| Paso | Descripción |
|------|-------------|
| **Inventory → Products** | Llamada HTTP `GET /api/products/{id}` con header `X-API-Key`. Implementada por `ProductsServiceAdapter` (WebClient + Retry + Circuit Breaker). |
| **Products 404** | Si el producto no existe, Inventory responde 404 sin consultar su base de datos. |
| **Idempotencia** | Si el cliente envía `Idempotency-Key` y ya hubo una compra con esa clave, se devuelve la respuesta cacheada sin llamar a Products ni modificar stock. |
| **Optimistic lock** | `findByProductIdForUpdate` (SELECT FOR UPDATE) y `saveAndFlush` con `@Version` evitan condiciones de carrera; conflicto → 409. |

---

## Resumen

| Diagrama | Contenido |
|----------|-----------|
| **Casos de uso** | Actores (Usuario, Frontend, Inventory Service) y casos de uso por servicio (login, CRUD productos, inventario, compras). |
| **Clases** | Dominio: Product, ProductStatus, SkuAlreadyExistsException (Products); Inventory, Purchase, IdempotencyRecord (Inventory). |
| **Despliegue** | Nodo host, contenedores products-service e inventory-service, bases PostgreSQL y conexiones JDBC/HTTP. |
| **Secuencia** | Consultar inventario y Registrar compra: flujos donde Inventory llama a Products (X-API-Key) y accede a inventory_db. |

Para arquitectura C4 (contexto y contenedores): [ARQUITECTURA_C4.md](ARQUITECTURA_C4.md).
