package com.gasolinerajsm.adengine.domain.event

import com.gasolinerajsm.adengine.domain.valueobject.AdvertisementId
import com.gasolinerajsm.adengine.domain.valueobject.CampaignId
import com.gasolinerajsm.adengine.domain.valueobject.AdStatistics
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Event fired when an advertisement is completed
 */
data class AdvertisementCompletedEvent(
    val advertisementId: AdvertisementId,
    val campaignId: CampaignId,
    val title: String,
    val finalStatistics: AdStatistics,
    val totalSpend: BigDecimal,
    val completedBy: String?,
    override val occurredAt: LocalDateTime
) : DomainEvent