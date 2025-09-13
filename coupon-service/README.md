# 🎫 Coupon Service - Gasolinera JSM

## 📋 Descripción

El **Coupon Service** es el corazón del sistema de cupones digitales de Gasolinera JSM. Maneja todo el ciclo de vida de los cupones: desde la compra y generación de códigos QR criptográficamente seguros, hasta el canje en estaciones de servicio y la generación automática de tickets de rifa. Integra múltiples métodos de pago y proporciona analytics detallados de uso.

## 🏗️ Arquitectura Hexagonal

```
┌─────────────────────────────────────────────────────────────┐
│                      Coupon Service                          │
├─────────────────────────────────────────────────────────────┤
│                     Web Layer (Adapters)                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │CouponController │  │RedemptionController│ │AdminController│ │
│  │  (Purchase)     │  │   (Redeem)      │  │ (Analytics)  │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                   Application Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │CouponPurchase   │  │CouponRedemption │  │CouponAnalytics│ │
│  │   UseCase       │  │    UseCase      │  │   UseCase     │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │     Coupon      │  │    QRCode       │  │  Redemption   │ │
│  │  (Aggregate)    │  │ (Value Object)  │  │ (Aggregate)   │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │QRCodeService    │  │ PaymentService  │  │TicketService  │ │
│  │                 │  │                 │  │              │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                Infrastructure Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │CouponRepository │  │ PaymentGateway  │  │EventPublisher │ │
│  │  (PostgreSQL)   │  │   (Stripe)      │  │ (RabbitMQ)   │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 Características Principales

### 🎫 Gestión de Cupones

- **Compra Digital** con múltiples métodos de pago
- **QR Codes Seguros** con firma criptográfica
- **Validación en Tiempo Real** anti-fraude
- **Expiración Flexible** con políticas configurables
- **Cancelación y Reembolsos** automatizados

### 💳 Procesamiento de Pagos

- **Múltiples Gateways** (Stripe, PayPal, SPEI)
- **Tokenización Segura** de tarjetas
- **Procesamiento Asíncrono** para alta disponibilidad
- **Reconciliación Automática** de transacciones
- **Manejo de Disputas** y chargebacks

### 🔐 Seguridad y Anti-fraude

- **Códigos QR Únicos** con timestamp y checksum
- **Validación Geográfica** por ubicación de estación
- **Rate Limiting** por usuario y endpoint
- **Detección de Patrones** sospechosos
- **Audit Trail** completo de transacciones

### 🎰 Integración con Rifas

- **Generación Automática** de tickets de rifa
- **Multiplicadores Dinámicos** por tipo de combustible
- **Bonificaciones** por engagement con anuncios
- **Tracking de Participación** en tiempo real

## 🛠️ Tecnologías

- **Spring Boot 3.2** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **PostgreSQL** - Base de datos transaccional
- **Redis** - Cache y rate limiting
- **RabbitMQ** - Messaging asíncrono
- **Stripe API** - Procesamiento de pagos
- **ZXing** - Generación de códigos QR
- **Jackson** - Serialización JSON
- **Testcontainers** - Testing con containers

## 🚀 Quick Start

### Prerrequisitos

- Java 21+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- RabbitMQ 3.12+
- Cuenta de Stripe (para pagos)

### 1. Clonar y Configurar

```bash
git clone https://github.com/gasolinera-jsm/coupon-service.git
cd coupon-service

# Copiar configuración de ejemplo
cp src/main/resources/application-example.yml src/main/resources/application-local.yml
```

### 2. Configurar Variables de Entorno

```bash
# .env.local
DATABASE_URL=jdbc:postgresql://localhost:5432/gasolinera_coupons
DATABASE_USERNAME=gasolinera_user
DATABASE_PASSWORD=secure_password
REDIS_HOST=localhost
REDIS_PORT=6379
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
STRIPE_SECRET_KEY=sk_test_your_stripe_secret_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret
QR_SIGNING_KEY=your-32-character-signing-key-here
```

### 3. Configurar Stripe Webhooks

```bash
# Instalar Stripe CLI
brew install stripe/stripe-cli/stripe

# Login a Stripe
stripe login

