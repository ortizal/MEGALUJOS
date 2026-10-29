# MEGALUJOS — imagen del job de backup
# Basada en postgres:16-alpine para disponer de pg_dump/pg_restore y de
# busybox crond para la programación diaria (sin dependencias externas).
FROM postgres:16-alpine

RUN apk add --no-cache bash openssl tzdata curl

COPY backup.sh /usr/local/bin/backup.sh
COPY entrypoint.sh /usr/local/bin/backup-entrypoint.sh
RUN chmod 755 /usr/local/bin/backup.sh /usr/local/bin/backup-entrypoint.sh \
    && mkdir -p /backups \
    && chmod 700 /backups

VOLUME ["/backups"]

ENTRYPOINT ["/usr/local/bin/backup-entrypoint.sh"]
