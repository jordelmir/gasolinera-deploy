# Etapa 1: Compilación
FROM gradle:8.8.0-jdk17-alpine AS build
WORKDIR /home/gradle/project

# Copiar solo los archivos de build para cachear dependencias
COPY build.gradle.kts settings.gradle.kts ./ 
COPY services/raffle-service/build.gradle.kts ./services/raffle-service/

# Descargar dependencias
RUN gradle :services:raffle-service:dependencies

# Copiar el resto del código fuente
COPY . .

# Construir la aplicación
RUN gradle :services:raffle-service:build -x test

# Etapa 2: Producción
FROM openjdk:17-alpine

WORKDIR /app

# Copiar el JAR construido desde la etapa de compilación
COPY --from=build /home/gradle/project/services/raffle-service/build/libs/*.jar app.jar

# Exponer el puerto en el que corre la aplicación
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
