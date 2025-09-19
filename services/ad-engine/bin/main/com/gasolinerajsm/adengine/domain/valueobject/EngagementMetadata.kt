package com.gasolinerajsm.adengine.domain.valueobject

/**
 * Value object representing engagement metadata
 */
data class EngagementMetadata(
    val source: String = "UNKNOWN",
    val campaignId: String? = null,
    val sessionId: String? = null,
    val userAgent: String? = null,
    val referrer: String? = null,
    val additionalData: Map<String, Any> = emptyMap()
) {
    init {
        require(source.isNotBlank()) { "Source cannot be blank" }
    }

    fun hasAdditionalData(key: String): Boolean = additionalData.containsKey(key)

    fun getAdditionalData(key: String): Any? = additionalData[key]

    fun getAdditionalDataAsString(key: String): String? = getAdditionalData(key)?.toString()

    fun merge(other: EngagementMetadata): EngagementMetadata {
        return copy(
            additionalData = additionalData + other.additionalData
        )
    }

    fun value(): String {
        return additionalData.toString()
    }

    companion object {
        fun fromWeb(campaignId: String? = null): EngagementMetadata {
            return EngagementMetadata(
                source = "WEB",
                campaignId = campaignId
            )
        }

        fun fromMobile(campaignId: String? = null): EngagementMetadata {
            return EngagementMetadata(
                source = "MOBILE",
                campaignId = campaignId
            )
        }
    }
}