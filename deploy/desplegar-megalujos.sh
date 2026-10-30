#!/usr/bin/env bash
# ============================================================================
# MEGALUJOS — Script de despliegue para el servidor
# ============================================================================
# Uso (desde la raíz del repo en el servidor):
#   chmod +x deploy/desplegar-megalujos.sh
#   sudo ./deploy/desplegar-megalujos.sh
#
# Qué hace:
#   1. Compila el frontend (admin + storefront) con Angular CLI.
#   2. Copia los dist a /var/www/megalujos/{admin,store} (nginx nativo).
#   3. Construye la imagen del backend y la corre en Docker, publicada SOLO
#      en 127.0.0.1:18090 (puerto de host distinto de 8080).
#   4. NO toca /etc/nginx/sites-available/control-servidor: ese archivo lo
#      editás a mano pegando el bloque de deploy/megalujos-nginx.txt.
#
# La base de datos NO va en Docker: se asume PostgreSQL nativo en el host.
# El esquema lo crea Flyway automáticamente al arrancar el backend.
# ============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuración (editar según el servidor)
# ---------------------------------------------------------------------------
APP_NAME="megalujos"
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Rutas de frontend (nginx nativo)
WEB_ROOT="/var/www/megalujos"

# Backend Docker: SOLO loopback, puerto de host != 8080
BACKEND_HOST="127.0.0.1"
BACKEND_PORT="18090"
CONTAINER_NAME="megalujos-backend"
IMAGE_NAME="megalujos-backend:latest"

# Base de datos NATIVA (no Docker) — credenciales del servidor
DB_NAME="megalujos_db"
DB_USER="postgres"
DB_PASS="086411421"
DB_HOST="127.0.0.1"                # host-local (para psql desde el servidor)
DB_PORT="5432"
# Dentro del contenedor, 127.0.0.1 es el propio contenedor: para alcanzar
# el Postgres del host se usa host.docker.internal (creado con host-gateway).
DB_HOST_CONTAINER="host.docker.internal"

# Seguridad
JWT_SECRET="megalujos_produccion_secret_2026_ecuador_automotive_key_minimum_256_bits_long_for_hs256"

# Vitrina
SITE_URL="https://alan-tek.com/megalujos/store"
CORS_ORIGINS="https://alan-tek.com,https://www.alan-tek.com,http://alan-tek.com,http://www.alan-tek.com"

# SRI: DESHABILITADO por defecto (regla: no simular autorizaciones)
SRI_ENABLED="false"

UPLOADS_DIR="/var/lib/megalujos/uploads"

