package com.gasolinerajsm.adengine.domain.valueobject

import com.gasolinerajsm.adengine.domain.model.DeviceType

/**
 * Value object representing device information
 */
data class DeviceInfo(
    val deviceType: DeviceType,
    val deviceId: String? = null,
    val operatingSystem: String? = null,
    val browser: String? = null,
    val screenResolution: String? = null,
    val userAgent: String? = null,
    val ipAddress: String? = null
) {
    fun isMobile(): Boolean = deviceType.isMobile()

    fun isDesktop(): Boolean = deviceType == DeviceType.DESKTOP

    fun getDeviceCategory(): String {
        return when {
            isMobile() -> "mobile"
            isDesktop() -> "desktop"
            else -> "other"
        }
    }
}