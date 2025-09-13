# Health Checks Comprehensivos - Gasolinera JSM

Este directorio contiene la configuración completa del sistema de health checks para todos los microservicios de Gasolinera JSM, incluyendo configuraciones para Kubernetes, monitoreo y alertas.

## Arquitectura del Sistema de Health Checks

### Componentes Principales

1. **Shared Health Library** - Biblioteca compartida para health checks personalizados
2. **Custom Health Indicators** - Indicadores específicos para cada componente
3. **Aggregated Health Checks** - Health checks agregados y centralizados
4. **Kubernetes Integration** - Readiness y liveness probes
5. **Monitoring & Alerting** - Integración con Prometheus y Grafana

### Características Implementadas

- ✅ Health checks personalizados para base de datos con métricas de pool de conexiones
- ✅ Health checks de Redis con métricas de memoria y performance
- ✅ Health checks de operaciones de negocio con tracking de errores
- ✅ Health checks de servicios externos con timeouts configurables
- ✅ Health checks de recursos del sistema (CPU, memoria, disco)
- ✅ Agregación inteligente de múltiples health checks
- ✅ Historial y estadísticas de disponibilidad
- ✅ Sistema de alertas con thresholds configurables
- ✅ Integración completa con Kubernetes probes
- ✅ Endpoints especializados para diferentes casos de uso

## Tipos de Health Checks

### 1. Database Health Check

```kotlin
// Métricas incluidas:
- Conectividad básica
- Pool de conexiones (activas, idle, uso %)
- Tiempo de respuesta de queries
- Detección de queries lentas
- Métricas de performance de PostgreSQL
```

### 2. Redis Health Check

```kotlin
// Métricas incluidas:
- Conectividad y ping
- Test de escritura/lectura
- Uso de memoria
- Información del servidor
- Métricas de cluster (si aplica)
- Performance de operaciones
```

### 3. Business Health Check

```kotlin
// Métricas incluidas:
- Operaciones críticas de negocio
- Tasas de error por operación
- Tiempos de respuesta promedio
- Fallos consecutivos
- Tracking de operaciones por usuario
```

### 4. External Services Health Check

```kotlin
// Métricas incluidas:
- Conectividad con servicios externos
- Tiempos de respuesta
- Status codes esperados
- Servicios críticos vs no críticos
- Headers personalizados para autenticación
```

### 5. System Resources Health Check

```kotlin
// Métricas incluidas:
- Uso de CPU
- Uso de memoria (heap y non-heap)
- Uso de disco
- Información de JVM
- Métricas de threads
- Estadísticas de garbage collection
```

## Configuración por Ambiente

### Desarrollo Local

```yaml
gasolinera:
  health:
    database:
      timeout: 10s
      connectionPoolThreshold: 70
    redis:
      timeout: 5s
      memoryThreshold: 90
    systemResources:
      cpuThreshold: 90.0
      memoryThreshold: 90.0
```

### Producción

```yaml
gasolinera:
  health:
    database:
      timeout: 3s
      connectionPoolThreshold: 70
      slowQueryThreshold: 1s
    redis:
      timeout: 2s
      memoryThreshold: 75
    systemResources:
      cpuThreshold: 70.0
      memoryThreshold: 75.0
      diskThreshold: 80.0
```

## Uso de la Biblioteca de Health Checks

### Integración Básica en un Servicio

1. **Agregar Dependencia**

```kotlin
// build.gradle.kts
implementation(project(":shared:health"))
```

2. **Configurar Application Properties**

```yaml
# application.yml
gasolinera:
  health:
    enabled: true
    database:
      enabled: true
    redis:
      enabled: true
    business:
      enabled: true
```

3. **Usar en el Código**

```kotlin
@RestController
class MyController(
    private val businessHealthIndicator: BusinessHealthIndicator
) {

    @PostMapping("/process")
    fun processOperation(@RequestBody request: ProcessRequest): ResponseEntity<*> {
        val context = businessHealthIndicator.startCriticalOperation(
            BusinessOperationType.COUPON_GENERATION
        )

        return try {
            val result = processBusinessLogic(request)
            businessHealthIndicator.completeCriticalOperation(context, true, request.userId)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            businessHealthIndicator.completeCriticalOperation(context, false, request.userId)
            throw e
        }
    }
}
```

### Health Checks con Anotaciones

