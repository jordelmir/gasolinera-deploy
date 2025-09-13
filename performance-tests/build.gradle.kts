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

    // Performance Testing
    testImplementation("io.gatling.highcharts:gatling-charts-highcharts:3.9.5")
    testImplementation("io.gatling:gatling-test-framework:3.9.5")

    // JMeter Integration
    testImplementation("org.apache.jmeter:ApacheJMeter_core:5.6.2")
    testImplementation("org.apache.jmeter:ApacheJMeter_http:5.6.2")
    testImplementation("org.apache.jmeter:ApacheJMeter_java:5.6.2")

    // HTTP Client for load testing
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Async and Reactive
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("io.projectreactor:reactor-core")

    // Metrics and Monitoring
    testImplementation("io.micrometer:micrometer-core")
    testImplementation("io.micrometer:micrometer-registry-prometheus")

    // Test Containers
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    // JSON Processing
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kotlin Test
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")

    // Statistics and Analysis
    testImplementation("org.apache.commons:commons-math3:3.6.1")
    testImplementation("org.apache.commons:commons-csv:1.10.0")

    // Database
    testImplementation("org.postgresql:postgresql")
    testImplementation("com.zaxxer:HikariCP")

    // Shared modules
    testImplementation(project(":shared:messaging"))
    testImplementation(project(":shared:security"))
    testImplementation(project(":shared:common"))
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Set system properties for performance tests
    systemProperty("spring.profiles.active", "performance-test")
    systemProperty("performance.test.duration", "300") // 5 minutes default
    systemProperty("performance.test.users", "100") // 100 concurrent users default

    // Increase memory for performance tests
    maxHeapSize = "4g"
    jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200")

    // Increase timeout for performance tests
    timeout.set(Duration.ofMinutes(30))

    // Configure test execution
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    forkEvery = 0 // Don't fork for each test
}

// Task to run performance tests
tasks.register<Test>("performanceTest") {
    description = "Runs performance tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("performance")
    }

    // Performance test specific configuration
    systemProperty("performance.test.mode", "full")
    systemProperty("performance.test.duration", "600") // 10 minutes for full tests
    systemProperty("performance.test.users", "200") // 200 concurrent users for full tests

    shouldRunAfter("test")
}

// Task to run load tests
tasks.register<Test>("loadTest") {
    description = "Runs load tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("load")
    }

    // Load test specific configuration
    systemProperty("performance.test.mode", "load")
    systemProperty("performance.test.duration", "1800") // 30 minutes for load tests
    systemProperty("performance.test.users", "500") // 500 concurrent users for load tests

    shouldRunAfter("performanceTest")
}

// Task to run stress tests
tasks.register<Test>("stressTest") {
    description = "Runs stress tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("stress")
    }

    // Stress test specific configuration
    systemProperty("performance.test.mode", "stress")
    systemProperty("performance.test.duration", "900") // 15 minutes for stress tests
    systemProperty("performance.test.users", "1000") // 1000 concurrent users for stress tests

    shouldRunAfter("loadTest")
}