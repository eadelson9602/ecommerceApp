# Diagramas C4 – Arquitectura E-commerce

Modelo [C4](https://c4model.com/): **Contexto** (nivel 1) y **Contenedores** (nivel 2). Los diagramas en Mermaid se renderizan en GitHub, GitLab y editores con soporte Markdown.

---

## Nivel 1: Contexto del sistema

Quién usa el sistema y qué sistema externo existe (el backend de la prueba).

```mermaid
flowchart LR
    subgraph users[" "]
        U[👤 Usuario<br/>Navegador]
        C[🖥️ Cliente<br/>Vue / API]
    end

    subgraph system["Sistema E-commerce"]
        direction TB
        API[📦 Backend<br/>Products + Inventory<br/>2 microservicios]
    end

    U -->|"Usa"| C
    C -->|"HTTP/JSON:API<br/>JWT o API Key"| API
```

- **Usuario**: persona que usa la aplicación (navegador).
- **Cliente**: frontend Vue o cualquier consumidor de la API (Postman, integraciones).
- **Sistema E-commerce**: el backend de la prueba; no hay API Gateway; cada microservicio se expone por su puerto (8080, 8081).

---

## Nivel 2: Contenedores (microservicios)

Contenedores de aplicación y bases de datos.

```mermaid
flowchart TB
    subgraph client[" "]
        FE[Frontend / Cliente API]
    end

    subgraph backend["Backend E-commerce"]
        direction LR
        subgraph PS["products-service :8080"]
            P[Spring Boot<br/>CRUD productos<br/>Listado, filtros, orden<br/>Login JWT]
        end
        subgraph IS["inventory-service :8081"]
            I[Spring Boot<br/>Inventario por producto<br/>POST /purchases<br/>Idempotencia, optimistic lock]
        end
    end

    subgraph data["Persistencia"]
        PG1[(PostgreSQL<br/>products_db)]
        PG2[(PostgreSQL<br/>inventory_db)]
    end

    FE -->|"HTTP + JWT"| P
    FE -->|"HTTP + JWT"| I
    I -->|"HTTP + X-API-Key<br/>WebClient"| P
    P --> PG1
    I --> PG2
```

| Contenedor | Tecnología | Responsabilidad |
|------------|------------|-----------------|
| **products-service** | Spring Boot, :8080 | Catálogo (CRUD), listado con paginación/filtros/orden, login y emisión de JWT. Acepta API Key (servicios) o JWT (frontend). |
| **inventory-service** | Spring Boot, :8081 | Inventario por producto, compras (POST /purchases) con idempotencia (`Idempotency-Key`) y control optimista. Llama a products-service para validar productos. |
| **products_db** | PostgreSQL | Datos de productos. |
| **inventory_db** | PostgreSQL | Inventario y compras. |

### Flujos principales

1. **Frontend → Products**: `GET/POST/PATCH/DELETE /api/products`, `POST /auth/login` (JWT).
2. **Frontend → Inventory**: `GET/PUT /api/inventory/{id}`, `POST /api/purchases` (JWT).
3. **Inventory → Products**: llamadas HTTP con header `X-API-Key` (WebClient + Resilience4j) para validar que el producto exista.

---

## Vista de despliegue (Docker)

Misma arquitectura en contexto de despliegue con Docker Compose.

```mermaid
flowchart LR
    subgraph host["Host (Docker Compose)"]
        subgraph services["Servicios"]
            A[products-service<br/>:8080]
            B[inventory-service<br/>:8081]
        end
        subgraph dbs["Bases de datos"]
            D1[(products-db<br/>:5432)]
            D2[(inventory-db<br/>:5433)]
        end
    end

    A --> D1
    B --> D2
    B -->|"red interna"| A
```

- En desarrollo/producción cada servicio puede ir en su propio contenedor o nodo; Inventory necesita conectividad a Products (por nombre de servicio o URL configurable).

---

## Resumen

| Nivel C4 | Contenido |
|----------|-----------|
| **1 – Contexto** | Usuario y cliente (Vue/API) consumen el sistema E-commerce (backend). |
| **2 – Contenedores** | products-service, inventory-service y sus bases PostgreSQL; comunicación HTTP (JWT/API Key) y entre servicios (WebClient + API Key). |

Documentación de la API: Swagger UI en cada servicio (`/swagger-ui.html`). Diagramas UML: [DIAGRAMAS_UML.md](DIAGRAMAS_UML.md).
