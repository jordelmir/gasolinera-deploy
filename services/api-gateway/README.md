# API Gateway

## 📋 Descripción

API Gateway para el sistema Gasolinera JSM Ultimate, construido con **Spring Cloud Gateway**. Actúa como punto de entrada único para todos los microservicios, manejando routing, autenticación, logging y circuit breaker patterns.

## 🏗️ Arquitectura

Este servicio implementa el patrón **API Gateway** usando Spring Cloud Gateway:

```
src/main/kotlin/com/gasolinerajsm/apigateway/
├── config/
│   ├── GatewayConfig.kt        # Configuración de rutas
│   └── SecurityConfig.kt       # Configuración de seguridad
├── filter/
│   └── LoggingFilter.kt        # Filtro global de logging
├── controller/
│   └── FallbackController.kt   # Controladores de fallback
└── ApiGatewayApplication.kt    # Aplicación principal
```

## 🚀 Funcionalidades

### Routing Inteligente

- **Auth Service**: `/auth/**` → `http://auth-service:8081`
- **Coupon Service**: `/coupons/**` → `http://coupon-service:8084`
- **Station Service**: `/api/v1/stations/**` → `http://station-service:8083`
- **Ad Engine**: `/ads/**`, `/campaigns/**` → `http://ad-engine:8082`
- **Raffle Service**: `/raffles/**` → `http://raffle-service:8085`

### Características Avanzadas

- ✅ **Circuit Breaker** con Resilience4j
- ✅ **Request/Response Logging** con correlation IDs
- ✅ **CORS Configuration** para desarrollo
- ✅ **JWT Authentication** (OAuth2 Resource Server)
- ✅ **Fallback Controllers** para servicios no disponibles
- ✅ **Health Checks** y métricas Prometheus

## 🔧 Configuración

### Variables de Entorno

```bash
# Perfiles
SPRING_PROFILES_ACTIVE=dev

# JWT Configuration
JWT_ISSUER_URI=http://auth-service:8081
JWT_JWK_SET_URI=http://auth-service:8081/.well-known/jwks.json

# Service URLs (opcional, usa defaults)
AUTH_SERVICE_URL=http://auth-service:8081
COUPON_SERVICE_URL=http://coupon-service:8084
STATION_SERVICE_URL=http://station-service:8083
AD_ENGINE_URL=http://ad-engine:8082
RAFFLE_SERVICE_URL=http://raffle-service:8085
```

### Configuración de Circuit Breaker

```yaml
resilience4j:
  circuitbreaker:
    instances:
      ad-engine-cb:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 5s
```

## 🏃‍♂️ Ejecución

### Desarrollo Local

```bash
# Compilar
gradle :services:api-gateway:build

# Ejecutar
gradle :services:api-gateway:bootRun

# Con Docker
docker-compose -f docker-compose.dev.yml up api-gateway
```

### Testing

```bash
# Ejecutar tests
gradle :services:api-gateway:test

# Health check
curl http://localhost:8080/actuator/health
```

## 📡 Endpoints

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Routing Examples

#### Autenticación

```bash
# Login
curl -X POST http://localhost:8080/auth/login/admin \\
  -H \"Content-Type: application/json\" \\
  -d '{\"username\": \"admin\", \"password\": \"password\"}'
```

#### Cupones (requiere autenticación)

```bash
# Listar cupones
curl -H \"Authorization: Bearer <token>\" \\
  http://localhost:8080/coupons

# Generar cupón
curl -X POST http://localhost:8080/coupons/generate \\
  -H \"Authorization: Bearer <token>\" \\
  -H \"Content-Type: application/json\" \\
  -d '{\"type\": \"DISCOUNT\", \"value\": 10}'
```

#### Estaciones (requiere autenticación)

```bash
# Listar estaciones
curl -H \"Authorization: Bearer <token>\" \\
  http://localhost:8080/api/v1/stations

# Buscar estaciones cercanas
curl -H \"Authorization: Bearer <token>\" \\
  \"http://localhost:8080/api/v1/stations/nearby?latitude=9.9281&longitude=-84.1402&radiusKm=10\"
```

### Fallback Endpoints

Cuando los servicios no están disponibles:

```bash
# Fallback para Ad Engine
curl http://localhost:8080/fallback/ads

# Fallback para Raffle Service
curl http://localhost:8080/fallback/raffles
```

## 🔒 Seguridad

### Endpoints Públicos

- `/auth/**` - Autenticación
- `/actuator/health` - Health checks
- `/fallback/**` - Fallbacks

### Endpoints Protegidos

- `/coupons/**` - Requiere autenticación
- `/api/v1/stations/**` - Requiere autenticación
- `/ads/**` - Requiere autenticación
- `/campaigns/**` - Requiere rol ADMIN
- `/raffles/**` - Requiere autenticación

### JWT Configuration

El gateway valida tokens JWT usando OAuth2 Resource Server:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://auth-service:8081
          jwk-set-uri: http://auth-service:8081/.well-known/jwks.json
```

## 📊 Monitoreo

### Actuator Endpoints

```bash
# Health
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# Prometheus
curl http://localhost:8080/actuator/prometheus

