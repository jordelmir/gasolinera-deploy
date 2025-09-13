# Multi-stage Dockerfile for API Gateway
# Optimized for security, size, and build caching

# Build stage - Use specific Gradle version with JDK 17
FROM gradle:8.8.0-jdk17-alpine AS builder

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S gradle && \
    adduser -S gradle -u 1001 -G gradle

# Copy dependency files first for better layer caching
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts gradle.properties ./
COPY --chown=gradle:gradle gradle/ ./gradle/

# Copy shared modules build files for dependency resolution
COPY --chown=gradle:gradle shared/common/build.gradle.kts ./shared/common/
COPY --chown=gradle:gradle shared/messaging/build.gradle.kts ./shared/messaging/
COPY --chown=gradle:gradle shared/security/build.gradle.kts ./shared/security/
COPY --chown=gradle:gradle services/api-gateway/build.gradle.kts ./services/api-gateway/

# Download dependencies (this layer will be cached if dependencies don't change)
RUN gradle :services:api-gateway:dependencies --no-daemon

# Copy shared modules source code
COPY --chown=gradle:gradle shared/ ./shared/

# Copy service source code
COPY --chown=gradle:gradle services/api-gateway/src ./services/api-gateway/src

# Build the application
RUN gradle :services:api-gateway:build -x test --no-daemon --parallel

# Runtime stage - Use distroless for security and minimal size
FROM gcr.io/distroless/java17-debian12:nonroot AS runtime

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/services/api-gateway/build/libs/*.jar app.jar

# Use non-root user (distroless already uses nonroot)
USER nonroot:nonroot

# Expose port (API Gateway typically runs on 8080)
EXPOSE 8080

# Set JVM options for containerized environment (API Gateway needs more memory)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=80.0 -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=docker"

# Add labels for better image management
LABEL maintainer="Gasolinera JSM Team"
LABEL service="api-gateway"
LABEL version="1.0.0"
LABEL description="API Gateway for Gasolinera JSM platform"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
