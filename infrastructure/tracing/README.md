# Distributed Tracing with Jaeger - Gasolinera JSM

Este directorio contiene la configuración completa de trazabilidad distribuida usando OpenTelemetry y Jaeger para el ecosistema de microservicios Gasolinera JSM.

## 📋 Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Configuración](#configuración)
- [Instalación](#instalación)
- [Uso](#uso)
- [Instrumentación](#instrumentación)
- [Análisis de Trazas](#análisis-de-trazas)
- [Troubleshooting](#troubleshooting)

## 🏗️ Arquitectura

### Componentes de Tracing

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Microservice   │    │ OpenTelemetry   │    │ Jaeger Collector│
│                 │───►│   SDK/Agent     │───►│                 │
│ - Auth Service  │    │                 │    │ - Receives      │
│ - Station Svc   │    │ - Instrumentation│    │ - Validates     │
│ - Coupon Svc    │    │ - Context Prop  │    │ - Stores        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                               ┌─────────────────┐
                                               │ Jaeger Query    │
                                               │                 │
                                               │ - UI Interface  │
                                               │ - Search API    │
                                               │ - Analytics     │
                                               └─────────────────┘
                                                       │
                                               ┌─────────────────┐
                                               │  Elasticsearch  │
                                               │                 │
                                               │ - Trace Storage │
                                               │ - Indexing      │
                                               │ - Retention     │
                                               └─────────────────┘
```

### Flujo de Trazas

1. **Generación**: Los microservicios generan spans usando OpenTelemetry SDK
2. **Propagación**: El contexto de traza se propaga entre servicios via HTTP headers
3. **Recolección**: Jaeger Collector recibe y procesa las trazas
4. **Almacenamiento**: Las trazas se almacenan en Elasticsearch
5. **Consulta**: Jaeger Query UI permite buscar y analizar trazas

## ⚙️ Configuración

### Variables de Entorno

```bash
# Tracing Configuration
export TRACING_ENABLED=true
export JAEGER_ENDPOINT=http://localhost:14250
export JAEGER_SAMPLER_TYPE=probabilistic
export JAEGER_SAMPLER_PARAM=0.1

# Service Configuration
export OTEL_SERVICE_NAME=auth-service
export OTEL_SERVICE_VERSION=1.0.0
export OTEL_RESOURCE_ATTRIBUTES=service.namespace=gasolinera-jsm,deployment.environment=development
```

### Configuración Spring Boot

```yaml
# application.yml
tracing:
  enabled: true
  jaeger:
    endpoint: http://localhost:14250
    timeoutSeconds: 10
  sampling:
    type: ratio
    ratio: 0.1
  interceptor:
    enabled: true
    includePatterns:
      - '/api/**'
    excludePatterns:
      - '/actuator/**'
      - '/swagger-ui/**'
  customSpans:
    database: true
    redis: true
    rabbitmq: true
    externalApis: true
    businessOperations: true
```

## 🚀 Instalación

### 1. Desarrollo (All-in-One)

```bash
# Iniciar Jaeger All-in-One para desarrollo
cd infrastructure/tracing
docker-compose --profile development up -d

# Verificar que Jaeger esté ejecutándose
curl http://localhost:16686
```

### 2. Producción (Componentes Separados)

```bash
# Iniciar stack completo de producción
cd infrastructure/tracing
docker-compose --profile production up -d

# Verificar componentes
curl http://localhost:9200/_cluster/health  # Elasticsearch
curl http://localhost:14269/               # Jaeger Collector
curl http://localhost:16686/               # Jaeger UI
```

### 3. Con OpenTelemetry Collector

```bash
# Iniciar con OTel Collector
cd infrastructure/tracing
docker-compose --profile otel up -d

# Verificar OTel Collector
curl http://localhost:13133/  # Health check
curl http://localhost:8888/   # Metrics
```

## 💻 Uso

### Instrumentación Automática

```kotlin
// Configuración automática en Spring Boot
@SpringBootApplication
@EnableAutoConfiguration
class GasolineraApplication

// Las trazas HTTP se crean automáticamente
@RestController
class CouponController {
    @GetMapping("/coupons/{id}")
    fun getCoupon(@PathVariable id: String): CouponDto {
        // Span automático: "GET /coupons/{id}"
        return couponService.findById(id)
    }
}
```

### Instrumentación Manual con Anotaciones

```kotlin
@Service
class CouponService(
    private val businessTracingService: BusinessTracingService
) {

    @TraceBusinessOperation(
        operation = "coupon.validate",
        entityType = "coupon",
        includeParameters = true
    )
    fun validateCoupon(couponCode: String): ValidationResult {
        // Span automático con contexto de negocio
        return performValidation(couponCode)
    }

    @TraceDatabaseOperation(
        operation = "SELECT",
        table = "coupons"
    )
    fun findByCode(code: String): Coupon? {
        // Span de base de datos automático
        return couponRepository.findByCode(code)
    }
}
```

### Instrumentación Programática

```kotlin
@Service
class RaffleService(
    private val businessTracingService: BusinessTracingService
) {

    fun conductDraw(raffleId: String): DrawResult {
        return businessTracingService.traceRaffleOperation(
            operation = RaffleOperation.DRAW,
            raffleId = raffleId,
            userId = getCurrentUserId()
        ) {
            // Lógica del sorteo
            val participants = getParticipants(raffleId)
            val winner = selectWinner(participants)

            // Agregar eventos al span
            businessTracingService.addSpanEvent("participants_loaded", mapOf(
                "count" to participants.size
            ))

            businessTracingService.addSpanEvent("winner_selected", mapOf(
                "winner_id" to winner.id
            ))

            DrawResult(winner, participants.size)
        }
    }
}
```

### Propagación de Contexto

```kotlin
@Service
class RedemptionService(
    private val contextPropagation: TracingContextPropagation,
    private val restTemplate: RestTemplate
) {

    fun processRedemption(redemption: Redemption) {
        // Crear RestTemplate con propagación automática
        val tracedRestTemplate = contextPropagation.createTracedRestTemplate()

        // El contexto se propaga automáticamente
        val response = tracedRestTemplate.postForEntity(
            "http://coupon-service/api/v1/coupons/validate",
            ValidationRequest(redemption.couponCode),
            ValidationResponse::class.java
        )

        // Procesar respuesta...
    }

    @Async
    fun processAsync(redemption: Redemption): CompletableFuture<Result> {
        // Propagar contexto en operaciones asíncronas
        return contextPropagation.withCurrentSpan {
            CompletableFuture.supplyAsync {
                // Procesamiento asíncrono con contexto
                performAsyncProcessing(redemption)
            }
        }
    }
}
```

## 🔍 Análisis de Trazas

### Jaeger UI

Accede a la interfaz de Jaeger en `http://localhost:16686`

**Características principales:**

- **Search**: Buscar trazas por servicio, operación, tags
- **Timeline**: Vista temporal de spans en una traza
- **Dependencies**: Mapa de dependencias entre servicios
- **Compare**: Comparar múltiples trazas
- **Statistics**: Estadísticas de latencia y errores

### Búsquedas Útiles

```
# Buscar trazas de errores
Tags: error=true

# Buscar operaciones lentas
Min Duration: 2s

# Buscar por usuario específico
Tags: user.id=12345

# Buscar operaciones de cupones
Service: coupon-service
Operation: coupon.use

# Buscar por estación
Tags: station.id=station-001

# Buscar trazas con muchos spans
Min Duration: 1s
Max Duration: 10s
```

### Métricas de Tracing

```
# Latencia por percentil
jaeger_query_requests_duration_seconds{quantile="0.95"}

# Tasa de errores
rate(jaeger_query_requests_total{status_code!="200"}[5m])

# Throughput de trazas
rate(jaeger_spans_received_total[5m])

# Uso de almacenamiento
elasticsearch_indices_store_size_bytes{index=~"jaeger.*"}
```

## 🔧 Configuración Avanzada

### Sampling Strategies

```yaml
# Estrategias de muestreo por servicio
sampling:
  default-strategy:
    type: probabilistic
    param: 0.1 # 10% por defecto

  per-service-strategies:
    - service: 'auth-service'
      type: probabilistic
      param: 0.2 # 20% para autenticación

    - service: 'coupon-service'
      type: adaptive
      max-traces-per-second: 100
```

### Custom Attributes

```kotlin
// Agregar atributos personalizados
businessTracingService.addSpanAttributes(mapOf(
    "business.campaign.id" to campaignId,
    "business.coupon.type" to couponType,
    "business.discount.amount" to discountAmount,
    "business.user.segment" to userSegment
))

// Agregar eventos con contexto
businessTracingService.addSpanEvent("coupon_validated", mapOf(
    "validation.result" to "success",
    "validation.duration_ms" to validationTime,
    "validation.rules_applied" to rulesCount
))
```

### Error Tracking

```kotlin
try {
    processPayment(payment)
} catch (ex: PaymentException) {
    // El error se registra automáticamente en el span
    Span.current().recordException(ex)
    Span.current().setStatus(StatusCode.ERROR, ex.message)
    throw ex
}
```

## 📊 Dashboards y Alertas

### Grafana Dashboards

Crear dashboards para:

- **Service Performance**: Latencia y throughput por servicio
- **Error Rates**: Tasa de errores por operación
- **Dependencies**: Mapa de dependencias en tiempo real
- **Business Metrics**: Métricas de negocio por traza

### Alertas Prometheus

```yaml
# Alta latencia en operaciones críticas
- alert: HighLatencyBusinessOperation
  expr: histogram_quantile(0.95, rate(jaeger_operation_duration_seconds_bucket{operation=~"coupon.use|raffle.draw"}[5m])) > 2
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: 'High latency in business operation'

# Tasa de errores alta
- alert: HighErrorRate
  expr: rate(jaeger_spans_total{status="error"}[5m]) / rate(jaeger_spans_total[5m]) > 0.05
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: 'High error rate in traces'
```

## 🔍 Troubleshooting

### Problemas Comunes

#### Trazas No Aparecen

```bash
# Verificar conectividad con Jaeger
curl http://localhost:14250/api/traces

# Verificar configuración de sampling
curl http://localhost:14268/api/sampling

# Verificar logs del collector
docker logs gasolinera-jaeger-collector
```

#### Contexto No Se Propaga

```kotlin
// Verificar headers HTTP
val traceInfo = contextPropagation.getCurrentTraceInfo()
logger.info("Current trace: ${traceInfo?.traceId}")

// Verificar propagación manual
val headers = contextPropagation.createTraceHeaders()
logger.info("Trace headers: $headers")
```

#### Performance Issues

```bash
# Verificar uso de memoria de Elasticsearch
curl http://localhost:9200/_nodes/stats/jvm

# Verificar índices de Jaeger
curl http://localhost:9200/_cat/indices/jaeger*

# Optimizar sampling rate
# Reducir sampling ratio en configuración
```

### Logs Útiles

```bash
# Logs de Jaeger Collector
docker logs gasolinera-jaeger-collector --tail 100

# Logs de Elasticsearch
docker logs gasolinera-elasticsearch --tail 100

# Logs de aplicación con trace IDs
grep "traceId" application.log
```

## 📚 Referencias

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Distributed Tracing Best Practices](https://opentelemetry.io/docs/concepts/observability-primer/)
- [Spring Boot OpenTelemetry](https://opentelemetry.io/docs/instrumentation/java/spring-boot/)
- [Jaeger Performance Tuning](https://www.jaegertracing.io/docs/deployment/#performance-tuning)