# Configurar webhook local
stripe listen --forward-to localhost:8084/api/v1/webhooks/stripe
```

### 4. Ejecutar con Docker Compose

```bash
# Levantar dependencias
docker-compose -f docker-compose.dev.yml up -d postgres redis rabbitmq

# Ejecutar migraciones
./gradlew flywayMigrate

# Ejecutar la aplicación
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 5. Verificar Funcionamiento

```bash
# Health check
curl http://localhost:8084/actuator/health

# Comprar cupón de prueba (requiere JWT token)
curl -X POST http://localhost:8084/api/v1/coupons/purchase \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "stationId": "123e4567-e89b-12d3-a456-426614174000",
    "amount": 500.00,
    "fuelType": "REGULAR",
    "paymentMethod": "CREDIT_CARD",
    "paymentToken": "tok_visa"
  }'
```

## 📁 Estructura del Proyecto

```
coupon-service/
├── src/main/kotlin/com/gasolinerajsm/coupon/
│   ├── domain/                    # Capa de Dominio
│   │   ├── model/
│   │   │   ├── Coupon.kt         # Agregado principal
│   │   │   ├── QRCode.kt         # Value Object
│   │   │   ├── Amount.kt         # Value Object
│   │   │   ├── Redemption.kt     # Agregado
│   │   │   └── CouponStatus.kt   # Enum
│   │   ├── service/
│   │   │   ├── QRCodeService.kt
│   │   │   ├── PaymentService.kt
│   │   │   ├── TicketService.kt
│   │   │   └── AntifraudService.kt
│   │   └── repository/
│   │       ├── CouponRepository.kt      # Puerto
│   │       └── RedemptionRepository.kt  # Puerto
│   ├── application/               # Capa de Aplicación
│   │   ├── usecase/
│   │   │   ├── CouponPurchaseUseCase.kt
│   │   │   ├── CouponRedemptionUseCase.kt
│   │   │   ├── CouponManagementUseCase.kt
│   │   │   └── CouponAnalyticsUseCase.kt
│   │   └── dto/
│   │       └── CouponCommands.kt
│   ├── infrastructure/            # Capa de Infraestructura
│   │   ├── persistence/
│   │   │   ├── CouponJpaRepository.kt
│   │   │   ├── CouponEntity.kt
│   │   │   └── CouponRepositoryImpl.kt
│   │   ├── payment/
│   │   │   ├── StripePaymentGateway.kt
│   │   │   ├── PayPalPaymentGateway.kt
│   │   │   └── PaymentGatewayFactory.kt
│   │   ├── messaging/
│   │   │   ├── CouponEventPublisher.kt
│   │   │   ├── RedemptionEventListener.kt
│   │   │   └── RabbitMQConfig.kt
│   │   ├── cache/
│   │   │   ├── CouponCache.kt
│   │   │   └── RedisConfig.kt
│   │   └── security/
│   │       ├── QRCodeGenerator.kt
│   │       ├── QRCodeValidator.kt
│   │       └── CryptoService.kt
│   ├── web/                       # Capa Web
│   │   ├── controller/
│   │   │   ├── CouponController.kt
│   │   │   ├── RedemptionController.kt
│   │   │   ├── WebhookController.kt
│   │   │   └── AdminController.kt
│   │   ├── dto/
│   │   │   └── CouponDTOs.kt
│   │   └── filter/
│   │       ├── RateLimitFilter.kt
│   │       └── AntifraudFilter.kt
│   └── CouponServiceApplication.kt
├── src/main/resources/
│   ├── db/migration/              # Flyway migrations
│   │   ├── V1__Create_coupons_table.sql
│   │   ├── V2__Create_redemptions_table.sql
│   │   ├── V3__Create_payment_transactions_table.sql
│   │   └── V4__Add_indexes_and_constraints.sql
│   ├── qr-templates/              # Plantillas de QR
│   │   ├── coupon-qr-template.svg
│   │   └── branded-qr-template.svg
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

### Payment Gateways

```yaml
payment:
  gateways:
    stripe:
      secret-key: ${STRIPE_SECRET_KEY}
      webhook-secret: ${STRIPE_WEBHOOK_SECRET}
      api-version: '2023-10-16'
    paypal:
      client-id: ${PAYPAL_CLIENT_ID}
      client-secret: ${PAYPAL_CLIENT_SECRET}
      environment: ${PAYPAL_ENVIRONMENT:sandbox}

  default-gateway: stripe
  timeout: 30s
  retry-attempts: 3
