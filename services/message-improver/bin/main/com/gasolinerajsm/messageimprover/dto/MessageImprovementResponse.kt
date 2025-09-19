package com.gasolinerajsm.messageimprover.dto

data class MessageImprovementResponse(
    val originalMessage: String,
    val improvedMessage: String,
    val improvements: List<String>,
    val confidence: Double
)