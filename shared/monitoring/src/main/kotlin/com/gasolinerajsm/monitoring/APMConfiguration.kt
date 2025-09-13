package com.gasolinerajsm.monitoring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.core.instrument.binder.system.*
import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.Duration

/**
 * World-Class APM Configuration for Gasolinera JSM
 *
 * Implements comprehensive Application Performance Monitoring with:
 * - JVM metrics (memory, GC, threads, classes)
 * - System metrics (CPU, disk, network)
 * - Custom business metrics
 * - Performance profiling
 * - Real-time alerting
 */
@Configuration
class APMConfiguration {

    @Bean
    @Primary
    fun prometheusMeterRegistry(): PrometheusMeterRegistry {
        return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Bean
    fun meterRegistryCustomizer(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            // Add common tags to all metrics
            registry.config()
                .commonTags(
                    "application", "gasolinera-jsm",
                    "environment", System.getenv("SPRING_PROFILES_ACTIVE") ?: "development",
                    "version", System.getenv("APP_VERSION") ?: "1.0.0",
                    "instance", System.getenv("HOSTNAME") ?: "localhost"
                )
                // Configure metric filters for performance
                .meterFilter(MeterFilter.deny { id ->
                    // Exclude noisy metrics
                    id.name.startsWith("jvm.gc.pause") &&
                    id.getTag("cause")?.contains("Allocation Failure") == true
                })
                .meterFilter(MeterFilter.maximumExpectedValue("http.server.requests", Duration.ofSeconds(10)))
                .meterFilter(MeterFilter.maximumExpectedValue("database.query.time", Duration.ofSeconds(5)))
        }
    }

    @Bean
    fun jvmMemoryMetrics(): JvmMemoryMetrics = JvmMemoryMetrics()

    @Bean
    fun jvmGcMetrics(): JvmGcMetrics = JvmGcMetrics()

    @Bean
    fun jvmThreadMetrics(): JvmThreadMetrics = JvmThreadMetrics()

    @Bean
    fun jvmClassLoaderMetrics(): JvmClassLoaderMetrics = JvmClassLoaderMetrics()

    @Bean
    fun jvmCompilationMetrics(): JvmCompilationMetrics = JvmCompilationMetrics()

    @Bean
    fun processorMetrics(): ProcessorMetrics = ProcessorMetrics()

    @Bean
    fun fileDescriptorMetrics(): FileDescriptorMetrics = FileDescriptorMetrics()

    @Bean
    fun uptimeMetrics(): UptimeMetrics = UptimeMetrics()

    @Bean
    fun diskSpaceMetrics(): DiskSpaceMetrics = DiskSpaceMetrics(
        listOf(
            java.io.File("/"),
            java.io.File("/tmp")
        )
    )

    @Bean
    fun tomcatMetrics(): TomcatMetrics? {
        return try {
            TomcatMetrics.monitor(null, "tomcat")
        } catch (e: Exception) {
            // Tomcat not available (e.g., running with Netty)
            null
        }
    }

    @Bean
    fun performanceProfiler(meterRegistry: MeterRegistry): PerformanceProfiler {
        return PerformanceProfiler(meterRegistry)
    }

    @Bean
    fun businessMetrics(meterRegistry: MeterRegistry): BusinessMetrics {
        return BusinessMetrics(meterRegistry)
    }

    @Bean
    fun databaseMetrics(meterRegistry: MeterRegistry): DatabaseMetrics {
        return DatabaseMetrics(meterRegistry)
    }

    @Bean
    fun cacheMetrics(meterRegistry: MeterRegistry): CacheMetrics {
        return CacheMetrics(meterRegistry)
    }

    @Bean
    fun messagingMetrics(meterRegistry: MeterRegistry): MessagingMetrics {
        return MessagingMetrics(meterRegistry)
    }

    @Bean
    fun securityMetrics(meterRegistry: MeterRegistry): SecurityMetrics {
        return SecurityMetrics(meterRegistry)
    }
}