```

### QR Code Configuration

```yaml
qr-code:
  signing-key: ${QR_SIGNING_KEY}
  expiration-minutes: 43200 # 30 días
  error-correction-level: H # High (30%)
  size: 300x300
  format: PNG

  security:
    include-timestamp: true
    include-checksum: true
    encryption-enabled: true
```

### RabbitMQ Configuration

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}

messaging:
  exchanges:
    coupon-events: coupon.events
    redemption-events: redemption.events
  queues:
    coupon-purchased: coupon.purchased
    coupon-redeemed: coupon.redeemed
    ticket-generated: ticket.generated
```

## 🎫 Modelo de Dominio

### Coupon Aggregate

```kotlin
@Entity
@Table(name = "coupons")
class Coupon private constructor(
    @Id val id: UUID,
    val userId: UUID,
    val stationId: UUID,
    @Embedded val amount: Amount,
    @Enumerated(EnumType.STRING) val fuelType: FuelType,
    @Embedded val qrCode: QRCode,
    @Enumerated(EnumType.STRING) val status: CouponStatus,
    val paymentTransactionId: String,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val redeemedAt: LocalDateTime?,
    val cancelledAt: LocalDateTime?,
    val refundedAt: LocalDateTime?
) {
    companion object {
        fun create(
            userId: UUID,
            stationId: UUID,
            amount: BigDecimal,
            fuelType: FuelType,
            paymentTransactionId: String,
            qrCodeService: QRCodeService
        ): Coupon {
            val couponId = UUID.randomUUID()
            val qrCode = qrCodeService.generateQRCode(couponId, stationId, amount)

            return Coupon(
                id = couponId,
                userId = userId,
                stationId = stationId,
                amount = Amount(amount),
                fuelType = fuelType,
                qrCode = qrCode,
                status = CouponStatus.ACTIVE,
                paymentTransactionId = paymentTransactionId,
                createdAt = LocalDateTime.now(),
                expiresAt = LocalDateTime.now().plusDays(30),
                redeemedAt = null,
                cancelledAt = null,
                refundedAt = null
            )
        }
    }

    fun redeem(
        stationId: UUID,
        fuelAmount: BigDecimal,
        pricePerLiter: BigDecimal,
        redemptionLocation: Location
    ): RedemptionResult {
        // Validaciones de negocio
        if (status != CouponStatus.ACTIVE) {
            return RedemptionResult.InvalidStatus(status)
        }

        if (isExpired()) {
            return RedemptionResult.Expired(expiresAt)
        }

        if (this.stationId != stationId) {
            return RedemptionResult.WrongStation(this.stationId, stationId)
        }

        val totalCost = fuelAmount * pricePerLiter
        if (totalCost > amount.value) {
            return RedemptionResult.InsufficientBalance(amount.value, totalCost)
        }

        // Crear redención
        val redemption = Redemption.create(
            couponId = id,
            stationId = stationId,
            fuelAmount = fuelAmount,
            pricePerLiter = pricePerLiter,
            totalCost = totalCost,
            location = redemptionLocation
        )

        // Actualizar estado del cupón
        val updatedCoupon = copy(
            status = if (totalCost == amount.value) CouponStatus.REDEEMED else CouponStatus.PARTIALLY_REDEEMED,
            redeemedAt = LocalDateTime.now()
        )

        return RedemptionResult.Success(updatedCoupon, redemption)
    }

    fun cancel(reason: CancellationReason): CancellationResult {
        if (status != CouponStatus.ACTIVE) {
            return CancellationResult.InvalidStatus(status)
        }

        val hoursFromPurchase = Duration.between(createdAt, LocalDateTime.now()).toHours()
        val refundPercentage = when {
            hoursFromPurchase <= 2 -> 100  // Reembolso completo
            hoursFromPurchase <= 24 -> 90  // 90% de reembolso
            else -> 0                      // Sin reembolso
        }

        val updatedCoupon = copy(
            status = CouponStatus.CANCELLED,
            cancelledAt = LocalDateTime.now()
        )

        return CancellationResult.Success(updatedCoupon, refundPercentage)
    }

    fun regenerateQRCode(qrCodeService: QRCodeService): Coupon {
        val newQRCode = qrCodeService.generateQRCode(id, stationId, amount.value)
        return copy(qrCode = newQRCode)
    }

    private fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiresAt)
    }
}
```

