# ⛽ Station Service - Gasolinera JSM

## 📋 Descripción

El **Station Service** es el microservicio encargado de gestionar toda la información relacionada con las estaciones de gasolina en el sistema Gasolinera JSM. Proporciona funcionalidades de búsqueda geoespacial, gestión de precios de combustible, información de disponibilidad en tiempo real, y analytics de rendimiento de estaciones.

## 🏗️ Arquitectura Hexagonal

```
┌─────────────────────────────────────────────────────────────┐
│                      Station Service                         │
├─────────────────────────────────────────────────────────────┤
│                     Web Layer (Adapters)                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │StationController│  │ PriceController │  │AdminController│ │
│  │   (Search)      │  │   (Pricing)     │  │ (Management) │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                   Application Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │StationSearch    │  │  PriceManagement│  │StationAdmin   │ │
│  │   UseCase       │  │    UseCase      │  │   UseCase     │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │    Station      │  │   FuelPrice     │  │   Location    │ │
│  │  (Aggregate)    │  │ (Value Object)  │  │(Value Object) │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │GeospatialService│  │ PricingService  │  │AnalyticsService│ │
│  │                 │  │                 │  │              │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                Infrastructure Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │StationRepository│  │  PriceCache     │  │EventPublisher │ │
│  │  (PostgreSQL)   │  │   (Redis)       │  │ (RabbitMQ)   │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 Características Principales

### 🗺️ Búsqueda Geoespacial

- **Búsqueda por Radio** usando algoritmos de Haversine
- **Filtros Avanzados** por tipo de combustible, precio, rating
- **Ordenamiento Inteligente** por distancia, precio, popularidad
- **Geocoding** y reverse geocoding
- **Optimización de Rutas** para múltiples estaciones

### 💰 Gestión de Precios

- **Precios en Tiempo Real** actualizados cada 15 minutos
- **Histórico de Precios** para análisis de tendencias
- **Alertas de Cambios** de precios significativos
- **Comparación de Precios** entre estaciones
- **Predicción de Precios** usando ML

### 📊 Analytics y Reportes

- **Métricas de Rendimiento** por estación
- **Análisis de Tráfico** y patrones de uso
- **Reportes de Ventas** y revenue
- **Dashboards Interactivos** para operadores
- **KPIs de Negocio** en tiempo real

### 🔄 Disponibilidad en Tiempo Real

- **Estado Operacional** de estaciones
- **Disponibilidad de Combustible** por tipo
- **Tiempos de Espera** estimados
- **Información de Colas** en tiempo real
- **Alertas de Mantenimiento**

## 🛠️ Tecnologías

- **Spring Boot 3.2** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **PostgreSQL + PostGIS** - Base de datos geoespacial
- **Redis** - Cache de precios y sesiones
- **Elasticsearch** - Búsqueda y analytics
- **RabbitMQ** - Messaging asíncrono
- **Micrometer** - Métricas y observabilidad
- **Testcontainers** - Testing con containers

## 🚀 Quick Start

### Prerrequisitos

- Java 21+
- Docker & Docker Compose
- PostgreSQL 15+ con extensión PostGIS
- Redis 7+
- Elasticsearch 8+

### 1. Clonar y Configurar

```bash
git clone https://github.com/gasolinera-jsm/station-service.git
cd station-service

# Copiar configuración de ejemplo
cp src/main/resources/application-example.yml src/main/resources/application-local.yml
```

### 2. Configurar Variables de Entorno

```bash
# .env.local
DATABASE_URL=jdbc:postgresql://localhost:5432/gasolinera_stations
DATABASE_USERNAME=gasolinera_user
DATABASE_PASSWORD=secure_password
REDIS_HOST=localhost
REDIS_PORT=6379
ELASTICSEARCH_HOST=localhost
ELASTICSEARCH_PORT=9200
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

### 3. Ejecutar con Docker Compose

