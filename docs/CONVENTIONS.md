# MEGALUJOS — Convenciones de implementación (obligatorio para agentes)

Lee además `MEGALUJOS_DOCUMENTO_MAESTRO.md` (fuente de verdad del alcance).

## 1. Stack y entorno

- Backend: Java 21, Spring Boot 3.4.1, JPA/Hibernate, PostgreSQL 16, Flyway, Spring Security + JWT (jjwt 0.12.6), springdoc OpenAPI.
- Compilación (desde la raíz del repo):
  `$env:JAVA_HOME='C:\Program Files\Java\jdk-26.0.2'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -q -B compile -f backend\pom.xml`
- NO editar: `pom.xml`, `application.yml`, `SecurityConfig.java`, `GlobalExceptionHandler.java`, migraciones `V1..V11`. Si necesitas configuración, usa `@Value` con valor por defecto.
- NO ejecutar `docker` ni `mvn test` (solo el orquestador). Compilar solo al final de tu trabajo y solo corregir errores en TUS archivos.

## 2. Estructura por módulo (package-by-feature)

```
ec/com/megalujos/<modulo>/
├── api/           # @RestController; solo llama a application/
├── application/   # @Service; transacciones; @Transactional
├── domain/        # @Entity + <X>Repository (interface JpaRepository)
└── dto/           # request/response con jakarta.validation; NUNCA exponer entidades JPA en JSON
```

- Módulos existentes: common, identity, catalog, inventory, purchasing, customers, pos, cash, services, invoicing, showcase, reporting.
- Dependencias permitidas entre módulos: `api → application` propio; `application → domain` propio y `application → application/Servicio` de otro módulo (p.ej. `StockService`, `AuditService`, `TaxService`). NO acceder a repositories de otro módulo si existe un servicio que lo haga; excepción: consultas de solo lectura donde no exista servicio.
- Controladores con `@Tag` y `@Operation` en español.

## 3. Endpoints REST

- Prefijo `/api/v1`, JSON camelCase, UUID como string, fechas ISO-8601 con zona.
- Paginación: aceptar `page` (0-based), `size`, `sort`; responder con `ec.com.megalujos.common.web.PageResponse<T>` (`PageResponse.from(page, mapper)`).
- Permisos con `@PreAuthorize` usando códigos exactos de BD (tabla `permissions`):
  `catalog:read`, `catalog:write`, `inventory:read`, `inventory:adjust`, `inventory:count`,
  `sales:create`, `sales:read`, `sales:return`, `cash:manage`, `services:manage`, `services:read`,
  `invoicing:issue`, `invoicing:read`, `reports:view`, `customers:manage`, `purchasing:manage`,
  `users:manage`, `showcase:manage`, `settings:manage`, `print:manage`, `audit:read`.
  Patrón: `@PreAuthorize("hasAuthority('codigo') or hasRole('ADMIN')")`.
- Usuario actual: `@AuthenticationPrincipal UserPrincipal principal` → `principal.getId()`.
- Cabecera `Idempotency-Key` para confirmar venta, cobros y emisión de documentos.

## 4. Errores y validación

- `BusinessRuleException(code, message)` para reglas de negocio; `ResourceNotFoundException("Entidad", id)` para 404.
- DTOs con `@NotBlank`, `@NotNull`, `@Positive`, `@DecimalMin`… y `@Valid` en el controller.
- Nunca confiar en totales/precios enviados por el cliente: recalcular en servidor.

## 5. Servicios transversales ya disponibles

- `common.audit.AuditService#record(actorId, action, entityType, entityId, metadata, ipAddress)` — llamar en ventas, descuentos, ajustes, cierres, facturas, cambios de permisos.
- `common.tax.TaxService` — `resolveDefault()` / `resolveByCode(code, date)` / `taxAmount(base, rate)`. NUNCA hardcodear IVA.
- `common.files.FileStorageService#store(MultipartFile, isPublic)` y `MediaController` ya existen (subida de archivos).
- `inventory.application.StockService` — `addStock(...)`, `deductStock(...)`, `getAvailableStock(...)`, `getMainWarehouseId()`; locking pesimista ya implementado. Todo cambio de stock DEBE pasar por aquí y generar `stock_movements` inmutable.
- `common.web.PageResponse`.

## 6. Migraciones Flyway (números RESERVADOS por agente — no uses otros)

