# Módulo de Optimización de Base de Datos Avanzado

Este módulo proporciona herramientas avanzadas de optimización, análisis y mantenimiento para PostgreSQL en la aplicación Gasolinera JSM, incluyendo soporte para read replicas, detección N+1 y monitoreo de performance avanzado.

## 🚀 Características Principales

### Optimizaciones Implementadas

1. **📊 Análisis de Base de Datos**: Análisis completo de rendimiento y estadísticas
2. **🔍 Optimización de Índices**: Gestión inteligente de índices y sugerencias
3. **⚡ Optimización de Queries**: Análisis y optimización de consultas SQL
4. **📈 Gestión de Particiones**: Manejo automático de particionado de tablas
5. **🔗 Connection Pooling**: Configuración optimizada de HikariCP
6. **🔧 Mantenimiento Automático**: VACUUM y ANALYZE inteligentes
7. **🔄 Read Replicas**: Configuración automática con load balancing
8. **🚨 Detección N+1**: Detección automática de problemas N+1 en queries
9. **📈 Monitoreo de Performance**: Integración con pg_stat_statements

### Componentes Principales

- **DatabaseAnalyzer**: Análisis completo de rendimiento de base de datos
- **IndexOptimizer**: Gestión y optimización de índices
- **QueryOptimizer**: Análisis y optimización de consultas
- **PartitionManager**: Gestión automática de particiones
- **ReadReplicaManager**: Gestión de read replicas con load balancing
- **NPlusOneDetector**: Detección automática de problemas N+1
- **QueryPerformanceMonitor**: Monitoreo avanzado con pg_stat_statements
- **DatabaseMaintenanceService**: Mantenimiento programado

## 📦 Instalación y Configuración

### Dependencias

Añadir en `build.gradle.kts`:

```kotlin
implementation(project(":shared:database"))
```

### Configuración Básica

En `application.yml`:

```yaml
spring:
  profiles:
    include: database

gasolinera:
  database:
    optimization:
      enabled: true
      performance-monitoring: true
      n-plus-one-detection: true
      query-optimization: true
```

### Configuración de Read Replicas

```yaml
gasolinera:
  database:
    read-replica:
      enabled: true
      replicas:
        - name: 'replica-1'
          url: 'jdbc:postgresql://replica1:5432/gasolinera'
          username: 'replica_user'
          password: 'replica_pass'
          max-pool-size: 15
          weight: 1
        - name: 'replica-2'
          url: 'jdbc:postgresql://replica2:5432/gasolinera'
          username: 'replica_user'
          password: 'replica_pass'
          max-pool-size: 15
          weight: 1
      load-balancing-strategy: ROUND_ROBIN
      failover-enabled: true
```

## 🔄 Uso de Read Replicas

### Anotaciones Automáticas

```kotlin
@Service
class UserService {

    @ReadOnlyQuery(strategy = ReplicaSelectionStrategy.ROUND_ROBIN)
    fun findAllUsers(): List<User> {
        // Esta query se ejecutará automáticamente en read replica
        return userRepository.findAll()
    }

    @WriteQuery
    fun createUser(user: User): User {
        // Esta operación se ejecutará en el servidor primario
        return userRepository.save(user)
    }

    @OptimizedQuery(
        optimization = QueryOptimization.AUTO,
        cacheable = true,
        cacheName = "users",
        cacheTtl = 300
    )
    fun findUsersByStatus(status: String): List<User> {
        // Query optimizada automáticamente con cache
        return userRepository.findByStatus(status)
    }
}
```

### Uso Programático

```kotlin
@Service
class CouponService(
    private val readReplicaManager: ReadReplicaManager
) {

    fun getActiveCoupons(): List<Coupon> {
        return readReplicaManager.executeOnReadReplica {
            couponRepository.findByStatusAndExpirationDateAfter(
                CouponStatus.ACTIVE,
                LocalDateTime.now()
            )
        }
    }

    fun createCoupon(coupon: Coupon): Coupon {
        return readReplicaManager.executeOnWrite {
            couponRepository.save(coupon)
        }
    }
}
```

