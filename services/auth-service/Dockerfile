FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiar JAR compilado
COPY services/auth-service/build/libs/auth-service.jar app.jar

# Exponer puerto
EXPOSE 8091

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8091

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8091/actuator/health || exit 1

# Comando de inicio
CMD ["java", "-jar", "app.jar"]