### Value Objects

```kotlin
@Embeddable
data class QRCode(
    @Column(name = "qr_code_data")
    val data: String,

    @Column(name = "qr_code_signature")
    val signature: String,

    @Column(name = "qr_generated_at")
    val generatedAt: LocalDateTime
) {
    fun isValid(cryptoService: CryptoService): Boolean {
        return cryptoService.verifySignature(data, signature)
    }

    fun isExpired(maxAgeMinutes: Long = 43200): Boolean { // 30 días
        return generatedAt.isBefore(LocalDateTime.now().minusMinutes(maxAgeMinutes))
    }
}

@Embeddable
data class Amount(
    @Column(name = "amount", precision = 10, scale = 2)
    val value: BigDecimal
) {
    init {
        require(value > BigDecimal.ZERO) { "Amount must be positive" }
        require(value <= BigDecimal("10000")) { "Amount cannot exceed $10,000" }
        require(value >= BigDecimal("50")) { "Minimum amount is $50" }
    }

    fun subtract(other: BigDecimal): Amount {
        return Amount(value - other)
    }

    fun canCover(cost: BigDecimal): Boolean {
        return value >= cost
    }
}
```

### Redemption Aggregate

```kotlin
@Entity
@Table(name = "redemptions")
class Redemption private constructor(
    @Id val id: UUID,
    val couponId: UUID,
    val stationId: UUID,
    val fuelAmount: BigDecimal,
    val pricePerLiter: BigDecimal,
    val totalCost: BigDecimal,
    @Embedded val location: Location,
    val redeemedAt: LocalDateTime,
    val attendantId: String?,
    val pumpNumber: Int?,
    val ticketsGenerated: Int
) {
    companion object {
        fun create(
            couponId: UUID,
            stationId: UUID,
            fuelAmount: BigDecimal,
            pricePerLiter: BigDecimal,
            totalCost: BigDecimal,
            location: Location,
            attendantId: String? = null,
            pumpNumber: Int? = null
        ): Redemption {
            // Calcular tickets de rifa basado en el monto
            val baseTickets = (totalCost / BigDecimal("100")).toInt() // 1 ticket por cada $100
            val bonusTickets = if (fuelAmount > BigDecimal("20")) 1 else 0 // Bonus por más de 20L

            return Redemption(
                id = UUID.randomUUID(),
                couponId = couponId,
                stationId = stationId,
                fuelAmount = fuelAmount,
                pricePerLiter = pricePerLiter,
                totalCost = totalCost,
                location = location,
                redeemedAt = LocalDateTime.now(),
                attendantId = attendantId,
                pumpNumber = pumpNumber,
                ticketsGenerated = baseTickets + bonusTickets
            )
        }
    }

    fun generateDigitalReceipt(): DigitalReceipt {
        return DigitalReceipt(
            redemptionId = id,
            couponId = couponId,
            stationId = stationId,
            fuelAmount = fuelAmount,
            pricePerLiter = pricePerLiter,
            totalCost = totalCost,
            ticketsEarned = ticketsGenerated,
            redeemedAt = redeemedAt
        )
    }
}
```

## 🔄 Casos de Uso

### Coupon Purchase Use Case