# Gateway routes
curl http://localhost:8080/actuator/gateway/routes
```

### Logging

El gateway incluye logging estructurado con correlation IDs:

```
2025-08-20 10:30:15 - Gateway Request - ID: abc123, Method: GET, URI: /coupons
2025-08-20 10:30:16 - Gateway Response - ID: abc123, Status: 200, Duration: 150ms
```

### Circuit Breaker Metrics

```bash
# Estado de circuit breakers
curl http://localhost:8080/actuator/health/circuitBreakers
```

## 🔄 Circuit Breaker

### Configuración

- **Sliding Window**: 10 requests
- **Minimum Calls**: 5 requests
- **Failure Rate**: 50%
- **Wait Duration**: 5 segundos

### Estados

1. **CLOSED**: Funcionamiento normal
2. **OPEN**: Servicio no disponible, usa fallback
3. **HALF_OPEN**: Probando si el servicio se recuperó

### Fallbacks

Cuando un servicio no está disponible, el gateway retorna:

```json
{
  \"message\": \"Ad Engine service is temporarily unavailable\",
  \"service\": \"ad-engine\",
  \"timestamp\": \"2025-08-20T10:30:15\",
  \"fallbackData\": {
    \"ads\": [],
    \"campaigns\": []
  }
}
```

## 🐛 Troubleshooting

### Problemas Comunes

1. **Service Unavailable (503)**

   ```bash
   # Verificar que los servicios downstream estén corriendo
   curl http://auth-service:8081/actuator/health
   curl http://coupon-service:8084/actuator/health
   ```

2. **Unauthorized (401)**

   ```bash
   # Verificar token JWT
   curl -H \"Authorization: Bearer <token>\" http://localhost:8080/auth/validate
   ```

3. **Circuit Breaker Open**
   ```bash
   # Verificar estado del circuit breaker
   curl http://localhost:8080/actuator/health/circuitBreakers
   ```

### Logs Útiles

```bash
# Ver logs del gateway
docker logs api-gateway

# Filtrar por correlation ID
docker logs api-gateway | grep \"ID: abc123\"
```

## 🧪 Testing

### Ejemplos de Tests

```bash
# Test de routing básico
curl http://localhost:8080/actuator/health

# Test de autenticación
curl -X POST http://localhost:8080/auth/login/admin \\
  -H \"Content-Type: application/json\" \\
  -d '{\"username\": \"admin\", \"password\": \"password\"}'

# Test de circuit breaker (simular falla)
# Detener ad-engine y hacer requests a /ads
curl http://localhost:8080/ads
```

### Load Testing

```bash
# Usar Apache Bench
ab -n 1000 -c 10 http://localhost:8080/actuator/health

# Usar curl en loop
for i in {1..100}; do
  curl -s http://localhost:8080/actuator/health > /dev/null
  echo \"Request $i completed\"
done
```

## 📝 TODO

- [x] Implementar Spring Cloud Gateway
- [x] Configurar routing básico
- [x] Implementar JWT authentication
- [x] Agregar circuit breaker
- [x] Implementar logging con correlation IDs
- [x] Crear fallback controllers
- [x] Configurar CORS
- [x] Agregar health checks
- [ ] Implementar rate limiting
- [ ] Agregar cache distribuido
- [ ] Implementar service discovery
- [ ] Agregar tests unitarios
- [ ] Implementar métricas custom
- [ ] Agregar distributed tracing

## 🏆 Características Técnicas

### Patrones Implementados

- **API Gateway Pattern**: Punto de entrada único
- **Circuit Breaker Pattern**: Resilience4j
- **Correlation ID Pattern**: Trazabilidad de requests
- **Fallback Pattern**: Degradación elegante

### Performance

- **Reactive Stack**: WebFlux para alta concurrencia
- **Connection Pooling**: Configurado para 100 conexiones
- **Timeouts**: 5s connect, 30s response
- **Circuit Breaker**: Protección contra cascading failures

### Observabilidad

- **Structured Logging**: JSON con correlation IDs
- **Prometheus Metrics**: Métricas de gateway y circuit breaker
- **Health Checks**: Endpoint y downstream services
- **Actuator**: Endpoints de management completos

## 🌐 Integración con Servicios

### Auth Service

- **Endpoint**: `/auth/**`
- **Público**: Sí
- **Circuit Breaker**: No (crítico)

### Coupon Service

- **Endpoint**: `/coupons/**`
- **Autenticación**: JWT requerido
- **Circuit Breaker**: No (estable)

### Station Service

- **Endpoint**: `/api/v1/stations/**`
- **Autenticación**: JWT requerido
- **Circuit Breaker**: No (estable)

### Ad Engine

- **Endpoint**: `/ads/**`, `/campaigns/**`
- **Autenticación**: JWT requerido
- **Circuit Breaker**: Sí (puede fallar)
- **Fallback**: `/fallback/ads`

### Raffle Service

- **Endpoint**: `/raffles/**`
- **Autenticación**: JWT requerido
- **Circuit Breaker**: Sí (puede fallar)
- **Fallback**: `/fallback/raffles`

---

**Preparado por**: Kiro AI Assistant
**Versión**: 1.0.0
**Fecha**: 20 de Agosto, 2025
