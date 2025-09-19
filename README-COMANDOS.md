# Comandos para Ejecutar Servicios - Gasolinera JSM

## 🚀 Inicio Rápido

### 1. Iniciar Base de Datos
```bash
# Iniciar PostgreSQL con Docker
docker-compose -f docker-compose.dev.yml up -d postgres

# Verificar que las bases de datos se crearon
docker exec gasolinera-postgres-dev psql -U gasolinera_user -d gasolinera_dev -c "\l"
```

### 2. Ejecutar Auth Service
```bash
# Configurar variables de entorno JWT (requeridas)
export JWT_SECRET="mySuperSecretKeyForJWTThatIsAtLeast32CharactersLong"
export JWT_REFRESH_SECRET="mySuperSecretRefreshKeyForJWTThatIsAtLeast32CharactersLong"

# Compilar y ejecutar
cd services/auth-service
./gradlew bootRun

# O desde el directorio raíz
./gradlew :services:auth-service:bootRun

# Ejecutar JAR compilado
java -Dapp.jwt.secret=mySuperSecretKeyForJWTThatIsAtLeast32CharactersLong \
     -Dapp.jwt.refreshSecret=mySuperSecretRefreshKeyForJWTThatIsAtLeast32CharactersLong \
     -Dspring.datasource.url=jdbc:postgresql://localhost:5433/auth_service_db \
     -Dspring.datasource.username=auth_service_user \
     -Dspring.datasource.password=auth_service_password \
     -jar services/auth-service/build/libs/auth-service.jar --server.port=8081
```

### 3. Ejecutar Station Service
```bash
# Compilar y ejecutar
cd services/station-service
./gradlew bootRun

# O desde el directorio raíz
./gradlew :services:station-service:bootRun
```

### 4. Ejecutar Coupon Service
```bash
# Compilar y ejecutar
cd services/coupon-service
./gradlew bootRun

# O desde el directorio raíz
./gradlew :services:coupon-service:bootRun
```

### 5. Ejecutar Frontend (Next.js)
```bash
# Instalar dependencias (si no están instaladas)
npm install

# Ejecutar en modo desarrollo
npm run dev

# O ejecutar owner-dashboard específicamente
npm run dev
```

## 🛠️ Comandos de Desarrollo

### Gradle
```bash
# Compilar todos los servicios
./gradlew build

# Compilar servicio específico
./gradlew :services:auth-service:build
./gradlew :services:station-service:build

# Ejecutar tests
./gradlew test

# Limpiar build
./gradlew clean
```

### Docker
```bash
# Iniciar todos los servicios de infraestructura
docker-compose -f docker-compose.dev.yml up -d

# Detener servicios
docker-compose -f docker-compose.dev.yml down

# Ver logs
docker-compose -f docker-compose.dev.yml logs -f postgres

# Acceder a base de datos
docker exec -it gasolinera-postgres-dev psql -U gasolinera_user -d station_db
```

### Base de Datos
```bash
# Conectar a PostgreSQL
psql -h localhost -p 5432 -U gasolinera_user -d station_db

# Ver tablas
\d

# Ver datos de stations
SELECT * FROM stations;
```

## 📊 Endpoints Disponibles

### Auth Service (Puerto 8080)
- `POST /api/v1/auth/register` - Registrar usuario
- `POST /api/v1/auth/login` - Iniciar sesión
- `POST /api/v1/auth/request-otp` - Solicitar OTP
- `GET /health` - Health check

### Station Service (Puerto 8083)
- `POST /api/v1/stations` - Crear estación
- `GET /api/v1/stations/{id}` - Obtener estación por ID
- `GET /api/v1/stations/code/{code}` - Obtener estación por código
- `GET /api/v1/stations` - Listar todas las estaciones
- `GET /api/v1/stations/active` - Listar estaciones activas
- `GET /api/v1/stations/city/{city}` - Listar estaciones por ciudad
- `PUT /api/v1/stations/{id}` - Actualizar estación
- `DELETE /api/v1/stations/{id}` - Eliminar estación

