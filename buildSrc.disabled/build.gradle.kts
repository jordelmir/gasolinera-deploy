plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.9.24")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.3.3")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.6")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.6")
}