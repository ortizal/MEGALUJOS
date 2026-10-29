# MEGALUJOS — DOCUMENTO MAESTRO DEL PROYECTO

**Versión:** 3.0 | **Fecha:** 2026-10-17 | **Estado:** especificación para implementación | **Moneda:** USD | **País:** Ecuador

> **Propósito de este archivo:** fuente de verdad del alcance, experiencia de usuario, arquitectura, modelo de datos, reglas de negocio, contratos, infraestructura, pruebas y planificación de MEGALUJOS. Los ejemplos de código y migraciones son punto de partida y deben revisarse antes de producción.

## A. Visión y objetivos

MEGALUJOS es un sistema web integral para un local de accesorios automotrices que comercializa luces, aceites, parlantes, accesorios y realiza impresión de stickers e instalaciones. Conecta administración interna, inventario, caja, facturación electrónica ecuatoriana, órdenes de trabajo y vitrina pública de consulta por WhatsApp.

**Objetivos medibles de la primera versión:**
1. Registrar el 100 % de productos y variantes vendibles con SKU, categoría, precios, fotos y estado de publicación.
2. Trazar cada cambio de existencias a una compra, venta, devolución, ajuste o consumo de servicio.
3. Registrar ventas, pagos y cierres de caja sin duplicaciones por reintentos.
4. Emitir y seguir comprobantes electrónicos hasta autorización o resolución de incidencia.
5. Gestionar órdenes de stickers e instalaciones con materiales, técnicos, anticipos y entregas.
6. Publicar productos con precios visibles y consulta por WhatsApp, sin checkout online.

### Alcance confirmado

| Decisión | Primera versión |
|---|---|
| Locales | Uno; modelo preparado para expansión |
| Canal digital | Vitrina de productos, precios, disponibilidad general, WhatsApp |
| Compra en línea | **Fuera de alcance**: sin pasarela ni checkout |
| Facturación | Desarrollo propio, firma electrónica e integración SRI |
| Infraestructura | VPS Linux, Docker, Nginx, HTTPS |
| Impresión | Ticket térmico y PDF A4 |
| Servicios | Stickers personalizados e instalaciones incluidos |
| Modo offline | Fuera de alcance de v1 |
| Idioma | Español; moneda USD; zona horaria America/Guayaquil |

### No objetivos de v1

No implementar app móvil nativa, marketplace multivendedor, contabilidad general completa, nómina, múltiples sucursales operativas, pagos web, GPS de repartidores ni recomendaciones con IA como dependencia de lanzamiento. Preparar interfaces de extensión, sin construirlas prematuramente.

## B. Identidad visual y experiencia de usuario

### Paleta obligatoria

| Token | Hex | Uso |
|---|---|---|
| `--mega-red` | `#D71920` | CTA primario, acentos, promociones |
| `--mega-red-hover` | `#B9141A` | Hover/active |
| `--mega-black` | `#111318` | Encabezados, fondos oscuros |
| `--mega-carbon` | `#1B1F25` | Superficies dark |
| `--mega-silver` | `#AEB4BC` | Bordes, íconos, acentos metálicos |
| `--mega-white` | `#FFFFFF` | Fondos claros y textos dark |
| `--mega-light` | `#F4F6F8` | Fondos secundarios light |
| `--mega-text` | `#22262C` | Texto en light |
| `--mega-success` | `#18855B` | Confirmaciones, no decorativo |
| `--mega-warning` | `#B66A00` | Advertencias |

**Reglas:** usar Bootstrap 5 para grid/responsive/utilidades y SCSS con tokens para identidad; no hardcodear colores por pantalla. Usar tipografía Inter o alternativa libre similar, títulos semibold/bold, controles compactos pero accesibles, tarjetas de radio 12–16 px, sombras sutiles, gradientes negro→carbono y acentos rojo→rojo oscuro. Los gradientes plateados se reservan para separadores/detalles, no fondos que comprometan legibilidad. Light y dark deben tener contraste suficiente y persistir preferencia por usuario. Respetar `prefers-reduced-motion`.

### Diseño responsive

- **Desktop ≥1200 px:** sidebar expandible, dashboard de varias columnas, POS dividido búsqueda/carrito.
- **Tablet 768–1199 px:** sidebar plegable, tablas con scroll horizontal, POS adaptado.
- **Móvil <768 px:** navegación compacta, formularios de una columna, carrito accesible sin perder estado.
- Estados obligatorios: cargando, vacío, error, sin permisos, sin conexión, éxito, validación.
- Confirmar acciones destructivas y presentar mensajes accionables; nunca depender solo del color.

### Componentes compartidos

`MegaButton`, `MegaInput`, `MegaSelect`, `MegaTextarea`, `MegaModal`, `MegaConfirmDialog`, `MegaTable`, `MegaPagination`, `MegaUploader`, `MegaProductCard`, `MegaProductGallery`, `MegaCarousel`, `MegaPrice`, `MegaStockBadge`, `MegaStatusBadge`, `MegaCurrencyInput`, `MegaDatePicker`, `MegaToast`, `MegaEmptyState`, `MegaSkeleton`, `MegaCashSummary`.

### Vitrina y carrusel/modal

- Home con hero automotriz, CTA a catálogo, categorías, novedades, promociones y productos destacados.
- Catálogo con búsqueda por texto, categoría, marca, precio mínimo/máximo, orden y paginación; filtros de compatibilidad solo con datos verificados.
- Tarjeta con imagen principal, nombre, precio USD, disponibilidad general y botón consultar WhatsApp.
- Detalle con galería de varias fotos, zoom, atributos, compatibilidades y productos relacionados.
- Carrusel de home con avance automático configurable (predeterminado 6 segundos), flechas, indicadores, pausa en hover/foco y controles de teclado.
- **Modal de presentación** con avance automático configurable, botón pausa/reanudar, siguiente/anterior, cierre con Escape, foco atrapado y restaurado al cerrar, desactivación de animación por accesibilidad.
- **Modo TV:** pantalla completa, loop continuo de productos publicados, precios y promociones vigentes; no mostrar datos privados ni precios de costo; recuperación tras recarga.
- WhatsApp con enlace `wa.me` configurado y mensaje prellenado con nombre, SKU y URL pública del producto. No prometer integración de chatbot en v1.
- SEO: rutas semánticas, meta tags, sitemap, Open Graph, renderizado apropiado para indexación, optimización WebP/AVIF y lazy loading.
- La publicación es explícita; producto inactivo, sin precio válido o no publicado no aparece en vitrina. El administrador configura si mostrar agotados y el texto de disponibilidad.

## C. Actores, permisos y módulos

### Matriz de roles base

| Operación | Administrador | Supervisor | Cajero | Bodeguero | Técnico |
|---|---|---|---|---|---|
| Configurar negocio y usuarios | Sí | Lectura limitada | No | No | No |
| Gestionar catálogo/precios | Sí | Sí | Lectura | Lectura | Lectura |
| Ajustar inventario | Sí | Autoriza | No | Solicita | No |
| Compras y recepciones | Sí | Sí | No | Recepción | No |
| Apertura/cierre de caja | Sí | Supervisa | Su caja | No | No |
| Registrar ventas | Sí | Sí | Sí | No | No |
| Emitir/consultar facturas | Sí | Sí | Según permiso | No | No |
| Órdenes de servicio | Sí | Sí | Crear/consultar | Materiales | Ejecutar |
| Vitrina/promociones | Sí | Según permiso | No | No | No |
| Reportes financieros | Sí | Según permiso | Solo caja propia | No | No |