```kotlin
@Service
@Transactional
class CouponPurchaseUseCase(
    private val couponRepository: CouponRepository,
    private val paymentService: PaymentService,
    private val qrCodeService: QRCodeService,
    private val antifraudService: AntifraudService,
    private val eventPublisher: EventPublisher
) {
    fun purchaseCoupon(command: PurchaseCouponCommand): PurchaseCouponResult {
        // Validación anti-fraude
        val fraudCheck = antifraudService.validatePurchase(command)
        if (!fraudCheck.isValid) {
            return PurchaseCouponResult.FraudDetected(fraudCheck.reason)
        }

        // Procesar pago
        val paymentResult = paymentService.processPayment(
            amount = command.amount,
            paymentMethod = command.paymentMethod,
            paymentToken = command.paymentToken,
            userId = command.userId
        )

        if (!paymentResult.isSuccess) {
            return PurchaseCouponResult.PaymentFailed(paymentResult.error)
        }

        // Crear cupón
        val coupon = Coupon.create(
            userId = command.userId,
            stationId = command.stationId,
            amount = command.amount,
            fuelType = command.fuelType,
            paymentTransactionId = paymentResult.transactionId,
            qrCodeService = qrCodeService
        )

        // Guardar cupón
        val savedCoupon = couponRepository.save(coupon)

        // Publicar evento
        eventPublisher.publishCouponPurchasedEvent(
            CouponPurchasedEvent(
                couponId = savedCoupon.id,
                userId = savedCoupon.userId,
                stationId = savedCoupon.stationId,
                amount = savedCoupon.amount.value,
                fuelType = savedCoupon.fuelType,
                purchasedAt = savedCoupon.createdAt
            )
        )

        return PurchaseCouponResult.Success(savedCoupon, paymentResult)
    }

    fun cancelCoupon(command: CancelCouponCommand): CancelCouponResult {
        // Buscar cupón
        val coupon = couponRepository.findById(command.couponId)
            ?: return CancelCouponResult.CouponNotFound

        // Verificar propiedad
        if (coupon.userId != command.userId) {
            return CancelCouponResult.AccessDenied
        }

        // Cancelar cupón
        val cancellationResult = coupon.cancel(command.reason)

        when (cancellationResult) {
            is CancellationResult.Success -> {
                // Guardar cupón actualizado
                val updatedCoupon = couponRepository.save(cancellationResult.coupon)

                // Procesar reembolso si aplica
                if (cancellationResult.refundPercentage > 0) {
                    val refundAmount = coupon.amount.value *
                        BigDecimal(cancellationResult.refundPercentage) / BigDecimal(100)

                    paymentService.processRefund(
                        originalTransactionId = coupon.paymentTransactionId,
                        refundAmount = refundAmount,
                        reason = command.reason.toString()
                    )
                }

                // Publicar evento
                eventPublisher.publishCouponCancelledEvent(
                    CouponCancelledEvent(
                        couponId = updatedCoupon.id,
                        userId = updatedCoupon.userId,
                        reason = command.reason,
                        refundPercentage = cancellationResult.refundPercentage,
                        cancelledAt = updatedCoupon.cancelledAt!!
                    )
                )

                return CancelCouponResult.Success(updatedCoupon, cancellationResult.refundPercentage)
            }
            is CancellationResult.InvalidStatus -> {
                return CancelCouponResult.InvalidStatus(cancellationResult.currentStatus)
            }
        }
    }
}
```

### Coupon Redemption Use Case

