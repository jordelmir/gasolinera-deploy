# 🚀 DESPLIEGUE COMPLETO EN RENDER.COM

## 📋 PREPARACIÓN PREVIA

### 1. Crear Dockerfiles para cada servicio

```bash
# Crear Dockerfiles optimizados para cada servicio
cd gasolinera-jsm-ultimate

# Auth Service
mkdir -p services/auth-service
cat > services/auth-service/Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiar JAR compilado
COPY services/auth-service/build/libs/auth-service.jar app.jar

# Exponer puerto
EXPOSE 8091

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8091

# Comando de inicio
CMD ["java", "-jar", "app.jar"]
EOF

# API Gateway
cat > services/api-gateway/Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiar JAR compilado
COPY services/api-gateway/build/libs/app.jar app.jar

# Exponer puerto
EXPOSE 8080

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8080

# Comando de inicio
CMD ["java", "-jar", "app.jar"]
EOF

# Station Service
cat > services/station-service/Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiar JAR compilado
COPY services/station-service/build/libs/app.jar app.jar

# Exponer puerto
EXPOSE 8092

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8092

# Comando de inicio
CMD ["java", "-jar", "app.jar"]
EOF

# Coupon Service
cat > services/coupon-service/Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiar JAR compilado
COPY services/coupon-service/build/libs/coupon-service.jar app.jar

# Exponer puerto
EXPOSE 8093

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8093

# Comando de inicio
CMD ["java", "-jar", "app.jar"]
EOF

# Raffle Service
cat > services/raffle-service/Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiar JAR compilado
COPY services/raffle-service/build/libs/app.jar app.jar

# Exponer puerto
EXPOSE 8094

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8094

# Comando de inicio
CMD ["java", "-jar", "app.jar"]
EOF

# Redemption Service
cat > services/redemption-service/Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiar JAR compilado
COPY services/redemption-service/build/libs/app.jar app.jar

# Exponer puerto
EXPOSE 8095

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8095

# Comando de inicio
CMD ["java", "-jar", "app.jar"]
EOF
```

### 2. Configurar render.yaml optimizado

```yaml
# render.yaml - Configuración completa para Render
databases:
  - name: gasolinera-postgres
    databaseName: gasolinera_db
    user: gasolinera_user
    plan: free
    postgresMajorVersion: 16

  - name: gasolinera-redis
    type: redis
    plan: free
    ipAllowList: []

services:
  # API Gateway - Punto de entrada principal
  - type: web
    name: gasolinera-api-gateway
    plan: free
    runtime: docker
    dockerfilePath: ./services/api-gateway/Dockerfile
    dockerContext: .
    buildCommand: './gradlew :services:api-gateway:build -x test'
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SERVER_PORT
        value: 8080
      - key: DATABASE_URL
        fromDatabase:
          name: gasolinera-postgres
          property: connectionString
      - key: REDIS_URL
        fromDatabase:
          name: gasolinera-redis
          property: connectionString

  # Auth Service
  - type: pserv
    name: gasolinera-auth-service
    plan: free
    runtime: docker
    dockerfilePath: ./services/auth-service/Dockerfile
    dockerContext: .
    buildCommand: './gradlew :services:auth-service:build -x test'
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SERVER_PORT
        value: 8091
      - key: DATABASE_URL
        fromDatabase:
          name: gasolinera-postgres
          property: connectionString

  # Station Service
  - type: pserv
    name: gasolinera-station-service
    plan: free
    runtime: docker
    dockerfilePath: ./services/station-service/Dockerfile
    dockerContext: .
    buildCommand: './gradlew :services:station-service:build -x test'
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SERVER_PORT
        value: 8092
      - key: DATABASE_URL
        fromDatabase:
          name: gasolinera-postgres
          property: connectionString

  # Coupon Service
  - type: pserv
    name: gasolinera-coupon-service
    plan: free
    runtime: docker
    dockerfilePath: ./services/coupon-service/Dockerfile
    dockerContext: .
    buildCommand: './gradlew :services:coupon-service:build -x test'
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: SERVER_PORT
        value: 8093
      - key: DATABASE_URL
        fromDatabase:
          name: gasolinera-postgres
          property: connectionString

  # Frontend Admin
  - type: web
    name: gasolinera-admin-frontend
    plan: free
    runtime: node
    buildCommand: 'cd apps/admin && npm install && npm run build'
    startCommand: 'cd apps/admin && npm start'
    envVars:
      - key: NEXT_PUBLIC_API_URL
        value: https://gasolinera-api-gateway.onrender.com
      - key: NODE_ENV
        value: production
```

## 🚀 PROCESO DE DESPLIEGUE

### Paso 1: Preparar el repositorio

```bash
# 1. Compilar todos los servicios
./gradlew build -x :integration-tests:test --no-daemon

# 2. Verificar que los JARs existen
ls -la services/*/build/libs/

# 3. Commit y push a GitHub
git add .
git commit -m "feat: Add Dockerfiles and Render configuration for deployment"
git push origin main
```

### Paso 2: Configurar Render

1. **Ir a [render.com](https://render.com)**
2. **Conectar repositorio GitHub**
3. **Crear nuevo Blueprint**
4. **Seleccionar `render.yaml`**
5. **Configurar variables de entorno**

### Paso 3: URLs de acceso

- **API Gateway**: `https://gasolinera-api-gateway.onrender.com`
- **Admin Frontend**: `https://gasolinera-admin-frontend.onrender.com`
- **Health Check**: `https://gasolinera-api-gateway.onrender.com/health`

## 🔧 CONFIGURACIÓN DE PRODUCCIÓN

### Variables de entorno críticas:

```bash
# Base de datos
DATABASE_URL=postgresql://user:pass@host:port/db
REDIS_URL=redis://host:port

# Seguridad
JWT_SECRET=your-super-secret-key
ENCRYPTION_KEY=your-encryption-key

# APIs externas
TWILIO_ACCOUNT_SID=your-twilio-sid
TWILIO_AUTH_TOKEN=your-twilio-token

# Monitoreo
SENTRY_DSN=your-sentry-dsn
```

## 📊 MONITOREO Y LOGS

### Comandos útiles:

```bash
# Ver logs en tiempo real
render logs --service gasolinera-api-gateway --tail

# Verificar estado de servicios
curl https://gasolinera-api-gateway.onrender.com/actuator/health

# Métricas de rendimiento
curl https://gasolinera-api-gateway.onrender.com/actuator/metrics
```

## 🎯 PRÓXIMOS PASOS

1. **Configurar dominio personalizado**
2. **Implementar CI/CD con GitHub Actions**
3. **Configurar alertas de monitoreo**
4. **Optimizar rendimiento**
5. **Implementar backup automático**
