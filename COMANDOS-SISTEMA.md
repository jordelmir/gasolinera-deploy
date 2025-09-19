# 🎮 **COMANDOS DE GESTIÓN - SISTEMA GASOLINERA JSM**

## **🚀 COMANDOS PRINCIPALES**

### **Iniciar Sistema Completo**

```bash
# Iniciar infraestructura (PostgreSQL, Redis, RabbitMQ)
docker-compose -f docker-compose.simple.yml up -d

# Iniciar todos los servicios con puertos corregidos
./start-services-fixed.sh
```

### **Verificar Estado del Sistema**

```bash
# Verificación completa del sistema
./verify-system.sh

# Verificación rápida del API Gateway
curl http://localhost:8080/health
```

### **Detener Sistema**

```bash
# Detener servicios de aplicación
./stop-services.sh

# Detener infraestructura
docker-compose -f docker-compose.simple.yml down
```

---

## **🔍 COMANDOS DE VERIFICACIÓN**

### **Health Checks Individuales**

```bash
# API Gateway
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health

# Servicios Backend
curl http://localhost:8091/actuator/health  # Auth Service
curl http://localhost:8092/actuator/health  # Station Service
curl http://localhost:8093/actuator/health  # Coupon Service
curl http://localhost:8094/actuator/health  # Raffle Service
curl http://localhost:8095/actuator/health  # Redemption Service
curl http://localhost:8096/actuator/health  # Ad Engine
curl http://localhost:8097/actuator/health  # Message Improver
```

### **Verificar Procesos Activos**

```bash
# Ver todos los procesos Java de Gasolinera
ps aux | grep java | grep gasolinera

# Ver puertos ocupados
lsof -i :8080-8100

# Ver estado de contenedores Docker
docker-compose -f docker-compose.simple.yml ps
```

### **Verificar Routing del API Gateway**

```bash
# Probar routing a través del API Gateway
curl http://localhost:8080/api/auth/actuator/health
curl http://localhost:8080/api/coupons/actuator/health
curl http://localhost:8080/api/stations/actuator/health
curl http://localhost:8080/api/raffles/actuator/health
```

---

## **📝 COMANDOS DE LOGS**

### **Ver Logs en Tiempo Real**

```bash
# API Gateway
tail -f logs/api-gateway.log

# Servicios específicos
tail -f logs/auth-service.log
tail -f logs/station-service.log
tail -f logs/coupon-service.log
tail -f logs/raffle-service.log
tail -f logs/redemption-service.log
tail -f logs/ad-engine.log
tail -f logs/message-improver.log
```

### **Ver Logs de Infraestructura**

```bash
# PostgreSQL
docker-compose -f docker-compose.simple.yml logs postgres

# Redis
docker-compose -f docker-compose.simple.yml logs redis

# RabbitMQ
docker-compose -f docker-compose.simple.yml logs rabbitmq
```

---

## **🔧 COMANDOS DE DESARROLLO**

### **Compilar Servicios**

```bash
# Compilar todos los servicios
./gradlew build -x test

# Compilar servicio específico
./gradlew :services:api-gateway:build -x test
./gradlew :services:auth-service:build -x test
```

### **Reiniciar Servicio Específico**

```bash
# Ejemplo: Reiniciar API Gateway
pkill -f "api-gateway"
nohup java -jar "services/api-gateway/build/libs/app.jar" \
    --server.port=8080 \
    --spring.main.allow-bean-definition-overriding=true \
    > "logs/api-gateway.log" 2>&1 &
```

### **Iniciar Servicio Individual**

```bash
# Auth Service
nohup java -jar "services/auth-service/build/libs/auth-service.jar" \
    --server.port=8091 \
    > "logs/auth-service.log" 2>&1 &

# Station Service
nohup java -jar "services/station-service/build/libs/station-service.jar" \
    --server.port=8092 \
    > "logs/station-service.log" 2>&1 &
```

---

## **🐛 COMANDOS DE TROUBLESHOOTING**

### **Limpiar Procesos Colgados**

```bash
# Matar todos los procesos Java de Gasolinera
pkill -f "gasolinera"

# Matar proceso específico por puerto
kill $(lsof -t -i:8080)  # API Gateway
kill $(lsof -t -i:8091)  # Auth Service
```

### **Verificar Conectividad de Base de Datos**

```bash
# Conectar a PostgreSQL
docker exec -it gasolinera-postgres psql -U gasolinera_user -d auth_db

# Verificar Redis
docker exec -it gasolinera-redis redis-cli ping

# Verificar RabbitMQ
curl -u gasolinera_user:gasolinera_password http://localhost:15672/api/overview
```

### **Reiniciar Infraestructura**

```bash
# Reiniciar contenedores Docker
docker-compose -f docker-compose.simple.yml restart

# Reiniciar solo PostgreSQL
docker-compose -f docker-compose.simple.yml restart postgres
```

---

## **📊 MAPEO DE PUERTOS**

| Servicio           | Puerto Original | Puerto Actual | Estado         |
| ------------------ | --------------- | ------------- | -------------- |
| API Gateway        | 8080            | 8080          | ✅ Funcionando |
| Auth Service       | 8081            | 8091          | ⚠️ Health DOWN |
| Station Service    | 8082            | 8092          | ⏳ Pendiente   |
| Coupon Service     | 8083            | 8093          | ⚠️ Health DOWN |
| Raffle Service     | 8084            | 8094          | ⏳ Pendiente   |
| Redemption Service | 8085            | 8095          | ⏳ Pendiente   |
| Ad Engine          | 8086            | 8096          | ⏳ Pendiente   |
| Message Improver   | 8087            | 8097          | 🔄 Iniciando   |

---

## **🎯 FLUJO DE TRABAJO RECOMENDADO**

### **Desarrollo Diario**

1. `docker-compose -f docker-compose.simple.yml up -d` - Iniciar infraestructura
2. `./start-services-fixed.sh` - Iniciar servicios
3. `./verify-system.sh` - Verificar estado
4. Desarrollar y probar
5. `./stop-services.sh` - Detener al finalizar

### **Debugging**

1. `./verify-system.sh` - Identificar problemas
2. `tail -f logs/[service].log` - Ver logs específicos
3. Reiniciar servicio problemático
4. Verificar nuevamente

### **Testing**

1. Verificar que API Gateway responda: `curl http://localhost:8080/health`
2. Probar routing: `curl http://localhost:8080/api/auth/actuator/health`
3. Verificar servicios individuales según necesidad
