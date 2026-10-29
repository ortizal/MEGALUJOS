# syntax=docker/dockerfile:1
# MEGALUJOS — Frontend Angular (workspace raíz con apps/admin y apps/storefront)
#
# Un mismo Dockerfile genera dos imágenes mediante el ARG APP:
#   docker build --build-arg APP=admin       -t megalujos-admin:latest       ./frontend
#   docker build --build-arg APP=storefront  -t megalujos-storefront:latest  ./frontend
#
# BASE_HREF permite servir admin bajo /admin/ (por defecto "/").
#   docker build --build-arg APP=admin --build-arg BASE_HREF=/admin/ ...

# ---------------------------------------------------------------------------
# 1) Etapa de compilación (Node 24 / Angular 22)
# ---------------------------------------------------------------------------
FROM node:24-alpine AS build

ARG APP=storefront
ARG BASE_HREF=/

WORKDIR /app

# Dependencias primero para cachear npm ci entre builds.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

RUN npx ng build ${APP} --base-href ${BASE_HREF} --configuration production

# ---------------------------------------------------------------------------
# 2) Etapa de servido (nginx)
# ---------------------------------------------------------------------------
FROM nginx:alpine

ARG APP=storefront

# Configuración mínima con fallback SPA (try_files) para rutas profundas.
RUN cat > /etc/nginx/conf.d/default.conf <<'NGINX'
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
NGINX

COPY --from=build /app/dist/${APP}/browser /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:80/ || exit 1
