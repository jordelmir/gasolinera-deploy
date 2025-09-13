# 🔐 Auth Service - Gasolinera JSM

## 📋 Descripción

El **Auth Service** es el servicio de autenticación y autorización centralizado del sistema Gasolinera JSM. Maneja el registro de usuarios, autenticación JWT, gestión de roles y permisos, y proporciona funcionalidades de seguridad como verificación de email, recuperación de contraseñas y gestión de sesiones.

## 🏗️ Arquitectura Hexagonal

```
┌─────────────────────────────────────────────────────────────┐
│                        Auth Service                          │
├─────────────────────────────────────────────────────────────┤
│                     Web Layer (Adapters)                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ AuthController  │  │  UserController │  │ AdminController│ │
│  │   (REST API)    │  │   (Profile)     │  │  (Management) │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                   Application Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │Authentication   │  │  UserProfile    │  │   UserAdmin   │ │
│  │   UseCase       │  │   UseCase       │  │   UseCase     │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   AuthUser      │  │     Email       │  │   Password    │ │
│  │  (Aggregate)    │  │ (Value Object)  │  │(Value Object) │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ AuthDomainService│  │PasswordService  │  │ TokenService  │ │
│  │                 │  │                 │  │              │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                Infrastructure Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │AuthUserRepository│  │  RedisTokenCache│  │EmailService  │ │
│  │  (PostgreSQL)   │  │   (Sessions)    │  │   (SMTP)     │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 Características Principales

### 🔐 Autenticación

- **JWT Tokens** con RS256 signing
- **Refresh Tokens** para renovación automática
- **Multi-factor Authentication** (MFA) opcional
- **Session Management** con Redis
- **Account Lockout** por intentos fallidos

### 👤 Gestión de Usuarios

- **Registro de Usuarios** con validación
- **Verificación de Email** obligatoria
- **Gestión de Perfiles** completa
- **Recuperación de Contraseñas** segura
- **Cambio de Contraseñas** con validación

### 🛡️ Seguridad

- **Password Hashing** con BCrypt
- **Rate Limiting** por endpoint
- **Brute Force Protection**
- **Email Verification** tokens
- **Password Reset** tokens seguros

### 🎭 Roles y Permisos

- **Role-Based Access Control** (RBAC)
- **Hierarchical Roles** (USER → STATION_OPERATOR → ADMIN)
- **Permission Management** granular
- **Dynamic Role Assignment**

## 🛠️ Tecnologías

- **Spring Boot 3.2** - Framework principal
- **Spring Security** - Seguridad y autenticación
- **Spring Data JPA** - Persistencia de datos
- **PostgreSQL** - Base de datos principal
- **Redis** - Cache de sesiones y tokens
- **JWT** - JSON Web Tokens
- **BCrypt** - Hash de contraseñas
- **JavaMail** - Envío de emails
- **Testcontainers** - Testing con containers

## 🚀 Quick Start

### Prerrequisitos

- Java 21+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+

### 1. Clonar y Configurar

```bash
git clone https://github.com/gasolinera-jsm/auth-service.git
cd auth-service

# Copiar configuración de ejemplo
cp src/main/resources/application-example.yml src/main/resources/application-local.yml
```

### 2. Configurar Variables de Entorno

```bash
# .env.local
DATABASE_URL=jdbc:postgresql://localhost:5432/gasolinera_auth
DATABASE_USERNAME=gasolinera_user
DATABASE_PASSWORD=secure_password
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_PRIVATE_KEY_PATH=/path/to/private.pem
JWT_PUBLIC_KEY_PATH=/path/to/public.pem
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=noreply@gasolinera-jsm.com
SMTP_PASSWORD=app_password
```

### 3. Generar Claves JWT

```bash
# Generar par de claves RSA
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem

# Mover a directorio de configuración
mkdir -p config/jwt
mv private.pem public.pem config/jwt/
```

### 4. Ejecutar con Docker Compose

```bash
# Levantar dependencias
docker-compose -f docker-compose.dev.yml up -d postgres redis

