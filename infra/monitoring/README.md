# Monitoreo — MEGALUJOS

Guía de observabilidad. **No se implementa aquí un stack completo**: la opción
mínima recomendada es Dozzle (logs en vivo) y, si se necesita métricas y
dashboards, sumar Prometheus + Grafana.

## 1. Nivel mínimo (recomendado para el VPS actual): logs con Dozzle

`docker-compose.example.yml` en este directorio levanta Dozzle sobre el socket
de Docker, sin agentes ni modificaciones en las apps:

```bash
docker compose -f infra/monitoring/docker-compose.example.yml up -d
# UI: http://<VPS>:8081  (proteger con reverse proxy + Basic Auth en producción)
```

## 2. Healthchecks ya disponibles (no duplicar)

| Servicio | Healthcheck | Señal |
|---|---|---|
| `postgres` | `pg_isready -U … -d …` | BD inaccesible / migraciones pendientes |
| `backend` | `curl http://localhost:8080/actuator/health` | API caída o sin conexión a BD |
| `admin` / `storefront` | `wget http://localhost:80/` | Build o nginx del SPA caído |
| `nginx` | sin healthcheck propio (usar `wget` a :80 desde el host) | Entrada pública caída |

Estado:

```bash
docker compose -f infra/compose.yaml --env-file .env ps
curl -fsS http://localhost/actuator/health    # vía nginx
```

> `management.endpoints.web.exposure.include` deja solo `health` por defecto.
> En nginx solo se enrutan `/api/`, `/uploads/`, Swagger y `/actuator/health`
> (para monitores externos); el resto de `/actuator/*` responde 404 desde el
> proxy, así que no hay exponer nada más.

## 3. Estructura sugerida (crecer hacia ahí)

```
infra/monitoring/
├── README.md                    # este documento
├── docker-compose.example.yml   # Dozzle (+ bloques comentados Prometheus/Grafana)
├── prometheus/
│   ├── prometheus.yml           # scrape de backend (si se expone /actuator/prometheus)
│   └── alerts.yml               # reglas de alerta
└── grafana/
    ├── provisioning/datasources/
    └── dashboards/              # JVM, HTTP, BD, disco
```

Stack típico: `prometheus` + `grafana` + `alertmanager` (+ `dozzle` para logs).
Requiere añadir `spring-boot-starter-actuator` con
`management.endpoints.web.exposure.include: health,prometheus` y **restringir
`/actuator/prometheus`** a la red interna (no publicarlo en nginx).

## 4. Alertas mínimas (antes de producción)

| Alerta | Señal | Umbral sugerido | Acción |
|---|---|---|---|
| API caída | `backend` unhealthy > 2 min | 2 min | `docs/runbooks/incidents.md` |
| **Errores SRI** | logs `backend` con `SRI` + `ERROR`/`RECHAZADO` | > 5 en 10 min | Revisar ambiente (1/2), certificado y secuenciales |
| Documentos sin autorizar | `electronic_documents` en estado pendiente/rechazado | > 0 durante 30 min | Reintentar con `POST /api/v1/electronic-documents/{id}/retry` |
| **Espacio en disco** | `df` del VPS | < 15% libre o < 5 GB | Ampliar volumen, subir `RETENTION_DAYS`, vaciar Docker (`docker system prune`) |
| Disco del volumen `pgdata` | uso del contenedor postgres | > 80% | Vigilar tablas grandes / vacuum |
| Backups fallidos | log de `megalujos-backup` sin copia en 26 h | 26 h | Revisar passphrase y credenciales |
| Certificado SRI por vencer | fecha del `.p12` | < 30 días | `docs/runbooks/sri-rotation.md` |

Implementación sin Prometheus: un cron en el host que envíe por correo/WhatsApp
(como el `Healthcheck` de UptimeRobot sobre `https://<dominio>/actuator/health`
desde fuera) cubre la caída total; el resto se cubre con Dozzle + revisión
diaria.

## 5. Logs estructurados

- Los logs del backend van a stdout → visibles con
  `docker compose logs -f backend` o Dozzle.
- Correlación: incluir `traceId` en los Problem Details (sección F del
  documento maestro) para rastrear una petición de extremo a extremo.
- Rotación: Docker aplica `json-file` con `max-size`/`max-file` en
  `/etc/docker/daemon.json`; configurarlo para no llenar el disco:

```json
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "50m", "max-file": "5" }
}
```