```kotlin
@Service
@Transactional
class CouponRedemptionUseCase(
    private val couponRepository: CouponRepository,
    private val redemptionRepository: RedemptionRepository,
    private val qrCodeService: QRCodeService,
    private val ticketService: TicketService,
    private val eventPublisher: EventPublisher
) {
    fun redeemCoupon(command: RedeemCouponCommand): RedeemCouponResult {
        // Validar y decodificar QR code
        val qrValidation = qrCodeService.validateQRCode(command.qrCode)
        if (!qrValidation.isValid) {
            return RedeemCouponResult.InvalidQRCode(qrValidation.error)
        }

        // Buscar cupón
        val coupon = couponRepository.findById(qrValidation.couponId)
            ?: return RedeemCouponResult.CouponNotFound

        // Validar ubicación (opcional pero recomendado)
        if (command.location != null) {
            val locationValidation = validateRedemptionLocation(
                command.stationId,
                command.location
            )
            if (!locationValidation.isValid) {
                return RedeemCouponResult.InvalidLocation(locationValidation.error)
            }
        }

        // Redimir cupón
        val redemptionResult = coupon.redeem(
            stationId = command.stationId,
            fuelAmount = command.fuelAmount,
            pricePerLiter = command.pricePerLiter,
            redemptionLocation = command.location ?: Location.unknown()
        )

        when (redemptionResult) {
            is RedemptionResult.Success -> {
                // Guardar cupón y redención
                val updatedCoupon = couponRepository.save(redemptionResult.coupon)
                val savedRedemption = redemptionRepository.save(redemptionResult.redemption)

                // Generar tickets de rifa
                val raffleTickets = ticketService.generateTickets(
                    userId = updatedCoupon.userId,
                    redemptionId = savedRedemption.id,
                    ticketCount = savedRedemption.ticketsGenerated,
                    fuelType = updatedCoupon.fuelType
                )

                // Publicar evento
                eventPublisher.publishCouponRedeemedEvent(
                    CouponRedeemedEvent(
                        couponId = updatedCoupon.id,
                        redemptionId = savedRedemption.id,
                        userId = updatedCoupon.userId,
                        stationId = command.stationId,
                        fuelAmount = command.fuelAmount,
                        totalCost = savedRedemption.totalCost,
                        ticketsGenerated = savedRedemption.ticketsGenerated,
                        redeemedAt = savedRedemption.redeemedAt
                    )
                )

                return RedeemCouponResult.Success(
                    coupon = updatedCoupon,
                    redemption = savedRedemption,
                    raffleTickets = raffleTickets
                )
            }
            is RedemptionResult.InvalidStatus -> {
                return RedeemCouponResult.InvalidCouponStatus(redemptionResult.status)
            }
            is RedemptionResult.Expired -> {
                return RedeemCouponResult.CouponExpired(redemptionResult.expiresAt)
            }
            is RedemptionResult.WrongStation -> {
                return RedeemCouponResult.WrongStation(
                    redemptionResult.expectedStationId,
                    redemptionResult.actualStationId
                )
            }
            is RedemptionResult.InsufficientBalance -> {
                return RedeemCouponResult.InsufficientBalance(
                    redemptionResult.availableBalance,
                    redemptionResult.requiredAmount
                )
            }
        }
    }
}
```

## 🧪 Testing

### Tests Unitarios

```kotlin
@ExtendWith(MockitoExtension::class)
class CouponPurchaseUseCaseTest {

    @Mock
    private lateinit var couponRepository: CouponRepository

    @Mock
    private lateinit var paymentService: PaymentService

    @Mock
    private lateinit var qrCodeService: QRCodeService

    @InjectMocks
    private lateinit var couponPurchaseUseCase: CouponPurchaseUseCase

    @Test
    fun `should purchase coupon successfully`() {
        // Given
        val command = PurchaseCouponCommand(
            userId = UUID.randomUUID(),
            stationId = UUID.randomUUID(),
            amount = BigDecimal("500.00"),
            fuelType = FuelType.REGULAR,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            paymentToken = "tok_visa"
        )

        val paymentResult = PaymentResult.Success("txn_123", "ch_456")
        given(paymentService.processPayment(any(), any(), any(), any()))
            .willReturn(paymentResult)

        given(couponRepository.save(any())).willAnswer { it.arguments[0] }

        // When
        val result = couponPurchaseUseCase.purchaseCoupon(command)

        // Then
        assertThat(result).isInstanceOf(PurchaseCouponResult.Success::class.java)
        verify(couponRepository).save(any())
        verify(eventPublisher).publishCouponPurchasedEvent(any())
    }

    @Test
    fun `should reject purchase when payment fails`() {
        // Given
        val command = PurchaseCouponCommand(
            userId = UUID.randomUUID(),
            stationId = UUID.randomUUID(),
            amount = BigDecimal("500.00"),
            fuelType = FuelType.REGULAR,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            paymentToken = "tok_declined"
        )

        val paymentResult = PaymentResult.Failed("card_declined", "Your card was declined")
        given(paymentService.processPayment(any(), any(), any(), any()))
            .willReturn(paymentResult)

        // When
        val result = couponPurchaseUseCase.purchaseCoupon(command)

        // Then
        assertThat(result).isInstanceOf(PurchaseCouponResult.PaymentFailed::class.java)
        verify(couponRepository, never()).save(any())
    }
}
```

### Tests de Integración

