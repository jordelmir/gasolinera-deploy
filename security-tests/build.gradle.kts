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
    testImplementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")

    // HTTP Client for security testing
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JWT Testing
    testImplementation("io.jsonwebtoken:jjwt-api:0.12.3")
    testImplementation("io.jsonwebtoken:jjwt-impl:0.12.3")
    testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Security Testing Tools
    testImplementation("org.owasp:dependency-check-gradle:8.4.0")
    testImplementation("com.github.spotbugs:spotbugs-gradle-plugin:5.0.14")

    // Test Containers
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    // JSON Processing
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kotlin Test
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")

    // Async and Reactive
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

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

    // Set system properties for security tests
    systemProperty("spring.profiles.active", "security-test")
    systemProperty("security.test.mode", "comprehensive")

    // Increase memory for security tests
    maxHeapSize = "2g"
    jvmArgs("-XX:+UseG1GC")

    // Increase timeout for security tests
    timeout.set(Duration.ofMinutes(20))

    // Configure test execution
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}

// Task to run security tests
tasks.register<Test>("securityTest") {
    description = "Runs security tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("security")
    }

    systemProperty("security.test.mode", "full")
    shouldRunAfter("test")
}

// Task to run penetration tests
tasks.register<Test>("penetrationTest") {
    description = "Runs penetration tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("penetration")
    }

    systemProperty("security.test.mode", "penetration")
    shouldRunAfter("securityTest")
}

// Task to run error handling tests
tasks.register<Test>("errorHandlingTest") {
    description = "Runs error handling tests"
    group = "verification"

    useJUnitPlatform {
        includeTags("error-handling")
    }

    systemProperty("security.test.mode", "error-handling")
    shouldRunAfter("test")
}