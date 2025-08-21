# Raffle Service - TODO

## 🚨 ERRORES CRÍTICOS DETECTADOS

### 1. **Dependencia Faltante - Google Guava**

**Problema**: MerkleTree.kt usa `com.google.common.hash.Hashing` sin dependencia declarada.

**Error**: `Unresolved reference: common` (línea 4)

**Solución**:

```kotlin
// build.gradle.kts - agregar:
implementation("com.google.guava:guava:32.1.3-jre")

// O mejor, reemplazar con implementación nativa:
import java.security.MessageDigest

private fun hash(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}
```

### 2. **Referencia a Método Privado**

**Problema**: RaffleService.kt intenta acceder a `MerkleTreeGenerator.sha256()` que es privado.

**Error**: `Cannot access 'sha256': it is private in 'MerkleTreeGenerator'` (línea 119)

**Solución**:

```kotlin
// util/MerkleTreeGenerator.kt - hacer público:
companion object {
    fun sha256(input: String): String {
        // implementación
    }
}
```

### 3. **Clase MerkleTreeGenerator No Existe**

**Problema**: RaffleService usa `MerkleTreeGenerator` pero solo existe `MerkleTree`.

**Solución**:

```kotlin
// CREAR: util/MerkleTreeGenerator.kt
object MerkleTreeGenerator {
    fun generateMerkleRoot(entries: List<String>): String {
        return MerkleTree.build(entries).root
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
```

### 4. **Variables Inmutables Reasignadas**

**Problema**: Intento de reasignar variables `val` en RaffleService.kt.

**Errores**:

- Línea 46: `raffle.merkleRoot = merkleRoot`
- Línea 50: `raffle.status = RaffleStatus.CLOSED`

**Solución**:

```kotlin
// model/Raffle.kt - cambiar a var:
@Entity
data class Raffle(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val period: String,
    var merkleRoot: String,
    var status: RaffleStatus = RaffleStatus.OPEN,
    var drawAt: LocalDateTime? = null,
    var externalSeed: String? = null,
    var winnerEntryId: String? = null
)
```

### 5. **Referencias a Métodos No Existentes**

**Problema**: RaffleScheduler.kt usa métodos que no existen:

- `createRaffleForCurrentPeriod()` (línea 12)
- `createCarRaffle()` (línea 17)

**Solución**:

```kotlin
// service/RaffleService.kt - agregar métodos:
fun createRaffleForCurrentPeriod(): Raffle {
    val currentPeriod = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    return raffleRepository.save(Raffle(period = currentPeriod, merkleRoot = ""))
}

fun createCarRaffle(): Raffle {
    val yearlyPeriod = LocalDate.now().year.toString()
    return raffleRepository.save(
        Raffle(
            period = "YEARLY_$yearlyPeriod",
            merkleRoot = "",
            prize = "Toyota Corolla 2024"
        )
    )
}
```

## 🏗️ PROBLEMAS DE ARQUITECTURA

### 1. **Lógica de Negocio Compleja en Service**

**Problema**: RaffleService tiene demasiadas responsabilidades.

**Solución**:

```kotlin
// Separar en múltiples servicios:
// - RaffleManagementService (crear, cerrar)
// - RaffleDrawService (ejecutar sorteo)
// - ExternalSeedService (obtener semilla externa)
// - MerkleTreeService (generar árboles Merkle)
```

### 2. **Dependencia Externa Bloqueante**

**Problema**: Llamada bloqueante a API de Bitcoin.

**Solución**:

```kotlin
// service/ExternalSeedService.kt
@Service
class ExternalSeedService {
    suspend fun getBitcoinBlockhash(): String? {
        return try {
            webClient.get()
                .uri("/q/latesthash")
                .retrieve()
                .awaitBody<String>()
        } catch (e: Exception) {
            logger.error("Failed to get Bitcoin hash, using fallback", e)
            generateFallbackSeed()
        }
    }

    private fun generateFallbackSeed(): String {
        return UUID.randomUUID().toString()
    }
}
```

### 3. **Falta Validación de Datos**

**Problema**: No hay validación de inputs ni manejo de edge cases.

**Solución**:

```kotlin
// dto/CreateRaffleRequest.kt
data class CreateRaffleRequest(
    @field:NotBlank(message = "Period cannot be blank")
    @field:Pattern(regexp = "\\d{4}-\\d{2}", message = "Period must be in format YYYY-MM")
    val period: String,

    @field:NotBlank(message = "Prize description required")
    val prize: String,

    @field:Future(message = "Draw date must be in the future")
    val drawDate: LocalDateTime
)
```

## 🔧 REFACTORING REQUERIDO

### Estructura Recomendada:

```
src/main/kotlin/com/gasolinerajsm/raffleservice/
├── adapter/
│   ├── in/
│   │   ├── web/           # REST Controllers
│   │   └── scheduler/     # Scheduled Tasks
│   └── out/
│       ├── persistence/   # JPA Repositories
│       └── external/      # External API Clients
├── application/
│   ├── port/
│   │   ├── in/           # Use Cases
│   │   └── out/          # Repository Interfaces
│   └── service/          # Application Services
├── domain/
│   ├── model/           # Domain Entities
│   ├── service/         # Domain Services
│   └── event/           # Domain Events
└── infrastructure/
    ├── config/          # Configuration
    └── util/           # Utilities
```

