# Station Service - TODO

## 🚨 ERRORES CRÍTICOS DETECTADOS

### 1. **Duplicación de Entidad Station**

**Problema**: Existen dos definiciones de la entidad `Station`:

- `services/station-service/src/main/kotlin/com/gasolinerajsm/stationservice/repository/StationRepository.kt` (líneas 12-20)
- `services/station-service/src/main/kotlin/com/gasolinerajsm/stationservice/model/Station.kt` (líneas 8-16)

**Impacto**: Error de compilación por definiciones conflictivas.

**Solución**:

```kotlin
// ELIMINAR: StationRepository.kt líneas 12-20
// MANTENER: model/Station.kt como única definición
// MOVER: toDto() extension function a un archivo separado
```

### 2. **Inconsistencia en Campos de Station**

**Problema**: Las dos definiciones tienen campos diferentes:

- Repository version: `id, name, location`
- Model version: `id, name, latitude, longitude, status`

**Solución**:

```kotlin
// Usar la definición del model con campos completos:
@Entity
@Table(name = "stations")
data class Station(
    @Id
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "ACTIVE"
)
```

### 3. **Import Circular y Dependencias Incorrectas**

**Problema**: StationService importa DTOs del controller, violando arquitectura hexagonal.

**Solución**:

```kotlin
// MOVER DTOs de controller/ a dto/ package
// ACTUALIZAR imports en StationService
// SEPARAR concerns: Controller -> Service -> Repository
```

### 4. **Falta Repository Interface**

**Problema**: No hay interface de repository separada de la entidad.

**Solución**:

```kotlin
// CREAR: repository/StationRepository.kt (solo interface)
@Repository
interface StationRepository : JpaRepository<Station, String> {
    fun findByStatus(status: String): List<Station>
    fun findByNameContainingIgnoreCase(name: String): List<Station>
}
```

## 🔧 REFACTORING REQUERIDO

### Estructura Recomendada:

```
src/main/kotlin/com/gasolinerajsm/stationservice/
├── controller/
│   └── StationController.kt
├── dto/
│   ├── StationDto.kt
│   ├── CreateStationDto.kt
│   └── UpdateStationDto.kt
├── service/
│   └── StationService.kt
├── repository/
│   └── StationRepository.kt
├── model/
│   └── Station.kt
└── exception/
    └── StationNotFoundException.kt
```

### Pasos de Refactoring:

1. **Eliminar duplicación de entidad**

   ```bash
   # Eliminar líneas 12-20 de StationRepository.kt
   # Mantener solo model/Station.kt
   ```

2. **Crear DTOs separados**

   ```kotlin
   // dto/StationDto.kt
   data class StationDto(
       val id: String,
       val name: String,
       val latitude: Double,
       val longitude: Double,
       val status: String
   )
   ```

3. **Crear extension functions**

   ```kotlin
   // mapper/StationMapper.kt
   fun Station.toDto(): StationDto = StationDto(...)
   fun CreateStationDto.toEntity(): Station = Station(...)
   ```

4. **Implementar exception handling**
   ```kotlin
   // exception/StationNotFoundException.kt
   class StationNotFoundException(id: String) :
       RuntimeException("Station with id $id not found")
   ```

## 🧪 TESTING REQUERIDO

### Tests Faltantes:

- [ ] Unit tests para StationService
- [ ] Integration tests para StationController
- [ ] Repository tests con @DataJpaTest
- [ ] Validation tests para DTOs

### Test Structure:

```
src/test/kotlin/com/gasolinerajsm/stationservice/
├── controller/
│   └── StationControllerTest.kt
├── service/
│   └── StationServiceTest.kt
├── repository/
│   └── StationRepositoryTest.kt
└── integration/
    └── StationIntegrationTest.kt
```

## 📋 DEPENDENCIAS FALTANTES

### Build.gradle.kts:

```kotlin
dependencies {
    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}
```

## 🔒 SEGURIDAD Y VALIDACIÓN

### Validaciones Faltantes:

- [ ] Validación de coordenadas GPS (latitude/longitude)
- [ ] Validación de nombres únicos por región
- [ ] Sanitización de inputs
- [ ] Rate limiting para endpoints públicos

### Seguridad:

- [ ] Autenticación JWT para endpoints de modificación
- [ ] Autorización por roles (ADMIN, MANAGER)
- [ ] Audit logging para cambios

## 📊 MONITOREO Y OBSERVABILIDAD

### Métricas Faltantes:

- [ ] Contador de estaciones activas/inactivas
- [ ] Tiempo de respuesta por endpoint
- [ ] Errores por tipo de operación

### Logging:

- [ ] Structured logging con correlationId
- [ ] Log de cambios de estado
- [ ] Performance logging

## 🚀 PRÓXIMOS PASOS

### Prioridad Alta:

1. Resolver duplicación de entidad Station
2. Separar DTOs del controller
3. Implementar exception handling
4. Agregar validaciones básicas

### Prioridad Media:

1. Implementar tests unitarios
2. Agregar seguridad JWT
3. Implementar audit logging
4. Optimizar queries de base de datos

### Prioridad Baja:

1. Implementar cache
2. Agregar métricas avanzadas
3. Implementar soft delete
4. Agregar soporte para búsqueda geoespacial

## 📝 NOTAS ADICIONALES

- El servicio está parcialmente implementado pero no compila debido a conflictos de entidades
- La arquitectura base es correcta pero necesita refactoring para seguir principios SOLID
- Falta implementación de manejo de errores y validaciones
- No hay tests implementados
- La configuración de base de datos necesita migration scripts
