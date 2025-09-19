package com.gasolinerajsm.adengine.domain.event

import com.gasolinerajsm.adengine.domain.model.AdType
import com.gasolinerajsm.adengine.domain.valueobject.*
import java.time.LocalDateTime

/**
 * Event fired when an advertisement is created
 */
data class AdvertisementCreatedEvent(
    val advertisementId: AdvertisementId,
    val campaignId: CampaignId,
    val title: String,
    val adType: AdType,
    val schedule: AdSchedule,
    val budget: AdBudget,
    override val occurredAt: LocalDateTime
) : DomainEvent