### Servicios a Crear:

1. **RaffleManagementService**:

```kotlin
@Service
class RaffleManagementService(
    private val raffleRepository: RaffleRepository,
    private val pointsService: PointsService
) {
    fun createRaffle(request: CreateRaffleRequest): Raffle
    fun closeRaffle(raffleId: Long): Raffle
    fun getRaffleStatus(raffleId: Long): RaffleStatus
}
```

2. **RaffleDrawService**:

```kotlin
@Service
class RaffleDrawService(
    private val externalSeedService: ExternalSeedService,
    private val merkleTreeService: MerkleTreeService
) {
    suspend fun executeDrawAsync(raffleId: Long): RaffleWinner
    fun validateDraw(raffleId: Long): Boolean
}
```

3. **MerkleTreeService**:

```kotlin
@Service
class MerkleTreeService {
    fun generateMerkleRoot(entries: List<String>): String
    fun verifyMerkleProof(leaf: String, proof: List<String>, root: String): Boolean
    fun generateMerkleProof(entries: List<String>, targetEntry: String): List<String>
}
```

## 🔒 SEGURIDAD Y TRANSPARENCIA

### Issues Identificados:

- [ ] No hay verificación de integridad del sorteo
- [ ] Falta audit trail completo
- [ ] No hay validación de duplicados
- [ ] Semilla externa puede fallar sin fallback seguro

### Implementación Requerida:

```kotlin
// domain/service/RaffleAuditService.kt
@Service
class RaffleAuditService {
    fun createAuditLog(raffleId: Long, action: String, details: Map<String, Any>)
    fun verifyRaffleIntegrity(raffleId: Long): Boolean
    fun generateTransparencyReport(raffleId: Long): TransparencyReport
}

// model/RaffleAuditLog.kt
@Entity
data class RaffleAuditLog(
    @Id @GeneratedValue
    val id: Long = 0,
    val raffleId: Long,
    val action: String,
    val details: String, // JSON
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val hash: String // Hash of previous log + current data
)
```

## 📊 MONITOREO Y MÉTRICAS

### Faltante:

- [ ] Métricas de participación en sorteos
- [ ] Alertas para fallos en obtención de semilla externa
- [ ] Dashboard de transparencia pública
- [ ] Métricas de tiempo de ejecución de sorteos

### Implementación:

```kotlin
// service/RaffleMetricsService.kt
@Service
class RaffleMetricsService(
    private val meterRegistry: MeterRegistry
) {
    private val raffleCounter = Counter.builder("raffle.created")
        .description("Total raffles created")
        .register(meterRegistry)

    private val drawTimer = Timer.builder("raffle.draw.duration")
        .description("Time taken to execute raffle draw")
        .register(meterRegistry)

    fun recordRaffleCreated(period: String) {
        raffleCounter.increment(Tags.of("period", period))
    }

    fun recordDrawExecution(duration: Duration) {
        drawTimer.record(duration)
    }
}
```

## 🧪 TESTING REQUERIDO

### Tests Faltantes:

- [ ] Unit tests para MerkleTree generation
- [ ] Integration tests para raffle flow completo
- [ ] Tests de determinismo del sorteo
- [ ] Tests de fallback cuando Bitcoin API falla
- [ ] Performance tests para sorteos con muchos participantes

### Test Structure:

```kotlin
// test/service/RaffleServiceTest.kt
@ExtendWith(MockitoExtension::class)
class RaffleServiceTest {
    @Test
    fun `should create raffle with valid entries`()

    @Test
    fun `should select winner deterministically`()

    @Test
    fun `should handle external seed failure gracefully`()

    @Test
    fun `should prevent double drawing`()
}
```

## 📋 DEPENDENCIAS FALTANTES

### build.gradle.kts:

```kotlin
dependencies {
    // Crypto y Hash
    implementation("com.google.guava:guava:32.1.3-jre")
    // O alternativa nativa:
    // implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    // WebClient para APIs externas
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Coroutines para async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.testcontainers:postgresql")
}
```

## 🚀 PRÓXIMOS PASOS

### Prioridad Alta:

1. Agregar dependencia Guava o implementar hash nativo
2. Crear MerkleTreeGenerator faltante
3. Corregir variables inmutables en modelo
4. Implementar métodos faltantes en RaffleService

### Prioridad Media:

1. Refactorizar a arquitectura hexagonal
2. Implementar manejo de errores robusto
3. Agregar audit logging completo
4. Implementar tests unitarios

### Prioridad Baja:

1. Optimizar algoritmo de Merkle Tree
2. Implementar múltiples fuentes de semilla externa
3. Agregar dashboard de transparencia
4. Implementar notificaciones de ganadores

## 📝 NOTAS ADICIONALES

- El concepto de usar Merkle Trees para transparencia es excelente
- La integración con Bitcoin para semilla externa es innovadora
- Necesita refactoring significativo para ser production-ready
- La lógica de sorteo determinístico es correcta en principio
- Falta implementación de verificación pública de resultados
- El servicio tiene potencial para ser muy robusto con las correcciones adecuadas