```kotlin
@SpringBootTest
@Testcontainers
class CouponControllerIntegrationTest {

    @Container
    static val postgres = PostgreSQLContainer("postgres:15")
        .withDatabaseName("test_coupons")
        .withUsername("test")
        .withPassword("test")

    @Container
    static val redis = GenericContainer("redis:7-alpine")
        .withExposedPorts(6379)

    @Container
    static val rabbitmq = RabbitMQContainer("rabbitmq:3.12-management-alpine")

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var paymentService: PaymentService

    @Test
    fun `should purchase coupon and return 201`() {
        // Given
        given(paymentService.processPayment(any(), any(), any(), any()))
            .willReturn(PaymentResult.Success("txn_123", "ch_456"))

        val request = """
            {
                "stationId": "123e4567-e89b-12d3-a456-426614174000",
                "amount": 500.00,
                "fuelType": "REGULAR",
                "paymentMethod": "CREDIT_CARD",
                "paymentToken": "tok_visa"
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(
            post("/api/v1/coupons/purchase")
                .header("Authorization", "Bearer $validJwtToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.couponId").exists())
        .andExpect(jsonPath("$.qrCode").exists())
        .andExpect(jsonPath("$.amount").value(500.00))
        .andExpect(jsonPath("$.fuelType").value("REGULAR"))
    }
}
```

### Tests de QR Code

```kotlin
@ExtendWith(MockitoExtension::class)
class QRCodeServiceTest {

    @Mock
    private lateinit var cryptoService: CryptoService

    @InjectMocks
    private lateinit var qrCodeService: QRCodeService

    @Test
    fun `should generate valid QR code`() {
        // Given
        val couponId = UUID.randomUUID()
        val stationId = UUID.randomUUID()
        val amount = BigDecimal("500.00")

        given(cryptoService.sign(any())).willReturn("signature123")

        // When
        val qrCode = qrCodeService.generateQRCode(couponId, stationId, amount)

        // Then
        assertThat(qrCode.data).contains(couponId.toString())
        assertThat(qrCode.data).contains(stationId.toString())
        assertThat(qrCode.signature).isEqualTo("signature123")
        assertThat(qrCode.generatedAt).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
    }

    @Test
    fun `should validate QR code successfully`() {
        // Given
        val qrCodeData = "coupon:$couponId:station:$stationId:amount:500.00:timestamp:${System.currentTimeMillis()}"
        val qrCode = QRCode(qrCodeData, "signature123", LocalDateTime.now())

        given(cryptoService.verifySignature(qrCodeData, "signature123")).willReturn(true)

        // When
        val validation = qrCodeService.validateQRCode(qrCode.data)

        // Then
        assertThat(validation.isValid).isTrue()
        assertThat(validation.couponId).isEqualTo(couponId)
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

### Dockerfile

```dockerfile
FROM openjdk:21-jdk-slim as builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM openjdk:21-jre-slim
WORKDIR /app

# Instalar herramientas para QR codes
RUN apt-get update && apt-get install -y \
    imagemagick \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar app.jar
COPY src/main/resources/qr-templates/ /app/qr-templates/

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  coupon-service:
    build: .
    ports:
      - '8084:8084'
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/gasolinera_coupons
      - REDIS_HOST=redis
      - RABBITMQ_HOST=rabbitmq
    depends_on:
      - postgres
      - redis
      - rabbitmq
    volumes:
      - ./config/qr-signing-key.txt:/app/config/qr-signing-key.txt:ro

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: gasolinera_coupons
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

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    ports:
      - '5672:5672'
      - '15672:15672'
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:
```

## 🚀 Deployment

### Variables de Entorno de Producción

```bash
# Database
DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/gasolinera_coupons
DATABASE_USERNAME=coupon_service_user
DATABASE_PASSWORD=super_secure_password

# Cache
REDIS_HOST=redis-cluster.example.com
REDIS_PORT=6379
REDIS_PASSWORD=redis_secure_password

# Messaging
RABBITMQ_HOST=rabbitmq-cluster.example.com
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=coupon_service
RABBITMQ_PASSWORD=rabbitmq_password

# Payment Gateways
STRIPE_SECRET_KEY=sk_live_your_live_stripe_key
STRIPE_WEBHOOK_SECRET=whsec_your_live_webhook_secret
PAYPAL_CLIENT_ID=your_paypal_client_id
PAYPAL_CLIENT_SECRET=your_paypal_client_secret
PAYPAL_ENVIRONMENT=live