```kotlin
@Service
class CouponService(
    private val businessHealthIndicator: BusinessHealthIndicator
) {

    // El health check se registra automáticamente
    fun generateCoupon(userId: String): Coupon {
        val context = businessHealthIndicator.startCriticalOperation(
            BusinessOperationType.COUPON_GENERATION
        )

        return try {
            val coupon = performCouponGeneration(userId)
            businessHealthIndicator.completeCriticalOperation(context, true, userId)
            coupon
        } catch (e: Exception) {
            businessHealthIndicator.completeCriticalOperation(context, false, userId)
            throw e
        }
    }
}
```

## Endpoints de Health Checks

### Endpoints Principales

| Endpoint                | Descripción                             | Uso               |
| ----------------------- | --------------------------------------- | ----------------- |
| `/api/health/detailed`  | Health check completo y detallado       | Monitoreo general |
| `/api/health/quick`     | Health check rápido para load balancers | Load balancing    |
| `/api/health/readiness` | Readiness probe para Kubernetes         | K8s readiness     |
| `/api/health/liveness`  | Liveness probe para Kubernetes          | K8s liveness      |

### Endpoints por Componente

| Endpoint                        | Descripción                          |
| ------------------------------- | ------------------------------------ |
| `/api/health/component/{name}`  | Health de componente específico      |
| `/api/health/database/detailed` | Métricas detalladas de base de datos |
| `/api/health/redis/detailed`    | Métricas detalladas de Redis         |
| `/api/health/system/detailed`   | Métricas detalladas del sistema      |
| `/api/health/external-services` | Estado de servicios externos         |

### Endpoints de Monitoreo

| Endpoint                               | Descripción                            |
| -------------------------------------- | -------------------------------------- |
| `/api/health/availability/{component}` | Estadísticas de disponibilidad         |
| `/api/health/history/{component}`      | Historial de health checks             |
| `/api/health/status`                   | Estado actual de todos los componentes |
| `/api/health/alerts`                   | Alertas activas                        |
| `/api/health/report`                   | Reporte completo de salud              |

## Configuración de Kubernetes

### Deployment con Health Checks

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: coupon-service
spec:
  template:
    spec:
      containers:
        - name: coupon-service
          readinessProbe:
            httpGet:
              path: /api/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3

          livenessProbe:
            httpGet:
              path: /api/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
            timeoutSeconds: 10
            failureThreshold: 3

          startupProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30
```

### Service Monitor para Prometheus

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: gasolinera-health-monitor
spec:
  selector:
    matchLabels:
      app: gasolinera-service
  endpoints:
    - port: http
      path: /api/health/detailed
      interval: 60s
      scrapeTimeout: 15s
```

## Monitoreo y Alertas

### Métricas de Prometheus

Las siguientes métricas se exponen automáticamente:

```prometheus
# Health check status (0=DOWN, 1=UP)
gasolinera_health_status{component="database"} 1

# Health check duration
gasolinera_health_check_duration_seconds{component="redis"} 0.045

# Business operation metrics
gasolinera_business_operations_total{operation="COUPON_GENERATION"} 1250
gasolinera_business_operations_errors_total{operation="COUPON_GENERATION"} 12
gasolinera_business_operations_duration_seconds{operation="COUPON_GENERATION"} 0.234

# System resource metrics
gasolinera_system_cpu_usage_percentage 45.2
gasolinera_system_memory_usage_percentage 67.8
gasolinera_system_disk_usage_percentage 23.1

# Database metrics
gasolinera_database_connection_pool_usage_percentage 45
gasolinera_database_query_duration_seconds 0.012

# Redis metrics
gasolinera_redis_memory_usage_percentage 34
gasolinera_redis_operations_per_second 1250
```

### Alertas de Prometheus

```yaml
groups:
  - name: gasolinera-health
    rules:
      - alert: GasolineraServiceDown
        expr: gasolinera_health_status == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: 'Gasolinera service is down'

      - alert: GasolineraHighErrorRate
        expr: rate(gasolinera_business_operations_errors_total[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: 'High error rate detected'
```

## Uso del Script de Monitoreo

### Comandos Básicos

```bash
# Configuración inicial completa
./scripts/setup-health-checks.sh setup

# Verificar servicios locales
./scripts/setup-health-checks.sh check-local

# Verificar servicios en Kubernetes
./scripts/setup-health-checks.sh check-k8s

# Obtener métricas detalladas
./scripts/setup-health-checks.sh detailed http://localhost:8080

# Monitoreo en tiempo real
./scripts/setup-health-checks.sh monitor http://localhost:8080 5

# Generar reporte de 48 horas
./scripts/setup-health-checks.sh report http://localhost:8080 48

# Configurar alertas
./scripts/setup-health-checks.sh alerts http://localhost:8080
```

