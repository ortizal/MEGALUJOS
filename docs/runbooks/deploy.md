# Runbook — Despliegue en VPS

Despliegue de MEGALUJOS con Docker Compose. Aplica a `infra/compose.yaml`.

## 1. Requisitos del VPS

- Ubuntu 22.04/24.04 (o Debian equivalente) con Docker Engine + plugin `docker compose`.
- Usuario sin privilegios para desplegar (`deploy`) en el grupo `docker`, SSH con clave.
- Firewall: solo 22/80/443 abiertos. **Nunca** abrir 5432, 8080, 4200, 4300.
- Disco: prever el doble del tamaño de `pgdata` + copias cifradas.

```bash
# Firewall (ufw)
sudo ufw default deny incoming
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

## 2. Primera instalación

```bash
# 1. Código
rsync -az --exclude node_modules --exclude target --exclude .git \
  ./ deploy@VPS:/opt/megalujos/

# 2. Secretos (NUNCA por Git)
scp .env deploy@VPS:/opt/megalujos/.env
chmod 600 /opt/megalujos/.env

# 3. Build y arranque
cd /opt/megalujos
docker compose -f infra/compose.yaml --env-file .env build
docker compose -f infra/compose.yaml --env-file .env up -d

# 4. Verificar
docker compose -f infra/compose.yaml --env-file .env ps
curl -fsS http://localhost/actuator/health     # vía nginx
```

URLs: `/` (vitrina), `/admin/` (panel), `/api/v1/...`, `/swagger-ui/index.html`.

## 3. Despliegue de una versión nueva

1. CI verde en `main` (`/.github/workflows/ci.yml`).
2. Sincronizar código (o `docker compose pull` si se usan imágenes de registry).
3. Revisar migraciones Flyway nuevas en el diff (¿son compatibles con datos existentes?).
4. `docker compose -f infra/compose.yaml --env-file .env build`
5. `docker compose -f infra/compose.yaml --env-file .env up -d --remove-orphans`
6. Smoke test (sección 6).

Plantilla automatizada: `.github/workflows/deploy.yml.example` (SSH/rsync con rollback).

## 4. Migraciones Flyway

- Son **automáticas**: al arrancar, el backend ejecuta `classpath:db/migration`
  (`V1..V16`) con `baseline-on-migrate: true`.
- Comprobar estado:

```bash
docker compose -f infra/compose.yaml --env-file .env exec postgres \
  psql -U megalujos_user -d megalujos_db -c \
  "SELECT version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;"
```

- **Antes** de desplegar: verificar que ningún `V*__*.sql` ya ejecutado fue
  modificado (`git diff`); editar migraciones ejecutadas está prohibido.
- Estrategia recomendada (expand/contract): primero una migración compatible
  hacia atrás (añadir columnas/tablas nullables), desplegar código, y solo en
  una versión posterior eliminar lo obsoleto.
- Si una migración falla, el backend no arranca (fail-fast). Ver logs:

```bash
docker compose -f infra/compose.yaml --env-file .env logs backend 2>&1 | grep -Ei "flyway|migrating|error"
```

### Rollback de migraciones (datos)

Flyway **no** revierte migraciones. Opciones, en orden de preferencia:

1. **Reversión manual**: escribir/ ejecutar un script `U*`-equivalente
   (`ALTER TABLE ... DROP COLUMN`) con el DBA, solo si se conoce exactamente
   el efecto. Dejarlo documentado en el PR.
2. **Restaurar backup**: ver `docs/runbooks/backup-restore.md` (pierde los
   datos posteriores a la copia → RPO).
3. **Reversible solo en staging** si el cambio aún no tocó producción.

## 5. Rollback de la aplicación

```bash
cd /opt/megalujos

# Opción A — volver al código anterior
git log --oneline -5
git revert <commit>            # o checkout a la tag anterior
docker compose -f infra/compose.yaml --env-file .env build
docker compose -f infra/compose.yaml --env-file .env up -d --remove-orphans

# Opción B — con imágenes etiquetadas (deploy.yml.example guarda .deploy-tag)
PREV=$(cat .deploy-tag)
docker tag megalujos-backend:$PREV megalujos-backend:latest
docker tag megalujos-admin:$PREV megalujos-admin:latest
docker tag megalujos-storefront:$PREV megalujos-storefront:latest
docker compose -f infra/compose.yaml --env-file .env up -d --no-build
```

**Importante:** un rollback de código no deshace migraciones. Si la versión
anterior es incompatible con el esquema nuevo, combinar con rollback de datos
(sección 4) o aplicar una migración correctora nueva.

## 6. Smoke test post-despliegue

- [ ] `docker compose ps` → todos `healthy`
- [ ] `GET /actuator/health` → `{"status":"UP"}`
- [ ] Login `admin` en `/admin/` funciona
- [ ] `GET /api/v1/products?page=0&size=5` responde 200 con token
- [ ] Vitrina carga imágenes (`/uploads/...`)
- [ ] Logs sin errores nuevos (`docker compose logs --since 10m backend`)
- [ ] Si cambió esquema: `flyway_schema_history` sin `success = false`

## 7. Operación recurrente

```bash
# Ver estado y logs
docker compose -f infra/compose.yaml --env-file .env ps
docker compose -f infra/compose.yaml --env-file .env logs -f --tail 200 nginx backend

# Reiniciar un servicio
docker compose -f infra/compose.yaml --env-file .env restart backend

# Actualizar imágenes base y reconstruir
docker compose -f infra/compose.yaml --env-file .env pull
docker compose -f infra/compose.yaml --env-file .env build --no-cache backend

# Limpieza si el disco se llena (sin --volumes: borrar volúmenes destruye pgdata)
docker system prune -af
docker volume ls   # pgdata, uploads_data y backups deben seguir listados
```

Referencias: `infra/backup/README.md`, `infra/monitoring/README.md`,
`docs/runbooks/incidents.md`.
