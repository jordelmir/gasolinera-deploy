package com.gasolinerajsm.adengine.dto

data class MessageImprovementRequest(
    val message: String,
    val context: String? = null,
    val targetAudience: String? = null,
    val tone: String? = null
)