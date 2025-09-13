package com.gasolinerajsm.testing.shared

import org.mockito.kotlin.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatcher
import org.mockito.Mockito
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.*
import kotlin.reflect.KClass

/**
 * Mockito Extensions for Gasolinera JSM Testing
 * Provides Kotlin-friendly mocking utilities and common patterns
 */

// Argument Captors
inline fun <reified T : Any> argumentCaptor(): ArgumentCaptor<T> = ArgumentCaptor.forClass(T::class.java)

inline fun <reified T : Any> capture(captor: ArgumentCaptor<T>): T = captor.capture()

// Custom Matchers
inline fun <reified T : Any> anyObject(): T = any<T>()

inline fun <reified T : Any> anyList(): List<T> = any<List<T>>()

inline fun <reified T : Any> anySet(): Set<T> = any<Set<T>>()

inline fun <reified T : Any> anyMap(): Map<String, T> = any<Map<String, T>>()

fun <T> anyPage(): Page<T> = any<Page<T>>()

fun anyPageable(): Pageable = any<Pageable>()

fun anyUUID(): UUID = any<UUID>()

// Mock Builders
class MockBuilder<T : Any>(private val clazz: KClass<T>) {
    private val mockInstance = mock<T>()
    private val stubbing = mutableListOf<() -> Unit>()

    fun stub(block: T.() -> Unit): MockBuilder<T> {
        stubbing.add { mockInstance.block() }
        return this
    }

    fun build(): T {
        stubbing.forEach { it() }
        return mockInstance
    }
}

inline fun <reified T : Any> mockBuilder(): MockBuilder<T> = MockBuilder(T::class)

// Repository Mocks
object RepositoryMocks {

    inline fun <reified T : Any, reified ID : Any> mockRepository(): org.springframework.data.jpa.repository.JpaRepository<T, ID> {
        return mock<org.springframework.data.jpa.repository.JpaRepository<T, ID>>()
    }

    fun <T> mockPagedResult(content: List<T>, pageable: Pageable = mock()): Page<T> {
        return PageImpl(content, pageable, content.size.toLong())
    }

    fun <T> mockEmptyPage(pageable: Pageable = mock()): Page<T> {
        return PageImpl(emptyList(), pageable, 0)
    }
}

// Service Mocks
object ServiceMocks {

    fun mockUserService(): Any = mock<Any>().apply {
        // Common user service stubbing
        whenever(this.toString()).thenReturn("MockUserService")
    }

    fun mockStationService(): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("MockStationService")
    }

    fun mockCouponService(): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("MockCouponService")
    }

    fun mockRedemptionService(): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("MockRedemptionService")
    }

    fun mockRaffleService(): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("MockRaffleService")
    }

    fun mockAdEngineService(): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("MockAdEngineService")
    }
}

// External Service Mocks
object ExternalServiceMocks {

    fun mockJwtService(): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("MockJwtService")
    }

    fun mockVaultService(): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("MockVaultService")
    }

    fun mockNotificationService(): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("MockNotificationService")
    }

    fun mockPaymentService(): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("MockPaymentService")
    }
}

// Verification Helpers
fun <T> verifyNever(mock: T): T = verify(mock, never())

fun <T> verifyOnce(mock: T): T = verify(mock, times(1))

fun <T> verifyTwice(mock: T): T = verify(mock, times(2))

fun <T> verifyAtLeast(mock: T, times: Int): T = verify(mock, atLeast(times))

fun <T> verifyAtMost(mock: T, times: Int): T = verify(mock, atMost(times))

// Stubbing Helpers
infix fun <T> T.returns(value: T): T = this.also { whenever(this).thenReturn(value) }

infix fun <T> T.throws(exception: Throwable): T = this.also { whenever(this).thenThrow(exception) }

infix fun <T> T.returnsSequentially(values: List<T>): T = this.also {
    whenever(this).thenReturn(values.first(), *values.drop(1).toTypedArray())
}

// Async Mocking
fun <T> mockCompletableFuture(value: T): java.util.concurrent.CompletableFuture<T> {
    return java.util.concurrent.CompletableFuture.completedFuture(value)
}

fun <T> mockFailedCompletableFuture(exception: Throwable): java.util.concurrent.CompletableFuture<T> {
    val future = java.util.concurrent.CompletableFuture<T>()
    future.completeExceptionally(exception)
    return future
}

// Reactor Mocking (for WebFlux)
fun <T> mockMono(value: T): reactor.core.publisher.Mono<T> {
    return reactor.core.publisher.Mono.just(value)
}

fun <T> mockEmptyMono(): reactor.core.publisher.Mono<T> {
    return reactor.core.publisher.Mono.empty()
}

fun <T> mockErrorMono(exception: Throwable): reactor.core.publisher.Mono<T> {
    return reactor.core.publisher.Mono.error(exception)
}

fun <T> mockFlux(vararg values: T): reactor.core.publisher.Flux<T> {
    return reactor.core.publisher.Flux.just(*values)
}

fun <T> mockEmptyFlux(): reactor.core.publisher.Flux<T> {
    return reactor.core.publisher.Flux.empty()
}

// Custom Argument Matchers
class UUIDMatcher(private val expectedUUID: UUID) : ArgumentMatcher<UUID> {
    override fun matches(argument: UUID?): Boolean = argument == expectedUUID
    override fun toString(): String = "UUID($expectedUUID)"
}

fun eqUUID(uuid: UUID): UUID = argThat(UUIDMatcher(uuid))

class EmailMatcher : ArgumentMatcher<String> {
    override fun matches(argument: String?): Boolean {
        return argument?.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) == true
    }
    override fun toString(): String = "validEmail()"
}

fun validEmail(): String = argThat(EmailMatcher())

class PhoneMatcher : ArgumentMatcher<String> {
    override fun matches(argument: String?): Boolean {
        return argument?.matches(Regex("^\\d{10,12}$")) == true
    }
    override fun toString(): String = "validPhone()"
}

fun validPhone(): String = argThat(PhoneMatcher())

// Test Doubles Factory
object TestDoubles {

    fun createStubUser(
        id: UUID = UUID.randomUUID(),
        email: String = "test@example.com",
        isActive: Boolean = true
    ): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("StubUser(id=$id, email=$email, isActive=$isActive)")
    }

    fun createStubStation(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Station",
        isActive: Boolean = true
    ): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("StubStation(id=$id, name=$name, isActive=$isActive)")
    }

    fun createStubCoupon(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        status: String = "ACTIVE"
    ): Any = mock<Any>().apply {
        whenever(this.toString()).thenReturn("StubCoupon(id=$id, userId=$userId, status=$status)")
    }
}

// Mock Reset Utilities
fun resetAllMocks(vararg mocks: Any) {
    mocks.forEach { Mockito.reset(it) }
}

fun clearInvocations(vararg mocks: Any) {
    mocks.forEach { Mockito.clearInvocations(it) }
}

// Verification DSL
class VerificationBuilder<T>(private val mock: T) {
    fun called(times: Int = 1): VerificationBuilder<T> {
        verify(mock, times(times))
        return this
    }

    fun never(): VerificationBuilder<T> {
        verify(mock, never())
        return this
    }

    fun atLeast(times: Int): VerificationBuilder<T> {
        verify(mock, atLeast(times))
        return this
    }

    fun atMost(times: Int): VerificationBuilder<T> {
        verify(mock, atMost(times))
        return this
    }
}

fun <T> T.shouldBe(): VerificationBuilder<T> = VerificationBuilder(this)