## 🚨 Detección de Problemas N+1

### Configuración Automática

```kotlin
@Service
class OrderService(
    private val nPlusOneDetector: NPlusOneDetector
) {

    @Transactional(readOnly = true)
    fun getOrdersWithItems(userId: String): List<Order> {
        // Iniciar tracking automático
        nPlusOneDetector.startTracking("get-orders-$userId")

        try {
            val orders = orderRepository.findByUserId(userId)

            // Esto podría generar N+1 si no está optimizado
            orders.forEach { order ->
                order.items // Lazy loading que podría causar N+1
            }

            return orders
        } finally {
            val analysis = nPlusOneDetector.finishTracking()
            if (analysis?.hasNPlusOneIssues == true) {
                logger.warn("N+1 problem detected: ${analysis.issues}")
            }
        }
    }
}
```

### Análisis Automático

El detector N+1 automáticamente:

- Identifica patrones de queries repetitivas
- Detecta SELECT por ID en bucles
- Sugiere optimizaciones (IN clauses, JOIN FETCH, etc.)
- Genera métricas para monitoreo

## 📊 Monitoreo de Performance

### Análisis con pg_stat_statements

```kotlin
@Service
class PerformanceService(
    private val queryPerformanceMonitor: QueryPerformanceMonitor
) {

    fun analyzePerformance(): QueryPerformanceReport {
        return queryPerformanceMonitor.analyzeQueryPerformance()
    }

    @Scheduled(fixedRate = 300000) // Cada 5 minutos
    fun monitorPerformance() {
        val report = analyzePerformance()

        if (report.recommendations.any { it.priority == Priority.HIGH }) {
            alertService.sendPerformanceAlert(report)
        }
    }
}
```

### Métricas Automáticas

El sistema expone métricas Prometheus:

```
# Queries lentas
database_slow_queries_count
database_slow_queries_avg_time

# Read replicas
database_read_replica_health{replica="replica-1"}
database_query_count{datasource="read"}
database_query_count{datasource="write"}

# N+1 Detection
database_nplus1_detected_total
database_nplus1_issues_total{type="SELECT_BY_ID"}

# Performance general
database_query_duration_seconds{method="UserService.findAll"}
```

## 🔧 Optimización Automática de Queries

### Análisis Inteligente

```kotlin
@Service
class ProductService {

    @MonitorQuery(
        slowQueryThreshold = 500,
        logSlowQueries = true,
        generateMetrics = true
    )
    fun searchProducts(criteria: SearchCriteria): List<Product> {
        // Query monitoreada automáticamente
        return productRepository.search(criteria)
    }

    @OptimizedQuery(
        optimization = QueryOptimization.PAGINATION,
        autoPagination = true,
        maxPageSize = 1000
    )
    fun getAllProducts(): List<Product> {
        // Paginación automática para resultados grandes
        return productRepository.findAll()
    }
}
```

### Sugerencias Automáticas

El sistema genera automáticamente:

- Sugerencias de índices faltantes
- Optimizaciones para queries lentas
- Recomendaciones de particionado
- Alertas de performance

## 🛠️ API REST para Gestión

### Endpoints de Performance

```bash
# Análisis de performance
GET /api/database/performance/analysis
GET /api/database/performance/slow-queries
GET /api/database/performance/frequent-queries
GET /api/database/performance/recommendations

# N+1 Detection
GET /api/database/performance/nplus1/stats
POST /api/database/performance/nplus1/cleanup

# Read Replicas
GET /api/database/performance/read-replicas/health
POST /api/database/performance/read-replicas/test

# Gestión
POST /api/database/performance/reset-stats
GET /api/database/performance/health
GET /api/database/performance/summary
```

