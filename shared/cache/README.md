# Módulo de Caching Avanzado con Redis

Este módulo proporciona una solución completa de caching distribuido usando Redis para la aplicación Gasolinera JSM, con funcionalidades avanzadas de gestión, monitoreo y optimización.

## Características Principales

### 🚀 Funcionalidades Core

1. **Cache Distribuido**: Sistema de cache distribuido usando Redis con soporte para clustering
2. **Invalidación Inteligente**: Invalidación automática basada en eventos de dominio
3. **Warmup Automático**: Precalentamiento inteligente de cache con múltiples estrategias
4. **Métricas Avanzadas**: Monitoreo completo con métricas Prometheus y análisis de rendimiento
5. **Locks Distribuidos**: Sistema de locks distribuidos para operaciones críticas
6. **Health Checks**: Verificación automática de salud del sistema de cache

### 🛠️ Componentes Principales

- **CacheService**: Servicio principal para operaciones de cache
- **CacheInvalidationService**: Gestión inteligente de invalidación
- **CacheWarmupService**: Precalentamiento automático y programado
- **CacheMetricsService**: Monitoreo y análisis de rendimiento
- **DistributedLockService**: Locks distribuidos para sincronización
- **CacheHealthIndicator**: Health checks integrados

## Instalación y Configuración

### Dependencias

Añadir en `build.gradle.kts`:

```kotlin
implementation(project(":shared:cache"))
```

### Configuración Básica

En `application.yml`:

```yaml
spring:
  profiles:
    include: cache

gasolinera:
  cache:
    enabled: true
    default-ttl: PT30M
    key-prefix: 'gasolinera'
```

### Configuración de Redis

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}

      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

## Uso del Sistema

### Operaciones Básicas de Cache

```kotlin
@Service
class UserService(private val cacheService: CacheService) {

    fun getUser(userId: String): User? {
        return cacheService.getOrCompute("users", userId) {
            // Cargar usuario desde base de datos
            userRepository.findById(userId)
        }
    }

    fun updateUser(user: User) {
        userRepository.save(user)

        // Invalidar cache del usuario
        cacheService.evict("users", user.id)

        // O usar invalidación por evento
        cacheInvalidationService.invalidateByEvent("user.updated", user.id)
    }
}
```

### Cache Asíncrono

```kotlin
@Service
class CouponService(private val cacheService: CacheService) {

    suspend fun getCouponAsync(couponId: String): Coupon? {
        return cacheService.getAsync<Coupon>("coupons", couponId) {
            couponRepository.findById(couponId)
        }.await()
    }

    suspend fun preloadCoupons(couponIds: List<String>) {
        val futures = couponIds.map { id ->
            cacheService.putAsync("coupons", id, loadCoupon(id))
        }

        CompletableFuture.allOf(*futures.toTypedArray()).await()
    }
}
```

### Operaciones Múltiples

```kotlin
@Service
class StationService(private val cacheService: CacheService) {

    fun getStations(stationIds: List<String>): Map<String, Station?> {
        return cacheService.multiGet<Station>("stations", stationIds)
    }

    fun cacheStations(stations: Map<String, Station>) {
        cacheService.multiPut("stations", stations, Duration.ofHours(2))
    }
}
```

## Invalidación Inteligente

### Configuración de Patrones

```yaml
gasolinera:
  cache:
    invalidation:
      patterns:
        user.updated: ['users:*', 'sessions:*']
        coupon.redeemed: ['coupons:*', 'users:*', 'campaigns:*']
        station.updated: ['stations:*', 'config:*']
```

### Uso Programático

