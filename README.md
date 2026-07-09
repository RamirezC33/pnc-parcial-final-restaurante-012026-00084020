# Sistema de Pedidos de Restaurante — API (N-Capas + Seguridad)

API REST para una **cadena de restaurantes con varias sucursales**, donde distintos
usuarios (administradores, encargados de turno y clientes) gestionan mesas y pedidos.
Desarrollada con **Spring Boot 4 / Java 21**, arquitectura **N-Capas**, autenticación
**JWT (access + refresh)**, autorización por **roles** y una **regla de negocio no
trivial de autorización por sucursal**.

> La consigna original del parcial está en [`docs/CONSIGNA.md`](docs/CONSIGNA.md).
> La bitácora de IA está en [`PROMPTS.md`](PROMPTS.md) y la reflexión en [`REFLEXION.md`](REFLEXION.md).

---

## 1. Arquitectura N-Capas

El proyecto separa responsabilidades en capas, tal como se vio en clase:

| Capa | Paquete | Responsabilidad |
|---|---|---|
| **Presentación** | `web.controller`, `web.dto`, `web.GlobalExceptionHandler` | Exponer endpoints REST, validar la entrada y mapear excepciones a HTTP. No contiene reglas de negocio. |
| **Lógica de Negocio** | `service` | Reglas del dominio: cálculo de totales, autorización por sucursal (regla B), gestión de tokens. |
| **Acceso a Datos** | `repository`, `domain` | Entidades JPA y repositorios Spring Data (Hibernate ↔ PostgreSQL). |
| **Seguridad (transversal)** | `security`, `config` | Filtro JWT, `SecurityFilterChain`, codificación de contraseñas, configuración. |

```
Cliente HTTP
    │
    ▼
[web.controller]  ← DTOs + validación + @PreAuthorize (rol)
    │
    ▼
[service]         ← reglas de negocio + REGLA B (sucursal)
    │
    ▼
[repository]      ← Spring Data JPA
    │
    ▼
[domain]  ⇄  PostgreSQL
```

El flujo de una petición autenticada atraviesa además el filtro
`security.JwtAuthenticationFilter`, que valida el Access Token antes de llegar al controller.

---

## 2. Cómo levantar el proyecto con Docker

**Requisito único:** Docker + Docker Compose.

```bash
# 1. (Opcional) copiar variables de entorno y ajustarlas
cp .env.example .env

# 2. Levantar API + base de datos
docker-compose up --build
```

Esto arranca:

- **`db`** — PostgreSQL 16 (puerto `5432`).
- **`api`** — la API en `http://localhost:8080` (espera a que la BD esté saludable).

Al iniciar por primera vez, se cargan datos de ejemplo (sucursales, usuarios de cada
rol y productos) mediante `config.DataInitializer`.

Para detener: `Ctrl+C` y luego `docker-compose down` (agregar `-v` para borrar la BD).

### Variables de entorno principales

| Variable | Descripción | Default |
|---|---|---|
| `APP_JWT_SECRET` | Secreto HMAC para firmar los JWT (≥ 32 bytes) | valor dev inseguro |
| `APP_JWT_ACCESS_MINUTES` | Expiración del Access Token | `15` |
| `APP_JWT_REFRESH_DAYS` | Expiración del Refresh Token | `7` |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL | `restaurante` |
| `APP_SEED_ADMIN_PASSWORD` | Contraseña del admin inicial | `Admin123!` |

> En producción, el secreto JWT y las contraseñas **nunca** se hardcodean: se
> inyectan por variables de entorno / gestor de secretos.

---

## 3. Roles y permisos

| Rol | Permisos |
|---|---|
| **ADMINISTRADOR** | Acceso total: gestiona restaurantes, mesas, usuarios y pedidos de **todas** las sucursales. |
| **ENCARGADO** | Gestiona pedidos y mesas **únicamente de la sucursal a la que pertenece** (regla B). |
| **CLIENTE** | Solo crea, ve y cancela **sus propios** pedidos. |

El control por rol se hace con `@PreAuthorize` en los controllers; el control fino por
sucursal se hace en la capa de negocio (ver siguiente sección).

### Usuarios de ejemplo (seed)

| Usuario | Contraseña | Rol | Sucursal |
|---|---|---|---|
| `admin` | `Admin123!` | ADMINISTRADOR | — |
| `encargado.centro` | `Encargado123!` | ENCARGADO | Sucursal Centro |
| `encargado.norte` | `Encargado123!` | ENCARGADO | Sucursal Norte |
| `cliente` | `Cliente123!` | CLIENTE | — |

---

## 4. Regla de negocio no trivial — Opción B: Autorización por sucursal

> **Un Encargado de turno solo puede confirmar, modificar o cancelar pedidos (y gestionar
> mesas) de su propia sucursal.**

Esto **no** se resuelve solo con el rol: dos encargados tienen el mismo rol `ENCARGADO`
pero distinta sucursal. La autorización compara un **atributo del usuario autenticado**
(su `sucursalId`, que viaja dentro del JWT) contra un **atributo del recurso** (la
`sucursalId` del pedido/mesa).

