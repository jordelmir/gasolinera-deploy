FROM openjdk:17-jdk-slim

WORKDIR /app

# Instalar curl para health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copiar JAR compilado
COPY services/auth-service/build/libs/auth-service.jar app.jar

# Usuario no-root para seguridad
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
RUN chown -R appuser:appgroup /app
USER appuser

# Exponer puerto
EXPOSE 8091

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8091
ENV JVM_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8091/actuator/health || exit 1

# Comando de inicio
CMD java $JVM_OPTS -jar app.jar