# Módulo de Resilience

Este módulo proporciona una implementación completa de patrones de resilience para la aplicación Gasolinera JSM, basado en Resilience4j y Spring Boot.

## Características

### Patrones de Resilience Implementados

1. **Circuit Breaker**: Previene llamadas a servicios que están fallando
2. **Retry**: Reintenta operaciones que fallan temporalmente
3. **Bulkhead**: Aísla recursos para prevenir fallos en cascada
4. **Rate Limiter**: Controla la tasa de llamadas a servicios
5. **Time Limiter**: Establece timeouts para operaciones
6. **Fallback**: Proporciona respuestas alternativas cuando fallan las operaciones

### Componentes Principales

- **ResilienceService**: Servicio central para aplicar patrones de resilience
- **FallbackService**: Gestiona respuestas de fallback personalizadas
- **ResilienceMetricsService**: Recopila y expone métricas de resilience
- **ResilienceController**: API REST para monitoreo y gestión
- **ResilienceAspect**: Interceptor AOP para anotaciones automáticas

## Uso

### Configuración

Añadir la dependencia en `build.gradle.kts`:

```kotlin
implementation(project(":shared:resilience"))
```

Incluir el perfil de resilience en `application.yml`:

```yaml
spring:
  profiles:
    include: resilience
```

### Uso Programático

```kotlin
@Service
class MyService(private val resilienceService: ResilienceService) {

    fun getData(id: String): String {
        return resilienceService.executeWithCircuitBreaker("my-service") {
            // Operación que puede fallar
            externalService.getData(id)
        }
    }

    fun getDataWithMultiplePatterns(id: String): String {
        val config = ResilienceConfig(
            circuitBreakerName = "external-service",
            retryName = "external-service",
            rateLimiterName = "api-calls"
        )

        return resilienceService.executeWithResilience(config, {
            externalService.getData(id)
        }, {
            "Fallback data"
        })
    }
}
```

### Uso con Anotaciones

```kotlin
@Service
class MyService {

    @CircuitBreaker(name = "database", fallbackMethod = "getFallbackData")
    fun getData(id: String): String {
        return databaseService.findById(id)
    }

    fun getFallbackData(id: String, ex: Exception): String {
        return "Fallback data for $id"
    }

    @Resilient(
        circuitBreaker = "external-service",
        retry = "external-service",
        rateLimiter = "api-calls",
        fallbackMethod = "getExternalDataFallback"
    )
    fun getExternalData(id: String): String {
        return externalService.getData(id)
    }

    fun getExternalDataFallback(id: String, ex: Exception): String {
        return "External service unavailable"
    }
}
```

## Configuración

### Configuración por Defecto

El módulo incluye configuraciones predefinidas para diferentes tipos de servicios:

- **database**: Circuit breaker optimizado para operaciones de base de datos
- **redis**: Circuit breaker para operaciones de cache
- **external-service**: Circuit breaker para servicios externos
- **payment-gateway**: Circuit breaker para gateway de pagos
- **notification-service**: Circuit breaker para servicios de notificación

### Configuración Personalizada

```yaml
gasolinera:
  resilience:
    enabled: true
    circuit-breakers:
      my-service:
        failure-rate-threshold: 50.0
        wait-duration-in-open-state: 60s
        sliding-window-size: 100
        minimum-number-of-calls: 10
    retries:
      my-service:
        max-attempts: 3
        wait-duration: 500ms
        interval-function: EXPONENTIAL_BACKOFF
    fallbacks:
      enabled: true
      cache-enabled: true
      cache-ttl: 5m
```

## Monitoreo

### API REST

El módulo expone endpoints REST para monitoreo:

- `GET /api/resilience/metrics` - Métricas consolidadas
- `GET /api/resilience/health` - Resumen de salud
- `GET /api/resilience/circuit-breakers` - Estado de circuit breakers
- `GET /api/resilience/circuit-breakers/{name}` - Estado específico
- `POST /api/resilience/circuit-breakers/{name}/transition` - Cambiar estado
- `POST /api/resilience/circuit-breakers/{name}/reset` - Resetear circuit breaker

### Métricas Prometheus

Las métricas se exponen automáticamente para Prometheus:

```
resilience_circuitbreaker_failure_rate{name="database"}
resilience_circuitbreaker_slow_call_rate{name="database"}
resilience_retry_attempts{name="external-service"}
resilience_bulkhead_available_concurrent_calls{name="payment-gateway"}
resilience_ratelimiter_available_permissions{name="api-calls"}
```

### Health Checks

Los circuit breakers se integran automáticamente con Spring Boot Actuator:

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "database": {
          "status": "UP",
          "details": {
            "state": "CLOSED",
            "failureRate": "0.0%"
          }
        }
      }
    }
  }
}
```

## Fallbacks Personalizados

### Registro de Estrategias

```kotlin
@Component
class CustomFallbackStrategies(private val fallbackService: FallbackService) {

