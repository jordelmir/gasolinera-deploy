package com.gasolinerajsm.adengine.domain.event

import com.gasolinerajsm.adengine.domain.valueobject.AdvertisementId
import com.gasolinerajsm.adengine.domain.valueobject.CampaignId
import java.time.LocalDateTime

/**
 * Event fired when an advertisement is activated
 */
data class AdvertisementActivatedEvent(
    val advertisementId: AdvertisementId,
    val campaignId: CampaignId,
    val title: String,
    val activatedBy: String?,
    override val occurredAt: LocalDateTime
) : DomainEvent