Los permisos se modelan por acción (`product.read`, `sale.create`, `cash.close`, `invoice.issue`, `inventory.adjust.approve`, etc.) y se validan en backend. La matriz es una configuración inicial editable.

### Módulos y reglas de negocio

**1. Seguridad:** login, recuperación, sesiones, refresh tokens rotativos, roles/permisos, auditoría, rate limiting y bloqueo progresivo de intentos abusivos.

**2. Productos:** categorías anidadas, marcas, SKU, código de barras, variantes (potencia/color/medida), atributos, fotos, unidad, costo, precios, impuestos configurables, activación/publicación, compatibilidad con vehículo. Archivos privados o públicos según contexto; miniaturas derivadas.

**3. Inventario:** almacén principal, existencias por variante, kardex inmutable, conteos físicos, ajustes autorizados, stock mínimo, lotes/vencimientos cuando aplique, reservas únicamente si el flujo las requiere. Stock disponible = físico - reservado. No permitir ventas que excedan disponible; concurrencia controlada en BD.

**4. Proveedores y compras:** proveedores, órdenes de compra, recepciones parciales, costos de compra, devolución, cuentas por pagar si se habilitan. Recepción confirmada genera movimientos de stock una sola vez.

**5. Clientes y vehículos:** identificación y tipo de documento, contacto, dirección opcional, consentimiento de comunicaciones, vehículos (marca/modelo/año/versión/placa opcional), historial de compras/servicios. Restringir acceso a datos personales.

**6. POS:** búsqueda rápida, escáner de código de barras, carrito, descuentos con autorización, múltiples impuestos y medios de pago, pagos mixtos, venta atómica, ticket, devolución y consulta histórica. La emisión tributaria es un proceso independiente pero vinculado.

**7. Caja:** caja física, sesiones de apertura/cierre, saldo inicial, cobros, retiros, ingresos no comerciales, arqueo por medio de pago, diferencias, supervisión, prohibición de sesiones simultáneas incompatibles.

**8. Facturación electrónica:** configuración de emisor, establecimiento/punto de emisión, secuenciales atómicos, generación XML conforme a esquema SRI vigente, firma XAdES-BES, envío a recepción, consulta de autorización, persistencia de estados y respuestas, RIDE PDF, XML autorizado, envío por email, notas de crédito y contingencias según normativa vigente. La implementación requiere validar fichas técnicas, catálogos, ambientes y requisitos actuales del SRI antes de programar la integración productiva.

**9. Servicios y stickers:** orden con cliente/vehículo, descripción, medidas, colores, archivo original y previsualización, material vinilo/laminado, cantidad, precio de mano de obra, anticipo, técnico, fecha compromiso, estados, consumo real de materiales, entrega y garantía. No descontar insumos en cotización; descontar al confirmar consumo/ejecución mediante movimiento trazable. Un servicio puede incluir productos instalados.

**10. Vitrina:** banners, productos destacados, promociones, catálogo público, precios, disponibilidad general, carruseles, modal de presentación, modo TV, enlace WhatsApp, SEO y analítica básica de consultas sin rastreo invasivo.

**11. Reportes:** ventas diarias, margen bruto estimado, rotación, alertas de stock, ventas por categoría, cierres, impuestos, comprobantes pendientes, servicios por estado, materiales consumidos, productos populares.

**12. Configuración:** datos del negocio, logotipo, WhatsApp, políticas de precio/stock, impuestos, firma SRI cifrada, impresoras, numeración, backups, plantillas PDF y preferencias visuales.

## D. Reglas transversales y escenarios críticos

1. **Moneda:** USD; `NUMERIC(14,2)` para importes, `NUMERIC(14,4)` para costos unitarios y `NUMERIC(14,3)` para cantidades; redondeos definidos por normativa y comprobante.
2. **Tiempo:** persistir UTC (`TIMESTAMPTZ`) y mostrar `America/Guayaquil`.
3. **Transacciones:** venta, pagos internos y movimientos de stock consistentes dentro de transacción; comunicación SRI fuera de transacción con outbox y worker.
4. **Idempotencia:** cada venta confirmada recibe clave única; reintento devuelve resultado original, nunca duplica cobro/stock.
5. **Auditoría:** ventas, descuentos, ajustes, cierres, facturas y cambios de permisos guardan actor, instante y referencia.
6. **Devoluciones:** documento compensatorio, autorización, devolución de dinero y reintegro de stock según condición del artículo; no borrar venta original.
7. **Precio histórico:** venta guarda snapshot de nombre, SKU, impuestos, descuento y precio.
8. **Facturas:** no marcar autorizada sin respuesta oficial; conservar XML, firma, mensajes y número de autorización; manejar rechazo/incidencia/reconsulta.
9. **Anticipos:** conciliar con saldo de orden y venta; no duplicar ingreso al cobrar restante.
10. **Inventario:** no editar stock directamente; actualizar con movimientos y locking apropiado.
11. **Archivos:** validar MIME real, extensión, tamaño, virus si corresponde, permisos y rutas; nunca servir archivos privados por URL pública.
12. **Protección de datos:** políticas de retención, minimización, controles de acceso y cumplimiento aplicable de Ecuador.

## E. Navegación y pantallas

```text
ADMIN
├── Inicio / Dashboard
├── Catálogo
│   ├── Productos / Nuevo / Editar / Detalle
│   ├── Categorías / Marcas / Variantes
│   └── Fotos / Compatibilidades
├── Inventario
│   ├── Existencias / Kardex / Ajustes / Conteos / Alertas
├── Compras
│   ├── Proveedores / Órdenes / Recepciones / Devoluciones
├── Ventas
│   ├── POS / Historial / Devoluciones / Cotizaciones
├── Caja
│   ├── Apertura / Movimientos / Arqueo / Cierre
├── Facturación
│   ├── Emitidos / Pendientes / Incidencias / Notas de crédito
├── Servicios
│   ├── Órdenes / Agenda / Diseños / Materiales / Entregas
├── Clientes / Vehículos
├── Vitrina
│   ├── Banners / Destacados / Promociones / Modo TV
├── Reportes
└── Configuración / Usuarios / Roles / SRI / Impresión

PÚBLICO
├── Inicio
├── Catálogo / Categoría / Búsqueda
├── Producto / Detalle
├── Promociones
├── Servicios
├── Contacto / WhatsApp
└── Presentación TV
```

### Formularios mínimos

- **Producto:** SKU, nombre, categoría, marca, unidad, tipo, variantes, código de barras, atributos, impuestos, costo, precio, stock mínimo, imágenes, publicación.
- **Venta:** caja, cliente opcional conforme al tipo de comprobante permitido, ítems, cantidades, descuentos, impuestos, pagos, total, estado.
- **Servicio:** cliente, vehículo opcional, tipo, dimensiones, materiales previstos, diseño, técnico, fecha, anticipo, precio, estados y archivos.
- **Facturación:** datos del receptor, comprobante, detalle, impuestos, secuencial, firma, estado, XML y RIDE.


