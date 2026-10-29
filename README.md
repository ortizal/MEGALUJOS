# MEGALUJOS

Sistema de gestión integral para negocio de automoción y rotulación (Ecuador):
catálogo e inventario con kardex, punto de venta con cajas, servicios/órdenes de
trabajo (stickers e instalaciones), facturación electrónica SRI, vitrina pública
con WhatsApp y reportes.

> Fuente de verdad del alcance: [`MEGALUJOS_DOCUMENTO_MAESTRO.md`](MEGALUJOS_DOCUMENTO_MAESTRO.md).
> Convenciones obligatorias para colaborar: [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md).

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 3.4.1 (web, security+JWT, JPA, validation, actuator), PostgreSQL 16, Flyway, springdoc OpenAPI |
| Frontend | Angular 22 (workspace con `apps/admin` y `apps/storefront`), Bootstrap 5 + SCSS, librerías en `libs/` |
| Infraestructura | Docker Compose (`infra/compose.yaml`), nginx como reverse proxy, backups cifrados con OpenSSL |
| CI | GitHub Actions (`/.github/workflows/ci.yml`): Maven verify, builds Angular y build de imágenes Docker |

## Requisitos

- JDK 21 (compila también con JDK superior) y Maven 3.9+
- Node.js 24+ y npm 10+ (workspace Angular 22)
- Docker + Docker Compose v2 (solo para la base de datos y despliegues)
- PostgreSQL 16 (opcional si usas el contenedor de `infra/compose.yaml`)

## Puesta en marcha local

### 1. Variables de entorno

```powershell
Copy-Item .env.example .env   # y ajustar contraseñas/secretos
```

`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y
`SPRING_DATASOURCE_PASSWORD` del `.env` son los que usan tanto el backend como
el servicio `postgres` de Compose, así que ambos quedan consistentes.

### 2. Base de datos (Docker)

```powershell
# En local se publica 5432 (descomentar el bloque `ports:` comentado del
# servicio postgres en infra/compose.yaml). En el VPS NO se expone.
docker compose -f infra/compose.yaml --env-file .env up -d postgres
```

Flyway aplica `V1..V16` (esquema + seed) automáticamente al arrancar el backend.

### 3. Backend (Spring Boot, puerto 8080)

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'   # ajustar a tu instalación
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -B -f backend/pom.xml spring-boot:run
```

Verificación: <http://localhost:8080/actuator/health>

### 4. Frontend (dos apps)

```powershell
cd frontend
npm ci
npm start -- admin       --port 4200   # panel administrativo  → http://localhost:4200
npm start -- storefront  --port 4300   # vitrina pública       → http://localhost:4300
```

(Equivalente: `npx ng serve admin --port 4200` y `npx ng serve storefront --port 4300`.)

### 5. Todo con Docker (VPS / integración completa)

```powershell
# Obligatorio antes de levantar la pila (cifrado de backups):
#   BACKUP_PASSPHRETE=<openssl rand -base64 48>
docker compose -f infra/compose.yaml --env-file .env up -d --build
# → http://localhost/            storefront
# → http://localhost/admin/      panel administrativo
# → http://localhost/api/v1/...  API
```

## Credenciales demo (seed `V10__seed_data.sql`)

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | ROLE_ADMIN — acceso total |
| `cajero1` | `cajero123` | ROLE_CASHIER — POS, ventas y cobros |

Cambiar las contraseñas antes de cualquier entorno compartido.

## Endpoints Swagger / OpenAPI

| Recurso | URL local | URL vía nginx |
|---|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui/index.html> | `http://localhost/swagger-ui/index.html` |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> | `http://localhost/v3/api-docs` |
| Health | <http://localhost:8080/actuator/health> | `http://localhost/actuator/health` |

Prefijo de la API: **`/api/v1`** (contrato completo en
`docs/CONVENTIONS.md` §8). Endpoints públicos de vitrina bajo
`/api/v1/public/**`.

## Estructura del repositorio

```
MEGALUJOS/
├── backend/                     # API Spring Boot (Java 21)
│   ├── Dockerfile               # multi-stage: maven → eclipse-temurin:21-jre (no-root)
│   └── src/main/java/ec/com/megalujos/<modulo>/{api,application,domain,dto}
├── frontend/                    # Workspace Angular 22
│   ├── apps/admin/              # panel administrativo (dev :4200)
│   ├── apps/storefront/         # vitrina pública     (dev :4300)
│   ├── libs/                    # theme, ui, api-client, auth, models
│   └── Dockerfile               # multi-stage: node:24-alpine → nginx:alpine (ARG APP)
├── infra/
│   ├── compose.yaml             # postgres + backend + admin + storefront + nginx + backup
│   ├── nginx/nginx.conf         # reverse proxy (HTTP)
│   ├── nginx/nginx-tls.conf.example
│   ├── backup/                  # backup.sh cifrado + imagen con cron + README
│   └── monitoring/              # guía + docker-compose.example.yml (Dozzle)
├── docs/
│   ├── CONVENTIONS.md           # convenciones de implementación (obligatorio leer)
│   ├── runbooks/                # deploy, backup-restore, printing, sri-rotation, incidents
│   └── adr/                     # decisiones arquitectónicas
├── .github/workflows/           # ci.yml + deploy.yml.example (plantilla SSH)
├── .env.example                 # plantilla de variables (sin secretos)
└── MEGALUJOS_DOCUMENTO_MAESTRO.md
```

## Documentación operativa

- Despliegue y rollback: [`docs/runbooks/deploy.md`](docs/runbooks/deploy.md)
- Backups y restauración: [`docs/runbooks/backup-restore.md`](docs/runbooks/backup-restore.md)
- Impresora térmica 80 mm: [`docs/runbooks/printing.md`](docs/runbooks/printing.md)
- Rotación del certificado SRI (P12): [`docs/runbooks/sri-rotation.md`](docs/runbooks/sri-rotation.md)
- Incidencias (SRI caído, disco lleno, certificado vencido): [`docs/runbooks/incidents.md`](docs/runbooks/incidents.md)
- Monitoreo: [`infra/monitoring/README.md`](infra/monitoring/README.md)
- Decisiones arquitectónicas: [`docs/adr/`](docs/adr/)

## Seguridad

- Secretos solo en `.env` (fuera de Git; `.env.example` no contiene valores reales).
- PostgreSQL **no** se publica al host en el VPS; la red `megalujos-network` es privada.
- El backend corre como usuario no-root con `HEALTHCHECK` en `/actuator/health`.
- nginx aplica `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`,
  `client_max_body_size 50M` y timeouts de 60 s para la API.
