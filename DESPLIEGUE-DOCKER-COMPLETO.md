# 🐳 DESPLIEGUE COMPLETO CON DOCKER

## 🎯 ESTRATEGIA DOCKER PROFESIONAL

Despliegue completo usando Docker Compose con múltiples entornos.

## 📋 PREPARACIÓN DE CONTENEDORES

### 1. Dockerfile multi-stage optimizado para servicios

```dockerfile
# docker/Dockerfile.service
FROM gradle:8.8-jdk17 AS builder

WORKDIR /app
COPY . .

# Compilar servicio específico
ARG SERVICE_NAME
RUN ./gradlew :services:${SERVICE_NAME}:build -x test --no-daemon

# Runtime optimizado
FROM openjdk:17-jdk-slim

# Instalar herramientas de monitoreo
RUN apt-get update && apt-get install -y \
    curl \
    jq \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copiar JAR compilado
ARG SERVICE_NAME
COPY --from=builder /app/services/${SERVICE_NAME}/build/libs/*.jar app.jar

# Usuario no-root para seguridad
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
RUN chown -R appuser:appgroup /app
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

# Variables de entorno
ENV SPRING_PROFILES_ACTIVE=docker
ENV JVM_OPTS="-Xmx512m -Xms256m"

# Puerto dinámico
EXPOSE ${SERVER_PORT:-8080}

# Comando optimizado
CMD java $JVM_OPTS -jar app.jar
```

### 2. Dockerfile para Frontend (Next.js)

```dockerfile
# apps/admin/Dockerfile
FROM node:18-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

FROM node:18-alpine AS builder
WORKDIR /app
COPY . .
COPY --from=deps /app/node_modules ./node_modules
RUN npm run build

FROM node:18-alpine AS runner
WORKDIR /app

ENV NODE_ENV production

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000
ENV PORT 3000

CMD ["node", "server.js"]
```

## 🚀 DOCKER COMPOSE COMPLETO

### docker-compose.production.yml

```yaml
version: '3.8'

networks:
  gasolinera-network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:
  prometheus_data:
  grafana_data:

services:
  # ===== INFRAESTRUCTURA =====
  postgres:
    image: postgres:15-alpine
    container_name: gasolinera-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-gasolinera_db}
      POSTGRES_USER: ${POSTGRES_USER:-gasolinera_user}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-gasolinera_password}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    ports:
      - '5432:5432'
    networks:
      - gasolinera-network
    healthcheck:
      test: ['CMD-SHELL', 'pg_isready -U ${POSTGRES_USER:-gasolinera_user}']
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: gasolinera-redis
    volumes:
      - redis_data:/data
    ports:
      - '6379:6379'
    networks:
      - gasolinera-network
    healthcheck:
      test: ['CMD', 'redis-cli', 'ping']
      interval: 10s
      timeout: 3s
      retries: 5
    restart: unless-stopped

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    container_name: gasolinera-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME:-gasolinera_user}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-gasolinera_password}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    ports:
      - '5672:5672'
      - '15672:15672'
    networks:
      - gasolinera-network
    healthcheck:
      test: ['CMD', 'rabbitmq-diagnostics', 'ping']
      interval: 30s
      timeout: 10s
      retries: 5
    restart: unless-stopped

  # ===== MICROSERVICIOS =====
  api-gateway:
    build:
      context: .
      dockerfile: docker/Dockerfile.service
      args:
        SERVICE_NAME: api-gateway
    container_name: gasolinera-api-gateway
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=8080
      - DATABASE_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB:-gasolinera_db}
      - REDIS_HOST=redis
      - RABBITMQ_HOST=rabbitmq
    ports:
      - '8080:8080'
    networks:
      - gasolinera-network
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped

  auth-service:
    build:
      context: .
      dockerfile: docker/Dockerfile.service
      args:
        SERVICE_NAME: auth-service
    container_name: gasolinera-auth-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=8091
      - DATABASE_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB:-gasolinera_db}
      - REDIS_HOST=redis
    ports:
      - '8091:8091'
    networks:
      - gasolinera-network
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped

  station-service:
    build:
      context: .
      dockerfile: docker/Dockerfile.service
      args:
        SERVICE_NAME: station-service
    container_name: gasolinera-station-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=8092
      - DATABASE_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB:-gasolinera_db}
    ports:
      - '8092:8092'
    networks:
      - gasolinera-network
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  coupon-service:
    build:
      context: .
      dockerfile: docker/Dockerfile.service
      args:
        SERVICE_NAME: coupon-service
    container_name: gasolinera-coupon-service
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SERVER_PORT=8093
      - DATABASE_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB:-gasolinera_db}
    ports:
      - '8093:8093'
    networks:
      - gasolinera-network
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

  # ===== FRONTEND =====
  admin-frontend:
    build:
      context: ./apps/admin
      dockerfile: Dockerfile
    container_name: gasolinera-admin-frontend
    environment:
      - NEXT_PUBLIC_API_URL=http://api-gateway:8080
      - NODE_ENV=production
    ports:
      - '3000:3000'
    networks:
      - gasolinera-network
    depends_on:
      - api-gateway
    restart: unless-stopped

  owner-dashboard:
    build:
      context: ./apps/owner-dashboard
      dockerfile: Dockerfile
    container_name: gasolinera-owner-dashboard
    environment:
      - NEXT_PUBLIC_API_URL=http://api-gateway:8080
      - NODE_ENV=production
    ports:
      - '3001:3000'
    networks:
      - gasolinera-network
    depends_on:
      - api-gateway
    restart: unless-stopped

  # ===== MONITOREO =====
  prometheus:
    image: prom/prometheus:latest
    container_name: gasolinera-prometheus
    volumes:
      - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    ports:
      - '9090:9090'
    networks:
      - gasolinera-network
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: gasolinera-grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards
    ports:
      - '3002:3000'
    networks:
      - gasolinera-network
    depends_on:
      - prometheus
    restart: unless-stopped

  # ===== REVERSE PROXY =====
  nginx:
    image: nginx:alpine
    container_name: gasolinera-nginx
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
    ports:
      - '80:80'
      - '443:443'
    networks:
      - gasolinera-network
    depends_on:
      - api-gateway
      - admin-frontend
      - owner-dashboard
    restart: unless-stopped
```