---

# ANEXO TÉCNICO — DISEÑO FÍSICO, MIGRACIONES Y CONTRATOS

# MEGALUJOS — Plan técnico ejecutable v2.0

**Fecha:** 2026-10-17  
**Alcance:** un local, catálogo con precios y consultas por WhatsApp, órdenes de stickers e instalaciones, POS, inventario, facturación electrónica propia SRI, VPS, tickets y A4.  
**Estado:** especificación inicial para implementación; verificar normativa y esquemas SRI vigentes antes de homologar.

## 1. Decisiones de arquitectura

- Monolito modular Spring Boot, Java 21, PostgreSQL 16+, Flyway, JPA/Hibernate, Spring Security y OpenAPI.
- Angular (versión compatible y soportada al inicio) en dos aplicaciones: `admin` y `storefront`; librería compartida de modelos, componentes y estilos. Bootstrap 5, SCSS, SSR/prerender para catálogo.
- Docker Compose en VPS Ubuntu, Nginx con HTTPS, PostgreSQL sin puerto público, volúmenes persistentes, backups cifrados externos y observabilidad.
- Una sucursal y una bodega por defecto; esquema preparado para varias. Inventario por variante y bodega.
- Dinero en NUMERIC(18,2), costos NUMERIC(18,4), cantidades NUMERIC(18,3), fechas TIMESTAMPTZ; UUID como PK.
- Impuestos por línea y snapshot de tarifa vigente. No fijar tasas tributarias en código.
- Facturación SRI en worker asíncrono con outbox transaccional, reintentos seguros, XML firmado, respuestas persistidas, RIDE y nota de crédito.
- Archivos privados fuera de base de datos; metadatos en `media_files`. Certificado y secretos protegidos con permisos mínimos.
- Las órdenes de servicio manejan anticipos, materiales consumidos, técnico, estado, archivos y entrega; los anticipos se aplican una sola vez.

## 2. Estructura de repositorio

```text
megalujos/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/ec/com/megalujos/
│       │   ├── MegalujosApplication.java
│       │   ├── common/{config,exception,security,audit,files}/
│       │   ├── identity/{api,application,domain,infrastructure}/
│       │   ├── catalog/{api,application,domain,infrastructure}/
│       │   ├── inventory/{api,application,domain,infrastructure}/
│       │   ├── purchasing/{api,application,domain,infrastructure}/
│       │   ├── customers/{api,application,domain,infrastructure}/
│       │   ├── pos/{api,application,domain,infrastructure}/
│       │   ├── cash/{api,application,domain,infrastructure}/
│       │   ├── services/{api,application,domain,infrastructure}/
│       │   ├── invoicing/{api,application,domain,infrastructure,sri}/
│       │   ├── showcase/{api,application,domain,infrastructure}/
│       │   └── reporting/{api,application,domain,infrastructure}/
│       └── resources/{application.yml,db/migration/,reports/}
│   └── src/test/
├── frontend/
│   ├── apps/{admin,storefront}/
│   └── libs/{ui,api-client,models,auth,theme}/
├── infra/{compose.yaml,nginx/,backup/,monitoring/}
├── docs/{api/,architecture/,adr/,testing/,runbooks/}
└── .github/workflows/
```

**Regla de módulos:** controladores solo llaman casos de uso; no acceder a repositorios de otro módulo directamente. Transacciones delimitadas en capa de aplicación. DTO separados de entidades JPA. No exponer entidades por JSON.

## 3. Migraciones Flyway: orden y dependencias

| Archivo | Contenido |
|---|---|
| V1__extensions_and_core.sql | UUID, sucursales, bodegas, usuarios, roles, permisos, auditoría |
| V2__catalog.sql | Categorías, marcas, productos, variantes, fotos, compatibilidad |
| V3__inventory.sql | Saldos, movimientos, conteos y lotes |
| V4__customers_purchasing.sql | Clientes, vehículos, proveedores, compras, recepciones |
| V5__pos_cash.sql | Cajas, sesiones, ventas, líneas, pagos, movimientos, devoluciones |
| V6__services.sql | Órdenes, líneas, materiales, archivos, anticipos y estados |
| V7__invoicing.sql | Emisor, secuencias, documentos, eventos, archivos, outbox |
| V8__showcase.sql | Banners, carruseles, productos destacados, consultas |
| V9__indexes_constraints.sql | Índices, constraints y vistas de consulta |

### V1__extensions_and_core.sql

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE branches (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(20) NOT NULL UNIQUE, name varchar(150) NOT NULL,
  active boolean NOT NULL DEFAULT true
);
CREATE TABLE warehouses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  branch_id uuid NOT NULL REFERENCES branches(id),
  code varchar(30) NOT NULL, name varchar(150) NOT NULL,
  UNIQUE(branch_id, code)
);
CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  username varchar(80) NOT NULL UNIQUE,
  password_hash varchar(255) NOT NULL,
  full_name varchar(160) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE roles (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), code varchar(60) NOT NULL UNIQUE, name varchar(100) NOT NULL);