# Ejecutar migraciones
./gradlew flywayMigrate

# Ejecutar la aplicación
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 5. Verificar Funcionamiento

```bash
# Health check
curl http://localhost:8081/actuator/health

# Registrar usuario de prueba
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "phone": "5551234567",
    "firstName": "Test",
    "lastName": "User",
    "password": "SecurePassword123!"
  }'
```

## 📁 Estructura del Proyecto

```
auth-service/
├── src/main/kotlin/com/gasolinerajsm/auth/
│   ├── domain/                    # Capa de Dominio
│   │   ├── model/
│   │   │   ├── AuthUser.kt       # Agregado principal
│   │   │   ├── Email.kt          # Value Object
│   │   │   ├── Password.kt       # Value Object
│   │   │   ├── Phone.kt          # Value Object
│   │   │   └── Role.kt           # Enum de roles
│   │   ├── service/
│   │   │   ├── AuthDomainService.kt
│   │   │   ├── PasswordService.kt
│   │   │   └── TokenService.kt
│   │   └── repository/
│   │       └── AuthUserRepository.kt  # Puerto
│   ├── application/               # Capa de Aplicación
│   │   ├── usecase/
│   │   │   ├── AuthenticationUseCase.kt
│   │   │   ├── UserProfileUseCase.kt
│   │   │   └── UserAdminUseCase.kt
│   │   └── dto/
│   │       └── AuthCommands.kt
│   ├── infrastructure/            # Capa de Infraestructura
│   │   ├── persistence/
│   │   │   ├── AuthUserJpaRepository.kt
│   │   │   ├── AuthUserEntity.kt
│   │   │   └── AuthUserRepositoryImpl.kt
│   │   ├── cache/
│   │   │   ├── RedisTokenCache.kt
│   │   │   └── RedisConfig.kt
│   │   ├── email/
│   │   │   ├── EmailService.kt
│   │   │   └── EmailTemplates.kt
│   │   └── security/
│   │       ├── JwtTokenProvider.kt
│   │       ├── PasswordEncoder.kt
│   │       └── SecurityConfig.kt
│   ├── web/                       # Capa Web
│   │   ├── controller/
│   │   │   ├── AuthController.kt
│   │   │   ├── UserController.kt
│   │   │   └── AdminController.kt
│   │   ├── dto/
│   │   │   └── AuthDTOs.kt
│   │   └── filter/
│   │       └── RateLimitFilter.kt
│   └── AuthServiceApplication.kt
├── src/main/resources/
│   ├── db/migration/              # Flyway migrations
│   │   ├── V1__Create_users_table.sql
│   │   ├── V2__Create_roles_table.sql
│   │   └── V3__Create_user_sessions_table.sql
│   ├── templates/                 # Email templates
│   │   ├── welcome-email.html
│   │   ├── password-reset.html
│   │   └── email-verification.html
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

### Base de Datos

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
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### JWT Configuration

```yaml
jwt:
  private-key-path: ${JWT_PRIVATE_KEY_PATH}
  public-key-path: ${JWT_PUBLIC_KEY_PATH}
  access-token-expiration: 3600 # 1 hora
  refresh-token-expiration: 604800 # 7 días
  issuer: gasolinera-jsm-auth
  audience: gasolinera-jsm-api
```

### Redis Configuration

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
```

### Email Configuration