log()  { printf '\033[1;32m[MEGALUJOS]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[ERROR]\033[0m %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# 0. Preflight
# ---------------------------------------------------------------------------
log "Requisitos previos..."
command -v node   >/dev/null 2>&1 || fail "node no está instalado."
command -v npm    >/dev/null 2>&1 || fail "npm no está instalado."
command -v docker >/dev/null 2>&1 || fail "docker no está instalado."
command -v psql   >/dev/null 2>&1 || fail "psql no está instalado ( PostgreSQL nativo )."

[ "$(id -u)" -eq 0 ] || fail "Ejecutar como root (sudo)."

log "Repo: ${REPO_DIR}"

# ---------------------------------------------------------------------------
# 1. Frontend: instalación de dependencias y build de producción
# ---------------------------------------------------------------------------
log "Instalando dependencias del frontend (npm ci)..."
cd "${REPO_DIR}/frontend"
npm ci

log "Compilando ADMIN con base-href /megalujos/admin/ ..."
npx ng build admin --configuration production --base-href /megalujos/admin/

log "Compilando STOREFRONT con base-href /megalujos/store/ ..."
npx ng build storefront --configuration production --base-href /megalujos/store/

# ---------------------------------------------------------------------------
# 2. Frontend: copia a /var/www (nginx nativo)
# ---------------------------------------------------------------------------
log "Copiando builds a ${WEB_ROOT}..."
mkdir -p "${WEB_ROOT}/admin" "${WEB_ROOT}/store"
rm -rf "${WEB_ROOT}/admin/"* "${WEB_ROOT}/store/"*
cp -a "${REPO_DIR}/frontend/dist/admin/browser/."   "${WEB_ROOT}/admin/"
cp -a "${REPO_DIR}/frontend/dist/storefront/browser/." "${WEB_ROOT}/store/"

# Permisos para que nginx (www-data) pueda leer
chown -R www-data:www-data "${WEB_ROOT}"
chmod -R 755 "${WEB_ROOT}"

log "Frontend desplegado:"
log "  Admin:   ${WEB_ROOT}/admin"
log "  Store:   ${WEB_ROOT}/store"

# ---------------------------------------------------------------------------
# 3. Base de datos NATIVA (no Docker)
#    El backend se conecta como 'postgres' (superuser), así que Flyway puede
#    crear la extensión pgcrypto y todo el esquema sin grants adicionales.
#    Aquí solo se crea la base si no existe.
# ---------------------------------------------------------------------------
log "Preparando base de datos nativa ${DB_NAME}..."
sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
-- Base de datos (si no existe)
SELECT 'CREATE DATABASE ${DB_NAME}'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${DB_NAME}')
\gexec
SQL

# pgcrypto dentro de la base: la migración V1 la usa (gen_random_uuid) y V10
# usa crypt/gen_salt para el hash de la contraseña del admin. Al conectar como
# superuser, Flyway también podría crearla; se precra aquí por si se ejecuta
# el SQL por separado antes del primer arranque.
sudo -u postgres psql -d "${DB_NAME}" -v ON_ERROR_STOP=1 <<SQL
CREATE EXTENSION IF NOT EXISTS pgcrypto;
SQL

log "Base de datos lista (el esquema lo crea Flyway en el primer arranque)."

# ---------------------------------------------------------------------------
# 3b. Postgres debe aceptar conexiones desde la red puente de Docker
#     (el contenedor se conecta vía host.docker.internal -> IP del bridge,
#      autenticándose como 'postgres' con contraseña)
# ---------------------------------------------------------------------------
LISTEN="$(sudo -u postgres psql -tAc 'SHOW listen_addresses;' 2>/dev/null || echo '?')"
if [ "${LISTEN}" = "localhost" ]; then
  cat >&2 <<'EOF'
[ERROR] Postgres solo escucha en 'localhost': el contenedor NO podrá conectar.
        Solución (en el servidor):

          1) /etc/postgresql/<versión>/main/postgresql.conf:
                listen_addresses = '*'
          2) /etc/postgresql/<versión>/main/pg_hba.conf (agregar una línea):
                host  all  all  172.16.0.0/12  scram-sha-256
          3) sudo systemctl reload postgresql

        Luego volvé a correr este script.
EOF
  fail "Postgres no acepta conexiones desde Docker."
fi
log "Postgres escucha en '${LISTEN}' (acepta conexiones del contenedor)."

# ---------------------------------------------------------------------------
# 4. Backend: build de imagen Docker
# ---------------------------------------------------------------------------
log "Construyendo imagen ${IMAGE_NAME}..."
cd "${REPO_DIR}"
docker build -t "${IMAGE_NAME}" ./backend

# ---------------------------------------------------------------------------
# 5. Backend: volumen de uploads + contenedor
# ---------------------------------------------------------------------------
log "Preparando directorio de uploads ${UPLOADS_DIR}..."
mkdir -p "${UPLOADS_DIR}"
# El contenedor corre como appuser (uid del interior); se abre para lectura
# de nginx y escritura del backend.
chmod -R 777 "${UPLOADS_DIR}"

log "Deteniendo contenedor previo (si existe)..."
docker rm -f "${CONTAINER_NAME}" 2>/dev/null || true

