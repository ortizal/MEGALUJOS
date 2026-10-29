# ADR-001: Monolito modular Spring Boot frente a microservicios

- **Estado:** Aceptado
- **Fecha:** 2026
- **Contexto relacionado:** `MEGALUJOS_DOCUMENTO_MAESTRO.md` §F, §8, §11

## Contexto

MEGALUJOS es un sistema de gestión para un negocio (o red pequeña) de
automoción y rotulación: catálogo, inventario con kardex, POS con cajas,
servicios/órdenes, facturación electrónica SRI, vitrina pública y reportes.
El equipo es pequeño y el alcance v1 está fuertemente acotado (24 semanas,
sprints de 2 semanas). Existen requisitos transversales claros: transacciones
consistentes entre ventas/stock/caja, un solo despliegue en un VPS con recursos
limitados, y una base de datos PostgreSQL única.

Se evaluaron dos alternativas:

1. **Monolito modular** (una aplicación Spring Boot con paquetes por
   feature: `api`, `application`, `domain`, `dto` por módulo).
2. **Microservicios** (inventario, POS, facturación, vitrina… como servicios
   desplegados de forma independiente).

## Decisión

Se opta por un **monolito modular** en Spring Boot 3.4 / Java 21, organizado
*package-by-feature* con reglas estrictas de dependencia entre módulos:

- Cada módulo expone una fachada propia: `api → application` propio y
  `application → application` de otros módulos **solo vía servicios**
  (p. ej. `StockService`, `AuditService`, `TaxService`), nunca accediendo a
  repositories ajenos si existe un servicio que lo haga.
- Entidades JPA nunca expuestas en JSON (DTOs + validación + mappers).
- Comunicación interna mediante llamadas a métodos con `@Transactional` en una
  sola unidad de persistencia (ACID real entre ventas, stock y caja).
- Despliegue único (`backend/Dockerfile` → una imagen) orquestado con
  `infra/compose.yaml` junto a nginx, frontend, postgres y backup.
- La separación a servicios queda abierta para el futuro: los límites por
  feature ya están marcados y pueden extraerse (p. ej. `sri-worker`, vitrina)
  si aparece una razón operativa real.

## Consecuencias

**Positivas**

- Transacciones de negocio complejas (venta + stock + pago + auditoría) en un
  solo `@Transactional`, sin sagas ni compensaciones.
- Un solo build, una sola imagen, un solo proceso que supervisar: operación
  simple en el VPS, backups y restauración directos sobre una BD.
- Menor superficie de red/seguridad: la red interna solo necesita alcanzar
  `backend:8080` y `postgres:5432`.
- Desarrollo sin dependencias entre equipos/repositorios; refactor de
  módulos sin contratos de red ni versionado de APIs internas.
- CI simple y rápido (un job de Maven, dos de Angular, build de imágenes).

**Negativas / riesgos**

- Un fallo de torno (OOM, *thread pool* saturado) afecta a toda la
  aplicación; se mitiga con healthchecks, límites de Hikari (pool 10) y
  timeouts en nginx (60 s).
- El despliegue es atómico para todo: no se puede publicar solo facturación;
  se mitiga con migraciones *expand/contract* (ADR de despliegue: runbook).
- Escalado vertical único: más tráfico ⇒ más recursos del VPS o extraer
  procesos largos (worker SRI) a contenedor propio, ya previsto en el
  documento maestro §8 (perfil `sri-worker`).
- Riesgo de acoplamiento si no se respetan las reglas de dependencia entre
  módulos → se revisa en *code review* según `docs/CONVENTIONS.md`.

**Neutrales**

- La observabilidad (logs con `traceId`, métricas) es más simple que en
  microservicios, pero exige disciplina de trazabilidad desde el inicio.