**Dónde vive la regla:** `service.SucursalAccessGuard#verificarGestionSucursal(...)`,
invocada desde `PedidoService` y `MesaService`.

```java
if (role == Role.ENCARGADO) {
    Long sucursalActor = actor.getSucursalId();          // atributo del usuario
    if (sucursalActor == null || !sucursalActor.equals(sucursalIdRecurso)) { // vs recurso
        throw new ForbiddenOperationException(...);      // -> HTTP 403
    }
}
```

Detalles de diseño:

- La `sucursalId` se firma dentro del Access Token, así el chequeo no depende de datos
  manipulables por el cliente.
- El `Pedido` **denormaliza** su sucursal desde la mesa al crearse, de modo que la
  comparación es siempre directa y estable.
- El listado de pedidos/mesas también se filtra por sucursal, para que un encargado ni
  siquiera *vea* recursos de otra sucursal.

### Demostración rápida de la regla

```bash
# El encargado de la Sucursal Norte se autentica...
TOKEN_NORTE=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"encargado.norte","password":"Encargado123!"}' | jq -r .accessToken)

# ...e intenta cambiar el estado de un pedido de la Sucursal Centro (id 1) -> 403 Forbidden
curl -i -X PATCH localhost:8080/api/pedidos/1/estado \
  -H "Authorization: Bearer $TOKEN_NORTE" \
  -H 'Content-Type: application/json' \
  -d '{"estado":"CONFIRMADO"}'
```

---

## 5. Autenticación JWT

- **Login** (`POST /api/auth/login`) devuelve `accessToken` + `refreshToken`.
- **Access Token**: JWT firmado (HS256), *stateless*, expira en **15 min**. Lleva
  `sub`, `uid`, `role` y `sucursalId`.
- **Refresh Token**: opaco (UUID), **persistido en BD**, expira en **7 días**. Al
  persistirlo se puede **revocar** antes de tiempo (logout, rotación, cambio de contraseña).
- **Refresh** (`POST /api/auth/refresh`) valida el refresh token, lo **rota** (revoca el
  anterior y emite uno nuevo) y devuelve un nuevo Access Token.
- Las contraseñas se almacenan con **BCrypt**; nunca en texto plano.

### Ejemplo de flujo

```bash
# Login
curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"cliente","password":"Cliente123!"}'

# Usar el access token
curl localhost:8080/api/pedidos -H "Authorization: Bearer <ACCESS_TOKEN>"

# Renovar el access token
curl -X POST localhost:8080/api/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<REFRESH_TOKEN>"}'
```

---

## 6. Endpoints principales

| Método | Ruta | Rol requerido | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | público | Login (access + refresh) |
| POST | `/api/auth/refresh` | público | Renovar access token |
| POST | `/api/auth/register` | público | Registro de cliente |
| POST | `/api/auth/change-password` | autenticado | Cambiar contraseña (revoca sesiones) |
| GET/POST/PUT/DELETE | `/api/sucursales` | ADMIN (escritura) | CRUD de sucursales |
| GET/POST/PUT/DELETE | `/api/productos` | ADMIN (escritura) | CRUD del menú |
| GET | `/api/mesas` | autenticado | Listar mesas (filtrado por sucursal si es encargado) |
| POST | `/api/mesas` | ADMIN, ENCARGADO | Crear mesa (regla B) |
| PATCH | `/api/mesas/{id}/estado` | ADMIN, ENCARGADO | Cambiar estado (regla B) |
| GET/POST | `/api/usuarios` | ADMIN | Gestión de usuarios |
| POST | `/api/pedidos` | autenticado | Crear pedido |
| GET | `/api/pedidos` | autenticado | Listar (filtrado por rol/sucursal/dueño) |
| PATCH | `/api/pedidos/{id}/estado` | ADMIN, ENCARGADO | Cambiar estado (regla B) |
| PATCH | `/api/pedidos/{id}/cancelar` | autenticado | Cancelar (cliente: propio; encargado: su sucursal) |

Respuestas de error uniformes en JSON: `401` (sin token), `403` (sin permisos / regla B),
`400` (validación/negocio), `404` (no encontrado).

---

## 7. CI/CD (GitHub Actions)

El workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) corre en cada `push` a
`main` y en cada PR:

1. **Build & Test** — compila y ejecuta las pruebas con `./gradlew build`.
2. **Secret Scan** — `gitleaks` falla si detecta un secreto expuesto.
3. **Vulnerability Scan** — `Trivy` falla si detecta una vulnerabilidad **CRITICAL**.

---

## 8. Pruebas

```bash
./gradlew test
```

Incluye un test unitario de la regla B
(`SucursalAccessGuardTest`) y la carga de contexto de Spring (con H2 en memoria).

---

## 9. Stack técnico

- Java 21, Spring Boot 4.1, Spring Security 7, Spring Data JPA (Hibernate 7)
- PostgreSQL 16 (H2 en pruebas)
- JJWT 0.12 para los tokens
- Gradle 9, Docker / Docker Compose