### Ejemplo de Respuesta

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "slowQueries": [
    {
      "query": "SELECT * FROM coupons WHERE user_id = ? AND status = ?",
      "calls": 1500,
      "meanTime": 250.5,
      "hitPercent": 75.2
    }
  ],
  "recommendations": [
    {
      "type": "SLOW_QUERY",
      "priority": "HIGH",
      "description": "Query muy lenta detectada (250ms promedio)",
      "suggestion": "Considerar índice compuesto en (user_id, status)",
      "potentialImpact": "Reducción de 180ms por ejecución"
    }
  ],
  "nPlusOneIssues": 3,
  "readReplicasHealthy": true
}
```

## 🔧 Configuración Avanzada

### PostgreSQL Optimizado

```yaml
gasolinera:
  database:
    optimization:
      # HikariCP optimizado
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        leak-detection-threshold: 60000

      # Particionado automático
      partitioning:
        enabled: true
        auto-create-partitions: true
        partition-retention-months: 24
        maintenance-schedule: '0 2 1 * *'

      # Monitoreo de performance
      performance-monitoring: true
      n-plus-one-detection: true
      query-optimization: true
```

### Configuración de Producción

```yaml
# Producción
gasolinera:
  database:
    read-replica:
      enabled: true
      replicas:
        - name: 'replica-1'
          url: '${DATABASE_READ_REPLICA_1_URL}'
          username: '${DATABASE_READ_REPLICA_1_USERNAME}'
          password: '${DATABASE_READ_REPLICA_1_PASSWORD}'
          max-pool-size: 20
          weight: 2
        - name: 'replica-2'
          url: '${DATABASE_READ_REPLICA_2_URL}'
          username: '${DATABASE_READ_REPLICA_2_USERNAME}'
          password: '${DATABASE_READ_REPLICA_2_PASSWORD}'
          max-pool-size: 15
          weight: 1
      load-balancing-strategy: WEIGHTED
      health-check-interval: PT30S
      failover-enabled: true
      max-retries: 3

    primary:
      url: '${DATABASE_URL}'
      username: '${DATABASE_USERNAME}'
      password: '${DATABASE_PASSWORD}'
      max-pool-size: 30
      min-idle: 10
```

## 🛠️ Scripts de Configuración

### Setup de Read Replicas

```bash
# Configurar read replicas automáticamente
./scripts/setup-read-replicas.sh primary-host replica-host gasolinera_jsm

# Monitorear replicación
./scripts/monitor-replication.sh

# Failover de emergencia (solo en caso crítico)
./scripts/failover-replica.sh replica-host
```

### Habilitar pg_stat_statements

```sql
-- Ejecutar migración V004
-- Esto habilita pg_stat_statements y funciones de análisis

-- Funciones disponibles después de la migración:
SELECT * FROM get_slow_queries(20);
SELECT * FROM get_frequent_queries(20);
SELECT * FROM get_expensive_queries(20);
SELECT * FROM get_unused_indexes();
SELECT * FROM suggest_missing_indexes();
SELECT * FROM get_database_performance_stats();
```

## 🧪 Testing

### Tests de Read Replicas

```kotlin
@SpringBootTest
@Testcontainers
class ReadReplicaIntegrationTest {

    @Container
    private val primaryDb = PostgreSQLContainer<Nothing>("postgres:15-alpine")

    @Container
    private val replicaDb = PostgreSQLContainer<Nothing>("postgres:15-alpine")

    @Test
    fun `should route read queries to replica`() {
        // Given
        val service = TestService()

        // When
        val result = service.readOnlyOperation()

        // Then
        assertTrue(ReadReplicaContext.isReadOnly())
        assertNotNull(result)
    }
}
```

### Tests de N+1 Detection

```kotlin
@Test
fun `should detect N+1 pattern`() {
    // Given
    nPlusOneDetector.startTracking("test-request")

    // Simular patrón N+1
    repeat(10) { index ->
        nPlusOneDetector.recordQuery(
            sql = "SELECT * FROM orders WHERE user_id = ?",
            parameters = listOf(index),
            executionTime = Duration.ofMillis(25)
        )
    }

    // When
    val result = nPlusOneDetector.finishTracking()

    // Then
    assertTrue(result.hasNPlusOneIssues)
    assertEquals(NPlusOneType.SELECT_BY_ID, result.issues.first().type)
}
```

## 📈 Métricas y Monitoreo

### Dashboard de Grafana

Métricas clave para monitorear:

```promql
# Hit rate de read replicas
rate(database_query_count{datasource="read"}[5m]) /
rate(database_query_count[5m]) * 100

