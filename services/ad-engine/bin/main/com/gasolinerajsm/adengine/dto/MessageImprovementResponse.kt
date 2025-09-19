package com.gasolinerajsm.adengine.dto

data class MessageImprovementResponse(
    val originalMessage: String,
    val improvedMessage: String,
    val improvements: List<String>,
    val confidence: Double
)