```yaml
spring:
  mail:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

## 🔐 Modelo de Dominio

### AuthUser Aggregate

```kotlin
@Entity
@Table(name = "auth_users")
class AuthUser private constructor(
    @Id val id: UUID,
    @Embedded val email: Email,
    @Embedded val phone: Phone,
    val firstName: String,
    val lastName: String,
    @Embedded val password: Password,
    @Enumerated(EnumType.STRING) val role: Role,
    val isEmailVerified: Boolean,
    val isPhoneVerified: Boolean,
    val isActive: Boolean,
    val failedLoginAttempts: Int,
    val lockedUntil: LocalDateTime?,
    val createdAt: LocalDateTime,
    val lastLoginAt: LocalDateTime?
) {
    companion object {
        fun create(
            email: Email,
            phone: Phone,
            firstName: String,
            lastName: String,
            rawPassword: String,
            passwordService: PasswordService
        ): AuthUser {
            return AuthUser(
                id = UUID.randomUUID(),
                email = email,
                phone = phone,
                firstName = firstName,
                lastName = lastName,
                password = passwordService.hashPassword(rawPassword),
                role = Role.USER,
                isEmailVerified = false,
                isPhoneVerified = false,
                isActive = true,
                failedLoginAttempts = 0,
                lockedUntil = null,
                createdAt = LocalDateTime.now(),
                lastLoginAt = null
            )
        }
    }

    fun authenticate(
        rawPassword: String,
        passwordService: PasswordService
    ): AuthenticationResult {
        if (isLocked()) {
            return AuthenticationResult.AccountLocked(lockedUntil!!)
        }

        if (!passwordService.matches(rawPassword, password)) {
            return AuthenticationResult.InvalidCredentials
        }

        if (!isEmailVerified) {
            return AuthenticationResult.EmailNotVerified
        }

        return AuthenticationResult.Success(this)
    }

    fun recordFailedLogin(): AuthUser {
        val newAttempts = failedLoginAttempts + 1
        val shouldLock = newAttempts >= 5

        return copy(
            failedLoginAttempts = newAttempts,
            lockedUntil = if (shouldLock) LocalDateTime.now().plusMinutes(30) else null
        )
    }

    fun recordSuccessfulLogin(): AuthUser {
        return copy(
            failedLoginAttempts = 0,
            lockedUntil = null,
            lastLoginAt = LocalDateTime.now()
        )
    }

    private fun isLocked(): Boolean {
        return lockedUntil?.isAfter(LocalDateTime.now()) == true
    }
}
```

### Value Objects

```kotlin
@Embeddable
data class Email(
    @Column(name = "email", unique = true)
    val value: String
) {
    init {
        require(isValid(value)) { "Invalid email format: $value" }
    }

    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()

        fun isValid(email: String): Boolean = EMAIL_REGEX.matches(email)
    }
}

@Embeddable
data class Password(
    @Column(name = "password_hash")
    val hash: String
) {
    companion object {
        fun fromRaw(rawPassword: String): Password {
            require(isStrong(rawPassword)) { "Password does not meet security requirements" }
            return Password(BCrypt.hashpw(rawPassword, BCrypt.gensalt()))
        }

        private fun isStrong(password: String): Boolean {
            return password.length >= 8 &&
                   password.any { it.isUpperCase() } &&
                   password.any { it.isLowerCase() } &&
                   password.any { it.isDigit() } &&
                   password.any { !it.isLetterOrDigit() }
        }
    }
}
```

## 🔄 Casos de Uso

### Authentication Use Case

```kotlin
@Service
@Transactional
class AuthenticationUseCase(
    private val userRepository: AuthUserRepository,
    private val passwordService: PasswordService,
    private val tokenService: TokenService,
    private val emailService: EmailService,
    private val tokenCache: TokenCache
) {
    fun registerUser(command: RegisterUserCommand): RegisterUserResult {
        // Validar que email no existe
        if (userRepository.existsByEmail(command.email)) {
            return RegisterUserResult.EmailAlreadyExists
        }

        // Crear usuario
        val user = AuthUser.create(
            email = Email(command.email),
            phone = Phone(command.phone),
            firstName = command.firstName,
            lastName = command.lastName,
            rawPassword = command.password,
            passwordService = passwordService
        )

        // Guardar usuario
        val savedUser = userRepository.save(user)

        // Enviar email de verificación
        val verificationToken = tokenService.generateEmailVerificationToken(savedUser.id)
        emailService.sendEmailVerification(savedUser.email.value, verificationToken)

        return RegisterUserResult.Success(savedUser)
    }

    fun login(command: LoginCommand): LoginResult {
        // Buscar usuario
        val user = userRepository.findByEmailOrPhone(command.identifier)
            ?: return LoginResult.InvalidCredentials

        // Autenticar
        when (val result = user.authenticate(command.password, passwordService)) {
            is AuthenticationResult.Success -> {
                // Actualizar último login
                val updatedUser = user.recordSuccessfulLogin()
                userRepository.save(updatedUser)

                // Generar tokens
                val accessToken = tokenService.generateAccessToken(updatedUser)
                val refreshToken = tokenService.generateRefreshToken(updatedUser)

                // Cachear refresh token
                tokenCache.storeRefreshToken(updatedUser.id, refreshToken)

                return LoginResult.Success(accessToken, refreshToken, updatedUser)
            }
            is AuthenticationResult.InvalidCredentials -> {
                // Registrar intento fallido
                val updatedUser = user.recordFailedLogin()
                userRepository.save(updatedUser)

                return LoginResult.InvalidCredentials
            }
            is AuthenticationResult.AccountLocked -> {
                return LoginResult.AccountLocked(result.lockedUntil)
            }
            is AuthenticationResult.EmailNotVerified -> {
                return LoginResult.EmailNotVerified
            }
        }
    }
}
```

## 🧪 Testing

### Tests Unitarios

```kotlin
@ExtendWith(MockitoExtension::class)
class AuthenticationUseCaseTest {

