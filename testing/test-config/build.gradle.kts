// Shared Testing Configuration for Gasolinera JSM
// Common testing dependencies and configuration for all services

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("jacoco")
    id("org.sonarqube")
}

dependencies {
    // Testing Framework
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")

    // Mockito with Kotlin support
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")

    // AssertJ for fluent assertions
    testImplementation("org.assertj:assertj-core")

    // Testcontainers
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:redis")
    testImplementation("org.testcontainers:rabbitmq")

    // Spring Test
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.security:spring-security-test")

    // WebFlux Testing
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("io.projectreactor:reactor-test")

    // Database Testing
    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // JSON Testing
    testImplementation("com.jayway.jsonpath:json-path")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Faker for test data
    testImplementation("com.github.javafaker:javafaker:1.0.2")

    // WireMock for external service mocking
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")

    // Awaitility for async testing
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")

    // Kotest for Kotlin-specific testing
    testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
    testImplementation("io.kotest:kotest-property:5.7.2")
}

// JaCoCo Configuration
jacoco {
    toolVersion = "0.8.10"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec"))

    finalizedBy(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal() // 80% minimum coverage
            }
        }

        rule {
            element = "CLASS"
            excludes = listOf(
                "*.config.*",
                "*.dto.*",
                "*.entity.*",
                "*Application*",
                "*Config*",
                "*Exception*"
            )

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()
            }
        }

        rule {
            element = "METHOD"
            excludes = listOf(
                "*.equals",
                "*.hashCode",
                "*.toString",
                "*.copy*",
                "*.component*"
            )

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

// Test Configuration
tasks.test {
    useJUnitPlatform()

    // JVM arguments for testing
    jvmArgs = listOf(
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+UseG1GC",
        "-Xmx2g",
        "-Dspring.profiles.active=test"
    )

    // Test execution configuration
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1

    // Test reporting
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }

    // Fail fast on first test failure
    failFast = false

    // Include/exclude patterns
    include("**/*Test.class", "**/*Tests.class", "**/*Spec.class")
    exclude("**/*IntegrationTest.class", "**/*E2ETest.class")

    finalizedBy(tasks.jacocoTestReport)
}

// Separate task for integration tests
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests"
    group = "verification"

    useJUnitPlatform()

    include("**/*IntegrationTest.class", "**/*IT.class")
    exclude("**/*Test.class", "**/*UnitTest.class")

    shouldRunAfter(tasks.test)
}

// SonarQube Configuration
sonar {
    properties {
        property("sonar.projectKey", "gasolinera-jsm")
        property("sonar.projectName", "Gasolinera JSM")
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.junit.reportPaths", "${layout.buildDirectory.get()}/test-results/test")
        property("sonar.coverage.exclusions", [
            "**/*Application*",
            "**/*Config*",
            "**/*Exception*",
            "**/dto/**",
            "**/entity/**",
            "**/config/**"
        ].joinToString(","))
        property("sonar.cpd.exclusions", [
            "**/dto/**",
            "**/entity/**"
        ].joinToString(","))
    }
}

// Custom task to run all tests with coverage
tasks.register("testWithCoverage") {
    description = "Runs all tests with coverage verification"
    group = "verification"

    dependsOn(tasks.test, tasks.integrationTest, tasks.jacocoTestCoverageVerification)
}