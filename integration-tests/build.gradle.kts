import java.time.Duration

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "com.gasolinerajsm"
version = "1.0.0"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Service modules - only include working ones for now
    implementation(project(":services:auth-service"))
    implementation(project(":services:coupon-service"))
    implementation(project(":services:station-service"))
    // Exclude problematic services: raffle-service, ad-engine

    // Shared modules
    implementation(project(":shared:messaging"))
    implementation(project(":shared:security"))
    implementation(project(":shared:common"))

    // Database
    implementation("com.h2database:h2")
    implementation("org.flywaydb:flyway-core")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // JSON Processing
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Spring Boot Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")

    // Test Containers - commented out as we're using H2 instead
    // testImplementation("org.testcontainers:testcontainers")
    // testImplementation("org.testcontainers:junit-jupiter")
    // testImplementation("org.testcontainers:postgresql")
    // testImplementation("org.testcontainers:rabbitmq")

    // HTTP Client
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")

    // Hamcrest for matchers
    testImplementation("org.hamcrest:hamcrest:2.2")

    // AssertJ for better assertions
    testImplementation("org.assertj:assertj-core")

    // Spring JDBC for JdbcTemplate
    testImplementation("org.springframework:spring-jdbc:6.1.2")

    // Kotlin Test
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")

    // Awaitility for async testing
    testImplementation("org.awaitility:awaitility-kotlin")
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Set system properties for integration tests
    systemProperty("spring.profiles.active", "integration-test")
    systemProperty("testcontainers.reuse.enable", "true")

    // Increase timeout for integration tests
    timeout.set(Duration.ofMinutes(10))

    // Configure test execution
    maxParallelForks = 1
    forkEvery = 1

    // JVM arguments for better performance
    jvmArgs("-XX:+UseG1GC", "-Xmx2g")
}

// Task to run only integration tests
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("integration")
    }

    shouldRunAfter("test")
}

// Task to run end-to-end tests
tasks.register<Test>("e2eTest") {
    description = "Runs end-to-end tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("e2e")
    }

    shouldRunAfter("integrationTest")
}

// This is a test module, not a standalone application
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = false
}