# Security
QR_SIGNING_KEY=your-production-32-character-key
JWT_PUBLIC_KEY_PATH=/app/secrets/jwt/public.pem

# Observability
JAEGER_ENDPOINT=http://jaeger:14268/api/traces
PROMETHEUS_ENABLED=true
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: coupon-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: coupon-service
  template:
    metadata:
      labels:
        app: coupon-service
    spec:
      containers:
        - name: coupon-service
          image: gasolinera-jsm/coupon-service:latest
          ports:
            - containerPort: 8084
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: 'kubernetes'
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: coupon-service-secrets
                  key: database-url
            - name: STRIPE_SECRET_KEY
              valueFrom:
                secretKeyRef:
                  name: payment-secrets
                  key: stripe-secret-key
          volumeMounts:
            - name: qr-signing-key
              mountPath: /app/config
              readOnly: true
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
              port: 8084
            initialDelaySeconds: 60
            periodSeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8084
            initialDelaySeconds: 30
            periodSeconds: 10
      volumes:
        - name: qr-signing-key
          secret:
            secretName: qr-signing-key
```

## 🔧 Troubleshooting

### Problemas Comunes

#### 1. QR Code Inválido

```bash
# Verificar configuración de firma
cat /app/config/qr-signing-key.txt

# Decodificar QR code manualmente
echo "QR_A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6" | base64 -d

# Verificar logs de validación
docker logs coupon-service | grep "QR validation"
```

#### 2. Pago Fallido

```bash
# Verificar configuración de Stripe
curl -u sk_test_your_key: https://api.stripe.com/v1/balance

# Ver webhooks de Stripe
stripe listen --print-json

# Verificar logs de pagos
docker logs coupon-service | grep "Payment processing"
```

#### 3. Eventos No Se Publican

```bash
# Verificar conexión a RabbitMQ
rabbitmqctl status

# Ver colas y mensajes
rabbitmqctl list_queues name messages

# Verificar exchanges
rabbitmqctl list_exchanges name type
```

#### 4. Performance de Redención

```sql
-- Verificar queries lentas
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
WHERE query LIKE '%coupons%' OR query LIKE '%redemptions%'
ORDER BY mean_exec_time DESC
LIMIT 10;

-- Verificar índices
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('coupons', 'redemptions');
```

### Logs de Debug

```yaml
# application-debug.yml
logging:
  level:
    com.gasolinerajsm.coupon: DEBUG
    org.springframework.amqp: DEBUG
    org.springframework.transaction: DEBUG
    org.hibernate.SQL: DEBUG
```

## 📊 Monitoreo

### Métricas Disponibles

- **coupon.purchases.total** - Total de compras
- **coupon.purchases.amount** - Monto total de compras
- **coupon.redemptions.total** - Total de canjes
- **coupon.qr.validations** - Validaciones de QR
- **coupon.payment.failures** - Fallos de pago

### Alertas Recomendadas

```yaml
# prometheus-alerts.yml
groups:
  - name: coupon-service
    rules:
      - alert: HighPaymentFailureRate
        expr: rate(coupon_payment_failures_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: 'High payment failure rate detected'

      - alert: QRValidationErrors
        expr: rate(coupon_qr_validation_errors_total[5m]) > 0.05
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: 'High QR validation error rate'
```

## 📚 Referencias

- [Stripe API Documentation](https://stripe.com/docs/api)
- [ZXing QR Code Library](https://github.com/zxing/zxing)
- [Spring AMQP Documentation](https://docs.spring.io/spring-amqp/docs/current/reference/html/)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)

## 🤝 Contribución

1. Fork el repositorio
2. Crear feature branch (`git checkout -b feature/coupon-improvement`)
3. Commit cambios (`git commit -m 'Add QR code enhancement'`)
4. Push al branch (`git push origin feature/coupon-improvement`)
5. Crear Pull Request

## 📄 Licencia

Este proyecto es propiedad de Gasolinera JSM. Todos los derechos reservados.

---

**🎫 ¿Necesitas ayuda con cupones?**

- 📧 Email: coupons-team@gasolinera-jsm.com
- 💬 Slack: #coupon-service-support
- 📖 Docs: https://docs.gasolinera-jsm.com/coupons

_Última actualización: Enero 2024_