```kotlin
@Service
class CacheInvalidationExample(
    private val cacheInvalidationService: CacheInvalidationService
) {

    @EventListener
    fun handleUserUpdate(event: UserUpdatedEvent) {
        // Invalidación automática basada en evento
        cacheInvalidationService.invalidateByEvent("user.updated", event.userId)
    }

    fun invalidateUserData(userId: String) {
        // Invalidación en cascada
        cacheInvalidationService.cascadeInvalidation("user.deleted", userId)
    }

    fun customInvalidation() {
        // Invalidación por patrón
        cacheInvalidationService.invalidateByPattern("users:active:*")

        // Invalidación por múltiples patrones
        cacheInvalidationService.invalidateByPatterns(
            listOf("users:*", "sessions:*"),
            "user123"
        )
    }
}
```

## Warmup de Cache

### Estrategias de Warmup

```kotlin
@Service
class CustomWarmupService(
    private val cacheWarmupService: CacheWarmupService
) {

    @PostConstruct
    fun registerCustomWarmupStrategies() {
        // Estrategia personalizada para usuarios
        cacheWarmupService.registerWarmupStrategy("users", object : WarmupStrategy {
            override fun getKeysToWarmup(): List<String> {
                return userRepository.findActiveUserIds()
            }

            override fun getPriorityKeys(): List<String> {
                return userRepository.findVipUserIds()
            }

            override fun getRecentlyUsedKeys(): List<String> {
                return userRepository.findRecentlyActiveUserIds()
            }

            override fun loadValue(key: String): Any? {
                return userRepository.findById(key)
            }
        })
    }

    fun warmupSpecificCache() {
        // Warmup manual con estrategia específica
        cacheWarmupService.warmupCache("users", CacheProperties.WarmupStrategy.EAGER)
    }
}
```

### Warmup Programado

```yaml
gasolinera:
  cache:
    warmup:
      enabled: true
      on-startup: true
      scheduled: true
      schedule-cron: '0 0 6 * * ?' # 6 AM diariamente
      batch-size: 100
      parallelism: 4
```

## Locks Distribuidos

### Uso Básico

```kotlin
@Service
class CouponRedemptionService(
    private val distributedLockService: DistributedLockService
) {

    fun redeemCoupon(couponId: String, userId: String): Boolean {
        val lockKey = "coupon:redemption:$couponId"

        return distributedLockService.withLock(
            lockKey = lockKey,
            ttl = Duration.ofMinutes(2),
            waitTime = Duration.ofSeconds(5)
        ) {
            // Lógica de redención protegida por lock
            val coupon = couponRepository.findById(couponId)
            if (coupon?.isAvailable() == true) {
                coupon.redeem(userId)
                couponRepository.save(coupon)
                true
            } else {
                false
            }
        } ?: false
    }
}
```

### Locks con Auto-renovación

```kotlin
@Service
class LongRunningTaskService(
    private val distributedLockService: DistributedLockService
) {

    fun executeLongTask(taskId: String) {
        val lock = distributedLockService.tryLockWithAutoRenew(
            lockKey = "task:$taskId",
            ttl = Duration.ofMinutes(5),
            renewInterval = Duration.ofMinutes(2)
        )

        if (lock != null) {
            try {
                // Tarea de larga duración
                performLongRunningTask(taskId)
            } finally {
                lock.release()
            }
        }
    }
}
```

## Monitoreo y Métricas

### Métricas Automáticas

El sistema expone automáticamente métricas Prometheus:

```
# Operaciones de cache
cache_operation_duration_seconds{cache="users",operation="get",success="true"}
cache_operation_count_total{cache="users",operation="get",success="true"}

# Estadísticas de cache
cache_requests_total{cache="users",result="hit"}
cache_requests_total{cache="users",result="miss"}
cache_puts_total{cache="users"}
cache_evictions_total{cache="users"}
cache_errors_total{cache="users"}
```

### Análisis de Rendimiento

