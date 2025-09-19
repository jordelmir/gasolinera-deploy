package com.gasolinerajsm.adengine.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "gemini.api")
data class GeminiConfig(
    var key: String = "",
    var baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    var timeoutSeconds: Int = 30,
    var model: String = "gemini-pro",
    var maxTokens: Int = 1000,
    var temperature: Double = 0.7
)