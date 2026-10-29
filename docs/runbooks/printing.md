# Runbook — Impresión térmica 80 mm

Estado actual: **impresión mediante el navegador** (PDF de 80 mm generado por la
aplicación). El agente local ESC/POS está **pendiente** (ver §4).

## 1. Configuración de la impresora (vía navegador)

Requisitos: impresora térmica instalada en el equipo del mostrador (USB o LAN),
ancho de papel **80 mm** (típico: rollo 80 × 80 mm; también sirve 58 mm con
plantilla ajustada).

1. Instalar el driver del fabricante e imprimir una página de prueba desde el
   SO.
2. Imprimir desde la app (botón *Imprimir ticket*) y, en el diálogo del
   navegador:
   - **Destino:** la impresora térmica (no "Guardar como PDF").
   - **Tamaño del papel:** `80 × 200 mm` o `Personalizado → 80 mm` (Chrome/Edge:
     *Más ajustes → Tamaño personalizado*).
   - **Margen:** `Ninguno` / `Sin márgenes`.
   - **Escala:** `Predeterminada` o `100 %` (nunca "Ajustar a la página").
   - Desactivar encabezados y pies de página.
3. Configuración recomendada del navegador:
   - Chrome/Edge: `edge://settings/printing` o `chrome://settings` →
     *Impresión predeterminada* → tamaño papel 80 mm, márgenes ninguno.
   - Permitir ventanas emergentes del dominio del sistema (si no, el diálogo de
     impresión puede bloquearse).
   - Activar "Imprimir sin cabecera y pie" por defecto.
4. Verificar: el ticket debe salir con el logotipo, RUC, secuencial y QR del
   comprobante, sin escalado ni cabeceras del navegador.

Ajustes del sistema en la app: `PRINT_THERMAL_WIDTH_MM` (default 80) y
`PRINT_DEFAULT_PRINTER` (nombre del equipo, si se usa impresión sin diálogo).

## 2. Cola de trabajos (`print_jobs`)

- Cada solicitud crea un registro en `print_jobs` con tipo de plantilla
  (`RECEIPT`, `CASH_OPEN_CLOSE`, `SERVICE_ORDER`, `PRODUCT_LABEL`).
- Endpoints: `POST /api/v1/print-jobs`, `GET /api/v1/print-jobs`.
- Las reimpresiones quedan auditadas (`AuditService`); el comprobante interno
  **no sustituye** el documento tributario (RIDE/factura) correspondiente.
- Permiso requerido: `print:manage`.

## 3. Problemas frecuentes

| Síntoma | Causa probable | Acción |
|---|---|---|
| Sale en A4 o con márgenes | tamaño/papel mal configurado | repetir §1 (papel 80 mm, márgenes ninguno) |
| Texto muy pequeño/grande | escala automática | escala 100 % |
| No aparece la impresora | driver o equipo incorrecto | instalar driver, imprimir prueba desde el SO |
| Botón de impresión no hace nada | popup bloqueado | permitir emergentes del dominio |
| Corte incorrecto | plantilla demasiado larga | revisar plantilla y `thermal-width-mm` |
| Cortina/achicado al cortar | driver "corte parcial" | desactivar cortes parciales en el driver |

## 4. Agente local ESC/POS (pendiente)

Pendiente de decisión (documento maestro §7 y §10):

- ¿Basta el diálogo de impresión del navegador (PDF 80 mm) o se requiere
  ESC/POS nativo para **corte de papel, apertura de cajón o cola directa**?
- Si se requiere: implementar un **agente local seguro y firmado** en el equipo
  del mostrador que reciba trabajos autenticados desde la aplicación
  (`print_jobs`), con TLS/mutual auth y whitelist de equipo.
- **Prohibido** cualquier enlace directo VPS → USB de impresora.
- Decisiones abiertas: marca/modelo de impresora, 58 vs 80 mm, conexión
  USB/LAN, soporte de cajón.

## 5. Checklist de alta de un equipo de mostrador

- [ ] Driver instalado y página de prueba correcta
- [ ] Papel 80 mm, márgenes "ninguno", escala 100 %
- [ ] Ticket de venta de prueba (incl. QR y secuencial)
- [ ] Apertura/cierre de caja impresos
- [ ] Orden de servicio / etiqueta de producto (si aplica)
- [ ] Reimpresión auditada visible en `GET /api/v1/print-jobs`
