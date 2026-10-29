-- ============================================================
-- V12 - Sesiones de autenticación: refresh tokens rotativos
--       e historial de intentos de login (bloqueo progresivo)
-- ============================================================

-- 1. Refresh tokens con rotación (hash SHA-256 del token emitido)
CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id),
    token_hash varchar(128) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked boolean NOT NULL DEFAULT false,
    replaced_by uuid,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 2. Intentos de login (fallidos y exitosos) para bloqueo progresivo
CREATE TABLE failed_login_attempts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username varchar(80) NOT NULL,
    ip_address varchar(60),
    attempted_at timestamptz NOT NULL DEFAULT now(),
    success boolean NOT NULL DEFAULT false
);

CREATE INDEX ix_failed_login_attempts_username_time ON failed_login_attempts(username, attempted_at);
