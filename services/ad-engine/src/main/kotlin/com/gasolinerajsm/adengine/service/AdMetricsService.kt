package com.gasolinerajsm.adengine.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.stereotype.Service

@Service
class AdMetricsService(
    private val meterRegistry: MeterRegistry
) {
    private val impressionCounter = Counter.builder("ad.impressions")
        .description("Total ad impressions")
        .register(meterRegistry)

    private val clickCounter = Counter.builder("ad.clicks")
        .description("Total ad clicks")
        .register(meterRegistry)

    private val completionCounter = Counter.builder("ad.completions")
        .description("Total ad completions")
        .register(meterRegistry)

    private val errorCounter = Counter.builder("ad.errors")
        .description("Total ad errors")
        .register(meterRegistry)

    fun recordImpression(campaignId: Long, stationId: String? = null) {
        Counter.builder("ad.impressions")
            .description("Total ad impressions")
            .tags(
                "campaign_id", campaignId.toString(),
                "station_id", stationId ?: "unknown"
            )
            .register(meterRegistry)
            .increment()
    }

    fun recordClick(campaignId: Long, stationId: String? = null) {
        Counter.builder("ad.clicks")
            .description("Total ad clicks")
            .tags(
                "campaign_id", campaignId.toString(),
                "station_id", stationId ?: "unknown"
            )
            .register(meterRegistry)
            .increment()
    }

    fun recordCompletion(campaignId: Long, stationId: String? = null) {
        Counter.builder("ad.completions")
            .description("Total ad completions")
            .tags(
                "campaign_id", campaignId.toString(),
                "station_id", stationId ?: "unknown"
            )
            .register(meterRegistry)
            .increment()
    }

    fun recordError(errorType: String, campaignId: Long? = null) {
        Counter.builder("ad.errors")
            .description("Total ad errors")
            .tags(
                "error_type", errorType,
                "campaign_id", campaignId?.toString() ?: "unknown"
            )
            .register(meterRegistry)
            .increment()
    }
}