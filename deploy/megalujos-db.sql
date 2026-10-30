-- ============================================================================
-- MEGALUJOS — Preparación de la base de datos para PostgreSQL NATIVO
-- ============================================================================
-- Este script NO crea el esquema de tablas: eso lo hace Flyway automáticamente
-- al arrancar el backend (V1..V17). Aquí solo se deja la base lista.
--
-- El backend se conecta como el superusuario 'postgres' con la contraseña
-- configurada en deploy/desplegar-megalujos.sh (SPRING_DATASOURCE_USERNAME /
-- SPRING_DATASOURCE_PASSWORD). Al ser superuser, Flyway puede crear la
-- extensión pgcrypto y todo el esquema sin permisos adicionales.
--
-- EJECUCIÓN:
--   Conectar como SUPERUSUARIO (postgres) y correr contra la base 'postgres':
--
--     psql -U postgres -f megalujos-db.sql
--
--   o desde pgAdmin/psql interactivo, seleccionando la base 'postgres'.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Base de datos (si no existe)
-- ---------------------------------------------------------------------------
SELECT 'CREATE DATABASE megalujos_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'megalujos_db')
\gexec

-- ---------------------------------------------------------------------------
-- 2. Extensión requerida por las migraciones
--    V1 la usa para gen_random_uuid(); V10 usa crypt/gen_salt para el hash
--    de la contraseña del usuario admin inicial.
--    Se crea DENTRO de megalujos_db.
-- ---------------------------------------------------------------------------
\connect megalujos_db

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- NOTA (si el backend corre en Docker, como en este despliegue):
--   El contenedor se conecta vía host.docker.internal (IP del puente Docker),
--   así que Postgres debe escuchar fuera de 'localhost':
--     postgresql.conf:  listen_addresses = '*'
--     pg_hba.conf:      host  all  all  172.16.0.0/12  scram-sha-256
--   y recargar con: sudo systemctl reload postgresql
--
-- LISTO. Al arrancar el backend, Flyway creará las tablas (V1..V17) y el
-- usuario inicial 'admin' con contraseña 'admin123'.
-- ============================================================================
