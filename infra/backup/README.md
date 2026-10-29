# Backups — MEGALUJOS

Copia de seguridad cifrada de PostgreSQL generada por el servicio `backup`
definido en `infra/compose.yaml`.

## Qué genera

| Elemento | Detalle |
|---|---|
| Herramienta | `pg_dump -Fc` (formato custom de PostgreSQL 16) |
| Cifrado | `openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000` |
| Archivo | `/backups/megalujos_YYYYMMDD_HHMMSS.sql.enc` |
| Volumen | `backups` (nombrado, dentro del VPS) |
| Horario | `BACKUP_SCHEDULE` (cron del contenedor), default `0 2 * * *` |
| Zona horaria | `TZ` (default `America/Guayaquil`) |
| Retención | `RETENTION_DAYS` (default 14 días) |

## Variables requeridas en `.env`

```bash
BACKUP_PASSPHRETE= cambia-esta-passphrase-larga-y-secreta   # obligatoria
RETENTION_DAYS=14                                            # opcional
BACKUP_SCHEDULE=0 2 * * *                                    # opcional (cron)
TZ=America/Guayaquil                                         # opcional
```

> El nombre de la variable es `BACKUP_PASSPHRETE` (se mantiene por compatibilidad;
> también se acepta `BACKUP_PASSPHRASE`). **No está en `.env.example` a propósito**:
> genera la passphrase con `openssl rand -base64 48` y guárdala solo en el `.env`
> del VPS, fuera de Git.

El servicio también recibe `PGPASSWORD` desde `SPRING_DATASOURCE_PASSWORD`.

## Ejecución manual

```bash
# Copia puntual
docker compose -f infra/compose.yaml --env-file .env run --rm backup /usr/local/bin/backup.sh

# Ver logs del cron
docker compose -f infra/compose.yaml --env-file .env exec backup tail -f /var/log/megalujos-backup.log

# Listar copias
docker compose -f infra/compose.yaml --env-file .env exec backup ls -lh /backups
```

> El script y el entrypoint viven **dentro de la imagen** (copiados con modo
> 755): tras modificar `backup.sh` o `entrypoint.sh`, reconstruye con
> `docker compose -f infra/compose.yaml --env-file .env build backup`.
> En repositorio Git, marcar el script como ejecutable:
> `git update-index --chmod=+x infra/backup/backup.sh`.

## Restauración (resumen)

```bash
# 1. Descifrar
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
  -in megalujos_YYYYMMDD_HHMMSS.sql.enc \
  -pass pass:"$BACKUP_PASSPHRETE" -out dump.sql

# 2. Restaurar
docker compose -f infra/compose.yaml --env-file .env exec -T postgres \
  pg_restore -U megalujos_user -d megalujos_db --clean --if-exists < dump.sql
```

Procedimiento completo, con restauración en una BD de staging y verificación:
**`docs/runbooks/backup-restore.md`**.

## Copia fuera del VPS

El script incluye un *hook* comentado al final (`rclone copy` / `scp`) que se
activa definiendo `REMOTE_TARGET`. Recomendado: subir a un storage con
versionado (S3/Backblaze/Drive) cifrando en origen, nunca en texto plano.

## Verificación y pruebas

- Probar restauración **mensualmente** en staging (criterio de aceptación del
  documento maestro, sección 8).
- Activar `VERIFY_BACKUP=true` para que cada copia se descifre y se valide con
  `pg_restore --list` antes de darla por buena (más lento).
- El servicio usa `restart: unless-stopped`; si falla una copia, el error queda
  en `/var/log/megalujos-backup.log` del contenedor.

## RPO / RTO sugeridos

| Métrica | Valor sugerido | Justificación |
|---|---|---|
| **RPO** (pérdida máxima de datos) | ≤ 24 h con copia diaria; ≤ 1 h si se pasa a horario cada hora | Solo hay un escritor (el backend); cada copia es consistente por `pg_dump` |
| **RTO** (tiempo de recuperación) | ≤ 60 min | Restauración de dump reciente + arranque de compose (~10–20 min) + verificación |

Ajustes recomendados según criticidad:

- Horario diario a las 02:00 (fuera del horario de caja).
- `RETENTION_DAYS=30` si el volumen lo permite.
- Mantener al menos una copia **fuera del VPS** con antigüedad ≤ 24 h.
- Registrar cada prueba de restauración (fecha, dump usado, duración, hallazgos).