```bash
# Levantar dependencias
docker-compose -f docker-compose.dev.yml up -d postgres redis elasticsearch rabbitmq

# Esperar a que PostgreSQL esté listo
./scripts/wait-for-postgres.sh

# Ejecutar migraciones
./gradlew flywayMigrate

# Cargar datos de prueba
./gradlew loadTestData

# Ejecutar la aplicación
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. Verificar Funcionamiento

```bash
# Health check
curl http://localhost:8083/actuator/health

# Buscar estaciones cercanas
curl "http://localhost:8083/api/v1/stations/nearby?latitude=19.4326&longitude=-99.1332&radius=10"

# Swagger UI
open http://localhost:8083/swagger-ui.html
```

## 📁 Estructura del Proyecto

```
station-service/
├── src/main/kotlin/com/gasolinerajsm/station/
│   ├── domain/                    # Capa de Dominio
│   │   ├── model/
│   │   │   ├── Station.kt        # Agregado principal
│   │   │   ├── Location.kt       # Value Object
│   │   │   ├── FuelPrice.kt      # Value Object
│   │   │   ├── OperatingHours.kt # Value Object
│   │   │   └── StationStatus.kt  # Enum
│   │   ├── service/
│   │   │   ├── GeospatialService.kt
│   │   │   ├── PricingService.kt
│   │   │   └── AnalyticsService.kt
│   │   └── repository/
│   │       ├── StationRepository.kt    # Puerto
│   │       └── PriceHistoryRepository.kt
│   ├── application/               # Capa de Aplicación
│   │   ├── usecase/
│   │   │   ├── StationSearchUseCase.kt
│   │   │   ├── PriceManagementUseCase.kt
│   │   │   └── StationAdminUseCase.kt
│   │   └── dto/
│   │       └── StationCommands.kt
│   ├── infrastructure/            # Capa de Infraestructura
│   │   ├── persistence/
│   │   │   ├── StationJpaRepository.kt
│   │   │   ├── StationEntity.kt
│   │   │   └── StationRepositoryImpl.kt
│   │   ├── cache/
│   │   │   ├── PriceCache.kt
│   │   │   └── RedisConfig.kt
│   │   ├── search/
│   │   │   ├── ElasticsearchConfig.kt
│   │   │   └── StationSearchRepository.kt
│   │   ├── messaging/
│   │   │   ├── PriceChangePublisher.kt
│   │   │   └── RabbitMQConfig.kt
│   │   └── external/
│   │       ├── FuelPriceProvider.kt
│   │       └── WeatherService.kt
│   ├── web/                       # Capa Web
│   │   ├── controller/
│   │   │   ├── StationController.kt
│   │   │   ├── PriceController.kt
│   │   │   └── AdminController.kt
│   │   ├── dto/
│   │   │   └── StationDTOs.kt
│   │   └── filter/
│   │       └── GeoLocationFilter.kt
│   └── StationServiceApplication.kt
├── src/main/resources/
│   ├── db/migration/              # Flyway migrations
│   │   ├── V1__Create_stations_table.sql
│   │   ├── V2__Add_postgis_extension.sql
│   │   ├── V3__Create_price_history_table.sql
│   │   └── V4__Create_spatial_indexes.sql
│   ├── data/                      # Datos de prueba
│   │   ├── stations-cdmx.json
│   │   └── fuel-prices.json
│   ├── elasticsearch/             # Mappings de ES
│   │   └── station-mapping.json
│   ├── application.yml
│   ├── application-local.yml
│   └── application-prod.yml
└── src/test/                      # Tests
    ├── kotlin/
    │   ├── domain/               # Tests de dominio
    │   ├── application/          # Tests de casos de uso
    │   ├── infrastructure/       # Tests de infraestructura
    │   └── integration/          # Tests de integración
    └── resources/
        └── application-test.yml
