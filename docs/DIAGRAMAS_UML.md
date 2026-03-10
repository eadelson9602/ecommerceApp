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

## Resumen

| Diagrama | Contenido |
|----------|-----------|
| **Casos de uso** | Actores (Usuario, Frontend, Inventory Service) y casos de uso por servicio (login, CRUD productos, inventario, compras). |
| **Clases** | Dominio: Product, ProductStatus, SkuAlreadyExistsException (Products); Inventory, Purchase, IdempotencyRecord (Inventory). |
| **Despliegue** | Nodo host, contenedores products-service e inventory-service, bases PostgreSQL y conexiones JDBC/HTTP. |

Para arquitectura C4 (contexto y contenedores): [ARQUITECTURA_C4.md](ARQUITECTURA_C4.md).
