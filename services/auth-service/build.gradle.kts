import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "com.gasolinerajsm"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- Spring Boot Starters ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config:4.1.3")

    // --- Observabilidad (Actuator + Prometheus) ---
    implementation("org.springframework.boot:spring-boot-starter-actuator") // NUEVO
    implementation("io.micrometer:micrometer-registry-prometheus")   // NUEVO

    // --- OpenTelemetry Tracing ---
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // --- Logging ---
    implementation("net.logstash.logback:logstash-logback-encoder:7.4") // For structured JSON logging

    // --- Kotlin y Jackson ---
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // --- Base de Datos ---
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // --- JWT ---
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // --- OpenAPI/Swagger ---
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // --- Jakarta Servlet API ---
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")

    // --- Annotations (for @PostConstruct) - Jakarta EE for Spring Boot 3 ---
    implementation("jakarta.annotation:jakarta.annotation-api")

    // --- Shared modules ---
    implementation(project(":shared:common"))
    implementation(project(":shared:messaging"))
    implementation(project(":shared:security"))

    // --- Tests ---
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")

    // --- Test Database ---
    testImplementation("com.h2database:h2")

    // --- Test Containers (for integration tests) ---
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("org.testcontainers:postgresql:1.19.7")


    // --- MockK for Kotlin mocking ---
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    // --- AssertJ for better assertions ---
    testImplementation("org.assertj:assertj-core")

    // --- Spring Web for contentType ---
    testImplementation("org.springframework:spring-web")

    // --- JSON testing ---
    testImplementation("com.jayway.jsonpath:json-path")

    // --- Embedded Redis for tests ---
    testImplementation("it.ozimov:embedded-redis:0.7.3") {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }

    // --- JUnit Platform Launcher ---
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // --- Kotest for testing ---
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")

    // --- Shared testing utilities ---
    testImplementation(testFixtures(project(":shared:common")))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    enabled = false
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("auth-service.jar")
}
