# Sistema de Logging Centralizado - Gasolinera JSM

Este directorio contiene la configuración completa del sistema de logging estructurado y centralizado para todos los microservicios de Gasolinera JSM.

## Arquitectura del Sistema de Logging

### Componentes Principales

1. **Shared Logging Library** - Biblioteca compartida para logging estructurado
2. **ELK Stack** - Elasticsearch, Logstash, Kibana para agregación y visualización
3. **Filebeat** - Recolección de logs desde archivos y contenedores Docker
4. **Structured Logging** - Formato JSON con contexto enriquecido

### Características Implementadas

- ✅ Logging estructurado en formato JSON
- ✅ Correlation IDs para tracking cross-service
- ✅ MDC (Mapped Diagnostic Context) automático
- ✅ Categorización de logs (Business, Security, Performance, HTTP)
- ✅ Filtros y appenders especializados
- ✅ Integración con tracing distribuido
- ✅ Configuración para múltiples ambientes

## Configuración por Ambiente

### Desarrollo Local

```yaml
# application-dev.yml
gasolinera:
  logging:
    level: DEBUG
    format: JSON
    mdc:
      enabled: true
    correlation:
      generateIfMissing: true
```

### Producción

```yaml
# application-prod.yml
gasolinera:
  logging:
    level: WARN
    format: JSON
    elk:
      enabled: true
      host: elasticsearch-cluster
      port: 9200
```

## Uso de la Biblioteca de Logging

### Logging Básico

```kotlin
@Service
class CouponService {
    private val logger = StructuredLogger.getLogger(CouponService::class.java)

    @Autowired
    private lateinit var structuredLogger: StructuredLogger

    fun generateCoupon(userId: String): Coupon {
        structuredLogger.info(
            logger,
            "Generating coupon for user",
            context = mapOf("userId" to userId)
        )

        // Lógica de negocio...

        structuredLogger.business(
            logger,
            BusinessOperation.COUPON_GENERATION,
            "Coupon generated successfully",
            userId = userId,
            entityId = coupon.id,
            additionalContext = mapOf(
                "couponType" to coupon.type,
                "campaignId" to coupon.campaignId
            )
        )

        return coupon
    }
}
```

### Logging con Contexto MDC

```kotlin
fun processRedemption(redemptionId: String) {
    MdcUtils.withMdcContext(
        mapOf(
            "businessOperation" to "COUPON_REDEMPTION",
            "entityId" to redemptionId
        )
    ) {
        // Todo el logging dentro de este bloque tendrá el contexto automáticamente
        structuredLogger.info(logger, "Processing redemption")

        // Lógica de procesamiento...

        structuredLogger.info(logger, "Redemption processed successfully")
    }
}
```

### Logging de Performance

```kotlin
@LogPerformance(operation = "GENERATE_COUPONS", threshold = 2000)
fun generateBulkCoupons(campaignId: String, count: Int): List<Coupon> {
    val startTime = System.currentTimeMillis()

    try {
        // Lógica de generación...

        val duration = System.currentTimeMillis() - startTime
        structuredLogger.performance(
            logger,
            "BULK_COUPON_GENERATION",
            duration,
            true,
            mapOf(
                "campaignId" to campaignId,
                "couponCount" to count
            )
        )

        return coupons
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        structuredLogger.performance(
            logger,
            "BULK_COUPON_GENERATION",
            duration,
            false,
            mapOf(
                "campaignId" to campaignId,
                "couponCount" to count,
                "error" to e.message
            )
        )
        throw e
    }
}
```

### Logging de Seguridad

```kotlin
@PostMapping("/auth/login")
fun login(@RequestBody request: LoginRequest, httpRequest: HttpServletRequest): ResponseEntity<*> {
    structuredLogger.security(
        logger,
        "LOGIN_ATTEMPT",
        userId = request.username,
        ipAddress = httpRequest.remoteAddr,
        userAgent = httpRequest.getHeader("User-Agent"),
        success = false, // Se actualizará después
        additionalContext = mapOf(
            "loginMethod" to "PASSWORD"
        )
    )

    try {
        val result = authService.authenticate(request)

        structuredLogger.security(
            logger,
            "LOGIN_SUCCESS",
            userId = result.userId,
            ipAddress = httpRequest.remoteAddr,
            success = true
        )

        return ResponseEntity.ok(result)
    } catch (e: AuthenticationException) {
        structuredLogger.security(
            logger,
            "LOGIN_FAILED",
            userId = request.username,
            ipAddress = httpRequest.remoteAddr,
            success = false,
            additionalContext = mapOf(
                "failureReason" to e.message
            )
        )
        throw e
    }
}
```

## Despliegue del Stack ELK

