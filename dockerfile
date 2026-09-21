FROM amazoncorretto:25-headless

RUN yum install -y shadow-utils \
    && yum clean all \
    && rm -rf /var/cache/yum \
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

# The configuration file is passed after JAVA_OPTS and in its own quoted variable, so that it can
# not be overridden through JAVA_OPTS (later -D options win) and is not subject to word splitting
ENV KARAKAL_CONFIG=/app/config/config.yaml

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -Dapplication.config=\"${KARAKAL_CONFIG}\" -jar /app/karakal.jar"]