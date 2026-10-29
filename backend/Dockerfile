# syntax=docker/dockerfile:1
# MEGALUJOS — API Spring Boot (multi-stage)
# Build:  docker build -t megalujos-backend:latest ./backend
# El resultado es una imagen mínima JRE, sin root, con healthcheck en /actuator/health.

# ---------------------------------------------------------------------------
# 1) Etapa de compilación
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copiar primero el pom.xml permite cachear la capa de dependencias cuando
# solo cambia código fuente (el COPY de src invalida la capa posterior).
COPY pom.xml ./
COPY src ./src

RUN mvn -B -DskipTests package

# ---------------------------------------------------------------------------
# 2) Etapa de ejecución
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre

# curl es necesario para el HEALTHCHECK de la imagen.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appuser \
    && useradd --system --gid appuser --home-dir /app --shell /usr/sbin/nologin appuser

WORKDIR /app

# El volumen de subidas se inicializa con los permisos de esta imagen,
# de modo que el usuario no-root pueda escribir en él.
RUN mkdir -p /app/uploads /app/certificates \
    && chown -R appuser:appuser /app

COPY --from=build /workspace/target/megalujos-backend-*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

USER appuser

ENV HOME=/app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