### Iniciar el Stack Completo

```bash
# Desde el directorio infrastructure/logging
docker-compose -f docker-compose.logging.yml up -d

# Verificar que todos los servicios estén funcionando
docker-compose -f docker-compose.logging.yml ps
```

### Acceso a las Interfaces

- **Kibana**: http://localhost:5601
- **Elasticsearch**: http://localhost:9200
- **Logstash**: http://localhost:9600 (API de monitoreo)

### Configurar Dashboards en Kibana

1. Acceder a Kibana en http://localhost:5601
2. Ir a "Stack Management" > "Index Patterns"
3. Crear index patterns:
   - `gasolinera-logs-*` (logs generales)
   - `gasolinera-business-*` (logs de negocio)
   - `gasolinera-security-*` (logs de seguridad)
   - `gasolinera-performance-*` (logs de performance)

### Queries Útiles en Kibana

#### Buscar por Correlation ID

```
mdc.correlationId: "abc123def456"
```

#### Logs de un Usuario Específico

```
mdc.userId: "user123" AND log_category: "BUSINESS"
```

#### Errores en los Últimos 15 minutos

```
level: "ERROR" AND @timestamp: [now-15m TO now]
```

#### Requests HTTP Lentos

```
log_category: "PERFORMANCE" AND duration_ms: >2000
```

#### Intentos de Login Fallidos

```
log_category: "SECURITY" AND event.action: "LOGIN_FAILED"
```

## Monitoreo y Alertas

### Métricas Importantes a Monitorear

1. **Volumen de Logs por Servicio**
2. **Errores por Minuto**
3. **Requests HTTP por Status Code**
4. **Tiempo de Respuesta Promedio**
5. **Intentos de Autenticación Fallidos**
6. **Operaciones de Negocio por Tipo**

### Configuración de Alertas

Las alertas se pueden configurar en Kibana usando Watcher o integrando con sistemas externos como:

- **Prometheus AlertManager**
- **Slack/Teams Webhooks**
- **Email Notifications**
- **PagerDuty**

## Integración con Servicios

### Agregar Logging a un Nuevo Servicio

1. **Agregar Dependencia**

```kotlin
// build.gradle.kts
implementation(project(":shared:logging"))
implementation("net.logstash.logback:logstash-logback-encoder:7.4")
```

2. **Configurar Application Properties**

```yaml
# application.yml
gasolinera:
  logging:
    level: INFO
    mdc:
      enabled: true
    business:
      enabled: true
```

3. **Usar en el Código**

```kotlin
@RestController
class MyController {
    private val logger = StructuredLogger.getLogger(MyController::class.java)

    @Autowired
    private lateinit var structuredLogger: StructuredLogger

    // Usar structuredLogger en lugar de logger tradicional
}
```

## Troubleshooting

### Logs No Aparecen en Kibana

1. Verificar que Elasticsearch esté funcionando: `curl http://localhost:9200/_cluster/health`
2. Verificar que Logstash esté procesando: `curl http://localhost:9600/_node/stats`
3. Revisar logs de Logstash: `docker logs gasolinera-logstash`
4. Verificar configuración de Filebeat: `docker logs gasolinera-filebeat`

### Performance del Stack ELK

1. **Ajustar Heap Size de Elasticsearch**

```yaml
environment:
  - 'ES_JAVA_OPTS=-Xms1g -Xmx1g'
```

2. **Optimizar Logstash Workers**

```yaml
# logstash.yml
pipeline.workers: 4
pipeline.batch.size: 250
```

3. **Configurar Retention de Índices**

```bash
# Eliminar índices antiguos (más de 30 días)
curl -X DELETE "localhost:9200/gasolinera-logs-$(date -d '30 days ago' +%Y.%m.%d)"
```

## Seguridad

### Configuración de Autenticación (Producción)

```yaml
# Para producción, habilitar seguridad en Elasticsearch
elasticsearch:
  environment:
    - xpack.security.enabled=true
    - ELASTIC_PASSWORD=your-secure-password
```

### Enmascaramiento de Datos Sensibles

Los logs automáticamente enmascaran:

- Passwords
- Tokens JWT (parcialmente)
- Números de tarjeta de crédito
- Información PII sensible

## Mantenimiento

### Rotación de Logs

- Los archivos de log se rotan automáticamente por tamaño y tiempo
- Los índices de Elasticsearch deben rotarse manualmente o con ILM

### Backup

- Configurar snapshots regulares de Elasticsearch
- Backup de configuraciones de Logstash y Kibana

### Monitoreo del Sistema

- Monitorear uso de disco de Elasticsearch
- Alertas por fallos en el pipeline de Logstash
- Monitoreo de memoria y CPU del stack ELK