| Agente | Migración |
|---|---|
| identity/usuarios | `V12__auth_sessions.sql` |
| catálogo admin | `V13__catalog_admin.sql` |
| inventory/customers/purchasing | `V14__inventory_purchasing_ext.sql` |
| pos/cash/services | `V15__pos_services_ext.sql` |
| invoicing/showcase/reports | `V16__invoicing_promotions.sql` |
| corrección anulación de venta (pos) | `V17__add_sale_void_movement_type.sql` |

Nunca editar migraciones ya ejecutadas (V1–V11). Todas las tablas principales ya existen (V1–V11); crea migración solo si falta columna/tabla real.

## 7. Convenciones de código

- Sin Lombok. Constructores inyectados, getters/setters manuales, comentarios de Javadoc en español solo donde aporte.
- `@Modifying` con `clearAutomatically=true` en un repositorio SEGUIDO de `save()` dentro del mismo bloque descarta los guardados pendientes (aún sin flush). Usar siempre `@Modifying(flushAutomatically = true, clearAutomatically = true)` (ver `OutboxEventRepository`, `ProductImageRepository`).
- `BigDecimal` para dinero (`NUMERIC(18,2)`), cantidades `BigDecimal` escala 3.
- Tiempos `OffsetDateTime` (UTC en BD; UI muestra America/Guayaquil).
- IDs UUID autogenerados.
- Endpoints de la vitrina pública bajo `/api/v1/public/**` (ya permitido en seguridad); solo productos `published=true`.

## 8. Endpoint contract (resumen del contrato esperado por frontend)

- `POST /api/v1/auth/login` (existe), `POST /api/v1/auth/refresh`, `GET /api/v1/auth/me`, `POST /api/v1/auth/change-password`.
- `GET/POST/PUT/DELETE /api/v1/users`, `GET /api/v1/roles`, `GET /api/v1/audit`.
- `GET/POST/PUT /api/v1/categories`, `/brands`, `/products`, `PUT /api/v1/products/{id}/publish`, `POST /api/v1/products/{id}/images`, `DELETE .../images/{imageId}`, `PUT .../images/primary`.
- `GET /api/v1/inventory/balances`, `GET /api/v1/inventory/movements` (kardex paginado), `POST /api/v1/inventory/adjustments`, `POST /api/v1/inventory/adjustments/{id}/approve`, `GET /api/v1/inventory/counts`, `POST /api/v1/inventory/counts`, `GET /api/v1/inventory/alerts`.
- `GET/POST/PUT /api/v1/customers` (+ `/{id}/vehicles`), `/api/v1/suppliers`, `/api/v1/purchase-orders`, `POST /api/v1/purchase-orders/{id}/receipts`.
- `GET /api/v1/sales` (paginado, filtros), `POST /api/v1/sales` (existe), `POST /api/v1/sales/{id}/returns`.
- Caja: endpoints existentes + `GET /api/v1/cash/sessions/{id}/reconciliation` (arqueo por medio de pago).
- `GET/POST/PUT /api/v1/service-orders`, `POST /api/v1/service-orders/{id}/status`, `POST .../materials/{id}/consume`, `POST .../advances`, `POST .../attachments`.
- `POST /api/v1/electronic-documents`, `GET /api/v1/electronic-documents` (lista), `GET /api/v1/electronic-documents/{id}`, `POST .../{id}/retry`, `POST /api/v1/electronic-documents/credit-notes`.
- `GET/POST/PUT /api/v1/banners`, `/api/v1/showcases`, `PUT /api/v1/showcases/{code}/products`.
- `POST /api/v1/public/catalog/inquiries` (registro consulta WhatsApp), `GET /api/v1/public/showcases/tv`.
- `GET /api/v1/reports/daily-sales`, `/margin`, `/stock-alerts`, `/sales-by-category`, `/tax-summary`, `/pending-documents`, `/services-by-status`, `/popular-products`, `/cash-closings`.
- `POST /api/v1/print-jobs`, `GET /api/v1/print-jobs`.

## 9. Estilo visual (frontend)

Tokens SCSS (ver `libs/theme`): `--mega-red:#D71920`, `--mega-red-hover:#B9141A`, `--mega-black:#111318`, `--mega-carbon:#1B1F25`, `--mega-silver:#AEB4BC`, `--mega-white:#FFFFFF`, `--mega-light:#F4F6F8`, `--mega-text:#22262C`, `--mega-success:#18855B`, `--mega-warning:#B66A00`. Bootstrap 5 + SCSS; radio 12–16px; estados cargando/vacío/error/sin permisos/sin conexión/éxito obligatorios; `prefers-reduced-motion` respetado.
