# Ad Engine - TODO

## 🚨 ERRORES CRÍTICOS DETECTADOS

### 1. **Referencias No Resueltas - DTOs Faltantes**

**Problema**: Múltiples clases referenciadas que no existen:

- `AdCreativeResponse` (usado en AdController.kt y AdSelectionService.kt)
- `AdImpression` (usado en AdController.kt y AdImpressionRepository.kt)
- `CampaignRepository` (usado en AdSelectionService.kt)

**Impacto**: Error de compilación por clases faltantes.

**Solución**:

```kotlin
// CREAR: dto/AdCreativeResponse.kt
data class AdCreativeResponse(
    val adUrl: String,
    val campaignId: Long,
    val creativeId: String,
    val duration: Int = 10,
    val skipAfter: Int = 5
)

// CREAR: model/AdImpression.kt
@Entity
@Table(name = "ad_impressions")
data class AdImpression(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val userId: String,
    val campaignId: Long,
    val creativeId: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val stationId: String? = null
)

// CREAR: repository/CampaignRepository.kt
@Repository
interface CampaignRepository : JpaRepository<AdCampaign, Long> {
    fun findActiveCampaignsForStation(stationId: String, currentDate: Date): List<AdCampaign>
}
```

### 2. **Configuración JWT Incorrecta**

**Problema**: JwtService usa `@Value("${jwt.secret}")` pero debería ser `@Value("${app.jwt.secret}")`.

**Solución**:

```kotlin
// config/JwtService.kt - línea 14
@Value("\${app.jwt.secret}")
private lateinit var secret: String
```

### 3. **Configuración Kafka Incorrecta**

**Problema**: KafkaConsumerConfig usa referencias no resueltas:

- `@Value("${spring.kafka.bootstrap-servers}")` - propiedad incorrecta
- Falta configuración de deserializers

**Solución**:

```kotlin
// config/KafkaConsumerConfig.kt
@Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
private lateinit var bootstrapServers: String
```

### 4. **Endpoint Duplicado en AdController**

**Problema**: `@GetMapping("/impressions")` aparece duplicado (líneas 43-44).

**Solución**:

```kotlin
// ELIMINAR: Una de las anotaciones duplicadas
@GetMapping("/impressions")
fun getImpressions(...)
```

### 5. **Dependencias JWT Faltantes**

**Problema**: Uso de `io.jsonwebtoken` sin dependencias en build.gradle.kts.

**Solución**:

```kotlin
// build.gradle.kts - agregar:
implementation("io.jsonwebtoken:jjwt-api:0.11.5")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
```

## 🏗️ PROBLEMAS DE ARQUITECTURA

### 1. **Arquitectura Hexagonal Incompleta**

**Problema**: Mezcla de patrones arquitectónicos sin consistencia.

**Estructura Actual**:

```
├── adapter/in/          # Hexagonal (incompleto)
├── config/             # Spring Config
├── controller/         # MVC Pattern
├── domain/            # DDD
├── dto/               # Data Transfer
├── service/           # Service Layer
└── repository/        # Data Access
```

**Solución Recomendada**:

```
src/main/kotlin/com/gasolinerajsm/adengine/
├── adapter/
│   ├── in/
│   │   ├── web/           # REST Controllers
│   │   └── messaging/     # Kafka Consumers
│   └── out/
│       ├── persistence/   # JPA Repositories
│       └── messaging/     # Kafka Producers
├── application/
│   ├── port/
│   │   ├── in/           # Use Cases
│   │   └── out/          # Repository Interfaces
│   └── service/          # Application Services
├── domain/
│   ├── model/           # Domain Entities
│   └── service/         # Domain Services
└── config/              # Configuration
```

### 2. **Lógica de Negocio en Controller**

**Problema**: AdController tiene lógica de creación de AdImpression.

**Solución**:

```kotlin
// MOVER: Lógica a AdImpressionService
// CREAR: application/service/AdImpressionService.kt
@Service
class AdImpressionService(
    private val adImpressionRepository: AdImpressionRepository
) {
    fun recordImpression(request: ImpressionRequest): AdImpression {
        val impression = AdImpression(
            userId = request.userId,
            campaignId = request.campaignId,
            creativeId = request.creativeId,
            timestamp = LocalDateTime.now()
        )
        return adImpressionRepository.save(impression)
    }
}
```

## 🔧 REFACTORING REQUERIDO

### Archivos a Crear:

1. **DTOs Faltantes**:

