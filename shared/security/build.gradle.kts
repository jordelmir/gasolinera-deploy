plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Spring Security dependencies needed by the security code
    api("org.springframework:spring-context:6.1.2")
    api("org.springframework:spring-web:6.1.2")
    api("org.springframework:spring-webmvc:6.1.2")
    api("org.springframework.security:spring-security-core:6.2.1")
    api("org.springframework.security:spring-security-web:6.2.1")
    api("org.springframework.security:spring-security-config:6.2.1")
    api("org.springframework.data:spring-data-commons:3.2.1")
    api("org.springframework.boot:spring-boot:3.2.1")
    api("org.springframework.boot:spring-boot-autoconfigure:3.2.1")
    api("jakarta.servlet:jakarta.servlet-api:6.0.0")
    api("jakarta.validation:jakarta.validation-api:3.0.2")
    api("org.slf4j:slf4j-api:2.0.9")

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