### Monitoreo en Tiempo Real

El script incluye un monitor en tiempo real que muestra:

```
=== Monitoreo de Health Checks - 2024-01-15 10:30:00 ===

📊 Estado General:
   ✅ Sistema Saludable

🔧 Componentes Críticos:
   Database: ✅ UP
   Redis: ✅ UP
   Business: ✅ UP

🚨 Alertas Activas:
   ✅ No hay alertas activas

Próxima actualización en 10 segundos...
```

## Integración con Servicios Existentes

### Auth Service

```kotlin
// Ejemplo de integración en AuthController
@PostMapping("/login")
fun login(@RequestBody request: LoginRequest): ResponseEntity<*> {
    val context = businessHealthIndicator.startCriticalOperation(
        BusinessOperationType.USER_AUTHENTICATION
    )

    return try {
        val result = authService.authenticate(request)
        businessHealthIndicator.completeCriticalOperation(context, true, request.username)
        ResponseEntity.ok(result)
    } catch (e: AuthenticationException) {
        businessHealthIndicator.completeCriticalOperation(context, false, request.username)
        throw e
    }
}
```

### Coupon Service

```kotlin
// Ejemplo de integración en CouponController
@PostMapping("/generate")
fun generateCoupons(@RequestBody request: GenerateCouponsRequest): ResponseEntity<*> {
    val context = businessHealthIndicator.startCriticalOperation(
        BusinessOperationType.COUPON_GENERATION
    )

    return try {
        val coupons = couponService.generateCoupons(request)
        businessHealthIndicator.completeCriticalOperation(context, true, request.userId)
        ResponseEntity.ok(coupons)
    } catch (e: Exception) {
        businessHealthIndicator.completeCriticalOperation(context, false, request.userId)
        throw e
    }
}
```

## Troubleshooting

### Health Checks Fallan

1. **Verificar Configuración**

```bash
# Verificar configuración de health checks
curl http://localhost:8080/api/health/detailed | jq '.'
```

2. **Verificar Componentes Individuales**

```bash
# Database
curl http://localhost:8080/api/health/component/database

# Redis
curl http://localhost:8080/api/health/component/redis

# Business
curl http://localhost:8080/api/health/component/business
```

3. **Verificar Logs**

```bash
# Logs de health checks
kubectl logs -f deployment/coupon-service -n gasolinera-jsm | grep -i health
```

### Performance de Health Checks

1. **Ajustar Timeouts**

```yaml
gasolinera:
  health:
    database:
      timeout: 3s # Reducir para mejor performance
    redis:
      timeout: 2s
```

2. **Configurar Cache**

```yaml
gasolinera:
  health:
    systemResources:
      checkInterval: 2m # Aumentar intervalo para reducir overhead
```

### Alertas Excesivas

1. **Ajustar Thresholds**

```bash
# Configurar thresholds más permisivos
curl -X POST http://localhost:8080/api/health/alerts/threshold/database \
  -H "Content-Type: application/json" \
  -d '{
    "maxConsecutiveFailures": 5,
    "maxFailureRate": 0.15,
    "timeWindowMinutes": 10
  }'
```

## Mejores Prácticas

### 1. Configuración de Timeouts

- **Readiness**: Timeouts cortos (5-10s) para detección rápida
- **Liveness**: Timeouts más largos (10-30s) para evitar restarts innecesarios
- **Health Checks**: Timeouts apropiados según criticidad del componente

### 2. Thresholds de Alertas

- **Componentes Críticos**: Thresholds estrictos (2-3 fallos consecutivos)
- **Componentes No Críticos**: Thresholds más permisivos (5+ fallos)
- **Ventanas de Tiempo**: Ajustar según patrones de tráfico

### 3. Monitoreo

- **Dashboards**: Crear dashboards específicos por servicio y generales
- **Alertas**: Configurar alertas por severidad y escalación
- **Reportes**: Generar reportes regulares de disponibilidad

### 4. Testing

- **Unit Tests**: Tests para cada health indicator
- **Integration Tests**: Tests de health checks con dependencias reales
- **Load Tests**: Verificar performance de health checks bajo carga

## Roadmap

### Próximas Mejoras

- [ ] Integración con sistemas de notificación (Slack, Teams, Email)
- [ ] Health checks predictivos con ML
- [ ] Auto-healing basado en health checks
- [ ] Métricas de SLA automáticas
- [ ] Dashboard móvil para health checks