    @Mock
    private lateinit var userRepository: AuthUserRepository

    @Mock
    private lateinit var passwordService: PasswordService

    @Mock
    private lateinit var tokenService: TokenService

    @InjectMocks
    private lateinit var authenticationUseCase: AuthenticationUseCase

    @Test
    fun `should register user successfully`() {
        // Given
        val command = RegisterUserCommand(
            email = "test@example.com",
            phone = "5551234567",
            firstName = "Test",
            lastName = "User",
            password = "SecurePassword123!"
        )

        given(userRepository.existsByEmail(any())).willReturn(false)
        given(userRepository.save(any())).willAnswer { it.arguments[0] }

        // When
        val result = authenticationUseCase.registerUser(command)

        // Then
        assertThat(result).isInstanceOf(RegisterUserResult.Success::class.java)
        verify(userRepository).save(any())
        verify(emailService).sendEmailVerification(any(), any())
    }
}
```

### Tests de Integración

```kotlin
@SpringBootTest
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    static val postgres = PostgreSQLContainer("postgres:15")
        .withDatabaseName("test_auth")
        .withUsername("test")
        .withPassword("test")

    @Container
    static val redis = GenericContainer("redis:7-alpine")
        .withExposedPorts(6379)

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should register user and return 201`() {
        val request = """
            {
                "email": "test@example.com",
                "phone": "5551234567",
                "firstName": "Test",
                "lastName": "User",
                "password": "SecurePassword123!"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.userId").exists())
        .andExpect(jsonPath("$.email").value("test@example.com"))
    }
}
```

### Ejecutar Tests

```bash
# Tests unitarios
./gradlew test

# Tests de integración
./gradlew integrationTest

# Tests con coverage
./gradlew jacocoTestReport

# Ver reporte de coverage
open build/reports/jacoco/test/html/index.html
```

## 🐳 Docker

### Dockerfile

```dockerfile
FROM openjdk:21-jdk-slim as builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM openjdk:21-jre-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
COPY config/jwt/ /app/config/jwt/
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  auth-service:
    build: .
    ports:
      - '8081:8081'
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/gasolinera_auth
      - REDIS_HOST=redis
    depends_on:
      - postgres
      - redis
    volumes:
      - ./config/jwt:/app/config/jwt:ro

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: gasolinera_auth
      POSTGRES_USER: gasolinera_user
      POSTGRES_PASSWORD: secure_password
    ports:
      - '5432:5432'
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - '6379:6379'
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

## 🚀 Deployment

### Variables de Entorno de Producción

```bash
# Database
DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/gasolinera_auth
DATABASE_USERNAME=auth_service_user
DATABASE_PASSWORD=super_secure_password

