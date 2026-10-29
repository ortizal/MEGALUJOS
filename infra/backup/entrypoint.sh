#!/bin/sh
# MEGALUJOS — entrypoint del contenedor de backup: instala el cron y deja
# crond en primer plano (el contenedor es el scheduler).
set -eu

# Ejecución puntual: `docker compose run --rm backup /usr/local/bin/backup.sh`
if [ "$#" -gt 0 ]; then
    exec "$@"
fi

TZ_VALUE="${TZ:-America/Guayaquil}"
SCHEDULE="${BACKUP_SCHEDULE:-0 2 * * *}"
LOG_FILE="${BACKUP_LOG_FILE:-/var/log/megalujos-backup.log}"

# Zona horaria del contenedor: crond y date() usan la hora local.
if [ -f "/usr/share/zoneinfo/${TZ_VALUE}" ]; then
    ln -sf "/usr/share/zoneinfo/${TZ_VALUE}" /etc/localtime
    echo "${TZ_VALUE}" > /etc/timezone
fi

CRON_FILE=/etc/crontabs/root
{
    printf 'SHELL=/bin/sh\n'
    printf 'PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n'
    printf '%s /usr/local/bin/backup.sh >> %s 2>&1\n' "$SCHEDULE" "$LOG_FILE"
} > "$CRON_FILE"
chmod 600 "$CRON_FILE"

if [ -z "${BACKUP_PASSPHRETE:-}" ] && [ -z "${BACKUP_PASSPHRASE:-}" ]; then
    echo "[backup] *******************************************************************"
    echo "[backup] ERROR: BACKUP_PASSPHRETE no está definida en .env."
    echo "[backup] Las copias NO se podrán cifrar ni generar. Configúrala y reinicia"
    echo "[backup] el servicio (infra/backup/README.md)."
    echo "[backup] *******************************************************************"
fi

echo "[backup] horario configurado: '${SCHEDULE}' (${TZ_VALUE}) -> ${LOG_FILE}"

# Copia de cortesía al arrancar, solo si no hay ninguna reciente (< 12 h).
# Así un reinicio del contenedor no genera una copia redundante.
RECENT="$(find /backups -maxdepth 1 -type f -name 'megalujos_*.sql.enc' -mmin -720 2>/dev/null | wc -l | tr -d ' ')"
if [ "${RECENT:-0}" = "0" ]; then
    echo "[backup] sin copias recientes: ejecutando copia inicial..."
    /usr/local/bin/backup.sh >> "$LOG_FILE" 2>&1 \
        || echo "[backup] WARN: copia inicial falló (revisa BACKUP_PASSPHRETE y credenciales)"
else
    echo "[backup] ya existe copia reciente; se omite la copia inicial."
fi

exec crond -f -l 8
