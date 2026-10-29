# Runbook — Rotación del certificado electrónico (P12)

El certificado PKCS#12 (`signature.p12`) contiene la clave privada con la que
el backend **firma** los comprobantes electrónicos (XML) antes de enviarlos al
SRI. Si vence o se bloquea, la firma falla y ningún documento se emite.

> Regla de oro: el `.p12` y su contraseña **nunca** se guardan en Git. Viven en
> el VPS (`.env` + volumen montado) y en el gestor de contraseñas.

## 1. Cuándo rotar

- Alerta preventiva: **30 días antes** de la fecha de vencimiento.
- Obligatoria: al vencimiento, o si la clave se expone / el responsable cambia.
- Verificar vigencia:

```bash
keytool -list -v -keystore signature.p12 -storetype PKCS12 -storepass "$SRI_CERTIFICATE_PASSWORD" \
  | grep -E "Alias name|Fecha de caducidad|Válido desde"
# Alternativa sin Java:
openssl pkcs12 -in signature.p12 -nokeys -passin pass:"$SRI_CERTIFICATE_PASSWORD" \
  | openssl x509 -noout -dates -subject
```

## 2. Obtener el nuevo certificado

1. Solicitar en una **entidad certificadora autorizada por el SRI** (banco /
   proveedor autorizado), con los datos del RUC del emisor.
2. Al generarlo, elegir y registrar la contraseña del archivo `.p12`.
3. Exportar el archivo `*.p12` (PKCS#12) y verificarlo antes de tocar el VPS:

```bash
keytool -list -keystore nuevo.p12 -storetype PKCS12
```

## 3. Instalar en el VPS

La ruta y la contraseña se configuran por variables (no editar `application.yml`):

| Variable | Valor en Compose |
|---|---|
| `SRI_CERTIFICATE_PATH` | `/app/certificates/signature.p12` |
| `SRI_CERTIFICATE_PASSWORD` | contraseña elegida al generar el P12 |
| `SRI_ENABLED` | `true` |
| `SRI_ENVIRONMENT` | `TEST` (pruebas) o `PRODUCTION` (producción) |

> Nota de consistencia: el código trata como **producción** únicamente el valor
> `PRODUCTION` (cualquier otro valor se toma como pruebas); el campo `environment`
> del emisor (`issuer`) admite `TEST`/`PRODUCTION`. `.env.example` comenta
> `1=Pruebas, 2=Producción`: para el entorno real usar `TEST`/`PRODUCTION`.

Pasos:

```bash
cd /opt/megalujos

# 1. Guardar el nuevo certificado (el volumen montado es infra/certs/sri)
scp nuevo.p12 deploy@VPS:/opt/megalujos/infra/certs/sri/signature.p12
ssh deploy@VPS "chmod 600 /opt/megalujos/infra/certs/sri/signature.p12"

# 2. Actualizar la contraseña en .env (y guardarla en el gestor de contraseñas)
#    SRI_CERTIFICATE_PASSWORD=<nueva>
#    (y descomentar en infra/compose.yaml el volumen:
#      - ./certs/sri:/app/certificates:ro)

# 3. Reiniciar solo el backend
docker compose -f infra/compose.yaml --env-file .env up -d backend
```

## 4. Verificación

1. Logs sin el error *"Certificado PKCS12 no encontrado"* ni fallos de firma:

```bash
docker compose -f infra/compose.yaml --env-file .env logs --since 5m backend
```

2. Emitir **un comprobante de prueba** en ambiente de pruebas
   (`SRI_ENVIRONMENT=TEST`, `SRI_ENABLED=true`):

   - `POST /api/v1/electronic-documents` → estado `SIGNED`/`SENT`
   - esperar autorización (worker) → estado `AUTHORIZED`
   - descargar el RIDE y comprobar que el XML está firmado.

3. Revisar que no haya documentos atascados:
   `GET /api/v1/electronic-documents?status=PENDING` (o reporte
   `/api/v1/reports/pending-documents`).
4. Solo después, si corresponde, pasar a `SRI_ENVIRONMENT=PRODUCTION` y
   repetir una emisión real pequeña (ej. nota de crédito o factura de baja
   incidencia), verificando la autorización.

## 5. Rollback

Si el nuevo certificado no funciona (contraseña incorrecta o archivo dañado):

```bash
# Restaurar el .p12 anterior (tenerlo resguardado)
scp firma-anterior.p12 deploy@VPS:/opt/megalujos/infra/certs/sri/signature.p12
# Revertir SRI_CERTIFICATE_PASSWORD en .env y:
docker compose -f infra/compose.yaml --env-file .env up -d backend
```

Documentar la fecha de la rotación y el resultado en el registro operativo.

## 6. Checklist

- [ ] Nuevo P12 generado en CA autorizada y verificado con `keytool -list`
- [ ] Passphrase guardada en gestor de contraseñas (nunca en Git)
- [ ] Archivo copiado al VPS con permisos `600`
- [ ] `SRI_CERTIFICATE_PASSWORD` actualizada en `.env`
- [ ] Backend reiniciado y `healthy`
- [ ] Comprobante de prueba autorizado en ambiente de pruebas
- [ ] Prueba de emisión en producción verificada
- [ ] Certificado anterior conservado (rollback) y antiguo revocado/expirado
