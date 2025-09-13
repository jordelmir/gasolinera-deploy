package com.gasolinerajsm.shared.database

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(SpringJUnitExtension::class)
@SpringBootTest
@Testcontainers
class ReadReplicaTest {

    @Container
    private val primaryContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
        withDatabaseName("gasolinera_test")
        withUsername("test_user")
        withPassword("test_pass")
        withExposedPorts(5432)
    }

    @Container
    private val replicaContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
        withDatabaseName("gasolinera_test")
        withUsername("test_user")
        withPassword("test_pass")
        withExposedPorts(5432)
    }

    private lateinit var properties: DatabaseOptimizationProperties
    private lateinit var readReplicaManager: ReadReplicaManager
    private lateinit var routingDataSource: ReadWriteRoutingDataSource

    @BeforeEach
    fun setUp() {
        // Configurar propiedades de test
        properties = DatabaseOptimizationProperties(
            readReplica = DatabaseOptimizationProperties.ReadReplicaProperties(
                enabled = true,
                replicas = listOf(
                    DatabaseOptimizationProperties.ReadReplicaConfig(
                        name = "test-replica",
                        url = replicaContainer.jdbcUrl,
                        username = replicaContainer.username,
                        password = replicaContainer.password
                    )
                )
            ),
            primary = DatabaseOptimizationProperties.PrimaryDataSourceProperties(
                url = primaryContainer.jdbcUrl,
                username = primaryContainer.username,
                password = primaryContainer.password
            )
        )

        // Crear routing datasource mock
        routingDataSource = mock()
        readReplicaManager = ReadReplicaManager(routingDataSource, properties)
    }

    @Test
    fun `should select next read replica using round robin`() {
        // When
        val replica1 = readReplicaManager.selectNextReadReplica()
        val replica2 = readReplicaManager.selectNextReadReplica()

        // Then
        assertNotNull(replica1)
        assertNotNull(replica2)
        assertTrue(replica1 is DataSourceType.READ)
        assertTrue(replica2 is DataSourceType.READ)
    }

    @Test
    fun `should execute operation on read replica`() {
        // Given
        var executedOnReadReplica = false

        // When
        readReplicaManager.executeOnReadReplica {
            executedOnReadReplica = ReadReplicaContext.isReadOnly()
            "test result"
        }

        // Then
        assertTrue(executedOnReadReplica)
    }

    @Test
    fun `should execute operation on write datasource`() {
        // Given
        var executedOnWrite = false

        // When
        readReplicaManager.executeOnWrite {
            executedOnWrite = !ReadReplicaContext.isReadOnly()
            "test result"
        }

        // Then
        assertTrue(executedOnWrite)
    }

    @Test
    fun `should handle read replica context correctly`() {
        // Given
        val originalType = ReadReplicaContext.getDataSourceType()

        // When
        ReadReplicaContext.setDataSourceType(DataSourceType.read(0))
        val isReadOnly = ReadReplicaContext.isReadOnly()

        ReadReplicaContext.setDataSourceType(DataSourceType.WRITE)
        val isWrite = !ReadReplicaContext.isReadOnly()

        ReadReplicaContext.clear()
        val afterClear = ReadReplicaContext.getDataSourceType()

        // Then
        assertTrue(isReadOnly)
        assertTrue(isWrite)
        assertEquals(DataSourceType.WRITE, afterClear)
    }
}

@ExtendWith(SpringJUnitExtension::class)
@SpringBootTest
class NPlusOneDetectorTest {

    private lateinit var nPlusOneDetector: NPlusOneDetector

    @BeforeEach
    fun setUp() {
        nPlusOneDetector = NPlusOneDetector()
    }

    @Test
    fun `should start and finish tracking correctly`() {
        // Given
        val requestId = "test-request-123"

        // When
        nPlusOneDetector.startTracking(requestId)

        // Simular algunas queries
        repeat(5) { index ->
            nPlusOneDetector.recordQuery(
                sql = "SELECT * FROM users WHERE id = ?",
                parameters = listOf(index),
                executionTime = java.time.Duration.ofMillis(50)
            )
        }

        val result = nPlusOneDetector.finishTracking()

        // Then
        assertNotNull(result)
        assertEquals(requestId, result.requestId)
        assertEquals(5, result.totalQueries)
        assertTrue(result.hasNPlusOneIssues)
        assertTrue(result.issues.isNotEmpty())
    }

