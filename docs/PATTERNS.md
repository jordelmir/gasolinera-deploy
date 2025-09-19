# 🎨 Patrones de Diseño - Gasolinera JSM Platform

## 📋 Tabla de Contenidos

- [Patrones Arquitectónicos](#patrones-arquitectónicos)
- [Patrones de Dominio](#patrones-de-dominio)
- [Patrones de Aplicación](#patrones-de-aplicación)
- [Patrones de Infraestructura](#patrones-de-infraestructura)
- [Patrones de Integración](#patrones-de-integración)
- [Patrones de Observabilidad](#patrones-de-observabilidad)

## 🏗️ Patrones Arquitectónicos

### 1. Hexagonal Architecture (Ports & Adapters)

**Propósito**: Aislar la lógica de negocio de los detalles técnicos externos.

```kotlin
// Domain Layer - Puerto (Interface)
interface CouponRepository {
    suspend fun save(coupon: Coupon): Result<Coupon>
    suspend fun findById(id: CouponId): Result<Coupon?>
    suspend fun findByUserId(userId: UserId): Result<List<Coupon>>
}

// Infrastructure Layer - Adaptador (Implementation)
@Repository
class JpaCouponRepository(
    private val jpaRepository: CouponJpaRepository,
    private val cacheManager: CacheManager
) : CouponRepository {

    @Cacheable("coupons")
    override suspend fun findById(id: CouponId): Result<Coupon?> {
        return try {
            val entity = jpaRepository.findById(id.value).orElse(null)
            Result.success(entity?.toDomainEntity())
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find coupon", e))
        }
    }
}
```

**Beneficios**:

- ✅ Testabilidad máxima
- ✅ Independencia de frameworks
- ✅ Flexibilidad para cambios tecnológicos
- ✅ Separación clara de responsabilidades

### 2. Domain-Driven Design (DDD)

**Propósito**: Modelar el software basado en el dominio del negocio.

```kotlin
// Aggregate Root
class Coupon private constructor(
    val id: CouponId,
    val campaignId: CampaignId,
    private var status: CouponStatus,
    val discountAmount: Money,
    val validityPeriod: DateRange,
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()
) {
    companion object {
        fun create(
            campaignId: CampaignId,
            discountAmount: Money,
            validityPeriod: DateRange
        ): Coupon {
            val coupon = Coupon(
                id = CouponId.generate(),
                campaignId = campaignId,
                status = CouponStatus.ACTIVE,
                discountAmount = discountAmount,
                validityPeriod = validityPeriod
            )

            coupon.addDomainEvent(
                CouponCreatedEvent(
                    couponId = coupon.id,
                    campaignId = campaignId,
                    occurredOn = Clock.System.now()
                )
            )

            return coupon
        }
    }

    fun redeem(userId: UserId, stationId: StationId): Result<RedemptionResult> {
        return when {
            !isActive() -> Result.failure(CouponNotActiveException())
            !isValid() -> Result.failure(CouponExpiredException())
            else -> {
                status = CouponStatus.REDEEMED
                addDomainEvent(
                    CouponRedeemedEvent(
                        couponId = id,
                        userId = userId,
                        stationId = stationId,
                        discountApplied = discountAmount,
                        occurredOn = Clock.System.now()
                    )
                )
                Result.success(RedemptionResult(discountAmount))
            }
        }
    }

    private fun isActive(): Boolean = status == CouponStatus.ACTIVE
    private fun isValid(): Boolean = validityPeriod.contains(Clock.System.now())

    private fun addDomainEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun getUncommittedEvents(): List<DomainEvent> = domainEvents.toList()
    fun markEventsAsCommitted() = domainEvents.clear()
}

// Value Objects
@JvmInline
value class CouponId(val value: UUID) {
    companion object {
        fun generate() = CouponId(UUID.randomUUID())
        fun from(value: String) = CouponId(UUID.fromString(value))
    }
}

data class Money(val amount: BigDecimal, val currency: Currency = Currency.USD) {
    init {
        require(amount >= BigDecimal.ZERO) { "Amount cannot be negative" }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies" }
        return Money(amount + other.amount, currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract different currencies" }
        return Money(amount - other.amount, currency)
    }

    companion object {
        val ZERO = Money(BigDecimal.ZERO)
    }
}
```

## 🎯 Patrones de Dominio

### 3. Repository Pattern

**Propósito**: Encapsular la lógica de acceso a datos y proporcionar una interfaz más orientada a objetos.

```kotlin
// Domain Layer - Interface
interface CouponRepository {
    suspend fun save(coupon: Coupon): Result<Coupon>
    suspend fun findById(id: CouponId): Result<Coupon?>
    suspend fun findActiveCouponsByUser(userId: UserId): Result<List<Coupon>>
    suspend fun findExpiredCoupons(): Result<List<Coupon>>
    suspend fun countByStatus(status: CouponStatus): Result<Long>
}

// Infrastructure Layer - Implementation with Caching
@Repository
class OptimizedCouponRepository(
    private val jpaRepository: CouponJpaRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry
) : CouponRepository {

    private val cacheHitCounter = Counter.builder("cache.hits")
        .tag("repository", "coupon")
        .register(meterRegistry)

    private val cacheMissCounter = Counter.builder("cache.misses")
        .tag("repository", "coupon")
        .register(meterRegistry)

    @Transactional
    override suspend fun save(coupon: Coupon): Result<Coupon> {
        return try {
            val entity = coupon.toJpaEntity()
            val saved = jpaRepository.save(entity)

            // Update cache
            val cacheKey = "coupon:${coupon.id.value}"
            val json = objectMapper.writeValueAsString(saved)
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofHours(1))

            Result.success(saved.toDomainEntity())
        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to save coupon", e))
        }
    }

    override suspend fun findById(id: CouponId): Result<Coupon?> {
        return try {
            val cacheKey = "coupon:${id.value}"
            val cached = redisTemplate.opsForValue().get(cacheKey)

            if (cached != null) {
                cacheHitCounter.increment()
                val entity = objectMapper.readValue(cached, CouponEntity::class.java)
                return Result.success(entity.toDomainEntity())
            }

            cacheMissCounter.increment()
            val entity = jpaRepository.findById(id.value).orElse(null)

            entity?.let {
                // Cache the result
                val json = objectMapper.writeValueAsString(it)
                redisTemplate.opsForValue().set(cacheKey, json, Duration.ofHours(1))
                Result.success(it.toDomainEntity())
            } ?: Result.success(null)

        } catch (e: Exception) {
            Result.failure(RepositoryException("Failed to find coupon", e))
        }
    }
}
```

### 4. Factory Pattern

**Propósito**: Encapsular la creación compleja de objetos de dominio.

```kotlin
// Domain Layer
@Component
class CouponFactory(
    private val qrCodeGenerator: QRCodeGenerator,
    private val securityService: SecurityService
) {

    fun createDiscountCoupon(
        campaignId: CampaignId,
        discountAmount: Money,
        validityPeriod: DateRange,
        maxUses: Int = 1
    ): Coupon {
        val couponId = CouponId.generate()
        val qrCode = qrCodeGenerator.generate(couponId.value.toString())
        val signature = securityService.signQRCode(qrCode)

        return Coupon.create(
            id = couponId,
            campaignId = campaignId,
            type = CouponType.FIXED_DISCOUNT,
            discountAmount = discountAmount,
            validityPeriod = validityPeriod,
            maxUses = maxUses,
            qrCode = qrCode,
            qrSignature = signature
        )
    }

    fun createPercentageCoupon(
        campaignId: CampaignId,
        discountPercentage: Percentage,
        validityPeriod: DateRange,
        maxAmount: Money? = null
    ): Coupon {
        val couponId = CouponId.generate()
        val qrCode = qrCodeGenerator.generate(couponId.value.toString())
        val signature = securityService.signQRCode(qrCode)

        return Coupon.create(
            id = couponId,
            campaignId = campaignId,
            type = CouponType.PERCENTAGE_DISCOUNT,
            discountPercentage = discountPercentage,
            maxDiscountAmount = maxAmount,
            validityPeriod = validityPeriod,
            qrCode = qrCode,
            qrSignature = signature
        )
    }

    fun createBulkCoupons(
        campaignId: CampaignId,
        template: CouponTemplate,
        quantity: Int
    ): List<Coupon> {
        require(quantity > 0) { "Quantity must be positive" }
        require(quantity <= 10000) { "Cannot create more than 10000 coupons at once" }

        return (1..quantity).map { index ->
            when (template.type) {
                CouponType.FIXED_DISCOUNT -> createDiscountCoupon(
                    campaignId = campaignId,
                    discountAmount = template.discountAmount!!,
                    validityPeriod = template.validityPeriod
                )
                CouponType.PERCENTAGE_DISCOUNT -> createPercentageCoupon(
                    campaignId = campaignId,
                    discountPercentage = template.discountPercentage!!,
                    validityPeriod = template.validityPeriod,
                    maxAmount = template.maxDiscountAmount
                )
            }
        }
    }
}

data class CouponTemplate(
    val type: CouponType,
    val discountAmount: Money? = null,
    val discountPercentage: Percentage? = null,
    val maxDiscountAmount: Money? = null,
    val validityPeriod: DateRange
) {
    init {
        when (type) {
            CouponType.FIXED_DISCOUNT -> {
                require(discountAmount != null) { "Discount amount is required for fixed discount coupons" }
            }
            CouponType.PERCENTAGE_DISCOUNT -> {
                require(discountPercentage != null) { "Discount percentage is required for percentage discount coupons" }
            }
        }
    }
}
```

### 5. Strategy Pattern

**Propósito**: Definir una familia de algoritmos, encapsular cada uno y hacerlos intercambiables.

```kotlin
// Domain Layer - Strategy Interface
interface DiscountCalculationStrategy {
    fun calculateDiscount(originalAmount: Money, coupon: Coupon): Money
    fun isApplicable(coupon: Coupon): Boolean
}

// Concrete Strategies
class FixedAmountDiscountStrategy : DiscountCalculationStrategy {
    override fun calculateDiscount(originalAmount: Money, coupon: Coupon): Money {
        return coupon.discountAmount ?: Money.ZERO
    }

    override fun isApplicable(coupon: Coupon): Boolean {
        return coupon.type == CouponType.FIXED_DISCOUNT && coupon.discountAmount != null
    }
}

class PercentageDiscountStrategy : DiscountCalculationStrategy {
    override fun calculateDiscount(originalAmount: Money, coupon: Coupon): Money {
        val percentage = coupon.discountPercentage ?: return Money.ZERO
        val calculatedDiscount = originalAmount * (percentage.value / 100.0)

        // Apply maximum discount limit if specified
        return coupon.maxDiscountAmount?.let { maxAmount ->
            if (calculatedDiscount > maxAmount) maxAmount else calculatedDiscount
        } ?: calculatedDiscount
    }

    override fun isApplicable(coupon: Coupon): Boolean {
        return coupon.type == CouponType.PERCENTAGE_DISCOUNT && coupon.discountPercentage != null
    }
}

class TieredDiscountStrategy : DiscountCalculationStrategy {
    override fun calculateDiscount(originalAmount: Money, coupon: Coupon): Money {
        val tiers = coupon.discountTiers ?: return Money.ZERO

        return tiers
            .filter { originalAmount >= it.minimumAmount }
            .maxByOrNull { it.minimumAmount }
            ?.let { tier ->
                when (tier.type) {
                    TierType.FIXED -> tier.discountAmount
                    TierType.PERCENTAGE -> originalAmount * (tier.discountPercentage / 100.0)
                }
            } ?: Money.ZERO
    }

    override fun isApplicable(coupon: Coupon): Boolean {
        return coupon.type == CouponType.TIERED_DISCOUNT && !coupon.discountTiers.isNullOrEmpty()
    }
}

// Context - Domain Service
@Component
class DiscountCalculatorService {
    private val strategies = listOf(
        FixedAmountDiscountStrategy(),
        PercentageDiscountStrategy(),
        TieredDiscountStrategy()
    )

    fun calculateDiscount(originalAmount: Money, coupon: Coupon): Result<Money> {
        return try {
            val strategy = strategies.find { it.isApplicable(coupon) }
                ?: return Result.failure(UnsupportedCouponTypeException("No strategy found for coupon type: ${coupon.type}"))

            val discount = strategy.calculateDiscount(originalAmount, coupon)

            // Ensure discount doesn't exceed original amount
            val finalDiscount = if (discount > originalAmount) originalAmount else discount

            Result.success(finalDiscount)
        } catch (e: Exception) {
            Result.failure(DiscountCalculationException("Failed to calculate discount", e))
        }
    }
}
```

## 🔄 Patrones de Aplicación

### 6. Command Query Responsibility Segregation (CQRS)

**Propósito**: Separar las operaciones de lectura y escritura para optimizar cada una independientemente.

```kotlin
// Commands (Write Side)
data class CreateCouponCommand(
    val campaignId: String,
    val type: String,
    val discountAmount: BigDecimal?,
    val discountPercentage: BigDecimal?,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val maxUses: Int = 1
)

data class RedeemCouponCommand(
    val couponCode: String,
    val userId: String,
    val stationId: String,
    val purchaseAmount: BigDecimal
)

// Queries (Read Side)
data class CouponQuery(
    val id: String? = null,
    val campaignId: String? = null,
    val userId: String? = null,
    val status: String? = null,
    val validFrom: LocalDateTime? = null,
    val validUntil: LocalDateTime? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class CouponAnalyticsQuery(
    val campaignId: String? = null,
    val stationId: String? = null,
    val dateFrom: LocalDate,
    val dateTo: LocalDate,
    val groupBy: AnalyticsGroupBy = AnalyticsGroupBy.DAY
)

// Command Handlers
@Component
class CreateCouponCommandHandler(
    private val repository: CouponRepository,
    private val factory: CouponFactory,
    private val eventPublisher: DomainEventPublisher,
    private val validator: CouponValidator
) {

    @Transactional
    suspend fun handle(command: CreateCouponCommand): Result<CouponResponse> {
        return try {
            // Validate command
            validator.validateCreateCommand(command)
                .onFailure { return Result.failure(it) }

            // Create coupon using factory
            val coupon = factory.createFromCommand(command)

            // Save to repository
            repository.save(coupon)
                .onSuccess { savedCoupon ->
                    // Publish domain events
                    eventPublisher.publishAll(savedCoupon.getUncommittedEvents())
                    savedCoupon.markEventsAsCommitted()
                }
                .map { it.toResponse() }

        } catch (e: Exception) {
            Result.failure(CommandHandlingException("Failed to create coupon", e))
        }
    }
}

// Query Handlers
@Component
class CouponQueryHandler(
    private val readRepository: CouponReadRepository,
    private val cacheManager: CacheManager
) {

    @Cacheable("coupon-queries")
    suspend fun handle(query: CouponQuery): Result<PagedResult<CouponResponse>> {
        return try {
            val specification = CouponSpecification.fromQuery(query)
            val page = readRepository.findAll(specification, query.toPageable())

            Result.success(
                PagedResult(
                    content = page.content.map { it.toResponse() },
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                    currentPage = query.page
                )
            )
        } catch (e: Exception) {
            Result.failure(QueryHandlingException("Failed to execute coupon query", e))
        }
    }

    @Cacheable("coupon-analytics", key = "#query.hashCode()")
    suspend fun handle(query: CouponAnalyticsQuery): Result<CouponAnalyticsResponse> {
        return try {
            val analytics = readRepository.getAnalytics(query)
            Result.success(analytics.toResponse())
        } catch (e: Exception) {
            Result.failure(QueryHandlingException("Failed to execute analytics query", e))
        }
    }
}
```

### 7. Mediator Pattern

**Propósito**: Definir cómo un conjunto de objetos interactúan entre sí, promoviendo el bajo acoplamiento.

```kotlin
// Mediator Interface
interface Mediator {
    suspend fun <T> send(request: Request<T>): Result<T>
    suspend fun publish(notification: Notification)
}

// Base Request and Notification
interface Request<T>
interface Notification

// Concrete Requests
data class CreateCouponRequest(
    val campaignId: String,
    val type: String,
    val discountAmount: BigDecimal?,
    val discountPercentage: BigDecimal?
) : Request<CouponResponse>

data class RedeemCouponRequest(
    val couponCode: String,
    val userId: String,
    val stationId: String
) : Request<RedemptionResponse>

// Concrete Notifications
data class CouponCreatedNotification(
    val couponId: String,
    val campaignId: String
) : Notification

data class CouponRedeemedNotification(
    val couponId: String,
    val userId: String,
    val discountApplied: BigDecimal
) : Notification

// Request Handlers
interface RequestHandler<TRequest : Request<TResponse>, TResponse> {
    suspend fun handle(request: TRequest): Result<TResponse>
}

@Component
class CreateCouponRequestHandler(
    private val commandHandler: CreateCouponCommandHandler
) : RequestHandler<CreateCouponRequest, CouponResponse> {

    override suspend fun handle(request: CreateCouponRequest): Result<CouponResponse> {
        val command = CreateCouponCommand(
            campaignId = request.campaignId,
            type = request.type,
            discountAmount = request.discountAmount,
            discountPercentage = request.discountPercentage,
            validFrom = LocalDateTime.now(),
            validUntil = LocalDateTime.now().plusDays(30)
        )

        return commandHandler.handle(command)
    }
}

// Notification Handlers
interface NotificationHandler<TNotification : Notification> {
    suspend fun handle(notification: TNotification)
}

@Component
class CouponCreatedNotificationHandler(
    private val analyticsService: AnalyticsService,
    private val emailService: EmailService
) : NotificationHandler<CouponCreatedNotification> {

    override suspend fun handle(notification: CouponCreatedNotification) {
        // Update analytics
        analyticsService.recordCouponCreation(notification.couponId, notification.campaignId)

        // Send notification email (if configured)
        emailService.sendCouponCreatedNotification(notification.couponId)
    }
}

// Mediator Implementation
@Component
class DefaultMediator(
    private val requestHandlers: Map<Class<*>, RequestHandler<*, *>>,
    private val notificationHandlers: Map<Class<*>, List<NotificationHandler<*>>>
) : Mediator {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> send(request: Request<T>): Result<T> {
        return try {
            val handler = requestHandlers[request::class.java] as? RequestHandler<Request<T>, T>
                ?: return Result.failure(HandlerNotFoundException("No handler found for ${request::class.simpleName}"))

            handler.handle(request)
        } catch (e: Exception) {
            Result.failure(MediatorException("Failed to handle request", e))
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun publish(notification: Notification) {
        try {
            val handlers = notificationHandlers[notification::class.java] as? List<NotificationHandler<Notification>>
                ?: emptyList()

            handlers.forEach { handler ->
                try {
                    handler.handle(notification)
                } catch (e: Exception) {
                    // Log error but don't fail the entire operation
                    logger.error("Failed to handle notification ${notification::class.simpleName}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to publish notification", e)
        }
    }
}
```

## 🔌 Patrones de Infraestructura

### 8. Adapter Pattern

**Propósito**: Permitir que interfaces incompatibles trabajen juntas.

```kotlin
// External Service Interface (Third Party)
interface ExternalPaymentGateway {
    fun processPayment(amount: Double, currency: String, cardToken: String): PaymentResult
    fun refundPayment(transactionId: String, amount: Double): RefundResult
}

// Domain Interface (Our System)
interface PaymentService {
    suspend fun processPayment(payment: Payment): Result<PaymentResult>
    suspend fun refundPayment(refund: Refund): Result<RefundResult>
}

// Adapter Implementation
@Component
class PaymentGatewayAdapter(
    private val externalGateway: ExternalPaymentGateway,
    private val currencyConverter: CurrencyConverter,
    private val meterRegistry: MeterRegistry
) : PaymentService {

    private val paymentCounter = Counter.builder("payments.processed")
        .register(meterRegistry)

    private val paymentTimer = Timer.builder("payments.duration")
        .register(meterRegistry)

    override suspend fun processPayment(payment: Payment): Result<PaymentResult> {
        return Timer.Sample.start(meterRegistry).use { sample ->
            try {
                // Convert domain object to external format
                val amount = currencyConverter.convertToDouble(payment.amount)
                val currency = payment.amount.currency.code
                val cardToken = payment.paymentMethod.token

                // Call external service
                val externalResult = externalGateway.processPayment(amount, currency, cardToken)

                // Convert external result to domain object
                val domainResult = PaymentResult(
                    transactionId = TransactionId(externalResult.transactionId),
                    status = PaymentStatus.fromExternal(externalResult.status),
                    processedAmount = Money(
                        amount = externalResult.processedAmount.toBigDecimal(),
                        currency = Currency.fromCode(externalResult.currency)
                    ),
                    processedAt = externalResult.timestamp.toInstant()
                )

                paymentCounter.increment(Tags.of("status", domainResult.status.name))
                sample.stop(paymentTimer)

                Result.success(domainResult)

            } catch (e: Exception) {
                paymentCounter.increment(Tags.of("status", "ERROR"))
                Result.failure(PaymentProcessingException("Failed to process payment", e))
            }
        }
    }
}

// External Service Client with Circuit Breaker
@Component
class ResilientExternalPaymentGateway(
    private val httpClient: WebClient,
    private val circuitBreakerRegistry: CircuitBreakerRegistry
) : ExternalPaymentGateway {

    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("payment-gateway")

    override fun processPayment(amount: Double, currency: String, cardToken: String): PaymentResult {
        return circuitBreaker.executeSupplier {
            val request = ExternalPaymentRequest(
                amount = amount,
                currency = currency,
                cardToken = cardToken
            )

            httpClient.post()
                .uri("/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ExternalPaymentResponse::class.java)
                .timeout(Duration.ofSeconds(30))
                .block()
                ?.toPaymentResult()
                ?: throw PaymentGatewayException("Empty response from payment gateway")
        }
    }
}
```

### 9. Circuit Breaker Pattern

**Propósito**: Prevenir cascadas de fallos en sistemas distribuidos.

```kotlin
// Circuit Breaker Configuration
@Configuration
class CircuitBreakerConfig {

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry {
        return CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .build()
        )
    }
}

// Service with Circuit Breaker
@Component
class ExternalServiceClient(
    private val webClient: WebClient,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val meterRegistry: MeterRegistry
) {

    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("external-service")
    private val fallbackCounter = Counter.builder("circuit.breaker.fallback")
        .tag("service", "external-service")
        .register(meterRegistry)

    init {
        // Register circuit breaker metrics
        circuitBreaker.eventPublisher.onStateTransition { event ->
            meterRegistry.gauge("circuit.breaker.state",
                Tags.of("service", "external-service"),
                event.stateTransition.toState.ordinal.toDouble()
            )
        }
    }

    suspend fun callExternalService(request: ExternalRequest): Result<ExternalResponse> {
        return try {
            val response = circuitBreaker.executeSupplier {
                webClient.post()
                    .uri("/external-endpoint")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ExternalResponse::class.java)
                    .timeout(Duration.ofSeconds(5))
                    .block()
            }

            Result.success(response ?: throw ExternalServiceException("Empty response"))

        } catch (e: CallNotPermittedException) {
            // Circuit breaker is open
            fallbackCounter.increment()
            handleFallback(request)
        } catch (e: Exception) {
            Result.failure(ExternalServiceException("External service call failed", e))
        }
    }

    private fun handleFallback(request: ExternalRequest): Result<ExternalResponse> {
        // Implement fallback logic
        return when (request.type) {
            RequestType.CRITICAL -> {
                // For critical requests, try alternative service or return cached data
                getCachedResponse(request) ?: Result.failure(
                    ExternalServiceException("Service unavailable and no cached data")
                )
            }
            RequestType.NON_CRITICAL -> {
                // For non-critical requests, return default response
                Result.success(ExternalResponse.default())
            }
        }
    }

    private fun getCachedResponse(request: ExternalRequest): Result<ExternalResponse>? {
        // Implementation to get cached response
        return null
    }
}
```

## 🔄 Patrones de Integración

### 10. Event Sourcing Pattern

**Propósito**: Persistir el estado como una secuencia de eventos en lugar del estado actual.

```kotlin
// Event Store Interface
interface EventStore {
    suspend fun saveEvents(aggregateId: String, events: List<DomainEvent>, expectedVersion: Long): Result<Unit>
    suspend fun getEvents(aggregateId: String, fromVersion: Long = 0): Result<List<DomainEvent>>
    suspend fun getAllEvents(fromPosition: Long = 0): Flow<DomainEvent>
}

// Event Store Implementation
@Repository
class PostgreSQLEventStore(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) : EventStore {

    @Transactional
    override suspend fun saveEvents(
        aggregateId: String,
        events: List<DomainEvent>,
        expectedVersion: Long
    ): Result<Unit> {
        return try {
            // Check current version
            val currentVersion = getCurrentVersion(aggregateId)
            if (currentVersion != expectedVersion) {
                return Result.failure(ConcurrencyException("Expected version $expectedVersion but was $currentVersion"))
            }

            // Save events
            events.forEachIndexed { index, event ->
                val sql = """
                    INSERT INTO event_store (aggregate_id, event_type, event_data, version, occurred_on)
                    VALUES (?, ?, ?::jsonb, ?, ?)
                """.trimIndent()

                jdbcTemplate.update(
                    sql,
                    aggregateId,
                    event::class.simpleName,
                    objectMapper.writeValueAsString(event),
                    expectedVersion + index + 1,
                    event.occurredOn
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(EventStoreException("Failed to save events", e))
        }
    }

    override suspend fun getEvents(aggregateId: String, fromVersion: Long): Result<List<DomainEvent>> {
        return try {
            val sql = """
                SELECT event_type, event_data, version, occurred_on
                FROM event_store
                WHERE aggregate_id = ? AND version > ?
                ORDER BY version
            """.trimIndent()

            val events = jdbcTemplate.query(sql, { rs, _ ->
                val eventType = rs.getString("event_type")
                val eventData = rs.getString("event_data")
                val eventClass = Class.forName("com.gasolinerajsm.events.$eventType")

                objectMapper.readValue(eventData, eventClass) as DomainEvent
            }, aggregateId, fromVersion)

            Result.success(events)
        } catch (e: Exception) {
            Result.failure(EventStoreException("Failed to get events", e))
        }
    }

    private fun getCurrentVersion(aggregateId: String): Long {
        val sql = "SELECT COALESCE(MAX(version), 0) FROM event_store WHERE aggregate_id = ?"
        return jdbcTemplate.queryForObject(sql, Long::class.java, aggregateId) ?: 0L
    }
}

// Aggregate with Event Sourcing
abstract class EventSourcedAggregate {
    protected var version: Long = 0
    private val uncommittedEvents = mutableListOf<DomainEvent>()

    protected fun applyEvent(event: DomainEvent) {
        applyChange(event, true)
    }

    fun replayEvents(events: List<DomainEvent>) {
        events.forEach { event ->
            applyChange(event, false)
        }
    }

    private fun applyChange(event: DomainEvent, isNew: Boolean) {
        // Apply the event to change state
        handle(event)

        if (isNew) {
            uncommittedEvents.add(event)
        }

        version++
    }

    protected abstract fun handle(event: DomainEvent)

    fun getUncommittedEvents(): List<DomainEvent> = uncommittedEvents.toList()

    fun markEventsAsCommitted() {
        uncommittedEvents.clear()
    }
}

// Coupon Aggregate with Event Sourcing
class CouponAggregate : EventSourcedAggregate() {
    private lateinit var id: CouponId
    private lateinit var campaignId: CampaignId
    private var status: CouponStatus = CouponStatus.DRAFT
    private var discountAmount: Money = Money.ZERO
    private var redemptions: MutableList<Redemption> = mutableListOf()

    companion object {
        fun create(
            campaignId: CampaignId,
            discountAmount: Money,
            validityPeriod: DateRange
        ): CouponAggregate {
            val aggregate = CouponAggregate()
            val event = CouponCreatedEvent(
                couponId = CouponId.generate(),
                campaignId = campaignId,
                discountAmount = discountAmount,
                validityPeriod = validityPeriod,
                occurredOn = Clock.System.now()
            )
            aggregate.applyEvent(event)
            return aggregate
        }

        fun fromEvents(events: List<DomainEvent>): CouponAggregate {
            val aggregate = CouponAggregate()
            aggregate.replayEvents(events)
            return aggregate
        }
    }

    fun activate() {
        if (status != CouponStatus.DRAFT) {
            throw IllegalStateException("Can only activate draft coupons")
        }

        applyEvent(
            CouponActivatedEvent(
                couponId = id,
                occurredOn = Clock.System.now()
            )
        )
    }

    fun redeem(userId: UserId, stationId: StationId, purchaseAmount: Money): Result<Unit> {
        return when {
            status != CouponStatus.ACTIVE -> Result.failure(CouponNotActiveException())
            else -> {
                applyEvent(
                    CouponRedeemedEvent(
                        couponId = id,
                        userId = userId,
                        stationId = stationId,
                        purchaseAmount = purchaseAmount,
                        discountApplied = discountAmount,
                        occurredOn = Clock.System.now()
                    )
                )
                Result.success(Unit)
            }
        }
    }

    override fun handle(event: DomainEvent) {
        when (event) {
            is CouponCreatedEvent -> {
                id = event.couponId
                campaignId = event.campaignId
                discountAmount = event.discountAmount
                status = CouponStatus.DRAFT
            }
            is CouponActivatedEvent -> {
                status = CouponStatus.ACTIVE
            }
            is CouponRedeemedEvent -> {
                status = CouponStatus.REDEEMED
                redemptions.add(
                    Redemption(
                        userId = event.userId,
                        stationId = event.stationId,
                        purchaseAmount = event.purchaseAmount,
                        discountApplied = event.discountApplied,
                        redeemedAt = event.occurredOn
                    )
                )
            }
        }
    }
}
```

## 📊 Patrones de Observabilidad

### 11. Correlation ID Pattern

**Propósito**: Rastrear requests a través de múltiples servicios en sistemas distribuidos.

```kotlin
// Correlation ID Context
object CorrelationContext {
    private val correlationIdThreadLocal = ThreadLocal<String>()

    fun setCorrelationId(correlationId: String) {
        correlationIdThreadLocal.set(correlationId)
        MDC.put("correlationId", correlationId)
    }

    fun getCorrelationId(): String? = correlationIdThreadLocal.get()

    fun clear() {
        correlationIdThreadLocal.remove()
        MDC.remove("correlationId")
    }

    fun generateNew(): String = UUID.randomUUID().toString()
}

// Correlation ID Filter
@Component
class CorrelationIdFilter : Filter {

    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER)
            ?: CorrelationContext.generateNew()

        CorrelationContext.setCorrelationId(correlationId)
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId)

        try {
            chain.doFilter(request, response)
        } finally {
            CorrelationContext.clear()
        }
    }
}

// HTTP Client with Correlation ID
@Component
class CorrelatedWebClient(
    private val webClientBuilder: WebClient.Builder
) {

    private val webClient = webClientBuilder
        .filter { request, next ->
            val correlationId = CorrelationContext.getCorrelationId()
            if (correlationId != null) {
                val newRequest = ClientRequest.from(request)
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .build()
                next.exchange(newRequest)
            } else {
                next.exchange(request)
            }
        }
        .build()

    fun get(): WebClient.RequestHeadersUriSpec<*> = webClient.get()
    fun post(): WebClient.RequestBodyUriSpec = webClient.post()
    fun put(): WebClient.RequestBodyUriSpec = webClient.put()
    fun delete(): WebClient.RequestHeadersUriSpec<*> = webClient.delete()
}

// Message Publisher with Correlation ID
@Component
class CorrelatedMessagePublisher(
    private val rabbitTemplate: RabbitTemplate
) {

    fun publish(exchange: String, routingKey: String, message: Any) {
        val correlationId = CorrelationContext.getCorrelationId()

        rabbitTemplate.convertAndSend(exchange, routingKey, message) { messageProperties ->
            correlationId?.let {
                messageProperties.headers["correlationId"] = it
            }
            messageProperties
        }
    }
}

// Message Listener with Correlation ID
@RabbitListener(queues = ["coupon.events"])
class CouponEventListener {

    @RabbitHandler
    fun handleCouponCreated(
        @Payload event: CouponCreatedEvent,
        @Header headers: Map<String, Any>
    ) {
        val correlationId = headers["correlationId"] as? String
        correlationId?.let { CorrelationContext.setCorrelationId(it) }

        try {
            // Process event
            logger.info("Processing coupon created event: ${event.couponId}")
            // ... business logic
        } finally {
            CorrelationContext.clear()
        }
    }
}
```

### 12. Health Check Pattern

**Propósito**: Proporcionar endpoints para verificar el estado de salud de los servicios.

```kotlin
// Custom Health Indicators
@Component
class DatabaseHealthIndicator(
    private val dataSource: DataSource
) : HealthIndicator {

    override fun health(): Health {
        return try {
            dataSource.connection.use { connection ->
                val isValid = connection.isValid(5) // 5 second timeout
                if (isValid) {
                    Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Connected")
                        .build()
                } else {
                    Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Connection invalid")
                        .build()
                }
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.message)
                .build()
        }
    }
}

@Component
class RedisHealthIndicator(
    private val redisTemplate: RedisTemplate<String, String>
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val pong = redisTemplate.connectionFactory?.connection?.ping()
            if (pong == "PONG") {
                Health.up()
                    .withDetail("cache", "Redis")
                    .withDetail("status", "Connected")
                    .build()
            } else {
                Health.down()
                    .withDetail("cache", "Redis")
                    .withDetail("status", "Ping failed")
                    .build()
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("cache", "Redis")
                .withDetail("error", e.message)
                .build()
        }
    }
}

@Component
class ExternalServiceHealthIndicator(
    private val externalServiceClient: ExternalServiceClient
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val response = externalServiceClient.healthCheck()
            if (response.isSuccessful) {
                Health.up()
                    .withDetail("externalService", "PaymentGateway")
                    .withDetail("status", "Available")
                    .withDetail("responseTime", "${response.responseTime}ms")
                    .build()
            } else {
                Health.down()
                    .withDetail("externalService", "PaymentGateway")
                    .withDetail("status", "Unavailable")
                    .withDetail("httpStatus", response.httpStatus)
                    .build()
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("externalService", "PaymentGateway")
                .withDetail("error", e.message)
                .build()
        }
    }
}

// Composite Health Indicator
@Component
class BusinessHealthIndicator(
    private val couponRepository: CouponRepository,
    private val campaignRepository: CampaignRepository
) : HealthIndicator {

    override fun health(): Health {
        return try {
            // Check if we can perform basic business operations
            val activeCouponsCount = couponRepository.countByStatus(CouponStatus.ACTIVE)
            val activeCampaignsCount = campaignRepository.countActive()

            val healthBuilder = Health.up()
                .withDetail("activeCoupons", activeCouponsCount)
                .withDetail("activeCampaigns", activeCampaignsCount)

            // Add warnings if numbers are concerning
            if (activeCouponsCount > 100000) {
                healthBuilder.withDetail("warning", "High number of active coupons")
            }

            healthBuilder.build()

        } catch (e: Exception) {
            Health.down()
                .withDetail("business", "CouponService")
                .withDetail("error", e.message)
                .build()
        }
    }
}
```

Estos patrones forman la base arquitectónica sólida de la plataforma Gasolinera JSM, garantizando escalabilidad, mantenibilidad, testabilidad y observabilidad de nivel mundial.