```

## ⚙️ Configuración

### Base de Datos con PostGIS

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### Elasticsearch Configuration

```yaml
elasticsearch:
  host: ${ELASTICSEARCH_HOST}
  port: ${ELASTICSEARCH_PORT}
  username: ${ELASTICSEARCH_USERNAME:}
  password: ${ELASTICSEARCH_PASSWORD:}
  connection-timeout: 5s
  socket-timeout: 30s

  indices:
    stations:
      name: stations
      settings:
        number_of_shards: 3
        number_of_replicas: 1
```

### Redis Cache Configuration

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

cache:
  prices:
    ttl: 900 # 15 minutos
  stations:
    ttl: 3600 # 1 hora
  search-results:
    ttl: 300 # 5 minutos
```

### RabbitMQ Configuration

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}

messaging:
  exchanges:
    station-events: station.events
  queues:
    price-changes: station.price.changes
    status-updates: station.status.updates
```

## 🗺️ Modelo de Dominio

### Station Aggregate

```kotlin
@Entity
@Table(name = "stations")
class Station private constructor(
    @Id val id: UUID,
    val name: String,
    val brand: String,
    val address: String,
    @Embedded val location: Location,
    @Embedded val operatingHours: OperatingHours,
    @ElementCollection
    @Enumerated(EnumType.STRING)
    val services: Set<StationService>,
    @ElementCollection
    val fuelPrices: Map<FuelType, FuelPrice>,
    @Enumerated(EnumType.STRING)
    val status: StationStatus,
    val rating: BigDecimal,
    val reviewCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun create(
            name: String,
            brand: String,
            address: String,
            latitude: Double,
            longitude: Double,
            operatingHours: OperatingHours
        ): Station {
            return Station(
                id = UUID.randomUUID(),
                name = name,
                brand = brand,
                address = address,
                location = Location(latitude, longitude),
                operatingHours = operatingHours,
                services = emptySet(),
                fuelPrices = emptyMap(),
                status = StationStatus.ACTIVE,
                rating = BigDecimal.ZERO,
                reviewCount = 0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    }

    fun updatePrices(
        newPrices: Map<FuelType, BigDecimal>,
        pricingService: PricingService
    ): Station {
        val updatedPrices = pricingService.validateAndUpdatePrices(fuelPrices, newPrices)

        return copy(
            fuelPrices = updatedPrices,
            updatedAt = LocalDateTime.now()
        )
    }

    fun calculateDistance(targetLocation: Location): Double {
        return location.distanceTo(targetLocation)
    }

    fun isOperational(): Boolean {
        return status == StationStatus.ACTIVE && operatingHours.isOpenNow()
    }

    fun hasService(service: StationService): Boolean {
        return services.contains(service)
    }

    fun updateRating(newRating: BigDecimal): Station {
        val totalRating = rating * reviewCount.toBigDecimal() + newRating
        val newReviewCount = reviewCount + 1
        val newAverageRating = totalRating / newReviewCount.toBigDecimal()

        return copy(
            rating = newAverageRating,
            reviewCount = newReviewCount,
            updatedAt = LocalDateTime.now()
        )
    }
}
```

### Value Objects

```kotlin
@Embeddable
data class Location(
    @Column(name = "latitude")
    val latitude: Double,

    @Column(name = "longitude")
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }

    fun distanceTo(other: Location): Double {
        val earthRadius = 6371.0 // km

        val lat1Rad = Math.toRadians(latitude)
        val lat2Rad = Math.toRadians(other.latitude)
        val deltaLatRad = Math.toRadians(other.latitude - latitude)
        val deltaLonRad = Math.toRadians(other.longitude - longitude)

        val a = sin(deltaLatRad / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}

@Embeddable
data class FuelPrice(
    @Column(name = "price")
    val amount: BigDecimal,

    @Column(name = "last_updated")
    val lastUpdated: LocalDateTime
) {
    init {
        require(amount > BigDecimal.ZERO) { "Fuel price must be positive" }
    }

    fun isStale(maxAgeMinutes: Long = 60): Boolean {
        return lastUpdated.isBefore(LocalDateTime.now().minusMinutes(maxAgeMinutes))
    }
}
```

## 🔄 Casos de Uso

### Station Search Use Case

```kotlin
@Service
@Transactional(readOnly = true)
class StationSearchUseCase(
    private val stationRepository: StationRepository,
    private val geospatialService: GeospatialService,
    private val priceCache: PriceCache,
    private val searchRepository: StationSearchRepository
) {
    fun findNearbyStations(command: FindNearbyStationsCommand): FindNearbyStationsResult {
        val searchLocation = Location(command.latitude, command.longitude)

        // Búsqueda geoespacial inicial
        val nearbyStations = stationRepository.findWithinRadius(
            location = searchLocation,
            radiusKm = command.radiusKm,
            limit = command.limit * 2 // Obtener más para filtrar
        )

        // Aplicar filtros
        val filteredStations = nearbyStations
            .filter { station ->
                command.fuelType?.let { station.hasFuelType(FuelType.valueOf(it)) } ?: true
            }
            .filter { station ->
                command.minRating?.let { station.rating >= it.toBigDecimal() } ?: true
            }
            .filter { station ->
                command.services?.all { service -> station.hasService(StationService.valueOf(service)) } ?: true
            }

        // Enriquecer con precios en tiempo real
        val enrichedStations = filteredStations.map { station ->
            val currentPrices = priceCache.getCurrentPrices(station.id)
            station.copy(fuelPrices = currentPrices ?: station.fuelPrices)
        }

        // Ordenar según criterio
        val sortedStations = when (command.sortBy) {
            SortCriteria.DISTANCE -> enrichedStations.sortedBy { it.calculateDistance(searchLocation) }
            SortCriteria.PRICE -> enrichedStations.sortedBy { it.getLowestPrice() }
            SortCriteria.RATING -> enrichedStations.sortedByDescending { it.rating }
            SortCriteria.NAME -> enrichedStations.sortedBy { it.name }
        }

        // Limitar resultados
        val finalResults = sortedStations.take(command.limit)

        // Generar recomendaciones
        val recommendations = geospatialService.generateRecommendations(
            searchLocation, finalResults, command
        )

        return FindNearbyStationsResult.Success(
            stations = finalResults,
            totalFound = nearbyStations.size,
            recommendations = recommendations,
            searchTime = measureTimeMillis { /* search time */ }
        )
    }

    fun searchStations(command: SearchStationsCommand): SearchStationsResult {
        // Usar Elasticsearch para búsqueda de texto
        val searchResults = searchRepository.search(
            query = command.query,
            location = command.location,
            radiusKm = command.radiusKm,
            filters = command.filters,
            pageable = command.pageable
        )

        // Enriquecer con datos de base de datos
        val stationIds = searchResults.map { it.id }
        val fullStations = stationRepository.findAllById(stationIds)

        return SearchStationsResult.Success(
            stations = fullStations,
            totalHits = searchResults.totalHits,
            searchTime = searchResults.searchTime
        )
    }
}
```

### Price Management Use Case

```kotlin
@Service
@Transactional
class PriceManagementUseCase(
    private val stationRepository: StationRepository,
    private val pricingService: PricingService,
    private val priceCache: PriceCache,
    private val eventPublisher: EventPublisher
) {
    fun updateStationPrices(command: UpdateStationPricesCommand): UpdateStationPricesResult {
        // Buscar estación
        val station = stationRepository.findById(command.stationId)
            ?: return UpdateStationPricesResult.StationNotFound

        // Validar permisos (el operador puede actualizar solo sus estaciones)
        if (!canUpdateStation(command.operatorId, station)) {
            return UpdateStationPricesResult.AccessDenied
        }

        // Validar precios
        val validationResult = pricingService.validatePrices(command.newPrices)
        if (!validationResult.isValid) {
            return UpdateStationPricesResult.InvalidPrices(validationResult.errors)
        }

        // Actualizar precios
        val updatedStation = station.updatePrices(command.newPrices, pricingService)
        val savedStation = stationRepository.save(updatedStation)

        // Actualizar cache
        priceCache.updatePrices(station.id, command.newPrices)

        // Publicar evento de cambio de precios
        eventPublisher.publishPriceChangeEvent(
            PriceChangeEvent(
                stationId = station.id,
                oldPrices = station.fuelPrices,
                newPrices = command.newPrices,
                changedBy = command.operatorId,
                timestamp = LocalDateTime.now()
            )
        )

        return UpdateStationPricesResult.Success(savedStation)
    }

    fun getPriceHistory(command: GetPriceHistoryCommand): GetPriceHistoryResult {
        val priceHistory = priceHistoryRepository.findByStationIdAndDateRange(
            stationId = command.stationId,
            startDate = command.startDate,
            endDate = command.endDate,
            fuelType = command.fuelType
        )

        val analytics = pricingService.calculatePriceAnalytics(priceHistory)

        return GetPriceHistoryResult.Success(
            history = priceHistory,
            analytics = analytics
        )
    }
}
```

## 🧪 Testing

### Tests Unitarios

```kotlin
@ExtendWith(MockitoExtension::class)
class StationSearchUseCaseTest {

    @Mock
    private lateinit var stationRepository: StationRepository

    @Mock
    private lateinit var geospatialService: GeospatialService

    @InjectMocks
    private lateinit var stationSearchUseCase: StationSearchUseCase

    @Test
    fun `should find nearby stations within radius`() {
        // Given
        val command = FindNearbyStationsCommand(
            latitude = 19.4326,
            longitude = -99.1332,
            radiusKm = 10.0,
            limit = 20
        )

        val mockStations = listOf(
            createMockStation("Station 1", 19.4300, -99.1300),
            createMockStation("Station 2", 19.4350, -99.1350)
        )

        given(stationRepository.findWithinRadius(any(), any(), any()))
            .willReturn(mockStations)

        // When
        val result = stationSearchUseCase.findNearbyStations(command)

        // Then
        assertThat(result).isInstanceOf(FindNearbyStationsResult.Success::class.java)
        val successResult = result as FindNearbyStationsResult.Success
        assertThat(successResult.stations).hasSize(2)
        assertThat(successResult.totalFound).isEqualTo(2)
    }
}
```

### Tests de Integración con PostGIS

```kotlin
@SpringBootTest
@Testcontainers
class StationRepositoryIntegrationTest {

    @Container
    static val postgres = PostgreSQLContainer("postgis/postgis:15-3.3")
        .withDatabaseName("test_stations")
        .withUsername("test")
        .withPassword("test")

    @Autowired
    private lateinit var stationRepository: StationRepository

    @Test
    fun `should find stations within radius using PostGIS`() {
        // Given - Crear estaciones de prueba
        val centerLocation = Location(19.4326, -99.1332) // Zócalo CDMX

        val nearStation = Station.create(
            name = "Near Station",
            brand = "Pemex",
            address = "Cerca del Zócalo",
            latitude = 19.4320, // ~67 metros del centro
            longitude = -99.1330,
            operatingHours = OperatingHours.always()
        )

        val farStation = Station.create(
            name = "Far Station",
            brand = "Shell",
            address = "Lejos del Zócalo",
            latitude = 19.5000, // ~7.5 km del centro
            longitude = -99.2000,
            operatingHours = OperatingHours.always()
        )

        stationRepository.saveAll(listOf(nearStation, farStation))

        // When - Buscar estaciones en radio de 5km
        val nearbyStations = stationRepository.findWithinRadius(
            location = centerLocation,
            radiusKm = 5.0,
            limit = 10
        )

        // Then - Solo debe encontrar la estación cercana
        assertThat(nearbyStations).hasSize(1)
        assertThat(nearbyStations[0].name).isEqualTo("Near Station")
    }
}
```

### Tests de Performance

```kotlin
@SpringBootTest
@Testcontainers
class StationSearchPerformanceTest {

    @Test
    fun `should handle 1000 concurrent searches efficiently`() {
        val executor = Executors.newFixedThreadPool(100)
        val latch = CountDownLatch(1000)
        val results = ConcurrentLinkedQueue<Long>()

        repeat(1000) {
            executor.submit {
                try {
                    val startTime = System.currentTimeMillis()

                    // Ejecutar búsqueda
                    stationSearchUseCase.findNearbyStations(
                        FindNearbyStationsCommand(
                            latitude = 19.4326 + Random.nextDouble(-0.1, 0.1),
                            longitude = -99.1332 + Random.nextDouble(-0.1, 0.1),
                            radiusKm = 10.0,
                            limit = 20
                        )
                    )

                    val duration = System.currentTimeMillis() - startTime
                    results.add(duration)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)

        val averageTime = results.average()
        val maxTime = results.maxOrNull() ?: 0

        assertThat(averageTime).isLessThan(200.0) // Promedio < 200ms
        assertThat(maxTime).isLessThan(1000) // Máximo < 1s
    }
}
```

### Ejecutar Tests

```bash
# Tests unitarios
./gradlew test

# Tests de integración
./gradlew integrationTest

# Tests de performance
./gradlew performanceTest

# Coverage report
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## 🐳 Docker

### Dockerfile Multi-stage

```dockerfile
FROM openjdk:21-jdk-slim as builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM openjdk:21-jre-slim
WORKDIR /app

# Instalar herramientas geoespaciales
RUN apt-get update && apt-get install -y \
    gdal-bin \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar app.jar
COPY src/main/resources/data/ /app/data/

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose para Desarrollo

```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  station-service:
    build: .
    ports:
      - '8083:8083'
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/gasolinera_stations
      - REDIS_HOST=redis
      - ELASTICSEARCH_HOST=elasticsearch
    depends_on:
      - postgres
      - redis
      - elasticsearch
    volumes:
      - ./src/main/resources/data:/app/data:ro

  postgres:
    image: postgis/postgis:15-3.3
    environment:
      POSTGRES_DB: gasolinera_stations
      POSTGRES_USER: gasolinera_user
      POSTGRES_PASSWORD: secure_password
    ports:
      - '5432:5432'
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-postgis.sql:/docker-entrypoint-initdb.d/init-postgis.sql

  redis:
    image: redis:7-alpine
    ports:
      - '6379:6379'
    volumes:
      - redis_data:/data

  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - 'ES_JAVA_OPTS=-Xms512m -Xmx512m'
    ports:
      - '9200:9200'
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data

volumes:
  postgres_data:
  redis_data:
  elasticsearch_data:
```

## 🚀 Deployment

### Variables de Entorno de Producción

```bash
# Database
DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/gasolinera_stations
DATABASE_USERNAME=station_service_user
DATABASE_PASSWORD=super_secure_password

# Cache
REDIS_HOST=redis-cluster.example.com
REDIS_PORT=6379
REDIS_PASSWORD=redis_secure_password

# Search
ELASTICSEARCH_HOST=elasticsearch-cluster.example.com
ELASTICSEARCH_PORT=9200
ELASTICSEARCH_USERNAME=elastic
ELASTICSEARCH_PASSWORD=elastic_password

# Messaging
RABBITMQ_HOST=rabbitmq-cluster.example.com
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=station_service
RABBITMQ_PASSWORD=rabbitmq_password

# External APIs
FUEL_PRICE_API_URL=https://api.fuel-prices.com
FUEL_PRICE_API_KEY=your_api_key
WEATHER_API_URL=https://api.weather.com
WEATHER_API_KEY=your_weather_key

# Observability
JAEGER_ENDPOINT=http://jaeger:14268/api/traces
PROMETHEUS_ENABLED=true
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: station-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: station-service
  template:
    metadata:
      labels:
        app: station-service
    spec:
      containers:
        - name: station-service
          image: gasolinera-jsm/station-service:latest
          ports:
            - containerPort: 8083
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: 'kubernetes'
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: station-service-secrets
                  key: database-url
          resources:
            requests:
              memory: '512Mi'
              cpu: '250m'
            limits:
              memory: '1Gi'
              cpu: '500m'
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8083
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8083
            initialDelaySeconds: 30
            periodSeconds: 10
```

## 🔧 Troubleshooting

### Problemas Comunes

#### 1. Búsqueda Geoespacial Lenta

```sql
-- Verificar índices espaciales
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename = 'stations' AND indexdef LIKE '%gist%';

-- Crear índice si no existe
CREATE INDEX CONCURRENTLY idx_stations_location
ON stations USING GIST (ST_Point(longitude, latitude));

-- Verificar estadísticas de la tabla
ANALYZE stations;
```

#### 2. Cache de Precios Desactualizado

```bash
# Verificar conexión a Redis
redis-cli -h localhost -p 6379 ping

# Ver claves de precios en cache
redis-cli -h localhost -p 6379
> KEYS price:*
> GET price:station:123e4567-e89b-12d3-a456-426614174000

# Limpiar cache de precios
> FLUSHDB
```

#### 3. Elasticsearch No Responde

```bash
# Verificar estado del cluster
curl -X GET "localhost:9200/_cluster/health?pretty"

# Verificar índices
curl -X GET "localhost:9200/_cat/indices?v"

# Reindexar estaciones
curl -X POST "localhost:9200/stations/_reindex" -H 'Content-Type: application/json' -d'
{
  "source": {
    "index": "stations_old"
  },
  "dest": {
    "index": "stations"
  }
}'
```

#### 4. Performance de Búsqueda

```bash
# Verificar queries lentas en PostgreSQL
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
WHERE query LIKE '%stations%'
ORDER BY mean_exec_time DESC
LIMIT 10;

# Habilitar log de queries lentas
ALTER SYSTEM SET log_min_duration_statement = 1000;
SELECT pg_reload_conf();
```

### Logs de Debug

```yaml
# application-debug.yml
logging:
  level:
    com.gasolinerajsm.station: DEBUG
    org.springframework.data.jpa: DEBUG
    org.elasticsearch: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

## 📊 Monitoreo

### Métricas Disponibles

- **station.searches.total** - Total de búsquedas
- **station.searches.duration** - Duración de búsquedas
- **station.price.updates** - Actualizaciones de precios
- **station.cache.hits** - Cache hits de precios
- **station.geospatial.queries** - Queries geoespaciales

### Dashboards de Grafana

```json
{
  "dashboard": {
    "title": "Station Service Metrics",
    "panels": [
      {
        "title": "Search Performance",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, station_searches_duration_seconds_bucket)",
            "legendFormat": "95th percentile"
          }
        ]
      },
      {
        "title": "Price Update Rate",
        "type": "singlestat",
        "targets": [
          {
            "expr": "rate(station_price_updates_total[5m])",
            "legendFormat": "Updates/sec"
          }
        ]
      }
    ]
  }
}
```

## 📚 Referencias

- [PostGIS Documentation](https://postgis.net/documentation/)
- [Elasticsearch Geo Queries](https://www.elastic.co/guide/en/elasticsearch/reference/current/geo-queries.html)
- [Spring Data JPA Spatial](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#spatial)
- [Redis Geospatial Commands](https://redis.io/commands/?group=geo)

## 🤝 Contribución

1. Fork el repositorio
2. Crear feature branch (`git checkout -b feature/station-improvement`)
3. Commit cambios (`git commit -m 'Add geospatial optimization'`)
4. Push al branch (`git push origin feature/station-improvement`)
5. Crear Pull Request

## 📄 Licencia

Este proyecto es propiedad de Gasolinera JSM. Todos los derechos reservados.

---

**⛽ ¿Necesitas ayuda con estaciones?**

- 📧 Email: stations-team@gasolinera-jsm.com
- 💬 Slack: #station-service-support
- 📖 Docs: https://docs.gasolinera-jsm.com/stations

_Última actualización: Enero 2024_