## 🔧 CONFIGURACIÓN NGINX

### nginx/nginx.conf

```nginx
events {
    worker_connections 1024;
}

http {
    upstream api_backend {
        server api-gateway:8080;
    }

    upstream admin_frontend {
        server admin-frontend:3000;
    }

    upstream owner_dashboard {
        server owner-dashboard:3000;
    }

    # Redirect HTTP to HTTPS
    server {
        listen 80;
        server_name _;
        return 301 https://$host$request_uri;
    }

    # Main API Gateway
    server {
        listen 443 ssl;
        server_name api.gasolinerajsm.com;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;

        location / {
            proxy_pass http://api_backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }

    # Admin Frontend
    server {
        listen 443 ssl;
        server_name admin.gasolinerajsm.com;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;

        location / {
            proxy_pass http://admin_frontend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }

    # Owner Dashboard
    server {
        listen 443 ssl;
        server_name dashboard.gasolinerajsm.com;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;

        location / {
            proxy_pass http://owner_dashboard;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
```

## 🚀 COMANDOS DE DESPLIEGUE

### Desarrollo

```bash
# Compilar servicios
./gradlew build -x :integration-tests:test --no-daemon

# Iniciar todo el stack
docker-compose -f docker-compose.production.yml up -d

# Ver logs
docker-compose -f docker-compose.production.yml logs -f

# Escalar servicios
docker-compose -f docker-compose.production.yml up -d --scale auth-service=3
```

### Producción

```bash
# Build optimizado
docker-compose -f docker-compose.production.yml build --no-cache

# Deploy con rolling update
docker-compose -f docker-compose.production.yml up -d --force-recreate

# Health check
curl -f http://localhost:8080/actuator/health
```

## 📊 URLS DE ACCESO

- **API Gateway**: `https://api.gasolinerajsm.com`
- **Admin Panel**: `https://admin.gasolinerajsm.com`
- **Owner Dashboard**: `https://dashboard.gasolinerajsm.com`
- **Grafana**: `https://monitoring.gasolinerajsm.com`
- **Prometheus**: `https://metrics.gasolinerajsm.com`

## 🎯 VENTAJAS DOCKER

✅ **Entorno consistente** en desarrollo y producción
✅ **Escalabilidad horizontal** fácil
✅ **Rollback rápido** con versiones de imagen
✅ **Monitoreo integrado** con Prometheus/Grafana
✅ **SSL automático** con Let's Encrypt
✅ **Alta disponibilidad** con health checks
