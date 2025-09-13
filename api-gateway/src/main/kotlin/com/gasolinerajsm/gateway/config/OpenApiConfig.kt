package com.gasolinerajsm.gateway.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * OpenAPI Configuration for Gasolinera JSM API Gateway
 * Generates comprehensive Swagger documentation for all microservices
 */
@Configuration
class OpenApiConfig(private val environment: Environment) {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(createApiInfo())
            .servers(createServers())
            .tags(createTags())
            .addSecurityItem(SecurityRequirement().addList("Bearer Authentication"))
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes("Bearer Authentication", createSecurityScheme())
            )
    }

    @Bean
    fun authenticationApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("01-authentication")
            .displayName("🔐 Authentication & Authorization")
            .pathsToMatch("/api/v1/auth/**")
            .build()
    }

    @Bean
    fun userManagementApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("02-user-management")
            .displayName("👤 User Management")
            .pathsToMatch("/api/v1/users/**")
            .build()
    }

    @Bean
    fun stationsApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("03-stations")
            .displayName("⛽ Gas Stations")
            .pathsToMatch("/api/v1/stations/**")
            .build()
    }

    @Bean
    fun couponsApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("04-coupons")
            .displayName("🎫 Coupons & Vouchers")
            .pathsToMatch("/api/v1/coupons/**")
            .build()
    }

    @Bean
    fun redemptionsApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("05-redemptions")
            .displayName("💰 Coupon Redemptions")
            .pathsToMatch("/api/v1/redemptions/**")
            .build()
    }

    @Bean
    fun rafflesApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("06-raffles")
            .displayName("🎰 Raffles & Prizes")
            .pathsToMatch("/api/v1/raffles/**")
            .build()
    }

    @Bean
    fun adEngineApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("07-advertising")
            .displayName("📢 Advertising Engine")
            .pathsToMatch("/api/v1/ads/**")
            .build()
    }

    @Bean
    fun dashboardApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("08-dashboard")
            .displayName("📊 Dashboard & Analytics")
            .pathsToMatch("/api/v1/dashboard/**", "/api/v1/analytics/**")
            .build()
    }

    @Bean
    fun adminApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("09-administration")
            .displayName("⚙️ Administration")
            .pathsToMatch("/api/v1/admin/**")
            .build()
    }

    @Bean
    fun systemApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("10-system")
            .displayName("🔧 System & Health")
            .pathsToMatch("/api/v1/health/**", "/api/v1/metrics/**", "/api/v1/system/**")
            .build()
    }

    private fun createApiInfo(): Info {
        return Info()
            .title("Gasolinera JSM API")
            .description("""
                # 🚀 Gasolinera JSM - Comprehensive Fuel Management Platform

                ## Overview
                The Gasolinera JSM API provides a complete solution for fuel station management,
                customer loyalty programs, coupon systems, and raffle management. Built with
                microservices architecture and modern cloud-native technologies.

                ## Key Features
                - 🔐 **Secure Authentication** - JWT-based authentication with role-based access control
                - ⛽ **Station Management** - Comprehensive gas station operations and fuel pricing
                - 🎫 **Digital Coupons** - QR code-based coupon system with real-time validation
                - 💰 **Redemption System** - Seamless coupon redemption with ticket generation
                - 🎰 **Raffle Platform** - Automated raffle system with prize distribution
                - 📢 **Smart Advertising** - Targeted advertising with engagement tracking
                - 📊 **Analytics Dashboard** - Real-time insights and business intelligence
                - 🔧 **Admin Tools** - Comprehensive administration and monitoring capabilities

                ## Architecture
                - **Microservices**: Independent, scalable services
                - **Event-Driven**: Asynchronous communication with RabbitMQ
                - **Cloud-Native**: Kubernetes-ready with Docker containers
                - **High Availability**: Redis caching and PostgreSQL replication
                - **Monitoring**: Prometheus metrics and Grafana dashboards

                ## Getting Started
                1. **Authentication**: Obtain JWT token via `/auth/login`
                2. **Explore Stations**: Find nearby stations via `/stations/nearby`
                3. **Purchase Coupons**: Buy fuel coupons via `/coupons/purchase`
                4. **Redeem & Win**: Redeem coupons and participate in raffles
                5. **Track Progress**: Monitor your activity via `/dashboard/user`

                ## Support
                - 📧 **Email**: api-support@gasolinera-jsm.com
                - 📱 **Phone**: +52 55 1234 5678
                - 🌐 **Website**: https://gasolinera-jsm.com
                - 📚 **Documentation**: https://docs.gasolinera-jsm.com

                ## Rate Limits
                - **Authenticated Users**: 1000 requests/hour
                - **Anonymous Users**: 100 requests/hour
                - **Admin Users**: 5000 requests/hour

                ## Environments
                - **Production**: https://api.gasolinera-jsm.com
                - **Staging**: https://staging-api.gasolinera-jsm.com
                - **Development**: https://dev-api.gasolinera-jsm.com
            """.trimIndent())
            .version("v1.0.0")
            .contact(
                Contact()
                    .name("Gasolinera JSM API Team")
                    .email("api-team@gasolinera-jsm.com")
                    .url("https://gasolinera-jsm.com/contact")
            )
            .license(
                License()
                    .name("Proprietary License")
                    .url("https://gasolinera-jsm.com/license")
            )
    }

    private fun createServers(): List<Server> {
        val servers = mutableListOf<Server>()

        // Production server
        servers.add(
            Server()
                .url("https://api.gasolinera-jsm.com")
                .description("🚀 Production Server - Live API")
        )

        // Staging server
        servers.add(
            Server()
                .url("https://staging-api.gasolinera-jsm.com")
                .description("🧪 Staging Server - Pre-production Testing")
        )

        // Development server
        if (environment.activeProfiles.contains("dev")) {
            servers.add(
                Server()
                    .url("http://localhost:8080")
                    .description("💻 Development Server - Local Development")
            )
        }

        return servers
    }

    private fun createTags(): List<Tag> {
        return listOf(
            Tag().name("Authentication").description("🔐 User authentication and authorization endpoints"),
            Tag().name("User Management").description("👤 User profile and account management"),
            Tag().name("Gas Stations").description("⛽ Gas station information and fuel pricing"),
            Tag().name("Coupons").description("🎫 Digital coupon creation and management"),
            Tag().name("Redemptions").description("💰 Coupon redemption and validation"),
            Tag().name("Raffles").description("🎰 Raffle participation and prize management"),
            Tag().name("Advertising").description("📢 Advertising campaigns and engagement"),
            Tag().name("Dashboard").description("📊 User dashboard and analytics"),
            Tag().name("Analytics").description("📈 Business intelligence and reporting"),
            Tag().name("Administration").description("⚙️ System administration and configuration"),
            Tag().name("Health & Monitoring").description("🔧 System health and monitoring endpoints")
        )
    }

    private fun createSecurityScheme(): SecurityScheme {
        return SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("""
                ## JWT Authentication

                To access protected endpoints, you need to include a valid JWT token in the Authorization header.

                ### How to obtain a token:
                1. Register a new account via `POST /auth/register`
                2. Login with your credentials via `POST /auth/login`
                3. Use the returned `accessToken` in the Authorization header

                ### Header format:
                ```
                Authorization: Bearer <your-jwt-token>
                ```

                ### Token expiration:
                - **Access Token**: 1 hour
                - **Refresh Token**: 7 days

                ### Refresh your token:
                Use `POST /auth/refresh` with your refresh token to get a new access token.

                ### Roles and Permissions:
                - **USER**: Basic user operations (coupons, redemptions, raffles)
                - **STATION_OPERATOR**: Station management operations
                - **ADMIN**: Full system administration access
                - **SUPER_ADMIN**: Complete system control
            """.trimIndent())
    }
}