log "Levantando ${CONTAINER_NAME} en ${BACKEND_HOST}:${BACKEND_PORT}..."
docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  -p "${BACKEND_HOST}:${BACKEND_PORT}:8080" \
  --add-host="${DB_HOST_CONTAINER}:host-gateway" \
  -e "SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST_CONTAINER}:${DB_PORT}/${DB_NAME}" \
  -e "SPRING_DATASOURCE_USERNAME=${DB_USER}" \
  -e "SPRING_DATASOURCE_PASSWORD=${DB_PASS}" \
  -e "JWT_SECRET=${JWT_SECRET}" \
  -e "CORS_ALLOWED_ORIGINS=${CORS_ORIGINS}" \
  -e "SITE_URL=${SITE_URL}" \
  -e "STORAGE_LOCAL_PATH=/app/uploads" \
  -e "SRI_ENABLED=${SRI_ENABLED}" \
  -v "${UPLOADS_DIR}:/app/uploads" \
  "${IMAGE_NAME}"

log "Contenedor lanzado. Esperando a que el health check pase (hasta ~120s)..."
for i in $(seq 1 24); do
  if curl -fsS "http://${BACKEND_HOST}:${BACKEND_PORT}/actuator/health" >/dev/null 2>&1; then
    log "Backend HEALTHY en http://${BACKEND_HOST}:${BACKEND_PORT}/actuator/health"
    break
  fi
  [ "$i" -eq 24 ] && fail "El backend no respondió a tiempo. Ver: docker logs ${CONTAINER_NAME}"
  sleep 5
done

# ---------------------------------------------------------------------------
# 6. nginx: recarga (NO modifica control-servidor)
# ---------------------------------------------------------------------------
log "Probando configuración de nginx..."
nginx -t || fail "La configuración de nginx tiene errores. Revisá el bloque de MEGALUJOS."

log "Recargando nginx..."
systemctl reload nginx

# ---------------------------------------------------------------------------
# 7. Smoke tests
# ---------------------------------------------------------------------------
log "Smoke tests..."

code=$(curl -s -o /dev/null -w '%{http_code}' "http://${BACKEND_HOST}:${BACKEND_PORT}/actuator/health" || true)
log "  /actuator/health -> ${code}"
[ "${code}" != "200" ] && log "  ⚠ el backend no responde 200. Ver: docker logs ${CONTAINER_NAME}"

code=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1/megalujos/admin/" || true)
log "  /megalujos/admin/ -> ${code}"
if [ "${code}" = "404" ] || [ "${code}" = "000" ]; then
  log "  ⚠ 404: pegá el bloque de deploy/megalujos-nginx.txt en"
  log "    /etc/nginx/sites-available/control-servidor y recargá nginx."
fi

code=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1/megalujos/store/" || true)
log "  /megalujos/store/ -> ${code}"

code=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1/megalujos/api/public/catalog/categories" || true)
log "  /megalujos/api/... -> ${code}"

# ---------------------------------------------------------------------------
# Resumen
# ---------------------------------------------------------------------------
cat <<EOF

============================================================
 MEGALUJOS DESPLEGADO
============================================================
  Admin:    http://$(hostname -f)/megalujos/admin/
  Store:    http://$(hostname -f)/megalujos/store/
  API:      http://$(hostname -f)/megalujos/api/
  Backend:  http://${BACKEND_HOST}:${BACKEND_PORT}  (solo loopback, Docker)
  DB:       PostgreSQL nativo ${DB_NAME} (usuario ${DB_USER})

  Usuario inicial: admin / admin123  (cambiar en el primer ingreso)

  CONTROL-SERVIDOR: NO fue modificado. Pegá el bloque de
  deploy/megalujos-nginx.txt dentro del server{} y recargá nginx.

  Logs del backend:  docker logs -f ${CONTAINER_NAME}
============================================================
EOF
