import java.time.Duration

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.gasolinerajsm"
version = "1.0.0"

dependencies {
    // Spring Boot Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")

    // Test Containers
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:rabbitmq")

    // HTTP Client
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")

    // JSON Processing
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kotlin Test
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.kotest:kotest-extensions-spring")

    // Awaitility for async testing
    testImplementation("org.awaitility:awaitility-kotlin")

    // Database
    testImplementation("org.postgresql:postgresql")
    testImplementation("org.flywaydb:flyway-core")

    // Messaging
    testImplementation("org.springframework.boot:spring-boot-starter-amqp")

    // Shared modules
    testImplementation(project(":shared:messaging"))
    testImplementation(project(":shared:security"))
    testImplementation(project(":shared:common"))
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