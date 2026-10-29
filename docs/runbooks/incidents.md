# Runbook — Incidencias operativas

Guía de primera respuesta. Escalar al responsable del proyecto si en 30 minutos
no hay mitigación.

## 0. Diagnóstico inicial (siempre)

```bash
cd /opt/megalujos
docker compose -f infra/compose.yaml --env-file .env ps
curl -fsS http://localhost/actuator/health
docker compose -f infra/compose.yaml --env-file .env logs --since 15m backend nginx postgres
df -h && docker system df
```

Señales: servicio `unhealthy`/`restarting`, `df` al límite, errores repetidos en
logs. Monitoreo: `infra/monitoring/README.md`.

---

## A. El SRI está caído o responde mal

**Síntomas:** documentos en `PENDING`/`SENT` sin autorizar, errores HTTP 5xx
del SRI en logs, alertas de errores SRI.

1. Confirmar que el servicio del SRI está fuera (probar desde otra red):
   recepción y autorización de comprobantes de prueba.
2. **No reintentar a mano en bucle**: el worker reintenta con backoff
   (`SRI_WORKER_MAX_ATTEMPTS`, default 5) y la cola es idempotente.
3. Verificar que la emisión sigue funcionando en firma:
   `GET /api/v1/electronic-documents` → los nuevos documentos deben quedar en
   `SIGNED`/`SENT`, no en `ERROR` de firma.
4. Comunicar a caja/ventas: **la venta no se detiene**; los comprobantes quedan
   en cola y se autorizan al volver el SRI.
5. Cuando el SRI vuelva, verificar el drenaje:
   `GET /api/v1/reports/pending-documents` debe bajar a 0.
6. Si algún documento quedó en error terminal:
   `POST /api/v1/electronic-documents/{id}/retry`.
7. Si hubo duplicados o rechazos por secuencial: revisar secuenciales del
   emisor y **no** reemitir sin verificar; usar nota de crédito si corresponde.

**Regla:** nunca duplicar un comprobante ya enviado; la idempotencia
(`Idempotency-Key`) y la cola del worker evitan reemisiones.

---

## B. La base de datos / el disco se está llenando

**Síntomas:** `no space left on device`, backend unhealthy, `pgdata` al 90%+,
logs Docker creciendo.

1. Identificar qué consume:

```bash
df -h
du -sh /var/lib/docker/volumes/megalujos_* 2>/dev/null
docker system df
docker compose -f infra/compose.yaml --env-file .env exec postgres \
  psql -U megalujos_user -d megalujos_db -c \
  "SELECT pg_size_pretty(pg_database_size('megalujos_db'));"
```

2. Liberar de forma segura (orden recomendado):

```bash
# a) Logs de contenedores
docker system prune -af                 # NO usar --volumes: borraría pgdata/backups
# b) Copias antiguas: reducir RETENTION_DAYS en .env y reiniciar backup
docker compose -f infra/compose.yaml --env-file .env restart backup
# c) Verificar que los volúmenes siguen intactos
docker volume ls                        # pgdata, uploads_data, backups
```

3. Si `pgdata` creció por tablas (no por disco):
   `VACUUM (VERBOSE, ANALYZE)` / revisar la tabla que más ocupa
   (`pg_total_relation_size`) — requiere DBA; no borrar datos sin plan.
4. Si el disco está **completamente** lleno, el backend y postgres pueden
   entrar en bucle de error: liberar espacio primero, luego
   `docker compose ... restart postgres backend`.
5. Prevención: alerta de disco < 15% libre, `RETENTION_DAYS` acotado, rotación
   de logs Docker (`max-size: 50m`, `max-file: 5`).

**RPO:** si el disco se llena y se corrompe la BD, restaurar desde
`docs/runbooks/backup-restore.md`.

---

## C. Certificado electrónico vencido o inválido

**Síntomas:** "Certificado PKCS12 no encontrado"/firma fallida en logs,
documentos en `ERROR` sin autorizar, alerta de vencimiento.

1. Confirmar el estado del P12:

```bash
docker compose -f infra/compose.yaml --env-file .env exec backend \
  sh -c 'ls -la /app/certificates'
# y desde el host:
keytool -list -keystore signature.p12 -storetype PKCS12 -storepass "$SRI_CERTIFICATE_PASSWORD"
```

2. Si está **vencido o dañado**: rotar según `docs/runbooks/sri-rotation.md`
   (nuevo P12 → `.env` → `up -d backend` → prueba en ambiente `TEST`).
3. Si la **contraseña** cambió/errónea: corregir `SRI_CERTIFICATE_PASSWORD` y
   reiniciar `backend`.
4. Mientras tanto: mantener ventas operativas; los comprobantes quedan en cola
   y se firman/reintentan una vez restablecido. Si el retraso supera el plazo
   tributario, escalar de inmediato (contabilidad + responsable del emisor).
5. Verificar tras la rotación: emisión de prueba autorizada + RIDE correcto.

---

## D. Anexos rápidos

### D.1 Backend caído / unhealthy

```bash
docker compose -f infra/compose.yaml --env-file .env logs --tail 200 backend
docker compose -f infra/compose.yaml --env-file .env restart backend
# Si persiste: rollback de app (docs/runbooks/deploy.md §5)
```

### D.2 Nginx caído (sitio no responde)

```bash
docker exec megalujos-nginx nginx -t        # valida sintaxis
docker compose -f infra/compose.yaml --env-file .env restart nginx
curl -I http://localhost/
```

### D.3 Sin copias de seguridad

```bash
docker compose -f infra/compose.yaml --env-file .env logs --tail 100 backup
# Revisar BACKUP_PASSPHRETE y credenciales; ejecutar copia manual:
docker compose -f infra/compose.yaml --env-file .env run --rm backup /usr/local/bin/backup.sh
```

### D.4 Login falla para todos los usuarios

- BD ok pero auth caída → revisar `backend` y `flyway_schema_history`.
- `JWT_SECRET` cambiado invalida tokens (esperado): pedir re-login.
- Bloqueo por intentos: 5 intentos / 15 min (config por defecto).

---

## Registro de incidencias

Registrar siempre: fecha/hora, servicio afectado, síntoma, causa raíz,
mitigación aplicada, tiempo de recuperación (MTTR), acciones preventivas.
