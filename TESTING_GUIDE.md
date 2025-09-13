# 🧪 Testing Guide - Gasolinera JSM

Esta guía describe la estrategia de testing completa implementada en el proyecto Gasolinera JSM.

## 📋 Estrategia de Testing

### Pirámide de Testing

```
        /\
       /  \
      / E2E \     10% - End-to-End Tests
     /______\
    /        \
   /Integration\ 20% - Integration Tests
  /__________\
 /            \
/  Unit Tests  \   70% - Unit Tests
/______________\
```

### Tipos de Tests Implementados

1. **Unit Tests (70%)** - Tests de lógica de negocio aislada
2. **Integration Tests (20%)** - Tests con base de datos y servicios externos
3. **End-to-End Tests (10%)** - Tests de flujos completos de usuario
4. **Performance Tests** - Tests de rendimiento y carga

## 🛠️ Herramientas de Testing

### Frameworks y Librerías

- **JUnit 5** - Framework base de testing
- **Kotest** - Framework de testing para Kotlin con BDD style
- **MockK** - Librería de mocking para Kotlin
- **TestContainers** - Containers para integration testing
- **Spring Boot Test** - Testing utilities para Spring Boot
- **Rest Assured** - Testing de APIs REST
- **Awaitility** - Testing asíncrono

### Infraestructura de Testing

- **PostgreSQL TestContainer** - Base de datos para integration tests
- **Redis TestContainer** - Cache para integration tests
- **RabbitMQ TestContainer** - Message queue para integration tests
- **Jaeger TestContainer** - Tracing para integration tests

## 🚀 Comandos de Testing

### Comandos Make

```bash
# Ejecutar todos los tests
make test-all

# Tests por tipo
make test-unit           # Solo unit tests
make test-integration    # Solo integration tests
make test-e2e           # Solo end-to-end tests
make test-performance   # Solo performance tests

# Coverage
make test-coverage      # Generar reporte de cobertura

# TestContainers
make containers-start   # Iniciar containers de test
make containers-stop    # Detener containers de test
make containers-clean   # Limpiar containers de test
```

### Comandos Gradle

```bash
# Tests básicos
./gradlew test                    # Unit tests
./gradlew integrationTest         # Integration tests
./gradlew e2eTest                # End-to-end tests

# Coverage
./gradlew jacocoTestReport       # Generar reporte de cobertura
./gradlew jacocoTestCoverageVerification  # Verificar cobertura mínima

# Tests por servicio
./gradlew :services:auth-service:test
./gradlew :services:coupon-service:test
```

### Scripts Personalizados

```bash
# Script completo de testing
./scripts/run-tests.sh [type] [generate_reports]

# Ejemplos
./scripts/run-tests.sh all true        # Todos los tests con reportes
./scripts/run-tests.sh unit false      # Solo unit tests sin reportes
./scripts/run-tests.sh integration     # Solo integration tests
```

## 📁 Estructura de Testing

### Organización de Archivos

```
services/
├── auth-service/
│   └── src/test/kotlin/
│       ├── integration/          # Integration tests
│       ├── unit/                # Unit tests
│       └── resources/           # Test resources
├── coupon-service/
│   └── src/test/kotlin/
│       ├── integration/
│       ├── unit/
│       └── resources/
└── ...

shared/common/src/test/kotlin/
├── testcontainers/              # TestContainers configuration
├── testing/                     # Testing utilities
└── resources/                   # Shared test resources

integration-tests/               # Cross-service integration tests
performance-tests/              # Performance and load tests
```

### Clases Base de Testing

```kotlin
// Para integration tests
abstract class BaseIntegrationTest : BaseIntegrationTest()

// Para unit tests
abstract class BaseUnitTest : BehaviorSpec()

// Para API testing
class ApiTestUtils(mockMvc: MockMvc, objectMapper: ObjectMapper)
```

## 🧪 Escribiendo Tests

### Unit Tests con Kotest

```kotlin
class UserServiceUnitTest : BaseUnitTest() {
    init {
        given("UserService") {
            `when`("creating a new user") {
                then("should validate email format") {
                    // Test implementation
                }
            }
        }
    }
}
```

### Integration Tests

```kotlin
@SpringBootTest
class UserServiceIntegrationTest : BaseIntegrationTest() {

    @Test
    fun `should create user in database`() {
        // Test with real database via TestContainers
    }
}
```

### API Tests

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var apiTestUtils: ApiTestUtils

    @Test
    fun `should return user profile`() {
        apiTestUtils.get("/api/users/profile")
            .expectSuccess()
            .expectJsonField("$.email", "test@example.com")
    }
}
```

## 🏭 Test Data Factory

### Generación de Datos de Prueba

```kotlin
// Usar TestDataFactory para datos consistentes
val user = TestDataFactory.run {
    User(
        id = randomId(),
        email = randomEmail(),
        phoneNumber = randomPhoneNumber(),
        createdAt = randomDateTime()
    )
}