```kotlin
// dto/AdCreativeResponse.kt
data class AdCreativeResponse(
    val adUrl: String,
    val campaignId: Long,
    val creativeId: String,
    val duration: Int,
    val skipAfter: Int,
    val metadata: Map<String, Any> = emptyMap()
)

// dto/StartAdSequenceRequest.kt
data class StartAdSequenceRequest(
    val userId: String,
    val stationId: String,
    val sessionId: String
)

// dto/CompleteAdRequest.kt
data class CompleteAdRequest(
    val sequenceId: String,
    val stepCompleted: Int,
    val watchedDuration: Int
)

// dto/AdSequenceResponse.kt
data class AdSequenceResponse(
    val sequenceId: String,
    val currentStep: Int,
    val totalSteps: Int,
    val nextAd: AdCreativeResponse?,
    val rewardEarned: Int = 0,
    val isComplete: Boolean = false
)
```

2. **Entidades Faltantes**:

```kotlin
// model/AdImpression.kt
@Entity
@Table(name = "ad_impressions")
data class AdImpression(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val userId: String,
    val campaignId: Long,
    val creativeId: String,
    val stationId: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val duration: Int = 0,
    val completed: Boolean = false
)
```

3. **Repositorios Faltantes**:

```kotlin
// repository/CampaignRepository.kt
@Repository
interface CampaignRepository : JpaRepository<AdCampaign, Long> {
    fun findActiveCampaignsForStation(stationId: String, currentDate: Date): List<AdCampaign>
    fun findByStatusAndStartDateBeforeAndEndDateAfter(
        status: String,
        startDate: Date,
        endDate: Date
    ): List<AdCampaign>
}

// repository/AdSequenceRepository.kt
@Repository
interface AdSequenceRepository : JpaRepository<AdSequence, String> {
    fun findByUserIdAndStationId(userId: String, stationId: String): List<AdSequence>
    fun findActiveSequences(): List<AdSequence>
}
```

## 🔒 SEGURIDAD Y VALIDACIÓN

### Issues Identificados:

- [ ] JWT validation incompleta
- [ ] No hay rate limiting
- [ ] Falta validación de input
- [ ] No hay autorización por roles
- [ ] Logs exponen información sensible

### Implementación Requerida:

```kotlin
// config/SecurityConfig.kt - Mejorar
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf().disable()
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/ad/select").authenticated()
                    .requestMatchers("/ad/impression").authenticated()
                    .requestMatchers("/actuator/health").permitAll()
                    .anyRequest().hasRole("ADMIN")
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt()
            }
            .build()
    }
}
```

## 📊 MONITOREO Y OBSERVABILIDAD

### Faltante:

- [ ] Métricas de performance de ads
- [ ] Tracking de conversion rates
- [ ] Alertas para campaigns fallidas
- [ ] Dashboard de analytics en tiempo real

### Implementación:

```kotlin
// service/AdMetricsService.kt
@Service
class AdMetricsService(
    private val meterRegistry: MeterRegistry
) {
    private val impressionCounter = Counter.builder("ad.impressions")
        .description("Total ad impressions")
        .register(meterRegistry)

    fun recordImpression(campaignId: Long) {
        impressionCounter.increment(
            Tags.of("campaign_id", campaignId.toString())
        )
    }
}
```

## 🧪 TESTING REQUERIDO

### Tests Faltantes:

- [ ] Unit tests para AdSelectionService
- [ ] Integration tests para AdController
- [ ] Tests para Kafka consumers
- [ ] Performance tests para ad selection
- [ ] Security tests para JWT validation

## 📋 DEPENDENCIAS FALTANTES

### build.gradle.kts:

```kotlin
dependencies {
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Testing
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:kafka")
}
```

## 🚀 PRÓXIMOS PASOS

### Prioridad Alta:

1. Crear DTOs y entidades faltantes
2. Implementar repositorios faltantes
3. Corregir configuración JWT
4. Eliminar endpoint duplicado

### Prioridad Media:

1. Refactorizar a arquitectura hexagonal
2. Implementar validaciones de seguridad
3. Agregar métricas y monitoring
4. Implementar tests unitarios

### Prioridad Baja:

1. Optimizar algoritmo de selección de ads
2. Implementar A/B testing
3. Agregar cache distribuido
4. Implementar machine learning para targeting

## 📝 NOTAS ADICIONALES

- El servicio tiene una base sólida pero necesita completar las implementaciones faltantes
- La arquitectura hexagonal está parcialmente implementada
- Falta integración completa con Kafka para eventos
- El algoritmo de selección de ads es muy básico
- No hay implementación de frecuency capping o targeting avanzado
