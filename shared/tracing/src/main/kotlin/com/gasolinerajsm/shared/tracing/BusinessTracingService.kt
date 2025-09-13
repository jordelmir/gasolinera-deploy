package com.gasolinerajsm.shared.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.Callable

/**
 * Business Tracing Service
 * Provides high-level tracing utilities for business operations
 */
@Service
class BusinessTracingService(
    private val openTelemetry: OpenTelemetry,
    private val tracingProperties: TracingProperties
) {

    companion object {
        private val logger = LoggerFactory.getLogger(BusinessTracingService::class.java)

        // Business operation attribute keys
        private val OPERATION_TYPE_KEY = AttributeKey.stringKey("business.operation.type")
        private val OPERATION_ID_KEY = AttributeKey.stringKey("business.operation.id")
        private val ENTITY_TYPE_KEY = AttributeKey.stringKey("business.entity.type")
        private val ENTITY_ID_KEY = AttributeKey.stringKey("business.entity.id")
        private val USER_ID_KEY = AttributeKey.stringKey("business.user.id")
        private val STATION_ID_KEY = AttributeKey.stringKey("business.station.id")
        private val CAMPAIGN_ID_KEY = AttributeKey.stringKey("business.campaign.id")
        private val COUPON_CODE_KEY = AttributeKey.stringKey("business.coupon.code")
        private val RAFFLE_ID_KEY = AttributeKey.stringKey("business.raffle.id")
        private val AMOUNT_KEY = AttributeKey.doubleKey("business.amount")
        private val QUANTITY_KEY = AttributeKey.longKey("business.quantity")
        private val SUCCESS_KEY = AttributeKey.booleanKey("business.success")
        private val ERROR_CODE_KEY = AttributeKey.stringKey("business.error.code")
        private val PROCESSING_TIME_KEY = AttributeKey.doubleKey("business.processing.time.ms")
    }

    private val tracer: Tracer = openTelemetry.getTracer("gasolinera-jsm-business")

    /**
     * Trace a business operation with automatic span management
     */
    fun <T> traceOperation(
        operationType: String,
        operationId: String? = null,
        attributes: Map<String, Any> = emptyMap(),
        operation: () -> T
    ): T {
        if (!tracingProperties.enabled || !tracingProperties.customSpans.businessOperations) {
            return operation()
        }

        val spanBuilder = tracer.spanBuilder(operationType)
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute(OPERATION_TYPE_KEY, operationType)

        operationId?.let { spanBuilder.setAttribute(OPERATION_ID_KEY, it) }

        // Add custom attributes
        attributes.forEach { (key, value) ->
            when (value) {
                is String -> spanBuilder.setAttribute(AttributeKey.stringKey(key), value)
                is Long -> spanBuilder.setAttribute(AttributeKey.longKey(key), value)
                is Double -> spanBuilder.setAttribute(AttributeKey.doubleKey(key), value)
                is Boolean -> spanBuilder.setAttribute(AttributeKey.booleanKey(key), value)
                else -> spanBuilder.setAttribute(AttributeKey.stringKey(key), value.toString())
            }
        }

        val span = spanBuilder.startSpan()
        val startTime = System.nanoTime()

        return span.makeCurrent().use { scope ->
            try {
                val result = operation()
                span.setStatus(StatusCode.OK)
                span.setAttribute(SUCCESS_KEY, true)
                result
            } catch (ex: Exception) {
                span.setStatus(StatusCode.ERROR, ex.message ?: "Unknown error")
                span.recordException(ex)
                span.setAttribute(SUCCESS_KEY, false)
                span.setAttribute(ERROR_CODE_KEY, ex.javaClass.simpleName)
                throw ex
            } finally {
                val processingTime = (System.nanoTime() - startTime) / 1_000_000.0
                span.setAttribute(PROCESSING_TIME_KEY, processingTime)
                span.end()
            }
        }
    }

    /**
     * Trace coupon operations
     */
    fun <T> traceCouponOperation(
        operation: CouponOperation,
        couponCode: String? = null,
        campaignId: String? = null,
        userId: String? = null,
        stationId: String? = null,
        amount: Double? = null,
        block: () -> T
    ): T {
        val attributes = mutableMapOf<String, Any>(
            "entity.type" to "coupon",
            "operation.category" to "coupon_management"
        )

        couponCode?.let { attributes["coupon.code"] = it }
        campaignId?.let { attributes["campaign.id"] = it }
        userId?.let { attributes["user.id"] = it }
        stationId?.let { attributes["station.id"] = it }
        amount?.let { attributes["amount"] = it }

        return traceOperation(
            operationType = "coupon.${operation.name.lowercase()}",
            attributes = attributes,
            operation = block
        )
    }

    /**
     * Trace raffle operations
     */
    fun <T> traceRaffleOperation(
        operation: RaffleOperation,
        raffleId: String? = null,
        userId: String? = null,
        prizeValue: Double? = null,
        ticketCount: Long? = null,
        block: () -> T
    ): T {
        val attributes = mutableMapOf<String, Any>(
            "entity.type" to "raffle",
            "operation.category" to "raffle_management"
        )

        raffleId?.let { attributes["raffle.id"] = it }
        userId?.let { attributes["user.id"] = it }
        prizeValue?.let { attributes["prize.value"] = it }
        ticketCount?.let { attributes["ticket.count"] = it }

        return traceOperation(
            operationType = "raffle.${operation.name.lowercase()}",
            attributes = attributes,
            operation = block
        )
    }

    /**
     * Trace station operations
     */
    fun <T> traceStationOperation(
        operation: StationOperation,
        stationId: String? = null,
        userId: String? = null,
        fuelType: String? = null,
        price: Double? = null,
        volume: Double? = null,
        block: () -> T
    ): T {
        val attributes = mutableMapOf<String, Any>(
            "entity.type" to "station",
            "operation.category" to "station_management"
        )

        stationId?.let { attributes["station.id"] = it }
        userId?.let { attributes["user.id"] = it }
        fuelType?.let { attributes["fuel.type"] = it }
        price?.let { attributes["fuel.price"] = it }
        volume?.let { attributes["fuel.volume"] = it }

        return traceOperation(
            operationType = "station.${operation.name.lowercase()}",
            attributes = attributes,
            operation = block
        )
    }

    /**
     * Trace user operations
     */
    fun <T> traceUserOperation(
        operation: UserOperation,
        userId: String? = null,
        role: String? = null,
        stationId: String? = null,
        block: () -> T
    ): T {
        val attributes = mutableMapOf<String, Any>(
            "entity.type" to "user",
            "operation.category" to "user_management"
        )

        userId?.let { attributes["user.id"] = it }
        role?.let { attributes["user.role"] = it }
        stationId?.let { attributes["station.id"] = it }

        return traceOperation(
            operationType = "user.${operation.name.lowercase()}",
            attributes = attributes,
            operation = block
        )
    }

    /**
     * Trace redemption operations
     */
    fun <T> traceRedemptionOperation(
        operation: RedemptionOperation,
        redemptionId: String? = null,
        userId: String? = null,
        couponCode: String? = null,
        ticketCount: Long? = null,
        multiplier: Double? = null,
        block: () -> T
    ): T {
        val attributes = mutableMapOf<String, Any>(
            "entity.type" to "redemption",
            "operation.category" to "redemption_processing"
        )

        redemptionId?.let { attributes["redemption.id"] = it }
        userId?.let { attributes["user.id"] = it }
        couponCode?.let { attributes["coupon.code"] = it }
        ticketCount?.let { attributes["ticket.count"] = it }
        multiplier?.let { attributes["multiplier"] = it }

        return traceOperation(
            operationType = "redemption.${operation.name.lowercase()}",
            attributes = attributes,
            operation = block
        )
    }

    /**
     * Trace database operations
     */
    fun <T> traceDatabaseOperation(
        operation: String,
        table: String,
        query: String? = null,
        block: () -> T
    ): T {
        if (!tracingProperties.customSpans.database) {
            return block()
        }

        val attributes = mapOf(
            "db.operation" to operation,
            "db.table" to table,
            "db.system" to "postgresql"
        ).let { attrs ->
            query?.let { attrs + ("db.statement" to it) } ?: attrs
        }

        return traceOperation(
            operationType = "db.$operation",
            attributes = attributes,
            operation = block
        )
    }

    /**
     * Trace Redis operations
     */
    fun <T> traceRedisOperation(
        operation: String,
        key: String? = null,
        block: () -> T
    ): T {
        if (!tracingProperties.customSpans.redis) {
            return block()
        }

        val attributes = mutableMapOf<String, Any>(
            "cache.operation" to operation,
            "cache.system" to "redis"
        )

        key?.let { attributes["cache.key"] = it }

        return traceOperation(
            operationType = "cache.$operation",
            attributes = attributes,
            operation = block
        )
    }

    /**
     * Trace external API calls
     */
    fun <T> traceExternalApiCall(
        service: String,
        operation: String,
        url: String? = null,
        method: String? = null,
        block: () -> T
    ): T {
        if (!tracingProperties.customSpans.externalApis) {
            return block()
        }

        val attributes = mutableMapOf<String, Any>(
            "external.service" to service,
            "external.operation" to operation
        )

        url?.let { attributes["http.url"] = it }
        method?.let { attributes["http.method"] = it }

        return traceOperation(
            operationType = "external.$service.$operation",
            attributes = attributes,
            operation = block
        )
    }

    /**
     * Add custom span attributes to current span
     */
    fun addSpanAttributes(attributes: Map<String, Any>) {
        val currentSpan = Span.current()
        if (currentSpan.isRecording) {
            attributes.forEach { (key, value) ->
                when (value) {
                    is String -> currentSpan.setAttribute(key, value)
                    is Long -> currentSpan.setAttribute(key, value)
                    is Double -> currentSpan.setAttribute(key, value)
                    is Boolean -> currentSpan.setAttribute(key, value)
                    else -> currentSpan.setAttribute(key, value.toString())
                }
            }
        }
    }

    /**
     * Add span event with attributes
     */
    fun addSpanEvent(name: String, attributes: Map<String, Any> = emptyMap()) {
        val currentSpan = Span.current()
        if (currentSpan.isRecording) {
            val eventAttributes = Attributes.builder().apply {
                attributes.forEach { (key, value) ->
                    when (value) {
                        is String -> put(AttributeKey.stringKey(key), value)
                        is Long -> put(AttributeKey.longKey(key), value)
                        is Double -> put(AttributeKey.doubleKey(key), value)
                        is Boolean -> put(AttributeKey.booleanKey(key), value)
                        else -> put(AttributeKey.stringKey(key), value.toString())
                    }
                }
            }.build()

            currentSpan.addEvent(name, eventAttributes)
        }
    }

    /**
     * Get current trace ID
     */
    fun getCurrentTraceId(): String? {
        val currentSpan = Span.current()
        return if (currentSpan.isRecording) {
            currentSpan.getSpanContext().getTraceId()
        } else null
    }

    /**
     * Get current span ID
     */
    fun getCurrentSpanId(): String? {
        val currentSpan = Span.current()
        return if (currentSpan.isRecording) {
            currentSpan.getSpanContext().getSpanId()
        } else null
    }
}

// Business operation enums
enum class CouponOperation {
    CREATE, VALIDATE, USE, EXPIRE, GENERATE_BATCH
}

enum class RaffleOperation {
    CREATE, PARTICIPATE, DRAW, SELECT_WINNER, DISTRIBUTE_PRIZE
}

enum class StationOperation {
    CREATE, UPDATE, UPDATE_FUEL_PRICE, PROCESS_TRANSACTION, CHECK_INVENTORY
}

enum class UserOperation {
    REGISTER, LOGIN, LOGOUT, UPDATE_PROFILE, CHANGE_PASSWORD, ASSIGN_ROLE
}

enum class RedemptionOperation {
    PROCESS, VALIDATE, GENERATE_TICKETS, CALCULATE_MULTIPLIER, COMPLETE
}