    @PostConstruct
    fun registerStrategies() {
        fallbackService.registerFallbackStrategy("user-profile", FallbackStrategy(
            cacheable = true,
            cacheTtl = Duration.ofMinutes(10)
        ) { context ->
            UserProfile(
                id = context.parameters["userId"] as String,
                name = "Usuario Temporal",
                email = "temp@example.com"
            )
        })
    }
}
```

### Fallbacks Predefinidos

El módulo incluye fallbacks predefinidos para operaciones comunes:

- **coupon-generation**: Respuesta cuando el servicio de cupones no está disponible
- **user-authentication**: Permite acceso de invitado cuando la autenticación falla
- **station-lookup**: Devuelve estaciones estáticas cuando el servicio falla
- **coupon-redemption**: Permite redención offline
- **raffle-participation**: Muestra rifas cacheadas
- **ad-serving**: Muestra anuncio por defecto

## Testing

### Tests Unitarios

```kotlin
@Test
fun `should use fallback when circuit breaker is open`() {
    // Given
    val fallbackResult = "fallback"

    // When
    val result = resilienceService.executeWithCircuitBreaker(
        name = "test-cb",
        operation = { throw RuntimeException("Service down") },
        fallback = { fallbackResult }
    )

    // Then
    assertEquals(fallbackResult, result)
}
```

### Tests de Integración

```kotlin
@SpringBootTest
@TestPropertySource(properties = ["gasolinera.resilience.enabled=true"])
class ResilienceIntegrationTest {

    @Autowired
    private lateinit var resilienceService: ResilienceService

    @Test
    fun `should apply multiple resilience patterns`() {
        val config = ResilienceConfig(
            circuitBreakerName = "test-cb",
            retryName = "test-retry"
        )

        val result = resilienceService.executeWithResilience(config) {
            "success"
        }

        assertEquals("success", result)
    }
}
```

## Mejores Prácticas

### 1. Configuración por Tipo de Servicio

- **Base de datos**: Circuit breaker con retry rápido
- **Servicios externos**: Circuit breaker con retry exponencial
- **Cache**: Circuit breaker permisivo con fallback rápido
- **Pagos**: Circuit breaker conservador con retry limitado

### 2. Fallbacks Significativos

- Proporcionar datos estáticos útiles
- Permitir funcionalidad degradada
- Informar al usuario sobre la situación
- Registrar eventos para análisis posterior

### 3. Monitoreo Continuo

- Configurar alertas para circuit breakers abiertos
- Monitorear tasas de fallo y latencia
- Revisar regularmente configuraciones
- Analizar patrones de fallo

### 4. Testing

- Probar todos los escenarios de fallo
- Validar comportamiento de fallbacks
- Verificar configuraciones en diferentes entornos
- Realizar pruebas de carga para validar bulkheads

## Integración con Otros Módulos

### Logging

Los eventos de resilience se registran automáticamente:

```
INFO  - Circuit breaker 'database' transitioned to OPEN state
WARN  - Retry attempt 2/3 failed for operation 'external-service'
INFO  - Fallback executed for operation 'coupon-generation'
```

### Métricas

Las métricas se integran con el sistema de monitoreo:

```kotlin
@Component
class BusinessMetrics(private val resilienceMetricsService: ResilienceMetricsService) {

    @Scheduled(fixedRate = 60000)
    fun reportResilienceHealth() {
        val health = resilienceMetricsService.getResilienceHealthSummary()
        if (health.overallHealth == ResilienceHealth.CRITICAL) {
            alertService.sendAlert("Resilience health is critical")
        }
    }
}
```

### Tracing

Los patrones de resilience se integran con el tracing distribuido para proporcionar visibilidad completa de las operaciones.

## Troubleshooting

### Circuit Breaker Siempre Abierto

1. Verificar configuración de `failure-rate-threshold`
2. Revisar logs para identificar causa de fallos
3. Validar que las excepciones están siendo registradas correctamente
4. Considerar ajustar `minimum-number-of-calls`

### Retry Infinito

1. Verificar configuración de `max-attempts`
2. Asegurar que las excepciones están en `retry-exceptions`
3. Revisar `interval-function` para evitar sobrecarga
4. Implementar fallback como último recurso

### Bulkhead Saturado

1. Revisar `max-concurrent-calls`
2. Analizar patrones de uso y latencia
3. Considerar aumentar capacidad o implementar queue
4. Optimizar operaciones para reducir tiempo de ejecución

### Rate Limiter Muy Restrictivo

1. Ajustar `limit-for-period` según capacidad real
2. Revisar `limit-refresh-period`
3. Implementar fallback para requests limitados
4. Considerar rate limiting distribuido para múltiples instancias
