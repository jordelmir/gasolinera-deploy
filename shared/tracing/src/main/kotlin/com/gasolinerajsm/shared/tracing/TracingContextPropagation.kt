package com.gasolinerajsm.shared.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapSetter
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * Tracing Context Propagation Utilities
 * Handles trace context propagation across service boundaries
 */
@Component
class TracingContextPropagation(
    private val openTelemetry: OpenTelemetry
) {

    private val textMapPropagator = openTelemetry.propagators.textMapPropagator

    /**
     * HTTP Headers Getter for extracting trace context
     */
    private val httpHeadersGetter = object : TextMapGetter<HttpHeaders> {
        override fun keys(carrier: HttpHeaders): Iterable<String> {
            return carrier.keys
        }

        override fun get(carrier: HttpHeaders?, key: String): String? {
            return carrier?.getFirst(key)
        }
    }

    /**
     * HTTP Headers Setter for injecting trace context
     */
    private val httpHeadersSetter = object : TextMapSetter<HttpHeaders> {
        override fun set(carrier: HttpHeaders?, key: String, value: String) {
            carrier?.set(key, value)
        }
    }

    /**
     * Message Properties Getter for RabbitMQ
     */
    private val messagePropertiesGetter = object : TextMapGetter<MessageProperties> {
        override fun keys(carrier: MessageProperties): Iterable<String> {
            return carrier.headers.keys.map { it.toString() }
        }

        override fun get(carrier: MessageProperties?, key: String): String? {
            return carrier?.headers?.get(key)?.toString()
        }
    }

    /**
     * Message Properties Setter for RabbitMQ
     */
    private val messagePropertiesSetter = object : TextMapSetter<MessageProperties> {
        override fun set(carrier: MessageProperties?, key: String, value: String) {
            carrier?.headers?.put(key, value)
        }
    }

    /**
     * Map Getter for generic map-based carriers
     */
    private val mapGetter = object : TextMapGetter<Map<String, String>> {
        override fun keys(carrier: Map<String, String>): Iterable<String> {
            return carrier.keys
        }

        override fun get(carrier: Map<String, String>?, key: String): String? {
            return carrier?.get(key)
        }
    }

    /**
     * Map Setter for generic map-based carriers
     */
    private val mapSetter = object : TextMapSetter<MutableMap<String, String>> {
        override fun set(carrier: MutableMap<String, String>?, key: String, value: String) {
            carrier?.put(key, value)
        }
    }

    /**
     * Extract trace context from HTTP headers
     */
    fun extractContext(headers: HttpHeaders): Context {
        return textMapPropagator.extract(Context.current(), headers, httpHeadersGetter)
    }

    /**
     * Inject trace context into HTTP headers
     */
    fun injectContext(headers: HttpHeaders, context: Context = Context.current()) {
        textMapPropagator.inject(context, headers, httpHeadersSetter)
    }

    /**
     * Extract trace context from RabbitMQ message properties
     */
    fun extractContext(messageProperties: MessageProperties): Context {
        return textMapPropagator.extract(Context.current(), messageProperties, messagePropertiesGetter)
    }

    /**
     * Inject trace context into RabbitMQ message properties
     */
    fun injectContext(messageProperties: MessageProperties, context: Context = Context.current()) {
        textMapPropagator.inject(context, messageProperties, messagePropertiesSetter)
    }

    /**
     * Extract trace context from generic map
     */
    fun extractContext(headers: Map<String, String>): Context {
        return textMapPropagator.extract(Context.current(), headers, mapGetter)
    }

    /**
     * Inject trace context into generic map
     */
    fun injectContext(headers: MutableMap<String, String>, context: Context = Context.current()) {
        textMapPropagator.inject(context, headers, mapSetter)
    }

    /**
     * Create a traced RestTemplate that automatically propagates context
     */
    fun createTracedRestTemplate(): RestTemplate {
        val restTemplate = RestTemplate()

        // Add interceptor to inject trace context
        restTemplate.interceptors.add { request, body, execution ->
            val headers = HttpHeaders()
            request.headers.forEach { (key, values) ->
                headers.addAll(key, values)
            }

            injectContext(headers)

            headers.forEach { (key, values) ->
                values.forEach { value ->
                    request.headers.add(key, value)
                }
            }

            execution.execute(request, body)
        }

        return restTemplate
    }

    /**
     * Create a traced WebClient that automatically propagates context
     */
    fun createTracedWebClient(): WebClient {
        return WebClient.builder()
            .filter { request, next ->
                val headers = request.headers()
                injectContext(headers)
                next.exchange(request)
            }
            .build()
    }

    /**
     * Wrap executor to propagate trace context
     */
    fun wrapExecutor(executor: Executor): Executor {
        return Executor { command ->
            val context = Context.current()
            executor.execute {
                context.makeCurrent().use {
                    command.run()
                }
            }
        }
    }

    /**
     * Wrap CompletableFuture to propagate trace context
     */
    fun <T> wrapCompletableFuture(future: CompletableFuture<T>): CompletableFuture<T> {
        val context = Context.current()
        return future.whenComplete { _, _ ->
            context.makeCurrent().use { }
        }
    }

    /**
     * Run code with specific trace context
     */
    fun <T> withContext(context: Context, block: () -> T): T {
        return context.makeCurrent().use { block() }
    }

    /**
     * Run code with current span as parent
     */
    fun <T> withCurrentSpan(block: () -> T): T {
        val currentSpan = Span.current()
        return currentSpan.makeCurrent().use { block() }
    }

    /**
     * Create child span and run code within it
     */
    fun <T> withChildSpan(
        operationName: String,
        attributes: Map<String, String> = emptyMap(),
        block: () -> T
    ): T {
        val tracer = openTelemetry.getTracer("gasolinera-jsm-context")
        val spanBuilder = tracer.spanBuilder(operationName)

        attributes.forEach { (key, value) ->
            spanBuilder.setAttribute(key, value)
        }

        val span = spanBuilder.startSpan()
        return span.makeCurrent().use {
            try {
                val result = block()
                span.setStatus(io.opentelemetry.api.trace.StatusCode.OK)
                result
            } catch (ex: Exception) {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, ex.message ?: "Unknown error")
                span.recordException(ex)
                throw ex
            } finally {
                span.end()
            }
        }
    }

    /**
     * Get current trace information
     */
    fun getCurrentTraceInfo(): TraceInfo? {
        val currentSpan = Span.current()
        return if (currentSpan.isRecording) {
            val spanContext = currentSpan.getSpanContext()
            TraceInfo(
                traceId = spanContext.getTraceId(),
                spanId = spanContext.getSpanId(),
                isSampled = spanContext.isSampled
            )
        } else null
    }

    /**
     * Create trace headers for manual propagation
     */
    fun createTraceHeaders(context: Context = Context.current()): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        injectContext(headers, context)
        return headers
    }

    data class TraceInfo(
        val traceId: String,
        val spanId: String,
        val isSampled: Boolean
    )
}

