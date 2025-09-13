package com.gasolinerajsm.benchmarks

import com.gasolinerajsm.coupon.domain.*
import com.gasolinerajsm.coupon.application.usecases.CreateCouponUseCase
import com.gasolinerajsm.coupon.application.usecases.RedeemCouponUseCase
import com.gasolinerajsm.coupon.application.commands.CreateCouponCommand
import com.gasolinerajsm.coupon.application.commands.RedeemCouponCommand
import com.gasolinerajsm.coupon.infrastructure.repositories.InMemoryCouponRepository
import com.gasolinerajsm.shared.domain.valueobjects.*
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * JMH Benchmarks for Coupon Service Performance
 *
 * These benchmarks measure the performance of critical coupon operations
 * to establish baseline performance and detect regressions.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = ["-Xms2G", "-Xmx2G"])
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
open class CouponServiceBenchmark {

    private lateinit var couponRepository: InMemoryCouponRepository
    private lateinit var createCouponUseCase: CreateCouponUseCase
    private lateinit var redeemCouponUseCase: RedeemCouponUseCase

    // Test data
    private lateinit var testUsers: List<UserId>
    private lateinit var testStations: List<StationId>
    private lateinit var testCoupons: List<Coupon>

    @Setup(Level.Trial)
    fun setupTrial() {
        couponRepository = InMemoryCouponRepository()

        // Mock dependencies for benchmarking
        val mockPaymentService = MockPaymentService()
        val mockDomainEventPublisher = MockDomainEventPublisher()
        val mockCouponDomainService = CouponDomainService()

        createCouponUseCase = CreateCouponUseCase(
            couponRepository = couponRepository,
            paymentService = mockPaymentService,
            domainEventPublisher = mockDomainEventPublisher,
            couponDomainService = mockCouponDomainService
        )

        redeemCouponUseCase = RedeemCouponUseCase(
            couponRepository = couponRepository,
            domainEventPublisher = mockDomainEventPublisher
        )

        // Generate test data
        testUsers = (1..1000).map { UserId.generate() }
        testStations = (1..100).map { StationId.generate() }

        // Pre-create coupons for redemption benchmarks
        testCoupons = (1..1000).map { index ->
            Coupon.create(
                userId = testUsers[index % testUsers.size],
                stationId = testStations[index % testStations.size],
                amount = Money(BigDecimal.valueOf(100.0 + (index % 400))),
                fuelType = FuelType.values()[index % FuelType.values().size],
                validityPeriod = Duration.ofDays(30)
            )
        }

        // Save test coupons to repository
        runBlocking {
            testCoupons.forEach { coupon ->
                couponRepository.save(coupon)
            }
        }
    }

    @Setup(Level.Iteration)
    fun setupIteration() {
        // Clear any iteration-specific state if needed
    }

    /**
     * Benchmark: Coupon Creation Performance
     * Measures the time to create a new coupon including validation and persistence
     */
    @Benchmark
    fun benchmarkCouponCreation(blackhole: Blackhole) {
        val command = CreateCouponCommand(
            userId = testUsers.random(),
            stationId = testStations.random(),
            amount = Money(BigDecimal.valueOf(50.0 + Math.random() * 450)),
            fuelType = FuelType.values().random(),
            paymentMethod = PaymentMethod.CREDIT_CARD
        )

        val result = runBlocking {
            createCouponUseCase.execute(command)
        }

        blackhole.consume(result)
    }

    /**
     * Benchmark: Coupon Creation with Validation
     * Measures coupon creation performance under various validation scenarios
     */
    @Benchmark
    fun benchmarkCouponCreationWithValidation(blackhole: Blackhole) {
        val userId = testUsers.random()

        // Create command that might trigger validation logic
        val command = CreateCouponCommand(
            userId = userId,
            stationId = testStations.random(),
            amount = Money(BigDecimal.valueOf(1000.0)), // Large amount to trigger validation
            fuelType = FuelType.PREMIUM,
            paymentMethod = PaymentMethod.CREDIT_CARD
        )

        val result = runBlocking {
            createCouponUseCase.execute(command)
        }

        blackhole.consume(result)
    }

