FROM gradle:8.8-jdk17 AS build

WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY services/coupon-service/build.gradle.kts ./services/coupon-service/
COPY services/coupon-service/src ./services/coupon-service/src

RUN gradle :services:coupon-service:build -x test --no-daemon

FROM openjdk:17-alpine

WORKDIR /app

# Install curl for health checks
RUN apk --no-cache add curl

COPY --from=build /app/services/coupon-service/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]