FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

# Copy the shaded JAR (contains all dependencies)
COPY target/atg-engine-verifier-*-shaded.jar verifier.jar

# Copy the entrypoint script
COPY docker-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