```kotlin
@Service
class CacheAnalysisService(
    private val cacheMetricsService: CacheMetricsService
) {

    fun analyzePerformance() {
        // Métricas del sistema
        val systemMetrics = cacheMetricsService.getSystemMetrics()
        println("Hit rate promedio: ${systemMetrics.averageHitRate}")

        // Análisis por cache
        val analysis = cacheMetricsService.getPerformanceAnalysis("users")
        println("Tendencia hit rate: ${analysis.hitRateTrend}")
        println("Recomendaciones: ${analysis.recommendations}")

        // Detección de anomalías
        val anomalies = cacheMetricsService.detectAnomalies()
        anomalies.forEach { anomaly ->
            println("Anomalía en ${anomaly.cacheName}: ${anomaly.description}")
        }
    }

    @Scheduled(fixedRate = 300000) // Cada 5 minutos
    fun generateHealthReport() {
        val report = cacheMetricsService.generateHealthReport()

        if (report.overallHealth != HealthStatus.HEALTHY) {
            // Enviar alerta
            alertService.sendCacheHealthAlert(report)
        }
    }
}
```

## API REST

### Endpoints de Gestión

```bash
# Estadísticas de cache
GET /api/cache/users/stats
GET /api/cache/system/metrics

# Invalidación
DELETE /api/cache/users
DELETE /api/cache/users/keys/user123
DELETE /api/cache/users/pattern?pattern=active:*

# Warmup
POST /api/cache/warmup
POST /api/cache/users/warmup?strategy=EAGER

# Análisis
GET /api/cache/users/analysis
GET /api/cache/anomalies
GET /api/cache/health/report
```

### Operaciones de Cache

```bash
# Verificar existencia
GET /api/cache/users/exists/user123

# Obtener TTL
GET /api/cache/users/ttl/user123

# Almacenar valor
POST /api/cache/users/put
{
  "key": "user123",
  "value": {"id": "user123", "name": "John"},
  "ttlSeconds": 1800
}

# Obtener valor
GET /api/cache/users/get/user123

# Operaciones múltiples
POST /api/cache/users/multi-get
["user123", "user456", "user789"]

POST /api/cache/users/multi-put
{
  "values": {
    "user123": {"id": "user123", "name": "John"},
    "user456": {"id": "user456", "name": "Jane"}
  },
  "ttlSeconds": 1800
}
```

## Configuración Avanzada

### Configuración por Ambiente

```yaml
# Desarrollo
gasolinera:
  cache:
    warmup:
      on-startup: false
      scheduled: false
    monitoring:
      slow-operation-threshold: PT500MS

# Producción
gasolinera:
  cache:
    clustering:
      enabled: true
      nodes: ["redis-1:6379", "redis-2:6379", "redis-3:6379"]

    warmup:
      parallelism: 8
      batch-size: 200

    monitoring:
      alert-on-high-miss-rate: true
      miss-rate-threshold: 0.7
```

### Configuración de Caches Específicos

```yaml
gasolinera:
  cache:
    caches:
      users:
        ttl: PT15M
        key-prefix: 'user'
        max-size: 5000
        warmup-enabled: true
        warmup-strategy: LAZY
        invalidation-strategy: EVENT_DRIVEN

      stations:
        ttl: PT2H
        key-prefix: 'station'
        max-size: 1000
        warmup-enabled: true
        warmup-strategy: EAGER
        invalidation-strategy: TTL_BASED

      sessions:
        ttl: PT30M
        key-prefix: 'session'
        max-size: 50000
        warmup-enabled: false
        invalidation-strategy: TTL_BASED
```

## Health Checks

### Health Indicator Integrado

```json
{
  "status": "UP",
  "components": {
    "cacheHealthIndicator": {
      "status": "UP",
      "details": {
        "redis": {
          "status": "UP",
          "responseTime": "15ms",
          "connection": "OK"
        },
        "systemMetrics": {
          "totalCaches": 5,
          "averageHitRate": "85.50%",
          "totalMemoryUsage": "256MB",
          "healthStatus": "HEALTHY"
        },
        "caches": {
          "users": {
            "hitRate": "87.30%",
            "totalOperations": 15420,
            "errorRate": "0.05%"
          },
          "stations": {
            "hitRate": "95.20%",
            "totalOperations": 8930,
            "errorRate": "0.00%"
          }
        }
      }
    }
  }
}
```

