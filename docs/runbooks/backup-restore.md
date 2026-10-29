# Runbook — Backup y restauración

Complementa [`infra/backup/README.md`](../../infra/backup/README.md).

## 1. Qué se respalda

| Elemento | Detalle |
|---|---|
| Base de datos | `pg_dump -Fc` de `megalujos_db` (esquema Flyway + datos) |
| Cifrado | `openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000` |
| Archivos | `uploads_data` (imágenes, RIDE, diseños) — **copiar aparte** (ver §4) |
| Destino | volumen `backups` del VPS + storage externo (hook `rclone`/`scp`) |
| Horario | `0 2 * * *` (TZ `America/Guayaquil`), retención `RETENTION_DAYS=14` |

## 2. Backup manual

```bash
docker compose -f infra/compose.yaml --env-file .env run --rm backup /usr/local/bin/backup.sh
docker compose -f infra/compose.yaml --env-file .env exec backup ls -lh /backups
```

Copiar la salida fuera del VPS (obligatorio para cumplir RPO real):

```bash
docker compose -f infra/compose.yaml --env-file .env cp \
  backup:/backups/megalujos_YYYYMMDD_HHMMSS.sql.enc ./megalujos_YYYYMMDD_HHMMSS.sql.enc
scp ./megalujos_*.sql.enc usuario@storage:/backups/megalujos/
```

## 3. Restauración completa (BD)

> **Riesgo:** `--clean` elimina los objetos existentes. Practicar en staging.
> Detener primero el backend para evitar escrituras concurrentes.

```bash
cd /opt/megalujos   # o el directorio del repo en tu máquina

# 0. Detener la app (la BD sigue arriba)
docker compose -f infra/compose.yaml --env-file .env stop backend admin storefront

# 1. Descifrar el dump
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
  -in megalujos_20260101_020000.sql.enc \
  -pass pass:"$BACKUP_PASSPHRETE" -out dump.sql

# 2. Restaurar
docker compose -f infra/compose.yaml --env-file .env exec -T postgres \
  pg_restore -U megalujos_user -d megalujos_db --clean --if-exists --no-owner < dump.sql

# 3. (Opcional) restaurar en una BD limpia de prueba
docker compose -f infra/compose.yaml --env-file .env exec -T postgres \
  createdb -U megalujos_user megalujos_restore_test
docker compose -f infra/compose.yaml --env-file .env exec -T postgres \
  pg_restore -U megalujos_user -d megalujos_restore_test --no-owner < dump.sql

# 4. Limpiar y levantar
rm -f dump.sql
docker compose -f infra/compose.yaml --env-file .env up -d
```

**Nota:** `pg_restore --clean` no elimina tablas creadas por migraciones que ya
no existen en el dump; tras restaurar, revisar `flyway_schema_history` y, si la
versión restaurada es anterior al código desplegado, Flyway aplicará las
migraciones pendientes al arrancar.

## 4. Restauración de archivos (`uploads`)

```bash
# Copiar de vuelta al volumen
docker compose -f infra/compose.yaml --env-file .env exec backend ls -la /app/uploads
docker cp ./uploads_backup/. megalujos-api:/app/uploads/
docker compose -f infra/compose.yaml --env-file .env exec backend \
  chown -R appuser:appuser /app/uploads
```

Si el volumen `uploads_data` está sano, no hace falta nada.

## 5. Verificación post-restauración

- [ ] `docker compose ps` → `backend` healthy
- [ ] `curl -fsS http://localhost/actuator/health` → `UP`
- [ ] Login `admin` funciona; se ven productos e imágenes
- [ ] `SELECT COUNT(*) FROM products;` y `FROM stock_movements;` > 0
- [ ] `SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;`
- [ ] Vitrina carga y muestra precios/stock coherentes
- [ ] Registrar la prueba: fecha, dump usado, duración, incidencias

## 6. Frecuencia y política

| Acción | Frecuencia |
|---|---|
| Backup automático | Diario 02:00 (contenedor `backup`) |
| Copia fuera del VPS | Diaria (hook rclone/scp) o como máximo 24 h |
| **Prueba de restauración** | Mensual, obligatoria en staging |
| Rotación de passphrase | Solo si hay sospecha de exposición (ver §7) |

RPO/RTO de referencia: RPO ≤ 24 h, RTO ≤ 60 min
(`infra/backup/README.md`).

## 7. Passphrase (`BACKUP_PASSPHRETE`)

- Vive solo en `.env` del VPS (`chmod 600`), nunca en Git ni en la imagen.
- Generar: `openssl rand -base64 48`.
- Si se pierde, **las copias cifradas no se pueden recuperar**: guardarla en el
  gestor de contraseñas del equipo antes de perder acceso.
- Rotar: conservar la passphrase anterior mientras existan copias antiguas
  (migrar copias antiguas: descifrar con la vieja → cifrar con la nueva).

## 8. Fallos frecuentes

| Síntoma | Causa probable | Acción |
|---|---|---|
| `no passphrase` en el log | falta `BACKUP_PASSPHRETE` en `.env` | definirla y reiniciar `backup` |
| `pg_dump: error: connection` | backend/BD caídos o credenciales | revisar `ps` y `SPRING_DATASOURCE_PASSWORD` |
| Sin copia en 26 h | contenedor `backup` detenido o cron roto | `logs backup`, `exec backup cat /var/log/megalujos-backup.log` |
| `openssl: bad decrypt` | passphrase incorrecta o archivo truncado | probar con la passphrase histórica; verificar tamaño/integridad |
| Disco lleno | retención muy larga o logs grandes | reducir `RETENTION_DAYS`, `docker system prune -af` |
