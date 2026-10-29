#!/usr/bin/env bash
# =============================================================================
# MEGALUJOS — Copia de seguridad de PostgreSQL (cifrada)
# =============================================================================
# Salida:  ${BACKUP_DIR}/megalujos_YYYYMMDD_HHMMSS.sql.enc
# Formato: pg_dump -Fc (custom) -> openssl enc -aes-256-cbc (PBKDF2)
# Retención: borra copias con más de ${RETENTION_DAYS} días.
#
# Variables de entorno:
#   DB_HOST            (default: postgres)
#   DB_PORT            (default: 5432)
#   DB_NAME            (default: ${POSTGRES_DB:-megalujos_db})
#   DB_USER            (default: ${POSTGRES_USER:-megalujos_user})
#   PGPASSWORD         / POSTGRES_PASSWORD / SPRING_DATASOURCE_PASSWORD
#   BACKUP_PASSPHRETE  passphrase de cifrado (OBLIGATORIA).
#                      Se acepta también BACKUP_PASSPHRASE como alias.
#   BACKUP_DIR         (default: /backups)
#   RETENTION_DAYS     (default: 14)
#   VERIFY_BACKUP      (true|false, default false) — descifra para validar
# =============================================================================
set -euo pipefail

log() { printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S %Z')" "$*"; }

DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-${POSTGRES_DB:-megalujos_db}}"
DB_USER="${DB_USER:-${POSTGRES_USER:-megalujos_user}}"
PGPASSWORD="${PGPASSWORD:-${POSTGRES_PASSWORD:-${SPRING_DATASOURCE_PASSWORD:-}}}"
export PGPASSWORD

BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
VERIFY_BACKUP="${VERIFY_BACKUP:-false}"
PASSPHRASE="${BACKUP_PASSPHRETE:-${BACKUP_PASSPHRASE:-}}"

command -v pg_dump  >/dev/null 2>&1 || { log "ERROR: pg_dump no está disponible"; exit 1; }
command -v openssl  >/dev/null 2>&1 || { log "ERROR: openssl no está disponible"; exit 1; }

if [ -z "$PASSPHRASE" ]; then
    log "ERROR: BACKUP_PASSPHRETE no está definida; no se genera la copia."
    exit 1
fi
if [ -z "$PGPASSWORD" ]; then
    log "ERROR: no hay contraseña de base de datos (PGPASSWORD/POSTGRES_PASSWORD)."
    exit 1
fi

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

STAMP="$(date +%Y%m%d_%H%M%S)"
PLAIN="$BACKUP_DIR/.tmp_megalujos_${STAMP}.sql"
ENC="$BACKUP_DIR/megalujos_${STAMP}.sql.enc"
trap 'rm -f "$PLAIN"' EXIT

log "Iniciando pg_dump de $DB_NAME @ $DB_HOST:$DB_PORT"
if ! pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -Fc -f "$PLAIN"; then
    log "ERROR: pg_dump falló"
    exit 1
fi
log "Dump completado ($(du -h "$PLAIN" | awk '{print $1}'))"

if ! openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 \
        -in "$PLAIN" -out "$ENC" -pass pass:"$PASSPHRASE"; then
    log "ERROR: el cifrado falló"
    rm -f "$PLAIN" "$ENC"
    exit 1
fi
rm -f "$PLAIN"
chmod 600 "$ENC"
log "Cifrada -> $ENC"

# Verificación opcional: descifrar y listar el contenido del dump.
if [ "$VERIFY_BACKUP" = "true" ]; then
    if openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
            -in "$ENC" -pass pass:"$PASSPHRASE" \
        | pg_restore --list >/dev/null 2>&1; then
        log "Verificación OK: el dump es legible con pg_restore"
    else
        log "ERROR: verificación fallida (passphrase incorrecta o dump corrupto)"
        exit 1
    fi
fi

# --- Retención ---------------------------------------------------------------
COUNT="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'megalujos_*.sql.enc' -mtime +"$RETENTION_DAYS" -delete -print | wc -l | tr -d ' ')"
if [ "$COUNT" != "0" ]; then
    log "Retención: eliminadas $COUNT copias con más de $RETENTION_DAYS días"
fi

# --- (Opcional) copia fuera del VPS -----------------------------------------
# Activar definiendo REMOTE_TARGET en .env. Requiere rclone o ssh configurados
# en la imagen/host.
#
# if [ -n "${REMOTE_TARGET:-}" ]; then
#     log "Subiendo copia a $REMOTE_TARGET"
#     rclone copy "$ENC" "$REMOTE_TARGET" --progress || log "WARN: subida con rclone falló"
#     # ... o bien:
#     # scp -o StrictHostKeyChecking=accept-new "$ENC" "$REMOTE_TARGET" || log "WARN: scp falló"
# fi

TOTAL="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name 'megalujos_*.sql.enc' | wc -l | tr -d ' ')"
log "Finalizado. Copias vigentes en $BACKUP_DIR: $TOTAL"