CREATE TABLE permissions (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), code varchar(100) NOT NULL UNIQUE);
CREATE TABLE user_roles (user_id uuid NOT NULL REFERENCES users(id), role_id uuid NOT NULL REFERENCES roles(id), PRIMARY KEY(user_id, role_id));
CREATE TABLE role_permissions (role_id uuid NOT NULL REFERENCES roles(id), permission_id uuid NOT NULL REFERENCES permissions(id), PRIMARY KEY(role_id, permission_id));
CREATE TABLE audit_logs (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), actor_id uuid REFERENCES users(id),
 action varchar(100) NOT NULL, entity_type varchar(80) NOT NULL,
 entity_id uuid, metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
 occurred_at timestamptz NOT NULL DEFAULT now()
);
```

### V2__catalog.sql

```sql
CREATE TABLE categories (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), parent_id uuid REFERENCES categories(id),
 name varchar(150) NOT NULL, slug varchar(180) NOT NULL UNIQUE, active boolean NOT NULL DEFAULT true
);
CREATE TABLE brands (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name varchar(150) NOT NULL UNIQUE);
CREATE TABLE products (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), category_id uuid NOT NULL REFERENCES categories(id),
 brand_id uuid REFERENCES brands(id), sku varchar(60) NOT NULL UNIQUE,
 name varchar(200) NOT NULL, description text,
 product_type varchar(20) NOT NULL CHECK(product_type IN ('PHYSICAL','SERVICE')),
 unit varchar(20) NOT NULL DEFAULT 'UNIT', track_stock boolean NOT NULL DEFAULT true,
 published boolean NOT NULL DEFAULT false, active boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now(),
 CHECK(product_type <> 'SERVICE' OR track_stock = false)
);
CREATE TABLE product_variants (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 sku varchar(80) NOT NULL UNIQUE, barcode varchar(80) UNIQUE,
 attributes jsonb NOT NULL DEFAULT '{}'::jsonb,
 sale_price numeric(18,2) NOT NULL CHECK(sale_price >= 0),
 min_stock numeric(18,3) NOT NULL DEFAULT 0 CHECK(min_stock >= 0),
 active boolean NOT NULL DEFAULT true
);
CREATE TABLE media_files (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), storage_key text NOT NULL UNIQUE,
 original_name varchar(255) NOT NULL, content_type varchar(100) NOT NULL,
 size_bytes bigint NOT NULL CHECK(size_bytes > 0), sha256 varchar(64),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE product_images (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), product_id uuid NOT NULL REFERENCES products(id),
 media_id uuid NOT NULL REFERENCES media_files(id), sort_order integer NOT NULL DEFAULT 0,
 is_primary boolean NOT NULL DEFAULT false, UNIQUE(product_id,media_id)
);
CREATE UNIQUE INDEX ux_product_primary_image ON product_images(product_id) WHERE is_primary;
CREATE TABLE vehicle_brands (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name varchar(120) NOT NULL UNIQUE);
CREATE TABLE vehicle_models (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), brand_id uuid NOT NULL REFERENCES vehicle_brands(id), name varchar(120) NOT NULL, UNIQUE(brand_id,name));
CREATE TABLE vehicle_versions (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), model_id uuid NOT NULL REFERENCES vehicle_models(id), name varchar(120) NOT NULL, year_from integer, year_to integer, CHECK(year_to IS NULL OR year_from IS NULL OR year_to >= year_from));
CREATE TABLE product_vehicle_compatibility (variant_id uuid NOT NULL REFERENCES product_variants(id), vehicle_version_id uuid NOT NULL REFERENCES vehicle_versions(id), verified boolean NOT NULL DEFAULT false, notes text, PRIMARY KEY(variant_id,vehicle_version_id));
```

### V3__inventory.sql

```sql
CREATE TABLE stock_balances (
 variant_id uuid NOT NULL REFERENCES product_variants(id), warehouse_id uuid NOT NULL REFERENCES warehouses(id),
 quantity numeric(18,3) NOT NULL DEFAULT 0 CHECK(quantity >= 0),
 reserved numeric(18,3) NOT NULL DEFAULT 0 CHECK(reserved >= 0 AND reserved <= quantity),
 version bigint NOT NULL DEFAULT 0,
 PRIMARY KEY(variant_id,warehouse_id)
);
CREATE TABLE stock_movements (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 variant_id uuid NOT NULL REFERENCES product_variants(id),
 warehouse_id uuid NOT NULL REFERENCES warehouses(id),
 movement_type varchar(30) NOT NULL CHECK(movement_type IN ('PURCHASE','SALE','SALE_RETURN','SUPPLIER_RETURN','ADJUSTMENT_IN','ADJUSTMENT_OUT','SERVICE_CONSUMPTION','SERVICE_RETURN')),
 quantity_delta numeric(18,3) NOT NULL CHECK(quantity_delta <> 0),
 unit_cost numeric(18,4) CHECK(unit_cost IS NULL OR unit_cost >= 0),
 source_type varchar(60) NOT NULL, source_id uuid NOT NULL,
 actor_id uuid REFERENCES users(id), created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(source_type,source_id,variant_id,warehouse_id,movement_type)
);
CREATE INDEX ix_stock_movements_variant_date ON stock_movements(variant_id,created_at DESC);
```

**Advertencia de diseño:** el índice UNIQUE anterior presupone una sola línea por combinación de origen/variante/bodega/tipo. Si se permiten líneas repetidas, incorporar `source_line_id` y hacer la unicidad por línea. Para producción se recomienda `source_line_id` y restricciones por caso de uso.

### V4__customers_purchasing.sql

```sql
CREATE TABLE customers (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), document_type varchar(20),
 document_number varchar(30), name varchar(200) NOT NULL,
 email varchar(200), phone varchar(40), address text,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_customer_document ON customers(document_type,document_number)
 WHERE document_number IS NOT NULL;
CREATE TABLE customer_vehicles (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), customer_id uuid NOT NULL REFERENCES customers(id),
 plate varchar(20), vehicle_version_id uuid REFERENCES vehicle_versions(id),
 year integer, color varchar(60), notes text
);
CREATE TABLE suppliers (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), tax_id varchar(30) UNIQUE,
 name varchar(200) NOT NULL, email varchar(200), phone varchar(40), active boolean NOT NULL DEFAULT true
);
CREATE TABLE purchase_orders (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), supplier_id uuid NOT NULL REFERENCES suppliers(id),
 order_number varchar(40) NOT NULL UNIQUE, status varchar(30) NOT NULL,
 ordered_at timestamptz NOT NULL DEFAULT now(), total numeric(18,2) NOT NULL DEFAULT 0
);
CREATE TABLE purchase_order_items (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_id uuid NOT NULL REFERENCES purchase_orders(id),
 variant_id uuid NOT NULL REFERENCES product_variants(id),
 quantity numeric(18,3) NOT NULL CHECK(quantity > 0), unit_cost numeric(18,4) NOT NULL CHECK(unit_cost >= 0)
);
CREATE TABLE goods_receipts (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_id uuid NOT NULL REFERENCES purchase_orders(id),
 warehouse_id uuid NOT NULL REFERENCES warehouses(id), received_at timestamptz NOT NULL DEFAULT now(),
 status varchar(30) NOT NULL
);
CREATE TABLE goods_receipt_items (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), receipt_id uuid NOT NULL REFERENCES goods_receipts(id),
 order_item_id uuid NOT NULL REFERENCES purchase_order_items(id),
 quantity numeric(18,3) NOT NULL CHECK(quantity > 0)
);
```

### V5__pos_cash.sql

```sql
CREATE TABLE cash_registers (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), branch_id uuid NOT NULL REFERENCES branches(id),
 code varchar(30) NOT NULL, name varchar(100) NOT NULL, UNIQUE(branch_id,code)
);
CREATE TABLE cash_sessions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), register_id uuid NOT NULL REFERENCES cash_registers(id),
 opened_by uuid NOT NULL REFERENCES users(id), closed_by uuid REFERENCES users(id),
 opened_at timestamptz NOT NULL DEFAULT now(), closed_at timestamptz,
 opening_amount numeric(18,2) NOT NULL CHECK(opening_amount >= 0),
 counted_amount numeric(18,2), status varchar(20) NOT NULL CHECK(status IN ('OPEN','CLOSED'))
);
CREATE UNIQUE INDEX ux_open_cash_session ON cash_sessions(register_id) WHERE status='OPEN';
CREATE TABLE sales (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), sale_number varchar(40) NOT NULL UNIQUE,
 customer_id uuid REFERENCES customers(id), cash_session_id uuid NOT NULL REFERENCES cash_sessions(id),
 status varchar(30) NOT NULL CHECK(status IN ('DRAFT','CONFIRMED','RETURNED','PARTIALLY_RETURNED','VOIDED')),
 subtotal numeric(18,2) NOT NULL DEFAULT 0, discount_total numeric(18,2) NOT NULL DEFAULT 0,
 tax_total numeric(18,2) NOT NULL DEFAULT 0, grand_total numeric(18,2) NOT NULL DEFAULT 0,
 idempotency_key uuid NOT NULL UNIQUE, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE sale_items (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), sale_id uuid NOT NULL REFERENCES sales(id),
 variant_id uuid REFERENCES product_variants(id), service_order_id uuid,
 description varchar(255) NOT NULL, quantity numeric(18,3) NOT NULL CHECK(quantity > 0),
 unit_price numeric(18,4) NOT NULL CHECK(unit_price >= 0), discount numeric(18,2) NOT NULL DEFAULT 0,
 tax_code varchar(30) NOT NULL, tax_rate numeric(9,5) NOT NULL,
 tax_amount numeric(18,2) NOT NULL, line_total numeric(18,2) NOT NULL
);
CREATE TABLE sale_payments (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), sale_id uuid NOT NULL REFERENCES sales(id),
 method varchar(30) NOT NULL CHECK(method IN ('CASH','TRANSFER','CARD','OTHER','APPLIED_ADVANCE')),
 amount numeric(18,2) NOT NULL CHECK(amount > 0), reference varchar(120), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE cash_movements (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), session_id uuid NOT NULL REFERENCES cash_sessions(id),
 movement_type varchar(30) NOT NULL, amount numeric(18,2) NOT NULL CHECK(amount > 0),
 source_type varchar(50), source_id uuid, actor_id uuid REFERENCES users(id),
 created_at timestamptz NOT NULL DEFAULT now()
);
```

### V6__services.sql

```sql
CREATE TABLE service_orders (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), order_number varchar(40) NOT NULL UNIQUE,
 customer_id uuid NOT NULL REFERENCES customers(id), vehicle_id uuid REFERENCES customer_vehicles(id),
 service_type varchar(40) NOT NULL CHECK(service_type IN ('STICKER','INSTALLATION','OTHER')),
 description text, status varchar(30) NOT NULL,
 assigned_user_id uuid REFERENCES users(id), estimated_total numeric(18,2) NOT NULL DEFAULT 0,
 scheduled_at timestamptz, completed_at timestamptz,
 created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE sale_items ADD CONSTRAINT fk_sale_item_service_order FOREIGN KEY(service_order_id) REFERENCES service_orders(id);