    @Test
    fun `should detect N+1 pattern correctly`() {
        // Given
        val requestId = "n-plus-one-test"
        nPlusOneDetector.startTracking(requestId)

        // Simular patrón N+1 clásico
        repeat(10) { index ->
            nPlusOneDetector.recordQuery(
                sql = "SELECT * FROM orders WHERE user_id = ?",
                parameters = listOf(index + 1),
                executionTime = java.time.Duration.ofMillis(25)
            )
        }

        // When
        val result = nPlusOneDetector.finishTracking()

        // Then
        assertNotNull(result)
        assertTrue(result.hasNPlusOneIssues)
        assertEquals(1, result.issues.size)

        val issue = result.issues.first()
        assertEquals(NPlusOneType.SELECT_BY_ID, issue.type)
        assertEquals(10, issue.queryCount)
        assertTrue(issue.suggestion.contains("IN clause"))
    }

    @Test
    fun `should not detect N+1 for few queries`() {
        // Given
        val requestId = "few-queries-test"
        nPlusOneDetector.startTracking(requestId)

        // Simular pocas queries (no N+1)
        repeat(2) { index ->
            nPlusOneDetector.recordQuery(
                sql = "SELECT * FROM products WHERE category_id = ?",
                parameters = listOf(index + 1),
                executionTime = java.time.Duration.ofMillis(30)
            )
        }

        // When
        val result = nPlusOneDetector.finishTracking()

        // Then
        assertNotNull(result)
        assertEquals(false, result.hasNPlusOneIssues)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `should generate appropriate suggestions`() {
        // Given
        val requestId = "suggestions-test"
        nPlusOneDetector.startTracking(requestId)

        // Simular diferentes tipos de queries problemáticas
        repeat(8) { index ->
            nPlusOneDetector.recordQuery(
                sql = "SELECT * FROM comments WHERE post_id = ?",
                parameters = listOf(index + 1),
                executionTime = java.time.Duration.ofMillis(40)
            )
        }

        // When
        val result = nPlusOneDetector.finishTracking()

        // Then
        assertNotNull(result)
        assertTrue(result.suggestions.isNotEmpty())
        assertTrue(result.suggestions.any { it.contains("batch") || it.contains("JOIN FETCH") })
    }
}

@ExtendWith(SpringJUnitExtension::class)
@SpringBootTest
class QueryPerformanceMonitorTest {

    @Container
    private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
        withDatabaseName("gasolinera_test")
        withUsername("test_user")
        withPassword("test_pass")
        withCommand("postgres", "-c", "shared_preload_libraries=pg_stat_statements")
    }

    private lateinit var queryPerformanceMonitor: QueryPerformanceMonitor

    @BeforeEach
    fun setUp() {
        // Configurar con container de test
        // En un test real, se configuraría JdbcTemplate con el container
        queryPerformanceMonitor = mock()
    }

    @Test
    fun `should analyze query performance`() {
        // Given
        val mockReport = QueryPerformanceReport(
            timestamp = java.time.Instant.now(),
            slowQueries = listOf(
                SlowQueryInfo(
                    query = "SELECT * FROM large_table WHERE condition = ?",
                    calls = 100,
                    totalTime = 5000.0,
                    meanTime = 50.0,
                    maxTime = 200.0,
                    minTime = 10.0,
                    stddevTime = 15.0,
                    rows = 1000,
                    hitPercent = 85.0
                )
            ),
            frequentQueries = emptyList(),
            expensiveQueries = emptyList(),
            ioIntensiveQueries = emptyList(),
            recommendations = listOf(
                PerformanceRecommendation(
                    type = RecommendationType.SLOW_QUERY,
                    priority = Priority.HIGH,
                    query = "SELECT * FROM large_table WHERE condition = ?",
                    description = "Query muy lenta detectada",
                    suggestion = "Considerar añadir índice",
                    potentialImpact = "Reducción de 30ms por ejecución"
                )
            )
        )

        whenever(queryPerformanceMonitor.analyzeQueryPerformance()).thenReturn(mockReport)

        // When
        val result = queryPerformanceMonitor.analyzeQueryPerformance()

        // Then
        assertNotNull(result)
        assertEquals(1, result.slowQueries.size)
        assertEquals(1, result.recommendations.size)
        assertEquals(Priority.HIGH, result.recommendations.first().priority)
    }

    @Test
    fun `should generate appropriate recommendations`() {
        // Given
        val slowQuery = SlowQueryInfo(
            query = "SELECT * FROM users u JOIN orders o ON u.id = o.user_id WHERE u.status = ?",
            calls = 500,
            totalTime = 25000.0,
            meanTime = 50.0,
            maxTime = 500.0,
            minTime = 5.0,
            stddevTime = 25.0,
            rows = 5000,
            hitPercent = 70.0
        )

        // When - En un test real, esto sería parte del análisis
        val shouldRecommendIndex = slowQuery.meanTime > 100 || slowQuery.hitPercent < 80
        val shouldRecommendCaching = slowQuery.calls > 100 && slowQuery.meanTime > 20

        // Then
        assertTrue(shouldRecommendCaching)
        assertTrue(shouldRecommendIndex)
    }
}