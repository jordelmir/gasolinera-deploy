# Auth Service

## 📋 Descripción

Servicio de autenticación para el sistema Gasolinera JSM Ultimate. Maneja la autenticación de usuarios mediante OTP (SMS), login de administradores y generación de tokens JWT.

## 🏗️ Arquitectura

Este servicio sigue los principios de **Arquitectura Hexagonal (Ports & Adapters)**:

```
src/main/kotlin/com/gasolinerajsm/authservice/
├── controller/          # Adapters - REST Controllers
├── service/            # Application Layer - Business Logic
├── repository/         # Ports - Data Access Interfaces
├── model/             # Domain - Entities
├── dto/               # Data Transfer Objects
└── exception/         # Exception Handling
```

## 🚀 Funcionalidades

### Autenticación por OTP

- **POST** `/auth/otp/request` - Solicitar código OTP
- **POST** `/auth/otp/verify` - Verificar OTP y obtener tokens

### Autenticación de Administradores

- **POST** `/auth/login/admin` - Login de administradores
- **POST** `/auth/login/advertiser` - Login de anunciantes

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
JWT_REFRESH_SECRET=your-refresh-secret-key

# Credenciales por defecto
ADMIN_EMAIL=admin@puntog.com
ADMIN_PASSWORD=admin123
ADVERTISER_EMAIL=anunciante@tosty.com
ADVERTISER_PASSWORD=tosty123
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
gradle :services:auth-service:build

# Ejecutar
gradle :services:auth-service:bootRun

# Con Docker
docker-compose -f docker-compose.dev.yml up auth-service
```

### Testing

```bash
# Ejecutar tests
gradle :services:auth-service:test

# Coverage
gradle :services:auth-service:jacocoTestReport
```

## 📡 Endpoints

### Health Check

```bash
curl http://localhost:8081/actuator/health
```

### Solicitar OTP

```bash
curl -X POST http://localhost:8081/auth/otp/request \
  -H "Content-Type: application/json" \
  -d '{"phone": "+50612345678"}'
```

### Verificar OTP

```bash
curl -X POST http://localhost:8081/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone": "+50612345678", "code": "123456"}'
```

### Login Admin

```bash
curl -X POST http://localhost:8081/auth/login/admin \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@puntog.com", "pass": "admin123"}'
```

## 🔒 Seguridad

- Tokens JWT con expiración (15 minutos para access, 7 días para refresh)
- OTP con expiración de 5 minutos
- Validación de entrada con Bean Validation
- Logging de eventos de seguridad

## 📊 Monitoreo

- **Actuator**: `/actuator/health`, `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`
- **OpenTelemetry**: Trazabilidad distribuida habilitada

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

3. **Token JWT inválido**
   - Verificar que JWT_SECRET esté configurado
   - Verificar que el token no haya expirado

## 📝 TODO

- [ ] Implementar envío real de SMS para OTP
- [ ] Agregar rate limiting para endpoints de OTP
- [ ] Implementar refresh token rotation
- [ ] Agregar tests de integración
- [ ] Documentar con OpenAPI/Swagger