CREATE TABLE service_order_items (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), service_order_id uuid NOT NULL REFERENCES service_orders(id),
 description varchar(255) NOT NULL, quantity numeric(18,3) NOT NULL CHECK(quantity > 0),
 unit_price numeric(18,2) NOT NULL CHECK(unit_price >= 0)
);
CREATE TABLE service_materials (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), service_order_id uuid NOT NULL REFERENCES service_orders(id),
 variant_id uuid NOT NULL REFERENCES product_variants(id),
 planned_quantity numeric(18,3) NOT NULL DEFAULT 0, consumed_quantity numeric(18,3) NOT NULL DEFAULT 0,
 CHECK(planned_quantity >= 0 AND consumed_quantity >= 0)
);
CREATE TABLE service_attachments (
 service_order_id uuid NOT NULL REFERENCES service_orders(id), media_id uuid NOT NULL REFERENCES media_files(id),
 PRIMARY KEY(service_order_id,media_id)
);
CREATE TABLE service_status_history (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), service_order_id uuid NOT NULL REFERENCES service_orders(id),
 old_status varchar(30), new_status varchar(30) NOT NULL, actor_id uuid REFERENCES users(id),
 changed_at timestamptz NOT NULL DEFAULT now(), notes text
);
CREATE TABLE service_advances (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), service_order_id uuid NOT NULL REFERENCES service_orders(id),
 cash_session_id uuid NOT NULL REFERENCES cash_sessions(id),
 amount numeric(18,2) NOT NULL CHECK(amount > 0),
 applied_amount numeric(18,2) NOT NULL DEFAULT 0,
 CHECK(applied_amount >= 0 AND applied_amount <= amount),
 created_at timestamptz NOT NULL DEFAULT now()
);
```

### V7__invoicing.sql

```sql
CREATE TABLE issuers (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), branch_id uuid NOT NULL REFERENCES branches(id),
 tax_id varchar(13) NOT NULL, legal_name varchar(255) NOT NULL,
 trade_name varchar(255), establishment_code varchar(3) NOT NULL,
 emission_point_code varchar(3) NOT NULL, environment varchar(12) NOT NULL,
 active boolean NOT NULL DEFAULT true
);
CREATE TABLE document_sequences (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), issuer_id uuid NOT NULL REFERENCES issuers(id),
 document_type varchar(10) NOT NULL, next_number bigint NOT NULL CHECK(next_number > 0),
 UNIQUE(issuer_id,document_type)
);
CREATE TABLE electronic_documents (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), issuer_id uuid NOT NULL REFERENCES issuers(id),
 sale_id uuid REFERENCES sales(id), document_type varchar(10) NOT NULL,
 sequence_number bigint NOT NULL, access_key varchar(49),
 status varchar(30) NOT NULL, authorization_number varchar(100),
 authorized_at timestamptz, last_error text, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(issuer_id,document_type,sequence_number), UNIQUE(access_key)
);
CREATE TABLE electronic_document_events (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), document_id uuid NOT NULL REFERENCES electronic_documents(id),
 stage varchar(40) NOT NULL, status varchar(40) NOT NULL,
 response_code varchar(100), response_payload text,
 occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE electronic_document_files (
 document_id uuid NOT NULL REFERENCES electronic_documents(id),
 media_id uuid NOT NULL REFERENCES media_files(id), file_type varchar(20) NOT NULL,
 PRIMARY KEY(document_id,file_type)
);
CREATE TABLE outbox_events (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), event_type varchar(80) NOT NULL,
 aggregate_id uuid NOT NULL, payload jsonb NOT NULL,
 status varchar(20) NOT NULL DEFAULT 'PENDING', attempts integer NOT NULL DEFAULT 0,
 next_attempt_at timestamptz NOT NULL DEFAULT now(), locked_until timestamptz,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_outbox_due ON outbox_events(next_attempt_at) WHERE status='PENDING';
