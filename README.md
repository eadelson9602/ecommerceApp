# E-commerce Backend

Backend de dos **microservicios** (Products e Inventory) con Java 17, Spring Boot 3.3, API JSON:API, arquitectura hexagonal y persistencia PostgreSQL.

---

## Índice (por orden de prioridad)

1. [Requisitos](#1-requisitos)
2. [Despliegue local con Docker](#2-despliegue-local-con-docker)
3. [Variables de entorno y archivo .env](#3-variables-de-entorno-y-archivo-env)
4. [Ejecución sin Docker](#4-ejecución-sin-docker)
5. [Seguridad y endpoints](#5-seguridad-y-endpoints)
6. [Pruebas](#6-pruebas)
7. [SonarQube (calidad de código)](#7-sonarqube-calidad-de-código)
8. [Pruebas E2E con Selenium](#8-pruebas-e2e-con-selenium)
9. [Despliegue en producción](#9-despliegue-en-producción)
10. [Arquitectura del proyecto](#10-arquitectura-del-proyecto)
11. [Estructura del repositorio](#11-estructura-del-repositorio)
12. [Documentación adicional](#12-documentación-adicional)
13. [Tecnologías](#14-tecnologías)

---

## 1. Requisitos

- **Java 17+**
- **Maven 3.9+** (o usar `./mvnw` incluido)
- **Docker y Docker Compose** (para despliegue con bases de datos o stack completo)

---

## 2. Despliegue local con Docker

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

En este caso, Inventory usa por defecto `http://localhost:8080` para Products. Si cambias el puerto de Products, configura las variables indicadas en la sección [Variables de entorno](#3-variables-de-entorno-y-archivo-env).

---

## 3. Variables de entorno y archivo `.env`

En la raíz del proyecto está **`.env-template`** con todas las variables. Copia a **`.env`**, rellena los valores y no versiones `.env` (está en `.gitignore`).

**Carga automática de `.env`:** al arrancar cada servicio (desde Maven, IDE o jar), la aplicación **carga automáticamente** el archivo `.env` si existe: lo busca en el directorio de trabajo actual o en directorios padre hasta la raíz del proyecto. No hace falta ejecutar `source .env`. Las variables ya definidas en el sistema (export, Docker, CI) **tienen prioridad** y no se sobrescriben.

Un solo `.env` en la raíz puede definir variables de **ambos** servicios usando prefijos:

- **products-service:** `PRODUCTS_*` (y compartidas sin prefijo como `JWT_SECRET`).
- **inventory-service:** `INVENTORY_*` (y las mismas compartidas).

Si ya tenías un `.env` con nombres sin prefijo (`SPRING_DATASOURCE_URL`, `INTERNAL_API_KEY`, etc.), sustituye por los nombres de la tabla (p. ej. `PRODUCTS_SPRING_DATASOURCE_URL`, `INVENTORY_APP_PRODUCTS_SERVICE_URL`). Así un único archivo sirve para los dos servicios.

Con **Docker Compose** puedes seguir usando `env_file: .env` en cada servicio; Compose inyecta las variables en el contenedor.

**Variables para products-service** (todas opcionales si usas los valores por defecto de `application.properties`):

| Variable en `.env`                       | Descripción                           | Por defecto (en código)                        |
| ---------------------------------------- | ------------------------------------- | ---------------------------------------------- |
| `PRODUCTS_SERVER_PORT`                   | Puerto HTTP                           | `8080`                                         |
| `PRODUCTS_SPRING_DATASOURCE_URL`         | JDBC URL PostgreSQL                   | `jdbc:postgresql://localhost:5432/products_db` |
| `PRODUCTS_SPRING_DATASOURCE_USERNAME`    | Usuario DB                            | `products`                                     |
| `PRODUCTS_SPRING_DATASOURCE_PASSWORD`    | Contraseña DB                         | `products`                                     |
| `PRODUCTS_INTERNAL_API_KEY`              | API Key para llamadas entre servicios | `internal-api-key-change-in-prod`              |
| `JWT_SECRET`                             | Secret para firmar/validar JWT        | (valor por defecto; **cambiar en prod**)       |
| `APP_JWT_EXPIRATION_SECONDS`             | Duración del token (s)                | `3600`                                         |
| `APP_RATE_LIMIT_MAX_REQUESTS_PER_MINUTE` | Rate limit por IP                     | `100`                                          |

**Variables para inventory-service**:

| Variable en `.env`                          | Descripción                             | Por defecto (en código)                         |
| ------------------------------------------- | --------------------------------------- | ----------------------------------------------- |
| `INVENTORY_SERVER_PORT`                     | Puerto HTTP                             | `8081`                                          |
| `INVENTORY_SPRING_DATASOURCE_URL`           | JDBC URL PostgreSQL                     | `jdbc:postgresql://localhost:5433/inventory_db` |
| `INVENTORY_SPRING_DATASOURCE_USERNAME`      | Usuario DB                              | `inventory`                                     |
| `INVENTORY_SPRING_DATASOURCE_PASSWORD`      | Contraseña DB                           | `inventory`                                     |
| `INVENTORY_APP_PRODUCTS_SERVICE_URL`        | URL del Products Service                | `http://localhost:8080`                         |
| `INVENTORY_APP_PRODUCTS_SERVICE_API_KEY`    | API Key para llamar a Products          | `internal-api-key-change-in-prod`               |
| `INVENTORY_APP_PRODUCTS_SERVICE_TIMEOUT_MS` | Timeout HTTP a Products (ms)            | `3000`                                          |
| `JWT_SECRET`                                | Mismo secret que Products (validar JWT) | (mismo que Products)                            |
| `APP_RATE_LIMIT_MAX_REQUESTS_PER_MINUTE`    | Rate limit por IP                       | `100`                                           |

---

## 4. Ejecución sin Docker

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

## 5. Seguridad y endpoints

- **Entre servicios**: Header `X-API-Key` con el valor configurado en `app.internal.api-key` (Products valida; Inventory lo envía en las llamadas a Products).
- **Frontend / usuarios**: Login en Products (`POST /auth/login`) devuelve un JWT. Enviar `Authorization: Bearer <token>` en las peticiones a `/api/**` de ambos servicios. Los seeders crean usuarios por defecto: **admin**, **operator** y **viewer**, todos con contraseña **`password`** (véase [Datos iniciales (seeders)](#datos-iniciales-seeders)).
- **Rate limit**: Por IP, configurable; respuestas 429 cuando se supera el límite.
- **Content-Type**: API en formato JSON:API (`Content-Type` y `Accept: application/vnd.api+json`).

**Lista de endpoints:** la referencia es la **documentación interactiva (Swagger UI)**. Ahí están todos los endpoints, parámetros, esquemas y la opción de probar con JWT o API Key. No se mantiene tabla duplicada en este README para evitar desfases.

- **Products:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) — OpenAPI: `/v3/api-docs`
- **Inventory:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) — OpenAPI: `/v3/api-docs`

Generado con [springdoc-openapi](https://springdoc.org/).  
Health: `http://localhost:8080/actuator/health` y `http://localhost:8081/actuator/health`.

**Postman:** En la raíz del repo está **`ecommerceApp-Postman-Collection.json`**. Importarla en Postman para probar Auth, Products e Inventory con variables `products_base` (8080) e `inventory_base` (8081). Tras ejecutar **Auth > Login** (admin/password), el token se guarda y se usa en el resto de peticiones.

**Verificación vs prueba técnica:** Ver **`VERIFICACION_PRUEBA_TECNICA_BACKEND.md`** para el checklist de cumplimiento de requisitos.

---

## 6. Pruebas

- **Unitarias**: Servicios de aplicación (casos de uso) con Mockito.
- **Integración**: Controladores con MockMvc y perfil `test` (H2 en memoria).

Ejecutar todos los tests:

```bash
./mvnw test -pl products-service,inventory-service -am
```

### Cobertura de código (JaCoCo)

La cobertura se genera con **JaCoCo** en cada ejecución de tests. **Objetivo: 70–80%** de líneas cubiertas. El POM exige un **mínimo del 70%** en servicios de aplicación y dominio; `./mvnw verify` falla si no se alcanza (dentro del rango 70–80%).

1. **Solo tests e informe de cobertura** (sin fallar si no llegas al mínimo):

   ```bash
   ./mvnw test -pl products-service,inventory-service -am
   ```

2. **Tests + verificación de cobertura mínima 70%** (recomendado para CI):

   ```bash
   ./mvnw verify -pl products-service,inventory-service -am
   ```

   El build falla si la cobertura en aplicación + dominio está por debajo del **70%** (rango senior 70–80%).

3. **Un solo módulo**:

   ```bash
   ./mvnw test -pl products-service
   ./mvnw verify -pl inventory-service
   ```

4. **Ver el informe HTML** por módulo:
   - **Products**: `products-service/target/site/jacoco/index.html`
   - **Inventory**: `inventory-service/target/site/jacoco/index.html`

   Abre el `index.html` en el navegador para ver líneas cubiertas, ramas y resumen por paquete.

Se excluyen del mínimo de cobertura: clase principal (`*Application`), paquetes `config`, DTOs y modelos de request/response, para centrar el objetivo en servicios, dominio, controladores y adaptadores.

---

## 7. SonarQube (calidad de código)

El proyecto incluye el **sonar-maven-plugin** para analizar calidad, duplicados, bugs, vulnerabilidades y cobertura.

**Dos caminos posibles:** puedes usar **SonarCloud** (en la nube, sin instalar nada) o **SonarQube local** (en tu máquina con Docker). Cada uno tiene su **propio token**: el token de SonarCloud no sirve para el servidor local y al revés. Si no tienes token de SonarCloud o prefieres no usarlo, sigue la **Opción A (local)** y genera el token en tu instancia local.

**Requisito previo:** Tienes que tener un servidor en ejecución (local o Cloud). Si ves **"Connection refused"** en `localhost:9000`, es que no hay SonarQube local levantado; elige una de las dos opciones siguientes.

---

**Opción A: SonarQube local (Docker)**

Token propio del servidor local (no uses el de SonarCloud).

**Si ves "You're running a version of SonarQube that is no longer active. Please upgrade to an active version immediately":** tu imagen está obsoleta. Usa una versión activa (ver paso 1) o actualiza el contenedor (ver apartado _Actualizar SonarQube local_ más abajo).

1. Levanta el servidor con una **versión activa** (solo una vez; la primera vez tarda unos minutos en arrancar):
   ```bash
   docker run -d --name sonarqube -p 9000:9000 sonarqube:community
   ```
   La etiqueta `community` usa la última Community Edition activa. Para una versión fija, consulta [Docker Hub – sonarqube/tags](https://hub.docker.com/_/sonarqube/tags) y elige una etiqueta reciente (p. ej. `10.4-community`).
2. Cuando esté listo, abre http://localhost:9000. Inicia sesión con `admin` / `admin` (la primera vez te pedirá cambiar la contraseña). **Para generar el token:** clic en el icono de tu usuario (arriba a la derecha) → **My Account** → pestaña **Security** → en "Generate Tokens" pon un nombre (ej. `ecommerce-local`) → **Generate** y copia el token (solo se muestra una vez).
3. Genera cobertura y ejecuta el análisis. **Importante:** el plugin de Maven **no** lee la variable de entorno `SONAR_TOKEN`; hay que pasarla con `-Dsonar.token=...`. Si tienes `SONAR_TOKEN` y `SONAR_HOST_URL` en tu `.env`, usa:
   ```bash
   ./mvnw verify -pl products-service,inventory-service -am
   set -a && [ -f .env ] && . ./.env && set +a
   ./mvnw sonar:sonar -Dsonar.host.url=${SONAR_HOST_URL:-http://localhost:9000} -Dsonar.token="$SONAR_TOKEN"
   ```
   O ejecuta el script incluido (carga `.env` y pasa el token): `./run-sonar.sh`
   Alternativa sin `.env`: `./mvnw sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=tu_token_creado_en_el_paso_2`
4. Revisa el resultado en http://localhost:9000 (el proyecto aparecerá en el listado).

**Actualizar SonarQube local:** para reemplazar un contenedor antiguo por una versión activa (los datos en volúmenes se conservan si los reutilizas):

```bash
docker stop sonarqube && docker rm sonarqube
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
```

Si usabas volúmenes con nombres (`-v sonarqube_data:/opt/sonarqube/data`, etc.), añádelos de nuevo al `docker run` para no perder datos. Luego abre http://localhost:9000; la primera vez puede pedir migrar la base de datos.

**Usuario y contraseña tras actualizar:** tras subir a una versión nueva, el usuario y la contraseña de la versión antigua **pueden dejar de funcionar** (cambios en el almacenamiento o en el algoritmo de hash). Puedes hacer lo siguiente:

- **Instalación nueva (contenedor nuevo sin volumen antiguo):** la instalación empieza con usuario **`admin`** y contraseña **`admin`**. En el primer acceso SonarQube pide cambiar la contraseña; así defines tu nueva contraseña.
- **Actualizaste reutilizando volumen y ya no entras:**
  - **Opción A – Empezar de cero (sin conservar proyectos/tokens):** elimina contenedor y volumen, y vuelve a crear. Tendrás de nuevo `admin` / `admin`:
    ```bash
    docker stop sonarqube && docker rm sonarqube
    docker volume rm sonarqube_data sonarqube_extensions sonarqube_logs 2>/dev/null || true
    docker run -d --name sonarqube -p 9000:9000 sonarqube:community
    ```
    Luego entra en http://localhost:9000 con `admin` / `admin` y cambia la contraseña cuando te lo pida.
  - **Opción B – Resetear solo la contraseña de admin (conservando datos):** entra al contenedor y resetea la contraseña en la base de datos. Con la imagen por defecto (base de datos embebida) puedes usar un contenedor temporal para obtener el hash de `admin`/`admin` de tu misma versión y aplicarlo en tu instancia (ver [Sonar Community – Reset admin password](https://community.sonarsource.com/t/how-to-access-the-sonarqube-database-to-reset-admin-password/110124)). Si usas PostgreSQL externo, ejecuta el `UPDATE` en ese servidor sobre la tabla `users` del esquema de SonarQube.

---

**Opción B: SonarCloud (sin instalar nada)**

Token propio de SonarCloud (no sirve para el servidor local).

1. Entra en [sonarcloud.io](https://sonarcloud.io), crea o vincula el repositorio y anota la **organization** y el **project key** que te asigne.
2. Crea un token en **My Account → Security** en la web de SonarCloud (icono de usuario → My Account → Security → Generate Tokens).
3. Genera cobertura y ejecuta el análisis. El plugin Maven **no** lee `SONAR_TOKEN` del entorno; hay que pasarla con `-Dsonar.token=...`. Si tienes las variables en `.env`:
   ```bash
   ./mvnw verify -pl products-service,inventory-service -am
   set -a && [ -f .env ] && . ./.env && set +a
   ./mvnw sonar:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.organization="$SONAR_ORGANIZATION" -Dsonar.projectKey="$SONAR_PROJECT_KEY" -Dsonar.token="$SONAR_TOKEN"
   ```
   Sustituye en `.env` (o en el comando) `SONAR_ORGANIZATION` y `SONAR_PROJECT_KEY` por los del paso 1. Alternativa sin `.env`: pasar `-Dsonar.token=tu_token -Dsonar.organization=TU_ORG -Dsonar.projectKey=TU_PROJECT_KEY`.
4. Revisa el resultado en la web de SonarCloud, en el proyecto correspondiente.

---

**Seguridad:** No subas el token al repositorio ni lo pongas en el POM. Usa la variable de entorno `SONAR_TOKEN` (o configúrala en tu pipeline de CI).

**Importante:** Ejecuta siempre `./mvnw verify` **antes** de `./mvnw sonar:sonar`. Si solo ejecutas `sonar:sonar`, Sonar puede usar reportes JaCoCo antiguos o incompletos y la cobertura mostrada (sobre todo en "código nuevo") será incorrecta o baja.

---

**Quality Gate en rojo (2 conditions failed)**

En este proyecto el **código nuevo** también debe cumplir el estándar de **70–80 % de cobertura** (y rating A en mantenibilidad). No se debe bajar el umbral del Quality Gate para "pasar"; hay que **ajustar código y pruebas** hasta cumplirlo.

Si SonarQube muestra el Quality Gate en **Failed**:

1. **Coverage on New Code is less than 80.0%** (p. ej. 48.5%)  
   Sonar mide la cobertura solo del **código nuevo** (desde la última versión o la rama de referencia). **Qué hacer:** añade o mejora tests para todo el código nuevo hasta alcanzar al menos **70–80 %** de cobertura. En la pestaña **Coverage** de Sonar puedes ver qué líneas nuevas no están cubiertas y priorizar qué probar. Asegúrate de haber ejecutado `./mvnw verify` antes de `sonar:sonar` para que los reportes de cobertura estén actualizados.

2. **Maintainability Rating on New Code is worse than A**  
   El código nuevo tiene rating B o inferior. **Qué hacer:** revisa en Sonar la pestaña **Issues** filtrando por "New Code" y corrige los code smells (duplicación, complejidad cognitiva, métodos largos, etc.) hasta que el rating pase a A.

**Configurar el estándar del proyecto (70–80 %):** si quieres que el Quality Gate exija explícitamente 70 % u 80 % en código nuevo, en SonarQube ve a **Administration** → **Quality Gates** → edita la puerta usada por el proyecto (p. ej. Sonar way) y define "Coverage on New Code" en **70%** u **80%** según el estándar que elijas. El objetivo es cumplir ese umbral con pruebas, no relajarlo.

**Nota:** Si tu instancia muestra "version no longer active", el menú puede variar; en esas versiones suele estar **Administration** → **Quality Gates**. Actualizar a una versión activa (ver _Actualizar SonarQube local_ arriba) te dará la interfaz actual.

**Propiedades opcionales** (POM o `-D`): `sonar.exclusions`, `sonar.coverage.jacoco.xmlReportPaths`. El POM ya define exclusiones y rutas de cobertura.

---

## 8. Pruebas E2E con Selenium (HtmlUnit)

Cada microservicio incluye **pruebas E2E con Selenium** usando el driver **HtmlUnit**: no hace falta instalar Chrome ni ningún navegador; todo corre en la JVM.

**Qué comprueban:** que Swagger UI (`/swagger-ui.html`) y el endpoint de health (`/actuator/health`) responden correctamente.

**Cómo ejecutar las pruebas con Selenium:**

| Objetivo                                                               | Comando                                                                                            |
| ---------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| Todos los tests (unitarios, integración y Selenium) de ambos servicios | `./mvnw test -pl products-service,inventory-service -am`                                           |
| Solo tests del proyecto (incluye Selenium)                             | `./mvnw test -pl products-service` o `./mvnw test -pl inventory-service`                           |
| Solo las pruebas E2E (Selenium) de un servicio                         | `./mvnw test -pl products-service -Dgroups=e2e` o `./mvnw test -pl inventory-service -Dgroups=e2e` |
| Build completo con cobertura (incluye Selenium)                        | `./mvnw verify -pl products-service,inventory-service -am`                                         |

Las pruebas E2E están etiquetadas con `@Tag("e2e")`. No se requiere ningún navegador instalado: HtmlUnit simula el navegador en memoria.

**Clases de test:** `products-service` → `ecommerce.app.products.e2e.SwaggerUiE2ETest`; `inventory-service` → `ecommerce.app.inventory.e2e.SwaggerUiE2ETest`.

---

## 9. Despliegue en producción

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
3. **Red**: En Kubernetes o ECS, configurar `INVENTORY_APP_PRODUCTS_SERVICE_URL` con el nombre de servicio interno (por ejemplo `http://products-service:8080`).
4. **Health checks**: Los endpoints de Actuator `liveness` y `readiness` están habilitados; usarlos para probes en el orquestador.
5. **Observabilidad**: Los logs incluyen `correlationId` (header `X-Correlation-ID`). Exponer métricas (Actuator) e integrar con tu sistema de monitoreo.
6. **Rate limit**: Ajustar `APP_RATE_LIMIT_MAX_REQUESTS_PER_MINUTE` (o en `.env`) según capacidad y política de uso.

### Orden de arranque

En producción, **products-service** debe estar disponible antes de que **inventory-service** reciba tráfico real, ya que Inventory llama a Products para validar productos. Usar health checks y políticas de reintento en el orquestador para el arranque en paralelo.

### Datos iniciales (seeders)

Con Flyway habilitado (por defecto en desarrollo/producción), al arrancar se ejecutan migraciones que insertan datos de prueba tanto en local como en producción.

#### Usuarios creados por defecto (products-service)

La migración `V4__seed_users.sql` crea tres usuarios en la tabla `users`. **Todos usan la misma contraseña** (cambiar en producción):

| Usuario   | Contraseña | Rol  | Uso recomendado      |
| --------- | ---------- | ---- | --------------------- |
| `admin`   | `password` | ADMIN | Administración        |
| `operator`| `password` | USER  | Operaciones / CRUD    |
| `viewer`  | `password` | USER  | Solo lectura          |

Para iniciar sesión desde el frontend o con `POST /auth/login` (Products), usar cualquiera de estos usuarios con contraseña `password`. El JWT devuelto sirve para llamar a `/api/**` en Products e Inventory.

#### Resumen de migraciones con datos de prueba

| Servicio              | Migración                    | Contenido                                                                                                |
| --------------------- | ---------------------------- | -------------------------------------------------------------------------------------------------------- |
| **products-service**  | `V2__create_users_table.sql` | Tabla `users` (username, password_hash, role).                                                           |
|                       | `V3__seed_products.sql`      | 5 productos básicos (Laptop, Mouse, Teclado, Monitor, Webcam) con UUIDs fijos.                           |
|                       | `V4__seed_users.sql`         | Los 3 usuarios anteriores (admin, operator, viewer; contraseña `password`).                              |
| **inventory-service** | `V2__seed_inventory.sql`     | Stock inicial para los 5 productos anteriores (mismos UUIDs).                                            |

En perfil `test` Flyway está desactivado y se usa H2 con `ddl-auto=create-drop`, por lo que los seeders no se aplican en pruebas. En producción, cambiar la contraseña por defecto de los usuarios.

---

## 10. Arquitectura del proyecto

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

| Capa                           | Descripción                                                                       |
| ------------------------------ | --------------------------------------------------------------------------------- |
| **domain**                     | Entidades, value objects y excepciones de dominio. Sin dependencias de framework. |
| **application.port.in**        | Casos de uso (interfaces que expone la aplicación).                               |
| **application.port.out**       | Puertos de salida (repositorios, cliente HTTP a Products).                        |
| **application.model**          | Comandos, filtros y resultados de casos de uso.                                   |
| **application.service**        | Implementación de los casos de uso.                                               |
| **infrastructure.persistence** | Adaptadores JPA (Spring Data).                                                    |
| **infrastructure.web**         | Controladores REST y DTOs JSON:API.                                               |
| **infrastructure.client**      | (solo inventory) Cliente WebClient a Products.                                    |
| **infrastructure.config**      | Seguridad, filtros (correlation-id, rate limit, JWT, API Key).                    |

Las dependencias apuntan hacia el dominio: la infraestructura implementa los puertos; la aplicación orquesta dominio y puertos.

---

## 11. Estructura del repositorio

```
ecommerceApp/
├── pom.xml                    # POM padre (solo módulos, no código)
├── compose.yaml               # Docker Compose: DBs + ambos servicios
├── .env-template              # Plantilla de variables (copiar a .env)
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
    ├── DIAGRAMAS_UML.md
    ├── DECISIONES_TECNICAS.md
    ├── COMPLIANCE_BACKEND.md
    └── curl-examples.md
```

---

## 12. Documentación adicional

| Documento                                                  | Contenido                                                                                                                                                                     |
| ---------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Swagger UI** (por servicio)                              | **Documentación de la API**: endpoints, parámetros, esquemas JSON:API, respuestas y autenticación. Products: `/swagger-ui.html` (8080); Inventory: `/swagger-ui.html` (8081). |
| [docs/ARQUITECTURA_C4.md](docs/ARQUITECTURA_C4.md)         | Diagramas C4 (contexto y contenedores).                                                                                                                                       |
| [docs/DIAGRAMAS_UML.md](docs/DIAGRAMAS_UML.md)             | Diagramas UML: casos de uso, clases (dominio) y despliegue.                                                                                                                   |
| [docs/DECISIONES_TECNICAS.md](docs/DECISIONES_TECNICAS.md) | Persistencia, resiliencia, idempotencia, concurrencia, seguridad.                                                                                                             |

---

## 13. Tecnologías

- **Java 17**, **Spring Boot 3.3**
- **Spring Web**, **Spring Data JPA**, **Flyway**, **Validation**
- **PostgreSQL** (producción/Docker); **H2** (tests)
- **Resilience4j**: retry y circuit breaker en el cliente Inventory → Products
- **Spring Security**: API Key, JWT (jjwt), rate limit por IP
- **Actuator**: health (liveness/readiness), info, metrics
- **Springdoc OpenAPI**: Swagger UI y `/v3/api-docs` en cada servicio
- **JUnit 5**, **Mockito**, **MockMvc** para pruebas; **Selenium + HtmlUnit** para E2E sin navegador (Swagger UI, health)
- **SonarQube** (sonar-maven-plugin) para análisis de calidad y cobertura