// Datos específicos del dominio
val coupon = TestDataFactory.run {
    Coupon(
        code = randomCouponCode(),
        qrCode = randomQrCode(),
        amount = randomAmount(min = 5.0, max = 100.0)
    )
}
```

## 🐳 TestContainers Configuration

### Configuración Automática

Los TestContainers se configuran automáticamente:

- **PostgreSQL**: Puerto 5432 (mapeado dinámicamente)
- **Redis**: Puerto 6379 (mapeado dinámicamente)
- **RabbitMQ**: Puerto 5672 (mapeado dinámicamente)

### Reutilización de Containers

```properties
# En application-integration-test.yml
testcontainers.reuse.enable=true
testcontainers.ryuk.disabled=true
```

### Container Personalizado

```kotlin
@TestConfiguration
class CustomTestContainersConfig {

    @Bean
    fun customContainer(): GenericContainer<*> {
        return GenericContainer("custom-image:latest")
            .withExposedPorts(8080)
            .withEnv("ENV_VAR", "test-value")
    }
}
```

## 📊 Coverage y Reportes

### Configuración de Coverage

- **Cobertura Mínima**: 80%
- **Exclusiones**: DTOs, Entities, Configuration classes
- **Reportes**: HTML, XML, CSV

### Ubicación de Reportes

```
build/reports/
├── tests/
│   ├── test/index.html              # Unit test report
│   ├── integrationTest/index.html   # Integration test report
│   └── e2eTest/index.html          # E2E test report
├── jacoco/
│   └── test/jacocoTestReport/
│       └── index.html              # Coverage report
└── detekt/
    └── detekt.html                 # Code quality report
```

## 🔧 Configuración por Ambiente

### Perfiles de Testing

- **unit**: Para unit tests (sin containers)
- **integration-test**: Para integration tests (con TestContainers)
- **e2e**: Para end-to-end tests (ambiente completo)
- **performance**: Para performance tests

### Variables de Ambiente

```yaml
# application-integration-test.yml
spring:
  profiles:
    active: integration-test
  datasource:
    # Configurado automáticamente por TestContainers
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## 🚨 Troubleshooting

### Problemas Comunes

#### TestContainers no inician

```bash
# Verificar Docker
docker --version
docker ps

# Limpiar containers
make containers-clean
docker system prune -f
```

#### Tests fallan por timeout

```kotlin
// Aumentar timeout en tests
@Test
@Timeout(value = 30, unit = TimeUnit.SECONDS)
fun `long running test`() {
    // Test implementation
}
```

#### Base de datos no se limpia entre tests

```kotlin
@Transactional
@Rollback
class MyIntegrationTest : BaseIntegrationTest() {
    // Tests se ejecutan en transacción que se rollback
}
```

### Logs de Debug

```bash
# Tests con logs detallados
./gradlew test --info --debug

# Solo logs de TestContainers
./gradlew test -Dorg.slf4j.simpleLogger.log.org.testcontainers=DEBUG
```

## 📈 Métricas de Calidad

### Objetivos de Testing

- **Unit Test Coverage**: > 80%
- **Integration Test Coverage**: > 60%
- **E2E Test Coverage**: Flujos críticos cubiertos
- **Performance**: < 200ms response time para APIs críticas

### Quality Gates

- Todos los tests deben pasar antes del merge
- Coverage mínimo debe cumplirse
- No debe haber tests flakey (intermitentes)
- Performance tests no deben degradar

## 🔄 CI/CD Integration

### GitHub Actions

Los tests se ejecutan automáticamente en:

- **Pull Requests**: Unit + Integration tests
- **Push a main**: Todos los tests + Coverage
- **Nightly**: Performance tests + E2E completos

### Pre-commit Hooks

```bash
# Ejecuta automáticamente antes de commit
make pre-commit  # Incluye unit tests
```

## 📚 Best Practices

### Naming Conventions

```kotlin
// Unit tests
class `UserService should validate email format`()
fun `should return error when email is invalid`()

// Integration tests
class `UserController integration tests`()
fun `should create user and return 201`()
```

### Test Organization

- **Arrange**: Preparar datos y mocks
- **Act**: Ejecutar la acción a testear
- **Assert**: Verificar el resultado

### Data Management

- Usar TestDataFactory para datos consistentes
- Limpiar datos entre tests
- Usar transacciones con rollback cuando sea posible

---

_Última actualización: $(date)_
_Configurado por: Kiro AI Assistant_