    /**
     * Benchmark: Coupon Redemption Performance
     * Measures the time to redeem an existing coupon
     */
    @Benchmark
    fun benchmarkCouponRedemption(blackhole: Blackhole) {
        val coupon = testCoupons.random()

        val command = RedeemCouponCommand(
            couponId = coupon.id,
            userId = coupon.userId,
            location = Location(19.4326 + Math.random() * 0.1, -99.1332 + Math.random() * 0.1),
            fuelAmount = FuelAmount(BigDecimal.valueOf(10.0 + Math.random() * 40))
        )

        val result = runBlocking {
            redeemCouponUseCase.execute(command)
        }

        blackhole.consume(result)
    }

    /**
     * Benchmark: Repository Find Operations
     * Measures database query performance for finding coupons
     */
    @Benchmark
    fun benchmarkRepositoryFindById(blackhole: Blackhole) {
        val couponId = testCoupons.random().id

        val result = runBlocking {
            couponRepository.findById(couponId)
        }

        blackhole.consume(result)
    }

    /**
     * Benchmark: Repository Find by User
     * Measures performance of finding coupons by user ID
     */
    @Benchmark
    fun benchmarkRepositoryFindByUserId(blackhole: Blackhole) {
        val userId = testUsers.random()

        val result = runBlocking {
            couponRepository.findActiveByUserId(userId)
        }

        blackhole.consume(result)
    }

    /**
     * Benchmark: QR Code Generation
     * Measures QR code generation performance
     */
    @Benchmark
    fun benchmarkQRCodeGeneration(blackhole: Blackhole) {
        val qrData = QRCodeData(
            couponId = CouponId.generate(),
            userId = testUsers.random(),
            stationId = testStations.random(),
            amount = Money(BigDecimal.valueOf(100.0)),
            timestamp = LocalDateTime.now()
        )

        val qrCode = QRCode.generate(qrData)
        blackhole.consume(qrCode)
    }

    /**
     * Benchmark: Domain Validation
     * Measures business rule validation performance
     */
    @Benchmark
    fun benchmarkDomainValidation(blackhole: Blackhole) {
        val couponDomainService = CouponDomainService()
        val userId = testUsers.random()
        val amount = Money(BigDecimal.valueOf(100.0 + Math.random() * 400))

        // Get existing coupons for validation
        val existingCoupons = runBlocking {
            couponRepository.findActiveByUserId(userId)
        }

        val result = couponDomainService.validateCouponCreation(
            userId = userId,
            amount = amount,
            existingActiveCoupons = existingCoupons
        )

        blackhole.consume(result)
    }

    /**
     * Benchmark: Concurrent Coupon Creation
     * Measures performance under concurrent load
     */
    @Benchmark
    @Group("concurrent")
    @GroupThreads(4)
    fun benchmarkConcurrentCouponCreation(blackhole: Blackhole) {
        benchmarkCouponCreation(blackhole)
    }

    /**
     * Benchmark: Concurrent Coupon Redemption
     * Measures redemption performance under concurrent load
     */
    @Benchmark
    @Group("concurrent")
    @GroupThreads(4)
    fun benchmarkConcurrentCouponRedemption(blackhole: Blackhole) {
        benchmarkCouponRedemption(blackhole)
    }

    /**
     * Benchmark: Memory Allocation
     * Measures memory allocation patterns during coupon operations
     */
    @Benchmark
    fun benchmarkMemoryAllocation(blackhole: Blackhole) {
        val coupons = mutableListOf<Coupon>()

        repeat(100) {
            val coupon = Coupon.create(
                userId = testUsers.random(),
                stationId = testStations.random(),
                amount = Money(BigDecimal.valueOf(50.0 + Math.random() * 450)),
                fuelType = FuelType.values().random(),
                validityPeriod = Duration.ofDays(30)
            )
            coupons.add(coupon)
        }

        blackhole.consume(coupons)
    }

