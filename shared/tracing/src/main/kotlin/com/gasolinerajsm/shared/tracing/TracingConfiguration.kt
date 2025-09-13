package com.gasolinerajsm.shared.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter
import io.opentelemetry.extension.trace.propagation.JaegerPropagator
import io.opentelemetry.instrumentation.spring.webmvc.v6_0.SpringWebMvcTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.semconv.ResourceAttributes
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

/**
 * Distributed Tracing Configuration with OpenTelemetry and Jaeger
 * Configures tracing for all microservices in Gasolinera JSM
 */
@Configuration
@EnableConfigurationProperties(TracingProperties::class)
class TracingConfiguration(
    private val environment: Environment,
    private val tracingProperties: TracingProperties
) : WebMvcConfigurer {

    @Bean
    fun openTelemetry(): OpenTelemetry {
        if (!tracingProperties.enabled) {
            return OpenTelemetry.noop()
        }

        val resource = Resource.getDefault()
            .merge(Resource.create(
                Attributes.of(
                    ResourceAttributes.SERVICE_NAME, getServiceName(),
                    ResourceAttributes.SERVICE_VERSION, getServiceVersion(),
                    ResourceAttributes.SERVICE_NAMESPACE, "gasolinera-jsm",
                    ResourceAttributes.DEPLOYMENT_ENVIRONMENT, getEnvironment()
                )
            ))

        val jaegerExporter = JaegerGrpcSpanExporter.builder()
            .setEndpoint(tracingProperties.jaeger.endpoint)
            .setTimeout(Duration.ofSeconds(tracingProperties.jaeger.timeoutSeconds))
            .build()

        val spanProcessor = BatchSpanProcessor.builder(jaegerExporter)
            .setMaxExportBatchSize(tracingProperties.batchSize)
            .setExportTimeout(Duration.ofSeconds(tracingProperties.exportTimeoutSeconds))
            .setScheduleDelay(Duration.ofMillis(tracingProperties.scheduleDelayMillis))
            .build()

        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(spanProcessor)
            .setResource(resource)
            .setSampler(createSampler())
            .build()

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setContextPropagators(
                ContextPropagators.create(
                    io.opentelemetry.context.propagation.TextMapPropagator.composite(
                        io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance(),
                        io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator.getInstance(),
                        JaegerPropagator.getInstance()
                    )
                )
            )
            .build()
    }

    @Bean
    fun tracer(openTelemetry: OpenTelemetry): Tracer {
        return openTelemetry.getTracer(getServiceName(), getServiceVersion())
    }

    @Bean
    fun springWebMvcTelemetry(openTelemetry: OpenTelemetry): SpringWebMvcTelemetry {
        return SpringWebMvcTelemetry.create(openTelemetry)
    }

    @Bean
    fun tracingInterceptor(openTelemetry: OpenTelemetry): TracingInterceptor {
        return TracingInterceptor(openTelemetry, tracingProperties)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        if (tracingProperties.enabled && tracingProperties.interceptor.enabled) {
            registry.addInterceptor(tracingInterceptor(openTelemetry()))
                .addPathPatterns(tracingProperties.interceptor.includePatterns)
                .excludePathPatterns(tracingProperties.interceptor.excludePatterns)
        }
    }

    private fun createSampler(): io.opentelemetry.sdk.trace.samplers.Sampler {
        return when (tracingProperties.sampling.type.lowercase()) {
            "always_on" -> io.opentelemetry.sdk.trace.samplers.Sampler.create(1.0)
            "always_off" -> io.opentelemetry.sdk.trace.samplers.Sampler.create(0.0)
            "ratio" -> io.opentelemetry.sdk.trace.samplers.Sampler.create(tracingProperties.sampling.ratio)
            "rate_limiting" -> io.opentelemetry.sdk.trace.samplers.Sampler.create(
                tracingProperties.sampling.maxTracesPerSecond / 100.0
            )
            else -> io.opentelemetry.sdk.trace.samplers.Sampler.parentBased(
                io.opentelemetry.sdk.trace.samplers.Sampler.create(tracingProperties.sampling.ratio)
            )
        }
    }

    private fun getServiceName(): String {
        return environment.getProperty("spring.application.name") ?: "gasolinera-service"
    }

    private fun getServiceVersion(): String {
        return environment.getProperty("app.version") ?: "1.0.0"
    }

    private fun getEnvironment(): String {
        return environment.getProperty("spring.profiles.active") ?: "development"
    }
}

/**
 * Tracing Properties Configuration
 */
@ConfigurationProperties(prefix = "tracing")
data class TracingProperties(
    val enabled: Boolean = true,
    val jaeger: JaegerProperties = JaegerProperties(),
    val sampling: SamplingProperties = SamplingProperties(),
    val interceptor: InterceptorProperties = InterceptorProperties(),
    val batchSize: Int = 512,
    val exportTimeoutSeconds: Long = 30,
    val scheduleDelayMillis: Long = 500,
    val customSpans: CustomSpansProperties = CustomSpansProperties()
) {

    data class JaegerProperties(
        val endpoint: String = "http://localhost:14250",
        val timeoutSeconds: Long = 10
    )

    data class SamplingProperties(
        val type: String = "ratio", // always_on, always_off, ratio, rate_limiting, parent_based
        val ratio: Double = 0.1, // 10% sampling by default
        val maxTracesPerSecond: Int = 100
    )

    data class InterceptorProperties(
        val enabled: Boolean = true,
        val includePatterns: List<String> = listOf("/api/**"),
        val excludePatterns: List<String> = listOf(
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
        )
    )

    data class CustomSpansProperties(
        val database: Boolean = true,
        val redis: Boolean = true,
        val rabbitmq: Boolean = true,
        val externalApis: Boolean = true,
        val businessOperations: Boolean = true
    )
}