## Testing

### Tests Unitarios

```kotlin
@SpringBootTest
@Testcontainers
class CacheIntegrationTest {

    @Container
    private val redisContainer = GenericContainer<Nothing>("redis:7-alpine")
        .withExposedPorts(6379)

    @Autowired
    private lateinit var cacheService: CacheService

    @Test
    fun `should cache and retrieve values correctly`() {
        // Given
        val key = "test-key"
        val value = "test-value"

        // When
        cacheService.put("test-cache", key, value)
        val retrieved = cacheService.get<String>("test-cache", key)

        // Then
        assertEquals(value, retrieved)
    }
}
```

### Tests de Rendimiento

```kotlin
@Test
fun `should handle high load operations`() {
    val operations = 10000
    val startTime = System.currentTimeMillis()

    repeat(operations) { i ->
        cacheService.put("load-test", "key-$i", "value-$i")
    }

    val duration = System.currentTimeMillis() - startTime
    assertTrue(duration < 5000) // Menos de 5 segundos
}
```

## Mejores Prácticas

### Diseño de Claves

```kotlin
// ✅ Buenas prácticas
"users:active:user123"
"campaigns:2024:campaign456"
"stations:region:north:station789"

// ❌ Evitar
"user123"
"very:long:key:with:too:many:segments:that:makes:it:hard:to:read"
```

### Gestión de TTL

```kotlin
// ✅ TTL apropiado según tipo de dato
cacheService.put("users", userId, user, Duration.ofMinutes(15))      // Datos de usuario
cacheService.put("stations", stationId, station, Duration.ofHours(2)) // Datos estáticos
cacheService.put("sessions", sessionId, session, Duration.ofMinutes(30)) // Sesiones
```

### Invalidación Eficiente

```kotlin
// ✅ Invalidación específica
cacheInvalidationService.invalidateByEvent("user.updated", userId)

// ✅ Invalidación por patrón
cacheInvalidationService.invalidateByPattern("users:active:*")

// ❌ Evitar invalidación masiva innecesaria
cacheInvalidationService.invalidateAll() // Solo en casos extremos
```

## Troubleshooting

### Problemas Comunes

#### Baja Tasa de Aciertos

```bash
# Verificar configuración de TTL
curl http://localhost:8080/api/cache/users/analysis

# Revisar patrones de invalidación
curl http://localhost:8080/api/cache/anomalies
```

#### Problemas de Conectividad

```bash
# Verificar health check
curl http://localhost:8080/actuator/health/cacheHealthIndicator

# Verificar métricas de Redis
curl http://localhost:8080/actuator/metrics/cache.operation.duration
```

#### Alto Uso de Memoria

```bash
# Analizar uso por cache
curl http://localhost:8080/api/cache/system/metrics

# Verificar configuración de límites
curl http://localhost:8080/api/cache/users/stats
```

### Logs Útiles

```bash
# Logs de cache
kubectl logs -f deployment/coupon-service | grep "CacheService"

# Métricas de rendimiento
kubectl logs -f deployment/coupon-service | grep "CacheMetrics"

# Logs de invalidación
kubectl logs -f deployment/coupon-service | grep "CacheInvalidation"
```

## Roadmap

### Funcionalidades Futuras

- [ ] Soporte para cache multi-nivel (L1/L2)
- [ ] Compresión automática de valores grandes
- [ ] Replicación geográfica de cache
- [ ] Machine Learning para optimización automática
- [ ] Integración con Apache Kafka para invalidación
- [ ] Dashboard web para monitoreo en tiempo real

### Mejoras Planificadas

- [ ] Optimización de serialización
- [ ] Soporte para transacciones distribuidas
- [ ] Cache warming predictivo
- [ ] Alertas inteligentes basadas en ML
- [ ] Integración con service mesh
