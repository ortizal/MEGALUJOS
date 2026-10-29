# ADR-003: Inventario por movimientos inmutables (kardex) frente a edición directa de stock

- **Estado:** Aceptado
- **Fecha:** 2026
- **Documentos de referencia:** `MEGALUJOS_DOCUMENTO_MAESTRO.md` §9 (sprint 3),
  escenarios E2E 2 y 3, `docs/CONVENTIONS.md` §5

## Contexto

El inventario es crítico para el negocio: cada venta, recepción de compra,
ajuste, devolución, consumo en orden de servicio o conteo físico modifica las
existencias. Se plantean dos modelos:

1. **Edición directa de stock**: una columna `stock` en el producto/variante
   que se incrementa o decrementa según la operación; el historial sería
   opcional o de registro suelto.
2. **Kardex por movimientos inmutables**: la existencia se calcula y se
   actualiza exclusivamente mediante registros inmutables en
   `stock_movements` (tipo, cantidad, saldo resultante, referencia a la
   operación de origen, actor), con `stock_balances` como proyección
   actualizada.

Exigencias del negocio: no vender más de lo disponible (incluyendo usuarios
concurrentes), poder explicar cualquier diferencia detectada en un conteo
físico, y trazar devoluciones y consumos de materiales en órdenes de trabajo.

## Decisión

Se adopta el **kardex inmutable**:

- Todo cambio de stock DEBE pasar por `inventory.application.StockService`
  (`addStock`, `deductStock`, `getAvailableStock`, `getMainWarehouseId`), que
  aplica **bloqueo pesimista** y escribe el `stock_movement` correspondiente en
  la misma transacción.
- Los movimientos son **inmutables**: no se editan ni se borran. Un error se
  corrige con un movimiento compensatorio (ajuste aprobado, devolución), nunca
  reescribiendo el histórico.
- `stock_balances` se mantiene como proyección actualizable, coherente siempre
  con la suma de movimientos (comprobable con una consulta de control).
- Ajustes por diferencia de conteo requieren aprobación
  (`POST /api/v1/inventory/adjustments/{id}/approve`); todo queda auditado con
  `AuditService` (`inventory:adjust` / `inventory:count`).
- Lecturas de solo consulta de otros módulos usan el servicio de stock, no
  tablas ajenas (CONVENTIONS §5).

## Consecuencias

**Positivas**

- Trazabilidad completa: cada unidad puede explicarse (venta S-102, recepción
  de OC-7, ajuste de conteo A-3), habilitando kardex paginado
  (`GET /api/v1/inventory/movements`) y detección de faltantes/sobrantes.
- Concurrencia segura: el bloqueo pesimista en `StockService` impide vender la
  última unidad dos veces (E2E obligatorio 3) y evita saldos negativos.
- Correcciones auditables: nunca se "arregla" un número a mano; hay movimiento,
  autor, fecha y motivo.
- Reconciliación posible: `SUM(movements) == balance` permite detectar
  corrupción de datos o escrituras fuera del servicio.

**Negativas / riesgos**

- Mayor volumen de tablas (crecimiento lineal con las operaciones) → requiere
  índices y poda/archivado a largo plazo (fuera del alcance v1, decidir cuando
  el volumen lo justifique).
- Más complejidad de implementación y pruebas (transacciones, locking,
  concurrencia) frente a un simple contador.
- Rendimiento de escritura ligeramente mayor (dos escrituras por operación);
  irrelevante a la escala de un VPS de negocio, pero a vigilar en cierres
  masivos.

**Neutrales**

- La valoración contable (costo promedio ponderado / FIFO) queda fuera del
  esquema v1 (documento maestro §10.3): el kardex registra cantidades y
  referencias; añadir costos por movimiento es compatible con esta decisión
  sin romper el histórico.
