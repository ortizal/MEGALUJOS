# ADR-002: Facturación electrónica propia frente a librería/tercero externo

- **Estado:** Aceptado, con validación pendiente de la ficha técnica SRI vigente
- **Fecha:** 2026
- **Documentos de referencia:** `MEGALUJOS_DOCUMENTO_MAESTRO.md` §7, §10 (riesgo 1),
  §9 sprints 7–8, `docs/CONVENTIONS.md`

## Contexto

El negocio debe emitir comprobantes electrónicos válidos ante el **SRI**
(Servicio de Rentas Internas de Ecuador): XML firmado con certificado PKCS#12,
clave de acceso con dígito de ambiente, recepción offline y consulta de
autorización, RIDE PDF, notas de crédito, secuenciales por establecimiento y
punto de emisión, y manejo de rechazos/contingencias.

Opciones consideradas:

1. **Implementación propia** dentro del módulo `invoicing` (constructor XML,
   firma, adaptador de recepción/autorización, worker de reintentos).
2. **Librería o proveedor externo** (SDK/tercero que firme y autorice por la
   aplicación, o servicio de facturación en la nube).
3. Híbrido: implementación propia delegando solo la firma.

## Decisión

Se implementa la **facturación electrónica de forma propia** en el backend,
dentro del módulo `invoicing` (`sri/SriXmlBuilder`, `AccessKeyGenerator`,
`SriSignatureService`, `SriOutboxWorker`), con estas restricciones:

- **Ningún valor tributario se asume ni se hardcodea**: impuestos provienen de
  `TaxService` y de la tabla de impuestos; IVA, tarifas y tratamientos se
  configuran, nunca se fijan en código (CONVENTIONS §5, documento maestro §10.3).
- El **ambiente** (pruebas/producción) es configurable (`SRI_ENVIRONMENT`,
  `issuer.environment`) y el certificado/contraseña se inyectan por entorno
  (`SRI_CERTIFICATE_PATH`, `SRI_CERTIFICATE_PASSWORD`) — nunca en el repo.
- Emisión **idempotente** y en cola: `Idempotency-Key`, persistencia de estados
  del comprobante y worker con reintentos, de modo que una caída del SRI no
  duplique documentos (escenario E2E obligatorio 7).
- Los **plazos tributarios y contingencias** se confirman contra la
  documentación oficial; hasta esa validación la facturación en producción
  permanece deshabilitada (`SRI_ENABLED=false` por defecto).

**Validación pendiente (obligatoria antes de producción):** contrastar esta
decisión con la **ficha técnica y especificaciones vigentes del SRI** (XSDs,
tipos de comprobante, estructura del RIDE, reglas de autorización y
contingencia, códigos de impuesto, formatos de nota de crédito). Si la
documentación oficial impone un mecanismo que la implementación propia no
cubre (p. ej. firma o servicio obligatorio de terceros), esta decisión se
revisa y se actualiza este ADR con el nuevo estado.

## Consecuencias

**Positivas**

- Control total del ciclo de vida: secuenciales, estados, reintentos, notas de
  crédito y auditoría integrados con `AuditService` y con el resto del
  monolito (ventas, caja, inventario) sin integraciones externas.
- Sin costos recurrentes ni dependencia de la disponibilidad de un tercero
  para emitir; la única dependencia externa es el propio SRI.
- Trazabilidad extremo a extremo (venta → XML → firma → recepción →
  autorización → RIDE) dentro de una misma BD.

**Negativas / riesgos**

- Mantenimiento normativo asumido por el equipo: cambios del SRI obligan a
  actualizar código y migraciones con plazos cortos.
- Riesgo de interpretación errónea de la especificación → mitigado con la
  validación obligatoria de la ficha técnica, pruebas contra ambiente de
  pruebas y revisión contable antes de producción.
- La firma exige custodia del `.p12` y su contraseña en el VPS (ver
  `docs/runbooks/sri-rotation.md`): si se pierde, no se puede firmar.
- Sin certificación previa de un tercero, la validación de "correcto ante el
  SRI" se obtiene únicamente con las pruebas del ambiente de certificación.

**Neutrales**

- Se mantiene abierta la opción de delegar la firma en un proveedor si el SRI
  lo exige o si el costo de mantenimiento crece; el módulo queda aislado
  (`invoicing.sri`) para sustituir el adaptador sin tocar el resto.
