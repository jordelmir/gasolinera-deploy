plugins {
    kotlin("jvm")
    id("me.champeau.jmh") version "0.7.2"
}

dependencies {
    // JMH dependencies
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    // Kotlin support
    jmh("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    jmh("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Spring Boot for realistic testing
    jmh("org.springframework.boot:spring-boot-starter:3.2.1")
    jmh("org.springframework.boot:spring-boot-starter-data-jpa:3.2.1")
    jmh("org.springframework.boot:spring-boot-starter-data-redis:3.2.1")

    // Database drivers
    jmh("org.postgresql:postgresql:42.7.1")

    // JSON processing
    jmh("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")

    // Testing utilities
    jmh("org.testcontainers:postgresql:1.19.3")
    jmh("org.testcontainers:redis:1.19.3")

    // Project modules
    jmh(project(":shared:monitoring"))
    jmh(project(":coupon-service"))
    jmh(project(":auth-service"))
    jmh(project(":station-service"))
}

jmh {
    jmhVersion.set("1.37")

    // Benchmark configuration for world-class performance testing
    iterations.set(5)
    warmupIterations.set(3)
    fork.set(2)
    threads.set(1)

    // Output configuration
    humanOutputFile.set(project.file("${project.buildDir}/reports/jmh/human.txt"))
    resultsFile.set(project.file("${project.buildDir}/reports/jmh/results.json"))

    // JVM arguments for optimal performance
    jvmArgs.addAll(listOf(
        "-Xms2g",
        "-Xmx4g",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=200",
        "-XX:+UseStringDeduplication",
        "-XX:+OptimizeStringConcat",
        "-server"
    ))

    // Profilers for detailed analysis
    profilers.addAll(listOf(
        "gc",           // GC profiling
        "stack",        // Stack profiling
        "perf",         // Linux perf profiling (if available)
        "async"         // Async profiler (if available)
    ))

    // Include/exclude patterns
    includes.addAll(listOf(
        ".*CouponServiceBenchmark.*",
        ".*AuthServiceBenchmark.*",
        ".*StationServiceBenchmark.*",
        ".*DatabaseBenchmark.*",
        ".*CacheBenchmark.*"
    ))
}

tasks.jmh {
    // Ensure clean state for benchmarks
    dependsOn("clean")

    // Generate reports
    finalizedBy("jmhReport")
}

tasks.register("jmhReport") {
    group = "reporting"
    description = "Generate JMH benchmark reports"

    doLast {
        val resultsFile = file("${project.buildDir}/reports/jmh/results.json")
        val reportDir = file("${project.buildDir}/reports/jmh")

        if (resultsFile.exists()) {
            println("JMH Benchmark Results:")
            println("- Human readable: ${reportDir}/human.txt")
            println("- JSON results: ${reportDir}/results.json")
            println("- HTML report: ${reportDir}/index.html")
        }
    }
}

// Task to run specific benchmark suites
tasks.register("benchmarkCouponService", JavaExec::class) {
    group = "benchmark"
    description = "Run Coupon Service benchmarks"

    classpath = sourceSets["jmh"].runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    args = listOf(
        ".*CouponServiceBenchmark.*",
        "-rf", "json",
        "-rff", "${project.buildDir}/reports/jmh/coupon-service-results.json"
    )
}

tasks.register("benchmarkAuthService", JavaExec::class) {
    group = "benchmark"
    description = "Run Auth Service benchmarks"

    classpath = sourceSets["jmh"].runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    args = listOf(
        ".*AuthServiceBenchmark.*",
        "-rf", "json",
        "-rff", "${project.buildDir}/reports/jmh/auth-service-results.json"
    )
}

tasks.register("benchmarkDatabase", JavaExec::class) {
    group = "benchmark"
    description = "Run Database benchmarks"

    classpath = sourceSets["jmh"].runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    args = listOf(
        ".*DatabaseBenchmark.*",
        "-rf", "json",
        "-rff", "${project.buildDir}/reports/jmh/database-results.json"
    )
}

tasks.register("benchmarkCache", JavaExec::class) {
    group = "benchmark"
    description = "Run Cache benchmarks"

    classpath = sourceSets["jmh"].runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    args = listOf(
        ".*CacheBenchmark.*",
        "-rf", "json",
        "-rff", "${project.buildDir}/reports/jmh/cache-results.json"
    )
}

tasks.register("benchmarkAll") {
    group = "benchmark"
    description = "Run all benchmark suites"

    dependsOn(
        "benchmarkCouponService",
        "benchmarkAuthService",
        "benchmarkDatabase",
        "benchmarkCache"
    )
}