### Coupon Service (Puerto 8082)
- `POST /api/v1/coupons` - Crear cupón
- `GET /api/v1/coupons/{id}` - Obtener cupón por ID
- `GET /api/v1/coupons/code/{code}` - Obtener cupón por código
- `GET /api/v1/coupons` - Listar todos los cupones
- `GET /api/v1/coupons/active` - Listar cupones activos
- `GET /api/v1/coupons/status/{status}` - Listar cupones por estado
- `GET /api/v1/coupons/search?name={name}` - Buscar cupones por nombre
- `PUT /api/v1/coupons/{id}` - Actualizar cupón
- `POST /api/v1/coupons/{id}/use` - Usar cupón
- `DELETE /api/v1/coupons/{id}` - Eliminar cupón

## 🔧 Configuración

### Variables de Entorno
```bash
# Base de datos
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=station_db
POSTGRES_USER=gasolinera_user
POSTGRES_PASSWORD=gasolinera_pass

# Auth Service
JWT_SECRET=your-secret-key
```

### Puertos
- PostgreSQL: 5432
- Auth Service: 8080
- Station Service: 8083
- Coupon Service: 8082
- Frontend: 3000
- RabbitMQ: 5672/15672
- Redis: 6379
- Vault: 8200

## 🐛 Troubleshooting

### Problema: "Connection refused" a base de datos
```bash
# Verificar que PostgreSQL esté corriendo
docker ps | grep postgres

# Reiniciar PostgreSQL
docker-compose -f docker-compose.dev.yml restart postgres
```

### Problema: Puerto ocupado
```bash
# Ver qué proceso usa el puerto
lsof -i :8083

# Matar proceso
kill -9 <PID>
```

### Problema: Error de compilación Kotlin
```bash
# Limpiar cache de Gradle
./gradlew clean build --refresh-dependencies

# Verificar Java version
java -version
```

## 📝 Notas

### ✅ Estado Actual del Proyecto (13/09/2025)

- **Auth-Service**: ✅ Completamente funcional y validado - ejecutándose correctamente en puerto 8081
- **Station-Service**: ✅ Modelos de dominio implementados (Location, StationId, OperatingHours), CRUD básico completado
- **Coupon-Service**: ✅ Errores de compilación corregidos, validaciones críticas implementadas
- **Base de Datos**: ✅ PostgreSQL configurado con Docker (bases: auth_service_db, station_db, coupon_db)
- **Frontend**: ❌ Problema de build en Next.js pendiente (bug conocido en generate-build-id.js)
- **Arquitectura**: ✅ Arquitectura hexagonal implementada en todos los servicios
- **Tecnologías**: Spring Boot con Kotlin, JPA, PostgreSQL, Docker

### 🎯 Próximos Pasos Recomendados

1. **Resolver Frontend**: Investigar y corregir el bug de Next.js (posiblemente actualizar a versión estable)
2. **Integración de Servicios**: Implementar comunicación entre servicios (REST/Feign clients)
3. **Testing**: Agregar tests unitarios e integración para todos los servicios
4. **Seguridad**: Implementar autenticación JWT entre servicios
5. **Monitoreo**: Configurar logging centralizado y métricas
6. **Despliegue**: Crear Docker Compose para todos los servicios
7. **Documentación API**: Generar documentación OpenAPI/Swagger completa

### 🚀 MVP Funcional

Con la implementación actual, tenemos un MVP funcional con:
- Autenticación completa (registro, login, OTP)
- Gestión básica de estaciones
- Sistema de cupones simplificado
- Base de datos persistente
- Arquitectura escalable preparada para crecimiento

### 🔧 Configuración de Producción

Para producción, configurar:
- Variables de entorno seguras para JWT
- Conexiones a base de datos externa
- Configuración de CORS apropiada
- Certificados SSL
- Balanceo de carga
- Monitoreo y alertas