```

**SRI:** generar XML según fichas técnicas vigentes, validar contra XSD, firmar con certificado autorizado, usar endpoints de recepción y autorización del ambiente correspondiente, registrar mensajes y autorizaciones, producir RIDE PDF. No asumir autorización por HTTP 200. Las notas de crédito requieren referencia y reglas propias. No incluir secretos del certificado en columnas de texto plano.

### V8__showcase.sql

```sql
CREATE TABLE banners (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), title varchar(180) NOT NULL,
 media_id uuid NOT NULL REFERENCES media_files(id), link_url text,
 starts_at timestamptz, ends_at timestamptz,
 sort_order integer NOT NULL DEFAULT 0, active boolean NOT NULL DEFAULT true
);
CREATE TABLE showcases (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), code varchar(60) NOT NULL UNIQUE,
 title varchar(180) NOT NULL, autoplay_seconds integer NOT NULL DEFAULT 7 CHECK(autoplay_seconds BETWEEN 3 AND 60),
 active boolean NOT NULL DEFAULT true
);
CREATE TABLE showcase_products (
 showcase_id uuid NOT NULL REFERENCES showcases(id), variant_id uuid NOT NULL REFERENCES product_variants(id),
 sort_order integer NOT NULL DEFAULT 0, PRIMARY KEY(showcase_id,variant_id)
);
CREATE TABLE catalog_inquiries (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), variant_id uuid REFERENCES product_variants(id),
 source varchar(30) NOT NULL DEFAULT 'WHATSAPP',
 created_at timestamptz NOT NULL DEFAULT now()
);
```

### V9__indexes_constraints.sql

```sql
CREATE INDEX ix_products_category_active ON products(category_id,active);
CREATE INDEX ix_products_published ON products(published) WHERE active;
CREATE INDEX ix_sales_created_at ON sales(created_at DESC);
CREATE INDEX ix_sales_cash_session ON sales(cash_session_id);
CREATE INDEX ix_service_orders_status ON service_orders(status,scheduled_at);
CREATE INDEX ix_electronic_documents_status ON electronic_documents(status,created_at);
CREATE INDEX ix_cash_movements_session ON cash_movements(session_id,created_at);
CREATE INDEX ix_product_variants_product ON product_variants(product_id);
```

**Extensiones antes de liberar producción:** tablas `sale_returns`, `sale_return_items`, `stock_counts`, `stock_count_items`, `stock_lots`, `supplier_returns`, `purchase_payables`, `payment_allocations`, `tax_rules`, `tax_snapshots`, `print_jobs`, `notification_deliveries`, y `cash_counts`; validación de consistencia de suma de líneas/pagos, política de cierre de caja y modelo de notas de crédito. No se considera esquema cerrado hasta completar esas migraciones y pruebas.

## 4. DTO y contratos REST

### Convenciones

- Prefijo `/api/v1`; JSON `camelCase`; UUID como string; fechas ISO-8601 con zona; importes como decimal en JSON (sin float binario en cálculos internos).
- Paginación: `page`, `size`, `sort`; respuesta `items`, `page`, `size`, `totalElements`.
- Errores: `application/problem+json` con `type`, `title`, `status`, `detail`, `instance`, `traceId`, `fieldErrors`.
- `Authorization: Bearer <token>`; CORS solo orígenes explícitos; validación de DTO con Bean Validation.
- `Idempotency-Key` obligatorio para confirmar venta, registrar cobros y emitir documentos.

### Catálogo

`POST /api/v1/products` → `CreateProductRequest`

```json
{
  "sku": "MEGA-LED-001",
  "name": "Luz LED H4",
  "categoryId": "00000000-0000-0000-0000-000000000001",
  "brandId": null,
  "productType": "PHYSICAL",
  "trackStock": true,
  "published": true,
  "variants": [{"sku":"MEGA-LED-001-6000K","barcode":"7860000000011","attributes":{"temperature":"6000K"},"salePrice":35.00,"minStock":5}]
}
```

`ProductResponse`: `id`, `sku`, `name`, `category`, `brand`, `images[]`, `variants[]`, `published`, `active`, `createdAt`.

### POS

`POST /api/v1/sales` con cabecera `Idempotency-Key: <UUID>`:

```json
{
  "cashSessionId":"00000000-0000-0000-0000-000000000002",
  "customerId":null,
  "items":[{"variantId":"00000000-0000-0000-0000-000000000003","quantity":2,"discountAmount":0}],
  "payments":[{"method":"CASH","amount":70.00}],
  "requestInvoice":true
}
```

El servidor obtiene precio e impuesto vigentes, recalcula importes, verifica disponibilidad y registra el resultado atómicamente. Nunca confía en totales enviados por el navegador. Respuesta `201`: `saleId`, `saleNumber`, `status`, `subtotal`, `taxTotal`, `grandTotal`, `payments[]`, `invoiceStatus`.

### Servicios

`POST /api/v1/service-orders`:

```json
{
 "customerId":"00000000-0000-0000-0000-000000000004",
 "vehicleId":null,
 "serviceType":"STICKER",
 "description":"Sticker de parabrisas 90 x 15 cm",
 "scheduledAt":"2026-10-21T10:00:00-05:00",
 "items":[{"description":"Diseño e impresión de vinilo","quantity":1,"unitPrice":20.00}],
 "materials":[{"variantId":"00000000-0000-0000-0000-000000000005","plannedQuantity":0.5}]
}
```

`ServiceOrderResponse`: `id`, `orderNumber`, `status`, `estimatedTotal`, `paidAdvances`, `remainingBalance`, `assignedUser`, `materials`, `attachments`, `statusHistory`.

### Facturación

`POST /api/v1/electronic-documents` con `saleId`, `documentType=INVOICE`; respuesta `202 Accepted`: `documentId`, `status=PENDING`. `GET /api/v1/electronic-documents/{id}` devuelve estado, eventos, mensajes, autorización y URLs temporales de archivos según permisos. La emisión se realiza en segundo plano.

### Vitrina pública

`GET /api/v1/public/catalog/products?q=led&category=luces&page=0&size=20` devuelve solo productos publicados, precios públicos, fotografías y disponibilidad aproximada. `GET /api/v1/public/showcases/home` devuelve banners, carruseles y productos destacados. Enlaces WhatsApp con mensaje prellenado, sin exponer números privados de clientes.

## 5. Casos de uso y transacciones críticas

### Confirmación de venta

1. Verificar JWT, permiso `sales:create`, caja abierta e idempotencia.
2. Resolver variantes, precios, descuentos autorizados y reglas de impuestos.
3. Bloquear filas `stock_balances` correspondientes (`SELECT ... FOR UPDATE` o actualización condicional).
4. Comprobar stock suficiente; registrar venta, detalles y pagos.
5. Descontar saldo y escribir movimientos inmutables de inventario.
6. Registrar movimiento de caja para componentes monetarios relevantes.
7. Insertar outbox `INVOICE_REQUESTED` si corresponde, en la misma transacción.
8. Commit y devolver venta; worker procesará SRI sin bloquear POS.

No marcar venta como pagada sin conciliar los pagos; distinguir pagos electrónicos pendientes de confirmación. Una caída antes del commit revierte todo; una repetición de solicitud con misma idempotencia devuelve el resultado original.

### Consumo de materiales de servicio

Al registrar consumo real: comprobar orden en estado permitido, bloquear stock, insertar `SERVICE_CONSUMPTION`, incrementar `consumed_quantity`, registrar usuario y fecha. No descontar al planificar materiales. Para devoluciones, usar movimiento compensatorio.

### Secuenciales SRI

Asignar secuencial por emisor, punto de emisión y tipo de documento dentro de transacción con bloqueo. Generar y persistir clave de acceso única. Reintentos reutilizan el mismo documento, XML y clave cuando corresponda. Separar respuesta de recepción de autorización; validar esquemas y normativa vigente.

## 6. Frontend Angular

```text
frontend/apps/admin/src/app/
├── core/{auth,guards,interceptors,layout}/
├── features/
│   ├── dashboard/
│   ├── catalog/{products,categories,brands,images}/
│   ├── inventory/{stock,kardex,adjustments,counts}/
│   ├── purchases/{suppliers,orders,receipts}/
│   ├── pos/{cart,checkout,history}/
│   ├── cash/{opening,closing,movements}/
│   ├── invoicing/{documents,settings,errors}/
│   ├── services/{orders,materials,attachments}/
│   ├── customers/
│   ├── showcase/{banners,featured,preview}/
│   └── reports/
└── app.routes.ts
frontend/apps/storefront/src/app/
├── pages/{home,catalog,product,offers,contact}/
├── components/{hero-carousel,product-card,product-modal,filters,whatsapp-button}/
└── app.routes.ts
```

- Tokens: rojo `#D71920`, negro `#111318`, plata `#AEB4BC`, blanco `#FFFFFF`.
- Componentes `MegaButton`, `MegaInput`, `MegaSelect`, `MegaTable`, `MegaModal`, `ProductCard`, `PriceDisplay`, `StockIndicator`.
- Catálogo con SSR/prerender y metadatos SEO, filtros en URL, lazy loading, imágenes optimizadas, carrusel accesible con pausa en hover/foco y preferencia de movimiento reducido.
- Pantalla completa de presentación con avance automático configurable y control manual.
- POS optimizado para teclado y lector de códigos; diseño responsive, pero evitar dependencias exclusivas de pantalla táctil.

