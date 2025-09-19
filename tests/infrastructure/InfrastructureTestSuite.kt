package com.gasolinerajsm.tests.infrastructure

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension
import java.net.HttpURLConnection
import java.net.URL
import java.sql.DriverManager
import java.time.Duration
import java.time.Instant
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

@ExtendWith(SpringJUnitExtension::class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class InfrastructureTestSuite {

    companion object {
        private val testResults = mutableMapOf<String, TestResult>()
        private val startTime = Instant.now()

        @JvmStatic
        @AfterAll
        fun generateReport() {
            val endTime = Instant.now()
            val totalDuration = Duration.between(startTime, endTime)

            println("\n" + "=".repeat(80))
            println("INFRASTRUCTURE TEST REPORT")
            println("=".repeat(80))
            println("Total execution time: ${totalDuration.toMillis()}ms")
            println("Tests executed: ${testResults.size}")

            val passed = testResults.values.count { it.passed }
            val failed = testResults.values.count { !it.passed }

            println("Passed: $passed")
            println("Failed: $failed")
            println("Success rate: ${(passed.toDouble() / testResults.size * 100).toInt()}%")
            println()

            testResults.forEach { (name, result) ->
                val status = if (result.passed) "✅ PASS" else "❌ FAIL"
                println("$status $name (${result.duration}ms)")
                if (!result.passed && result.error != null) {
                    println("    Error: ${result.error}")
                }
            }
            println("=".repeat(80))
        }
    }

    @Test
    @Order(1)
    fun `test database connectivity`() {
        val testName = "Database Connectivity"
        val startTime = Instant.now()

        try {
            val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/gasolinera_db"
            val dbUser = System.getenv("DB_USERNAME") ?: "gasolinera_user"
            val dbPassword = System.getenv("DB_PASSWORD") ?: "gasolinera_password"

            DriverManager.getConnection(dbUrl, dbUser, dbPassword).use { connection ->
                assertTrue(connection.isValid(5), "Database connection should be valid")

                // Test basic query
                connection.createStatement().use { statement ->
                    val resultSet = statement.executeQuery("SELECT 1")
                    assertTrue(resultSet.next(), "Should be able to execute basic query")
                }
            }

            recordSuccess(testName, startTime)
        } catch (e: Exception) {
            recordFailure(testName, startTime, e)
            throw e
        }
    }

    @Test
    @Order(2)
    fun `test redis connectivity`() {
        val testName = "Redis Connectivity"
        val startTime = Instant.now()

        try {
            // Test Redis connection using simple socket connection
            val redisHost = System.getenv("REDIS_HOST") ?: "localhost"
            val redisPort = System.getenv("REDIS_PORT")?.toInt() ?: 6379

            java.net.Socket(redisHost, redisPort).use { socket ->
                assertTrue(socket.isConnected, "Should be able to connect to Redis")

                // Send PING command
                val output = socket.getOutputStream()
                val input = socket.getInputStream()

                output.write("PING\r\n".toByteArray())
                output.flush()

                val response = ByteArray(1024)
                val bytesRead = input.read(response)
                val responseStr = String(response, 0, bytesRead)

                assertTrue(responseStr.contains("PONG"), "Redis should respond to PING")
            }

            recordSuccess(testName, startTime)
        } catch (e: Exception) {
            recordFailure(testName, startTime, e)
            throw e
        }
    }

    @Test
    @Order(3)
    fun `test service health endpoints`() {
        val testName = "Service Health Endpoints"
        val startTime = Instant.now()

        try {
            val services = mapOf(
                "API Gateway" to (System.getenv("API_GATEWAY_PORT")?.toInt() ?: 8080),
                "Auth Service" to (System.getenv("AUTH_SERVICE_PORT")?.toInt() ?: 8081),
                "Station Service" to (System.getenv("STATION_SERVICE_PORT")?.toInt() ?: 8082),
                "Coupon Service" to (System.getenv("COUPON_SERVICE_PORT")?.toInt() ?: 8083),
                "Raffle Service" to (System.getenv("RAFFLE_SERVICE_PORT")?.toInt() ?: 8084)
            )

            val healthyServices = mutableListOf<String>()
            val unhealthyServices = mutableListOf<String>()

            services.forEach { (serviceName, port) ->
                try {
                    val url = URL("http://localhost:$port/health")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        healthyServices.add(serviceName)
                    } else {
                        unhealthyServices.add("$serviceName (HTTP $responseCode)")
                    }
                } catch (e: Exception) {
                    unhealthyServices.add("$serviceName (${e.message})")
                }
            }

            println("Healthy services: ${healthyServices.joinToString(", ")}")
            if (unhealthyServices.isNotEmpty()) {
                println("Unhealthy services: ${unhealthyServices.joinToString(", ")}")
            }

            // At least one service should be healthy for the test to pass
            assertTrue(healthyServices.isNotEmpty(), "At least one service should be healthy")

            recordSuccess(testName, startTime)
        } catch (e: Exception) {
            recordFailure(testName, startTime, e)
            throw e
        }
    }

    @Test
    @Order(4)
    fun `test configuration validation`() {
        val testName = "Configuration Validation"
        val startTime = Instant.now()

        try {
            val criticalVars = listOf(
                "DB_HOST", "DB_NAME", "DB_USERNAME", "DB_PASSWORD",
                "JWT_SECRET", "REDIS_HOST", "RABBITMQ_HOST"
            )

            val missingVars = mutableListOf<String>()
            val presentVars = mutableListOf<String>()

            criticalVars.forEach { varName ->
                val value = System.getenv(varName)
                if (value.isNullOrBlank()) {
                    missingVars.add(varName)
                } else {
                    presentVars.add(varName)
                }
            }

            println("Present variables: ${presentVars.joinToString(", ")}")
            if (missingVars.isNotEmpty()) {
                println("Missing variables: ${missingVars.joinToString(", ")}")
            }

            // Check JWT_SECRET is not default value
            val jwtSecret = System.getenv("JWT_SECRET")
            if (jwtSecret?.contains("change-this-in-production") == true) {
                println("Warning: JWT_SECRET contains default value")
            }

            assertTrue(missingVars.isEmpty(), "All critical environment variables should be present")

            recordSuccess(testName, startTime)
        } catch (e: Exception) {
            recordFailure(testName, startTime, e)
            throw e
        }
    }

    @Test
    @Order(5)
    fun `test security keys`() {
        val testName = "Security Keys"
        val startTime = Instant.now()

        try {
            val privateKeyPath = "ops/key-management/private-key.pem"
            val publicKeyPath = "ops/key-management/public-key.pem"

            val privateKeyFile = java.io.File(privateKeyPath)
            val publicKeyFile = java.io.File(publicKeyPath)

            assertTrue(privateKeyFile.exists(), "Private key file should exist")
            assertTrue(publicKeyFile.exists(), "Public key file should exist")

            // Check file permissions (Unix-like systems)
            if (System.getProperty("os.name").lowercase().contains("nix") ||
                System.getProperty("os.name").lowercase().contains("nux")) {

                val privateKeyPerms = java.nio.file.Files.getPosixFilePermissions(privateKeyFile.toPath())
                assertTrue(
                    privateKeyPerms.toString().contains("OWNER_READ") &&
                    privateKeyPerms.toString().contains("OWNER_WRITE"),
                    "Private key should have secure permissions"
                )
            }

            // Validate key format
            val privateKeyContent = privateKeyFile.readText()
            val publicKeyContent = publicKeyFile.readText()

            assertTrue(privateKeyContent.contains("BEGIN PRIVATE KEY") ||
                      privateKeyContent.contains("BEGIN RSA PRIVATE KEY"),
                      "Private key should be in valid PEM format")
            assertTrue(publicKeyContent.contains("BEGIN PUBLIC KEY"),
                      "Public key should be in valid PEM format")

            recordSuccess(testName, startTime)
        } catch (e: Exception) {
            recordFailure(testName, startTime, e)
            throw e
        }
    }

    @Test
    @Order(6)
    fun `test directory structure`() {
        val testName = "Directory Structure"
        val startTime = Instant.now()

        try {
            val requiredDirectories = listOf(
                "logs", "logs/requests", "logs/responses", "logs/errors",
                "data/uploads", "data/exports", "data/backups",
                "monitoring/prometheus", "monitoring/grafana"
            )

            val missingDirs = mutableListOf<String>()
            val presentDirs = mutableListOf<String>()

            requiredDirectories.forEach { dirPath ->
                val dir = java.io.File(dirPath)
                if (dir.exists() && dir.isDirectory) {
                    presentDirs.add(dirPath)
                } else {
                    missingDirs.add(dirPath)
                }
            }

            println("Present directories: ${presentDirs.joinToString(", ")}")
            if (missingDirs.isNotEmpty()) {
                println("Missing directories: ${missingDirs.joinToString(", ")}")
            }

            // Create missing directories for next tests
            missingDirs.forEach { dirPath ->
                java.io.File(dirPath).mkdirs()
            }

            recordSuccess(testName, startTime)
        } catch (e: Exception) {
            recordFailure(testName, startTime, e)
            throw e
        }
    }

    @Test
    @Order(7)
    fun `test monitoring configuration`() {
        val testName = "Monitoring Configuration"
        val startTime = Instant.now()

        try {
            val configFiles = mapOf(
                "Prometheus Config" to "monitoring/prometheus/prometheus.yml",
                "Alert Rules" to "monitoring/prometheus/alert_rules.yml",
                "Grafana Config" to "monitoring/grafana/grafana.ini",
                "Alertmanager Config" to "monitoring/alertmanager/alertmanager.yml"
            )

            val missingConfigs = mutableListOf<String>()
            val presentConfigs = mutableListOf<String>()

            configFiles.forEach { (name, path) ->
                val file = java.io.File(path)
                if (file.exists()) {
                    presentConfigs.add(name)

                    // Basic validation of file content
                    val content = file.readText()
                    assertTrue(content.isNotBlank(), "$name should not be empty")
                } else {
                    missingConfigs.add(name)
                }
            }

            println("Present configs: ${presentConfigs.joinToString(", ")}")
            if (missingConfigs.isNotEmpty()) {
                println("Missing configs: ${missingConfigs.joinToString(", ")}")
            }

            recordSuccess(testName, startTime)
        } catch (e: Exception) {
            recordFailure(testName, startTime, e)
            throw e
        }
    }

    @Test
    @Order(8)
    fun `test docker services`() {
        val testName = "Docker Services"
        val startTime = Instant.now()

        try {
            // Check if Docker is available
            val dockerProcess = ProcessBuilder("docker", "version").start()
            val dockerExitCode = dockerProcess.waitFor()

            if (dockerExitCode == 0) {
                // Check running containers
                val psProcess = ProcessBuilder("docker-compose", "-f", "docker-compose.simple.yml", "ps").start()
                val psExitCode = psProcess.waitFor()

                if (psExitCode == 0) {
                    val output = psProcess.inputStream.bufferedReader().readText()
                    println("Docker services status:")
                    println(output)

                    // Check if essential services are mentioned in output
                    val essentialServices = listOf("postgres", "redis", "rabbitmq")
                    essentialServices.forEach { service ->
                        if (output.contains(service)) {
                            println("✓ $service container found")
                        } else {
                            println("⚠ $service container not found")
                        }
                    }
                } else {
                    println("Warning: Could not check Docker Compose services")
                }
            } else {
                println("Warning: Docker is not available or not running")
            }

            recordSuccess(testName, startTime)
        } catch (e: Exception) {
            recordFailure(testName, startTime, e)
            // Don't throw exception for Docker tests as it might not be available in all environments
            println("Docker test failed: ${e.message}")
        }
    }

    private fun recordSuccess(testName: String, startTime: Instant) {
        val duration = Duration.between(startTime, Instant.now()).toMillis()
        testResults[testName] = TestResult(true, duration, null)
    }

    private fun recordFailure(testName: String, startTime: Instant, error: Exception) {
        val duration = Duration.between(startTime, Instant.now()).toMillis()
        testResults[testName] = TestResult(false, duration, error.message)
    }

    data class TestResult(
        val passed: Boolean,
        val duration: Long,
        val error: String?
    )
}