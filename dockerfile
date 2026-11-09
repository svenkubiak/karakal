FROM amazoncorretto:25-headless

# AL2023 locks dnf to the base image release (system-release). A plain yum update
# stays on that snapshot, so security fixes published in a newer release never
# get installed. --releasever=latest moves the image to the current AL2023 repo.
RUN yum update -y --releasever=latest \
    && yum install -y shadow-utils \
    && yum clean all \
    && rm -rf /var/cache/yum /var/cache/dnf \
    && groupadd --gid 1000 appgroup \
    && useradd \
        --uid 33 \
        --gid 1000 \
        --no-create-home \
        --home-dir /app \
        --shell /sbin/nologin \
        appuser \
    && mkdir -p /app \
    && chown appuser:appgroup /app

WORKDIR /app

COPY --chown=appuser:appgroup target/karakal.jar ./karakal.jar

USER appuser

# config.yaml is packaged in the jar. An empty application.config after JAVA_OPTS wins over a
# -Dapplication.config supplied through JAVA_OPTS, so the packaged file stays the only configuration.
# Secrets are file{} values read from /run/secrets.
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -Dapplication.config= -jar /app/karakal.jar"]