/**
 * RabbitMQ Tracing Support
 */
@Component
class RabbitMQTracingSupport(
    private val contextPropagation: TracingContextPropagation,
    private val openTelemetry: OpenTelemetry
) {

    private val tracer = openTelemetry.getTracer("gasolinera-jsm-rabbitmq")

    /**
     * Create traced message with context propagation
     */
    fun createTracedMessage(
        message: Message,
        operationName: String = "message.send"
    ): Message {
        val span = tracer.spanBuilder(operationName)
            .setSpanKind(io.opentelemetry.api.trace.SpanKind.PRODUCER)
            .setAttribute("messaging.system", "rabbitmq")
            .setAttribute("messaging.destination", message.messageProperties.receivedRoutingKey ?: "")
            .startSpan()

        span.makeCurrent().use {
            contextPropagation.injectContext(message.messageProperties)
            span.setAttribute("messaging.message_id", message.messageProperties.messageId ?: "")
            span.setAttribute("messaging.correlation_id", message.messageProperties.correlationId ?: "")
        }

        span.end()
        return message
    }

    /**
     * Process received message with trace context
     */
    fun <T> processTracedMessage(
        message: Message,
        operationName: String = "message.receive",
        processor: () -> T
    ): T {
        val extractedContext = contextPropagation.extractContext(message.messageProperties)

        val span = tracer.spanBuilder(operationName)
            .setParent(extractedContext)
            .setSpanKind(io.opentelemetry.api.trace.SpanKind.CONSUMER)
            .setAttribute("messaging.system", "rabbitmq")
            .setAttribute("messaging.destination", message.messageProperties.receivedRoutingKey ?: "")
            .setAttribute("messaging.message_id", message.messageProperties.messageId ?: "")
            .startSpan()

        return span.makeCurrent().use {
            try {
                val result = processor()
                span.setStatus(io.opentelemetry.api.trace.StatusCode.OK)
                result
            } catch (ex: Exception) {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, ex.message ?: "Unknown error")
                span.recordException(ex)
                throw ex
            } finally {
                span.end()
            }
        }
    }
}

/**
 * Database Tracing Support
 */
@Component
class DatabaseTracingSupport(
    private val openTelemetry: OpenTelemetry
) {

    private val tracer = openTelemetry.getTracer("gasolinera-jsm-database")

    /**
     * Trace database operation
     */
    fun <T> traceQuery(
        operation: String,
        table: String,
        sql: String? = null,
        block: () -> T
    ): T {
        val span = tracer.spanBuilder("db.$operation")
            .setSpanKind(io.opentelemetry.api.trace.SpanKind.CLIENT)
            .setAttribute("db.system", "postgresql")
            .setAttribute("db.operation", operation)
            .setAttribute("db.sql.table", table)
            .startSpan()

        sql?.let { span.setAttribute("db.statement", it) }

        return span.makeCurrent().use {
            try {
                val result = block()
                span.setStatus(io.opentelemetry.api.trace.StatusCode.OK)
                result
            } catch (ex: Exception) {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, ex.message ?: "Unknown error")
                span.recordException(ex)
                throw ex
            } finally {
                span.end()
            }
        }
    }
}