## 7. Impresión

- **A4:** generar RIDE PDF y reportes con JasperReports o renderer PDF; impresión mediante navegador y descarga autorizada.
- **Térmica:** opción inicial de PDF de 80 mm imprimible desde navegador; si se requiere corte de papel, apertura de cajón o ESC/POS directo, implementar agente local seguro y firmado que reciba trabajos autenticados de la aplicación, nunca conexión directa VPS→USB.
- Crear cola `print_jobs` y plantillas por tipo: recibo, apertura/cierre, orden de servicio, etiqueta de producto.
- Reimpresiones auditadas; el comprobante interno no sustituye el documento tributario correspondiente.

## 8. VPS y operación

```text
Internet → Nginx TLS → admin Angular / storefront Angular / Spring API
                                   Spring API → PostgreSQL (red privada)
                                              → almacenamiento privado
                                              → worker SRI
```

- Docker Compose con servicios `nginx`, `api`, `sri-worker` (puede ser el mismo binario con perfil), `postgres`, `backup`.
- No exponer 5432 ni puertos internos; firewall SSH restringido, HTTPS, secretos fuera de Git, usuarios sin privilegios innecesarios.
- Backups diarios cifrados fuera del VPS; probar restauración mensualmente. Definir RPO/RTO y política de retención.
- Health checks, logs estructurados con `traceId`, métricas, alertas por errores de SRI y espacio en disco.
- CI/CD: compilar, test unitario, integración con PostgreSQL, análisis de dependencias, migraciones en staging, despliegue controlado, rollback de aplicación y plan de recuperación de datos.

## 9. Backlog por sprints (2 semanas cada uno)

| Sprint | Historias principales | Entregable / criterio de aceptación |
|---|---|---|
| 0 | Arquitectura, repositorio, Docker, CI, diseño UI, ADR | App y API arrancan, PostgreSQL con Flyway, pruebas en CI |
| 1 | Login, JWT, roles, permisos, auditoría, configuración sucursal | Cajero no accede a funciones administrativas; logs de acciones |
| 2 | Categorías, marcas, productos, variantes, imágenes | CRUD con validaciones, carga segura y fotos principales |
| 3 | Bodegas, stock, kardex, ajustes, conteo | Cada cambio genera movimiento; concurrencia sin stock negativo |
| 4 | Proveedores, compras, recepción, clientes y vehículos | Recepción parcial actualiza stock una sola vez |
| 5 | Apertura de caja, carrito POS, ventas, pagos | Venta atómica, cobros mixtos, idempotencia |
| 6 | Cierre de caja, devoluciones, reportes básicos, impresión 80 mm | Arqueo reproducible y devolución trazable |
| 7 | Emisor, secuenciales, XML, firma, integración SRI pruebas | Comprobantes validados y estados persistidos |
| 8 | Worker SRI, reintentos, autorización, RIDE A4, notas de crédito | Fallo temporal no duplica documentos; RIDE descargable |
| 9 | Órdenes de stickers, instalaciones, materiales, anticipos | Flujo cotización→entrega, consumo y pagos conciliados |
| 10 | Vitrina, búsqueda, precios, carruseles, WhatsApp, SEO | Productos publicados sincronizados y modal accesible |
| 11 | Pruebas E2E, seguridad, backups, monitoreo, capacitación | UAT aprobado, restauración verificada, checklist de producción |

**Duración orientativa:** 24 semanas con un equipo pequeño; ajustar después de estimar historias y dependencias. Los sprints 7–8 dependen de credenciales, certificados y especificaciones del SRI.

### Definición de terminado por historia

- Código revisado y sin secretos.
- Migración reproducible y compatible con datos existentes.
- Validación de permisos y entradas.
- Pruebas unitarias y de integración en verde.
- Manejo de errores, logs y trazabilidad.
- API OpenAPI y DTO documentados.
- Prueba funcional de aceptación ejecutada.

## 10. Riesgos y decisiones abiertas

1. **Normativa SRI:** confirmar ficha técnica, XSD, tipos de comprobante, firma, ambientes, códigos de impuesto, RIDE y reglas de contingencia vigentes antes de producción.
2. **Impresión térmica:** determinar marca/modelo y si basta diálogo de impresión o se requiere ESC/POS nativo.
3. **Costeo:** decidir costo promedio ponderado vs FIFO; el esquema inicial no implementa valoración contable completa.
4. **Tributación:** configurar tarifas y tratamiento por producto/fecha; nunca asumir tasa fija.
5. **Pagos:** definir cómo se verifica transferencia y tarjeta antes de confirmar cobro.
6. **Stickers:** determinar si se almacenan archivos de diseño originales, medidas, área y consumo en metros lineales/cuadrados.
7. **Inventario inicial:** preparar importación, conteo físico y corte de apertura.
8. **Protección de datos:** política de retención, consentimiento, acceso a archivos y datos personales.
9. **Manejo de devoluciones:** formalizar reglas comerciales y tributarias con notas de crédito y movimientos compensatorios.
10. **Continuidad operativa:** el POS web requiere conectividad con VPS; modo offline no está incluido en esta primera versión.

## 11. Primeras tareas del agente de desarrollo

1. Crear monorepo y convenciones de nombres, linter, formateador, Docker Compose local y CI.
2. Crear backend con paquetes modulares, configuración por entorno, seguridad y OpenAPI.
3. Implementar migraciones V1–V3 y pruebas Testcontainers; revisar los índices de idempotencia por línea antes de cerrar V3.
4. Crear Angular admin y storefront con tokens de diseño y librería de componentes.
5. Implementar CRUD de categorías, marcas, productos, variantes y fotografías.
6. Implementar servicio transaccional de stock y pruebas concurrentes.
7. Continuar según backlog, sin desarrollar facturación real hasta validar documentación SRI vigente.

**Nota:** los scripts aquí incluidos son una base física de arranque, no el esquema final de producción. Las tablas adicionales indicadas, reglas tributarias y pruebas de integridad son parte obligatoria del backlog antes del lanzamiento.

---


## F. Convenciones y estándares de implementación