    /**
     * Benchmark: Serialization Performance
     * Measures JSON serialization/deserialization performance
     */
    @Benchmark
    fun benchmarkSerialization(blackhole: Blackhole) {
        val coupon = testCoupons.random()
        val response = coupon.toResponse()

        // Simulate JSON serialization (would use actual JSON library in real scenario)
        val serialized = response.toString()
        blackhole.consume(serialized)
    }
}

/**
 * Mock implementations for benchmarking
 */
class MockPaymentService : PaymentService {
    override suspend fun processPayment(request: PaymentRequest): PaymentResult {
        // Simulate payment processing delay
        kotlinx.coroutines.delay(1) // 1ms simulated delay
        return PaymentResult.Success(
            transactionId = "txn_${UUID.randomUUID()}",
            amount = request.amount
        )
    }

    override suspend fun refundPayment(transactionId: String): RefundResult {
        kotlinx.coroutines.delay(1)
        return RefundResult.Success(transactionId)
    }
}

class MockDomainEventPublisher : DomainEventPublisher {
    override suspend fun publish(event: DomainEvent) {
        // No-op for benchmarking
    }

    override suspend fun publishAll(events: List<DomainEvent>) {
        // No-op for benchmarking
    }
}

/**
 * In-memory repository for benchmarking
 */
class InMemoryCouponRepository : CouponRepository {
    private val coupons = mutableMapOf<CouponId, Coupon>()
    private val userCoupons = mutableMapOf<UserId, MutableList<CouponId>>()

    override suspend fun save(coupon: Coupon): Coupon {
        coupons[coupon.id] = coupon
        userCoupons.computeIfAbsent(coupon.userId) { mutableListOf() }.add(coupon.id)
        return coupon
    }

    override suspend fun findById(id: CouponId): Coupon? {
        return coupons[id]
    }

    override suspend fun findActiveByUserId(userId: UserId): List<Coupon> {
        return userCoupons[userId]?.mapNotNull { coupons[it] }
            ?.filter { it.status == CouponStatus.ACTIVE }
            ?: emptyList()
    }

    override suspend fun findByQrCode(qrCode: QRCode): Coupon? {
        return coupons.values.find { it.qrCode == qrCode }
    }

    override suspend fun delete(id: CouponId) {
        val coupon = coupons.remove(id)
        coupon?.let {
            userCoupons[it.userId]?.remove(id)
        }
    }
}

/**
 * Benchmark for specific performance bottlenecks
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
open class PerformanceBottleneckBenchmark {

    /**
     * Benchmark: String Operations
     * Measures performance of string operations in QR code generation
     */
    @Benchmark
    fun benchmarkStringOperations(blackhole: Blackhole) {
        val data = "coupon_${UUID.randomUUID()}_${System.currentTimeMillis()}"
        val encoded = Base64.getEncoder().encodeToString(data.toByteArray())
        val decoded = String(Base64.getDecoder().decode(encoded))
        blackhole.consume(decoded)
    }

    /**
     * Benchmark: UUID Generation
     * Measures UUID generation performance
     */
    @Benchmark
    fun benchmarkUUIDGeneration(blackhole: Blackhole) {
        val uuid = UUID.randomUUID()
        blackhole.consume(uuid)
    }

    /**
     * Benchmark: BigDecimal Operations
     * Measures BigDecimal arithmetic performance
     */
    @Benchmark
    fun benchmarkBigDecimalOperations(blackhole: Blackhole) {
        val amount1 = BigDecimal.valueOf(100.50)
        val amount2 = BigDecimal.valueOf(25.75)

        val sum = amount1.add(amount2)
        val difference = amount1.subtract(amount2)
        val product = amount1.multiply(amount2)
        val quotient = amount1.divide(amount2, 2, java.math.RoundingMode.HALF_UP)

        blackhole.consume(listOf(sum, difference, product, quotient))
    }

    /**
     * Benchmark: Collection Operations
     * Measures performance of common collection operations
     */
    @Benchmark
    fun benchmarkCollectionOperations(blackhole: Blackhole) {
        val list = (1..1000).map { "item_$it" }

        val filtered = list.filter { it.contains("5") }
        val mapped = filtered.map { it.uppercase() }
        val sorted = mapped.sorted()

        blackhole.consume(sorted)
    }
}