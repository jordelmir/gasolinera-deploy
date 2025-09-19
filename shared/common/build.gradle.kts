plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("java-library")
    id("java-test-fixtures")
}

repositories {
    mavenCentral()
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Spring Boot dependencies with consistent versions
    api("org.springframework.boot:spring-boot-starter:3.2.1")
    api("org.springframework.boot:spring-boot-starter-actuator:3.2.1")
    api("org.springframework.boot:spring-boot-starter-web:3.2.1")
    api("org.springframework:spring-context:6.1.2")
    api("org.slf4j:slf4j-api:2.0.9")

    // Jakarta annotations for @PostConstruct
    api("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // Micrometer for metrics
    api("io.micrometer:micrometer-core:1.12.1")
    api("io.micrometer:micrometer-registry-prometheus:1.12.1")

    // Flyway for migrations
    api("org.flywaydb:flyway-core:9.22.3")


    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    implementation("org.springframework:spring-web:6.1.2")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:postgresql:1.21.3")
    testImplementation("org.testcontainers:rabbitmq:1.21.3")

    // Kotest for testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-framework-datatest:5.8.0")

    // Spring Test for MockMvc
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework:spring-test:6.1.2")
    testImplementation("org.springframework:spring-web:6.1.2")

    testImplementation("io.mockk:mockk:1.13.8")

    // Test Fixtures dependencies
    testFixturesApi("org.springframework.boot:spring-boot-starter-test")
    testFixturesApi("org.springframework:spring-test:6.1.2")
    testFixturesApi("org.springframework:spring-web:6.1.2")
    testFixturesApi("io.kotest:kotest-runner-junit5:5.8.0")
    testFixturesApi("io.kotest:kotest-assertions-core:5.8.0")
    testFixturesApi("io.mockk:mockk:1.13.8")
    testFixturesApi("org.testcontainers:junit-jupiter:1.21.3")
    testFixturesApi("org.testcontainers:postgresql:1.21.3")
    testFixturesApi("org.testcontainers:rabbitmq:1.21.3")
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

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// This is a library module, not a standalone application
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier = ""
}