- Arquitectura de backend **package-by-feature**: `api/controller`, `application/service`, `domain`, `infrastructure/persistence`, `dto`, `mapper`, `validation` por módulo. Dependencias explícitas entre dominios; evitar referencias circulares.
- Entidades JPA nunca expuestas directamente como JSON; DTO request/response, Bean Validation y mappers.
- Respuestas de error uniformes tipo Problem Details con `code`, `message`, `details`, `traceId` y status HTTP.
- Versionado `/api/v1`; paginación con límites, filtros validados, ordenación por lista permitida.
- Contratos OpenAPI generados y revisados; tests de integración con PostgreSQL real mediante Testcontainers.
- Frontend Angular con rutas lazy, guards de UX (sin sustituir permisos backend), interceptores HTTP, formularios reactivos, estado local por feature y servicios API tipados.
- Archivos `.env.example` sin secretos; variables reales mediante secretos de entorno/volúmenes protegidos.
- Docker Compose: `nginx`, `api`, `postgres`, `worker` (si proceso separado), backup job, almacenamiento persistente; DB no expuesta públicamente.
- Observabilidad: logs JSON, IDs de correlación, healthchecks, métricas, alertas por fallos SRI y backups.
- CI: lint, build, unit tests, integration tests, análisis de dependencias, build de imágenes, migración de staging y smoke tests.
- Documentar instalación, despliegue, rollback, recuperación, configuración de impresora y rotación de firma electrónica.

## G. Contratos DTO de referencia adicionales

```typescript
interface ProductPublicDto {
  id: string;
  slug: string;
  sku: string;
  name: string;
  category: { id: string; name: string };
  price: string; // decimal como string, USD
  currency: 'USD';
  availability: 'AVAILABLE' | 'LOW_STOCK' | 'OUT_OF_STOCK' | 'ON_REQUEST';
  images: Array<{ url: string; alt: string; primary: boolean }>;
  attributes: Record<string, string>;
  whatsappUrl: string;
}
interface CreateServiceOrderRequest {
  customerId: string;
  vehicleId?: string;
  serviceType: 'STICKER' | 'INSTALLATION' | 'OTHER';
  description: string;
  scheduledAt?: string;
  materials: Array<{ variantId: string; estimatedQuantity: string }>;
  laborAmount: string;
  estimatedTotal: string;
  attachmentIds: string[];
}
interface ConfirmSaleRequest {
  cashSessionId: string;
  customerId?: string;
  items: Array<{ variantId?: string; serviceOrderId?: string; quantity: string; unitPrice: string; discount: string }>;
  payments: Array<{ method: 'CASH'|'TRANSFER'|'CARD'; amount: string; reference?: string }>;
  idempotencyKey: string;
}
```

**Nota:** los DTO finales deben tener validaciones de precio, autorización de descuentos y cálculo monetario **en servidor**; nunca confiar en totales enviados por el navegador.

## H. Backlog operativo y criterios de aceptación

| Sprint | Historias principales | Entregable de aceptación |
|---|---|---|
| 0 | Repositorio, Docker, CI, convenciones, wireframes, tokens | Proyecto inicia localmente; healthcheck y build pasan |
| 1 | Auth, roles, permisos, auditoría, configuración | Roles bloquean endpoints sin permiso |
| 2 | Categorías, marcas, productos, variantes, imágenes | CRUD, validaciones y subida segura probados |
| 3 | Stock, kardex, ajustes, conteos, concurrencia | No se vende más stock del disponible |
| 4 | Proveedores, compras, recepciones | Recepción parcial aumenta stock una vez |
| 5 | Clientes, vehículos, POS y pagos | Venta atómica y reintentos idempotentes |
| 6 | Caja, cierres, tickets, devoluciones | Arqueo consistente y ticket reproducible |
| 7 | XML, secuenciales, firma, adaptador SRI | Comprobantes de prueba firmados y validados |
| 8 | Autorización, reintentos, RIDE, notas crédito | Incidencias recuperables y trazables |
| 9 | Órdenes stickers/instalaciones, materiales | Anticipos y consumos conciliados |
| 10 | Vitrina, SEO, filtros, WhatsApp | Catálogo público con precios correctos |
| 11 | Carrusel/modal/TV, reportes, promociones | Presentación accesible y configurable |
| 12 | Seguridad, carga, backup, despliegue, UAT | Checklist de producción aprobado |

**Definición de terminado:** código revisado, migración reversible mediante procedimiento documentado (Flyway no garantiza rollback automático), pruebas unitarias/integración, autorización, manejo de errores, accesibilidad, OpenAPI, documentación, evidencia de aceptación y ausencia de secretos en Git.

### Escenarios E2E obligatorios

1. Crear producto con dos variantes y fotos; publicar solo una y verificar vitrina.
2. Recibir 10 unidades, vender 3 y confirmar saldo 7 con kardex correcto.
3. Dos cajeros compiten por la última unidad; una operación se rechaza.
4. Reintentar POST de venta por timeout; mismo resultado sin segunda venta.
5. Cerrar caja con efectivo y transferencia, verificar diferencia.
6. Crear orden de sticker, subir diseño, registrar anticipo, consumir vinilo, cobrar saldo y entregar.
7. Emitir factura, simular caída SRI, reconsultar y autorizar sin nueva emisión duplicada.
8. Abrir vitrina en móvil, buscar por categoría/precio, navegar carrusel, modal y WhatsApp.
9. Imprimir ticket en equipo configurado y PDF A4 con datos completos.
10. Restaurar backup en staging y verificar consistencia.

## I. Guía para el agente desarrollador

1. Leer este documento completo y el plan técnico anexo antes de generar código.
2. Mantener el alcance v1; no añadir ecommerce ni microservicios sin autorización.
3. Antes de cada sprint, enumerar archivos, migraciones, endpoints, pantallas y pruebas a crear.
4. Implementar verticalmente: esquema → dominio → servicio → DTO/API → frontend → pruebas → documentación.
5. Nunca cambiar tablas ya desplegadas editando migraciones ejecutadas; crear nueva migración Flyway.
6. No introducir valores tributarios o esquemas SRI asumidos; consultar especificaciones oficiales vigentes y confirmar requisitos antes de habilitar producción.
7. No almacenar firma, contraseña, JWT secret ni credenciales de DB en repositorio.
8. Aplicar identidad visual MEGALUJOS en todos los módulos, incluyendo estados de error y carga.
9. Al finalizar cada sprint entregar resumen, comandos de ejecución, pruebas realizadas, decisiones y pendientes.
10. Ante ambigüedades tributarias, operativas o de hardware, registrar decisión abierta y solicitar confirmación; no inventar comportamiento crítico.

## J. Decisiones abiertas previas al despliegue

- RUC y datos fiscales del emisor, régimen, establecimientos/puntos de emisión y certificado válido.
- Políticas comerciales: descuentos máximos, devoluciones, stock negativo, precios por cliente y garantías.
- Inventario inicial: catálogo, fotos, códigos de barras, costos, existencias y proveedores.
- Número WhatsApp, dominio/subdominios, logotipo definitivo, textos legales y política de privacidad.
- Hardware: modelo de impresora térmica, tamaño 58/80 mm, conexión USB/LAN, escáner y lector.
- Método de envío de email, retención de XML/RIDE y resguardo de certificados.
- Recursos VPS, frecuencia de backup, RPO/RTO, estrategia de monitoreo y restauración.
- Política de visibilidad de stock agotado y actualización de precios en vitrina.

---

**Fin del documento maestro MEGALUJOS v3.0.**