# Redis
REDIS_HOST=redis-cluster.example.com
REDIS_PORT=6379
REDIS_PASSWORD=redis_secure_password

# JWT Keys
JWT_PRIVATE_KEY_PATH=/app/secrets/jwt/private.pem
JWT_PUBLIC_KEY_PATH=/app/secrets/jwt/public.pem

# Email
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=sendgrid_api_key

# Observability
JAEGER_ENDPOINT=http://jaeger:14268/api/traces
PROMETHEUS_ENABLED=true

# Security
ALLOWED_ORIGINS=https://app.gasolinera-jsm.com,https://admin.gasolinera-jsm.com
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
        - name: auth-service
          image: gasolinera-jsm/auth-service:latest
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: 'kubernetes'
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: auth-service-secrets
                  key: database-url
          volumeMounts:
            - name: jwt-keys
              mountPath: /app/secrets/jwt
              readOnly: true
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10
      volumes:
        - name: jwt-keys
          secret:
            secretName: jwt-keys
```

## 🔧 Troubleshooting

### Problemas Comunes

#### 1. Usuario No Puede Hacer Login

```bash
# Verificar si el usuario existe
psql -h localhost -U gasolinera_user -d gasolinera_auth
SELECT id, email, is_email_verified, is_active, failed_login_attempts, locked_until
FROM auth_users WHERE email = 'user@example.com';

# Verificar logs de autenticación
docker logs auth-service | grep "Authentication failed"
```

#### 2. JWT Token Inválido

```bash
# Verificar configuración de claves JWT
ls -la config/jwt/
cat config/jwt/public.pem

# Decodificar JWT token
echo "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..." | base64 -d
```

#### 3. Email No Se Envía

```bash
# Verificar configuración SMTP
curl -v telnet://smtp.gmail.com:587

# Verificar logs de email service
docker logs auth-service | grep "EmailService"
```

#### 4. Redis Connection Issues

```bash
# Verificar conexión a Redis
redis-cli -h localhost -p 6379 ping

# Verificar tokens en cache
redis-cli -h localhost -p 6379
> KEYS refresh_token:*
> GET refresh_token:user123
```

### Logs de Debug

```yaml
# application-debug.yml
logging:
  level:
    com.gasolinerajsm.auth: DEBUG
    org.springframework.security: DEBUG
    org.springframework.mail: DEBUG
    org.hibernate.SQL: DEBUG
```

## 📊 Monitoreo

### Métricas Disponibles

- **auth.registrations.total** - Total de registros
- **auth.logins.total** - Total de logins
- **auth.login.failures** - Fallos de login
- **auth.tokens.generated** - Tokens generados
- **auth.emails.sent** - Emails enviados

### Health Checks

```bash
# Health check general
curl http://localhost:8081/actuator/health

# Health check de base de datos
curl http://localhost:8081/actuator/health/db

# Health check de Redis
curl http://localhost:8081/actuator/health/redis
```

## 📚 Referencias

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [JWT.io](https://jwt.io/) - JWT Debugger
- [BCrypt Calculator](https://bcrypt-generator.com/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)

## 🤝 Contribución

1. Fork el repositorio
2. Crear feature branch (`git checkout -b feature/auth-improvement`)
3. Commit cambios (`git commit -m 'Add new auth feature'`)
4. Push al branch (`git push origin feature/auth-improvement`)
5. Crear Pull Request

## 📄 Licencia

Este proyecto es propiedad de Gasolinera JSM. Todos los derechos reservados.

---

**🔐 ¿Necesitas ayuda con autenticación?**

- 📧 Email: auth-team@gasolinera-jsm.com
- 💬 Slack: #auth-service-support
- 📖 Docs: https://docs.gasolinera-jsm.com/auth

_Última actualización: Enero 2024_
