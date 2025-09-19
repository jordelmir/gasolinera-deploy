pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "gasolinera-jsm-ultimate"

include(
    "services:auth-service",
    "services:coupon-service",
    "services:station-service", // ✅ Fixed and enabled
    "services:api-gateway", // ✅ Fixed and enabled
    "services:ad-engine", // ✅ Fixed and enabled
    "services:message-improver", // NEW - Message improvement service with Gemini
    "packages:internal-sdk", // NEW
    "services:raffle-service",
    "services:redemption-service",
    "integration-tests", // Integration tests module
    "shared:messaging",
    "shared:security",
    "shared:common"
    // "packages:temp-sdk"
)