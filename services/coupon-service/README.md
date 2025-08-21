# Coupon Service

## 📋 Descripción

Servicio de gestión de cupones QR para el sistema Gasolinera JSM Ultimate. Maneja la generación, validación y canje de cupones QR, así como el seguimiento de estadísticas de uso.

## 🏗️ Arquitectura

Este servicio sigue los principios de **Arquitectura Hexagonal (Ports & Adapters)**:

```
src/main/kotlin/com/gasolinerajsm/coupon/
├── controller/          # Adapters - REST Controllers
├── service/            # Application Layer - Business Logic
├── repository/         # Ports - Data Access Interfaces
├── model/             # Domain - Entities
├── dto/               # Data Transfer Objects
└── config/            # Configuration
```

## 🚀 Funcionalidades

### Gestión de Cupones QR

- **POST** `/coupons/generate` - Generar nuevo cupón QR
- **GET** `/coupons/{id}` - Obtener información del cupón
- **POST** `/coupons/{id}/redeem` - Canjear cupón
- **GET** `/coupons/station/{stationId}` - Cupones por estación

### Estadísticas

- **GET** `/coupons/stats` - Estadísticas generales
- **GET** `/coupons/stats/station/{stationId}` - Stats por estación

## 🔧 Configuración

### Variables de Entorno

```bash
# Base de datos
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=puntog
POSTGRES_USER=puntog
POSTGRES_PASSWORD=changeme

# Redis
REDIS_HOST=localhost

# JWT
JWT_SECRET=your-secret-key

# QR Configuration
QR_EXPIRATION_MINUTES=15
```

### Perfil de Desarrollo

```yaml
spring:
  profiles:
    active: dev
```

## 🏃‍♂️ Ejecución

### Desarrollo Local

```bash
# Compilar
gradle :services:coupon-service:build

# Ejecutar
gradle :services:coupon-service:bootRun

# Con Docker
docker-compose -f docker-compose.dev.yml up coupon-service
```

### Testing

```bash
# Ejecutar tests
gradle :services:coupon-service:test

# Coverage
gradle :services:coupon-service:jacocoTestReport
```

## 📡 Endpoints

### Health Check

```bash
curl http://localhost:8084/actuator/health
```

### Generar Cupón QR

```bash
curl -X POST http://localhost:8084/coupons/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "stationId": "station-123",
    "dispenserId": "dispenser-456",
    "amount": 50.00,
    "fuelType": "REGULAR"
  }'
```

### Obtener Cupón

```bash
curl http://localhost:8084/coupons/qr-12345 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Canjear Cupón

```bash
curl -X POST http://localhost:8084/coupons/qr-12345/redeem \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "employeeId": "emp-789",
    "actualAmount": 50.00
  }'
```

## 🔒 Seguridad

- Autenticación JWT requerida para todos los endpoints
- Validación de permisos por estación
- Tokens QR únicos con expiración
- Logging de todas las operaciones de canje

## 📊 Monitoreo

- **Actuator**: `/actuator/health`, `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`
- **OpenTelemetry**: Trazabilidad distribuida habilitada

## 🎯 Características Técnicas

### Generación de QR

- Códigos QR únicos con timestamp y nonce
- Expiración configurable (default: 15 minutos)
- Formato: `qr-{timestamp}-{nonce}`

### Validación

- Verificación de expiración
- Validación de integridad del token
- Prevención de doble canje

### Estadísticas

- Cupones generados por período
- Tasa de canje por estación
- Análisis de uso por empleado

## 🐛 Troubleshooting

### Problemas Comunes

1. **Error de conexión a PostgreSQL**

   ```bash
   # Verificar que PostgreSQL esté corriendo
   docker-compose -f docker-compose.dev.yml up postgres
   ```

2. **Error de conexión a Redis**

   ```bash
   # Verificar que Redis esté corriendo
   docker-compose -f docker-compose.dev.yml up redis
   ```

3. **QR Code expirado**

   - Verificar configuración de `QR_EXPIRATION_MINUTES`
   - Revisar logs para timestamp de generación

4. **Error de canje duplicado**
   - El sistema previene doble canje automáticamente
   - Revisar estado del cupón en base de datos

## 📝 TODO

- [ ] Implementar notificaciones push para cupones próximos a expirar
- [ ] Agregar soporte para cupones promocionales
- [ ] Implementar sistema de puntos/recompensas
- [ ] Agregar tests de integración
- [ ] Documentar con OpenAPI/Swagger
- [ ] Implementar cache distribuido para mejor performance