# Queries lentas por servicio
increase(database_slow_queries_count[1h])

# Problemas N+1 detectados
increase(database_nplus1_detected_total[1h])

# Latencia promedio por datasource
avg(database_query_duration_seconds) by (datasource)

# Salud de read replicas
database_read_replica_health
```

### Alertas Recomendadas

```yaml
# Alertas Prometheus
groups:
  - name: database_performance
    rules:
      - alert: HighSlowQueryRate
        expr: increase(database_slow_queries_count[5m]) > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: 'Alto número de queries lentas detectadas'

      - alert: NPlusOneDetected
        expr: increase(database_nplus1_detected_total[5m]) > 0
        for: 0m
        labels:
          severity: critical
        annotations:
          summary: 'Problema N+1 detectado en queries'

      - alert: ReadReplicaDown
        expr: database_read_replica_health == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: 'Read replica no disponible'
```

## 🔍 Troubleshooting

### Problemas Comunes

#### Read Replica No Disponible

```bash
# Verificar salud de replicas
curl http://localhost:8080/api/database/performance/read-replicas/health

# Test de conectividad
curl -X POST http://localhost:8080/api/database/performance/read-replicas/test

# Logs de read replica
kubectl logs -f deployment/coupon-service | grep "ReadReplica"
```

#### Queries Lentas

```bash
# Análisis de performance
curl http://localhost:8080/api/database/performance/analysis

# Queries más lentas
curl http://localhost:8080/api/database/performance/slow-queries?limit=10

# Recomendaciones
curl http://localhost:8080/api/database/performance/recommendations
```

#### Problemas N+1

```bash
# Estadísticas N+1
curl http://localhost:8080/api/database/performance/nplus1/stats

# Limpiar estadísticas antiguas
curl -X POST http://localhost:8080/api/database/performance/nplus1/cleanup?hoursOld=24
```

### Logs Útiles

```bash
# Logs de optimización
kubectl logs -f deployment/service | grep "DatabaseOptimization"

# Logs de N+1 detection
kubectl logs -f deployment/service | grep "NPlusOne"

# Logs de read replicas
kubectl logs -f deployment/service | grep "ReadReplica"

# Métricas de performance
curl http://localhost:8080/actuator/metrics/database.query.duration
```

## 🚀 Mejores Prácticas

### Desarrollo

1. **Usar anotaciones apropiadas** para read/write operations
2. **Monitorear N+1** en desarrollo con logs habilitados
3. **Probar con datos realistas** para detectar problemas de performance
4. **Revisar recomendaciones** del sistema regularmente
5. **Implementar paginación** para resultados grandes

### Producción

1. **Configurar read replicas** para distribución de carga
2. **Monitorear métricas** de performance continuamente
3. **Configurar alertas** para problemas críticos
4. **Realizar mantenimiento** programado de índices
5. **Revisar queries lentas** semanalmente

### Optimización

1. **Usar índices compuestos** para queries frecuentes
2. **Implementar particionado** para tablas grandes
3. **Optimizar connection pooling** según carga
4. **Configurar cache** para datos frecuentemente accedidos
5. **Monitorear uso de memoria** y ajustar configuración

## 🔮 Roadmap

### Funcionalidades Futuras

- [ ] Sharding automático de base de datos
- [ ] Optimización automática de índices con ML
- [ ] Cache distribuido integrado
- [ ] Análisis predictivo de performance
- [ ] Auto-scaling de read replicas
- [ ] Migración automática entre versiones de PostgreSQL

### Mejoras Planificadas

- [ ] Dashboard web integrado para monitoreo
- [ ] Integración con APM tools (New Relic, DataDog)
- [ ] Soporte para múltiples bases de datos
- [ ] Optimización automática de queries con IA
- [ ] Backup y recovery automatizado
