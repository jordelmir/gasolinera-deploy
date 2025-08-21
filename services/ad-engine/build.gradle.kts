

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
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
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config:4.1.3")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // --- Observabilidad (Actuator + Prometheus) ---
    implementation("org.springframework.boot:spring-boot-starter-actuator")      // NUEVO
    implementation("io.micrometer:micrometer-registry-prometheus") // NUEVO

    // --- OpenTelemetry Tracing ---
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // --- Kafka ---
    implementation("org.springframework.kafka:spring-kafka") // NUEVO

    // --- JWT ---
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // --- Logging ---
    implementation("net.logstash.logback:logstash-logback-encoder:7.4") // For structured JSON logging

    // --- Kotlin y Jackson ---
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // --- Base de Datos ---
    runtimeOnly("org.postgresql:postgresql")

    // --- Tests ---
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("ad-engine.jar")
}