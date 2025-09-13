import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.1" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.21" apply false
    kotlin("plugin.spring") version "1.9.21" apply false
    kotlin("plugin.jpa") version "1.9.21" apply false

    // Quality and Security plugins
    id("org.sonarqube") version "4.4.1.3373"
    id("jacoco")
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.github.ben-manes.versions") version "0.50.0"
    id("org.owasp.dependencycheck") version "9.0.7"
    id("com.gorylenko.gradle-git-properties") version "2.4.1"
}

allprojects {
    group = "com.gasolinerajsm"
    version = "1.0.0"

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")
    apply(plugin = "org.sonarqube")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.owasp.dependencycheck")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.jacocoTestReport)
    }

    // Jacoco Configuration
    jacoco {
        toolVersion = "0.8.8"
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
        executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec"))
    }

    tasks.jacocoTestCoverageVerification {
        dependsOn(tasks.jacocoTestReport)
        violationRules {
            rule {
                limit {
                    minimum = "0.85".toBigDecimal() // 85% minimum coverage
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
                    "*Configuration*"
                )
                limit {
                    counter = "LINE"
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }

    // Detekt Configuration
    detekt {
        toolVersion = "1.23.4"
        config.setFrom("$rootDir/config/detekt/detekt.yml")
        buildUponDefaultConfig = true
        autoCorrect = true

        reports {
            html.required.set(true)
            xml.required.set(true)
            txt.required.set(false)
            sarif.required.set(true)
        }
    }

    // KtLint Configuration
    ktlint {
        version.set("0.50.0")
        debug.set(false)
        verbose.set(true)
        android.set(false)
        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(false)

        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
        }
    }

    // OWASP Dependency Check
    dependencyCheck {
        autoUpdate = true
        format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL
        suppressionFile = "$rootDir/config/owasp/suppressions.xml"

        analyzers {
            experimentalEnabled = true
            archiveEnabled = true
            jarEnabled = true
            centralEnabled = true
            nexusEnabled = false
            pyDistributionEnabled = false
            pyPackageEnabled = false
            rubygemsEnabled = false
            opensslEnabled = false
            cmakeEnabled = false
            autoconfEnabled = false
            composerEnabled = false
            nodeEnabled = false
            nuspecEnabled = false
            assemblyEnabled = false
        }

        nvd {
            apiKey = System.getenv("NVD_API_KEY") ?: ""
            delay = 16000 // 16 seconds between requests
        }
    }

    // SonarQube Configuration
    sonarqube {
        properties {
            property("sonar.projectKey", "gasolinera-jsm-ultimate:${project.name}")
            property("sonar.projectName", "Gasolinera JSM - ${project.name}")
            property("sonar.sources", "src/main/kotlin,src/main/java")
            property("sonar.tests", "src/test/kotlin,src/test/java,src/integrationTest/kotlin")
            property("sonar.java.binaries", "build/classes/kotlin/main,build/classes/java/main")
            property("sonar.java.test.binaries", "build/classes/kotlin/test,build/classes/java/test")
            property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
            property("sonar.junit.reportPaths", "build/test-results/test")
            property("sonar.kotlin.detekt.reportPaths", "build/reports/detekt/detekt.xml")

            // Quality Gate thresholds
            property("sonar.coverage.exclusions", "**/config/**,**/dto/**,**/entity/**,**/*Application.kt,**/*Config.kt")
            property("sonar.cpd.exclusions", "**/dto/**,**/entity/**,**/generated/**")
            property("sonar.exclusions", "**/build/**,**/generated/**")
        }
    }

    dependencies {
        // Common dependencies for all modules
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

        // Testing dependencies
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testImplementation("io.mockk:mockk:1.13.8")
        testImplementation("com.ninja-squad:springmockk:4.0.2")
        testImplementation("org.testcontainers:junit-jupiter")
        testImplementation("org.testcontainers:postgresql")
        testImplementation("org.testcontainers:redis")
        testImplementation("org.testcontainers:rabbitmq")

        // Detekt plugins
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.4")
    }
}

// Root project SonarQube configuration
sonarqube {
    properties {
        property("sonar.projectKey", "gasolinera-jsm-ultimate")
        property("sonar.projectName", "Gasolinera JSM Ultimate")
        property("sonar.organization", "gasolinera-jsm")
        property("sonar.host.url", "https://sonarcloud.io")

        // Aggregate coverage from all modules
        property("sonar.coverage.jacoco.xmlReportPaths",
            subprojects.map { "${it.buildDir}/reports/jacoco/test/jacocoTestReport.xml" }.joinToString(","))

        // Quality Gate settings
        property("sonar.qualitygate.wait", "true")

        // Branch analysis
        property("sonar.pullrequest.provider", "github")
        property("sonar.pullrequest.github.repository", "gasolinera-jsm/gasolinera-jsm-ultimate")
    }
}

// Custom tasks for quality checks
tasks.register("qualityCheck") {
    group = "verification"
    description = "Run all quality checks"
    dependsOn(
        "ktlintCheck",
        "detekt",
        "test",
        "jacocoTestCoverageVerification",
        "dependencyCheckAnalyze"
    )
}

tasks.register("securityCheck") {
    group = "verification"
    description = "Run security vulnerability checks"
    dependsOn("dependencyCheckAnalyze")
}

tasks.register("generateQualityReports") {
    group = "reporting"
    description = "Generate all quality and security reports"
    dependsOn(
        "jacocoTestReport",
        "detekt",
        "dependencyCheckAnalyze"
    )
}

// Dependency versions management
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.1")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
        mavenBom("org.testcontainers:testcontainers-bom:1.19.3")
    }
}

// Version catalog for consistent dependency versions
val kotlinVersion = "1.9.21"
val springBootVersion = "3.2.1"
val springCloudVersion = "2023.0.0"
val testcontainersVersion = "1.19.3"
val mockkVersion = "1.13.8"
val detektVersion = "1.23.4"