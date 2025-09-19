# 🚀 DESPLIEGUE HÍBRIDO: VERCEL + RAILWAY

## 🎯 ESTRATEGIA HÍBRIDA

**Frontend en Vercel** (Optimizado para Next.js)
**Backend en Railway** (Optimizado para microservicios)

## 📋 PARTE 1: BACKEND EN RAILWAY

### 1. Preparar Railway

```bash
# Instalar Railway CLI
npm install -g @railway/cli

# Login
railway login

# Crear proyecto
railway new gasolinera-jsm-backend
```

### 2. Configurar railway.json

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile.railway"
  },
  "deploy": {
    "numReplicas": 1,
    "sleepApplication": false,
    "restartPolicyType": "ON_FAILURE"
  }
}
```

### 3. Dockerfile optimizado para Railway

```dockerfile
# Dockerfile.railway
FROM gradle:8.8-jdk17 AS builder

WORKDIR /app
COPY . .

# Compilar todos los servicios
RUN ./gradlew build -x :integration-tests:test --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiar JARs compilados
COPY --from=builder /app/services/*/build/libs/*.jar ./

# Script de inicio que detecta el servicio
COPY start-service.sh ./
RUN chmod +x start-service.sh

# Variables de entorno
ENV SPRING_PROFILES_ACTIVE=production

# Puerto dinámico de Railway
EXPOSE $PORT

CMD ["./start-service.sh"]
```

### 4. Script de inicio inteligente

```bash
# start-service.sh
#!/bin/bash

# Detectar servicio por variable de entorno
case "$SERVICE_NAME" in
  "api-gateway")
    java -jar app.jar --server.port=$PORT
    ;;
  "auth-service")
    java -jar auth-service.jar --server.port=$PORT
    ;;
  "station-service")
    java -jar app.jar --server.port=$PORT
    ;;
  *)
    echo "Servicio no reconocido: $SERVICE_NAME"
    exit 1
    ;;
esac
```

### 5. Desplegar servicios

```bash
# API Gateway
railway up --service api-gateway
railway variables set SERVICE_NAME=api-gateway

# Auth Service
railway up --service auth-service
railway variables set SERVICE_NAME=auth-service

# Otros servicios...
```

## 📋 PARTE 2: FRONTEND EN VERCEL

### 1. Configurar vercel.json optimizado

```json
{
  "version": 2,
  "builds": [
    {
      "src": "apps/admin/package.json",
      "use": "@vercel/next",
      "config": {
        "projectSettings": {
          "framework": "nextjs"
        }
      }
    },
    {
      "src": "apps/owner-dashboard/package.json",
      "use": "@vercel/next",
      "config": {
        "projectSettings": {
          "framework": "nextjs"
        }
      }
    }
  ],
  "routes": [
    {
      "src": "/admin/(.*)",
      "dest": "/apps/admin/$1"
    },
    {
      "src": "/dashboard/(.*)",
      "dest": "/apps/owner-dashboard/$1"
    }
  ]
}
```

### 2. Configurar variables de entorno en Vercel

```bash
# Admin Dashboard
NEXT_PUBLIC_API_URL=https://gasolinera-api-gateway.up.railway.app
NEXT_PUBLIC_AUTH_URL=https://gasolinera-auth-service.up.railway.app

# Owner Dashboard
NEXT_PUBLIC_API_URL=https://gasolinera-api-gateway.up.railway.app
NEXT_PUBLIC_STATION_URL=https://gasolinera-station-service.up.railway.app
```

### 3. Optimizar Next.js para producción

```javascript
// apps/admin/next.config.js
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  experimental: {
    outputFileTracingRoot: path.join(__dirname, '../../'),
  },
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_URL}/api/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
```

## 🔗 CONEXIÓN FRONTEND-BACKEND

### 1. Configurar CORS en backend

```kotlin
// services/api-gateway/src/main/kotlin/config/CorsConfig.kt
@Configuration
@EnableWebMvc
class CorsConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "https://gasolinera-admin.vercel.app",
                "https://gasolinera-dashboard.vercel.app",
                "http://localhost:3000"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}
```

### 2. Cliente API optimizado

```typescript
// packages/shared/src/api/client.ts
class ApiClient {
  private baseURL: string;

  constructor() {
    this.baseURL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  }

  async request<T>(endpoint: string, options?: RequestInit): Promise<T> {
    const url = `${this.baseURL}${endpoint}`;

    const response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    });

    if (!response.ok) {
      throw new Error(`API Error: ${response.status}`);
    }

    return response.json();
  }
}

export const apiClient = new ApiClient();
```

## 🚀 COMANDOS DE DESPLIEGUE

### Backend (Railway)

```bash
# Desplegar API Gateway
railway up --service api-gateway

# Desplegar Auth Service
railway up --service auth-service

# Ver logs
railway logs --service api-gateway
```

### Frontend (Vercel)

```bash
# Desplegar Admin
vercel --prod --cwd apps/admin

# Desplegar Dashboard
vercel --prod --cwd apps/owner-dashboard

# Ver deployments
vercel ls
```

## 📊 URLs FINALES

### Backend (Railway)

- **API Gateway**: `https://gasolinera-api-gateway.up.railway.app`
- **Auth Service**: `https://gasolinera-auth-service.up.railway.app`
- **Health Check**: `https://gasolinera-api-gateway.up.railway.app/health`

### Frontend (Vercel)

- **Admin Dashboard**: `https://gasolinera-admin.vercel.app`
- **Owner Dashboard**: `https://gasolinera-dashboard.vercel.app`

## 🎯 VENTAJAS DE ESTA ESTRATEGIA

✅ **Frontend ultra-rápido** con Vercel CDN global
✅ **Backend escalable** con Railway
✅ **SSL automático** en ambas plataformas
✅ **Despliegue independiente** de frontend y backend
✅ **Monitoreo integrado** en ambas plataformas
