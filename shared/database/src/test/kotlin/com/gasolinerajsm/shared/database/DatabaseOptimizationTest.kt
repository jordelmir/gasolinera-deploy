package com.gasolinerajsm.shared.database

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.sql.DataSource
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

@SpringBootTest
@Testcontainers
class DatabaseOptimizationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_gasolinera")
            withUsername("test_user")
            withPassword("test_pass")
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("gasolinera.database.optimization.url") { postgres.jdbcUrl }
            registry.add("gasolinera.database.optimization.username") { postgres.username }
            registry.add("gasolinera.database.optimization.password") { postgres.password }
        }
    }

    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var databaseAnalyzer: DatabaseAnalyzer
    private lateinit var indexOptimizer: IndexOptimizer
    private lateinit var queryOptimizer: QueryOptimizer
    private lateinit var partitionManager: PartitionManager
    private lateinit var properties: DatabaseOptimizationProperties

    @BeforeEach
    fun setup() {
        properties = DatabaseOptimizationProperties().apply {
            url = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        }

        // Create test data source
        val config = com.zaxxer.hikari.HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            driverClassName = "org.postgresql.Driver"
        }
        val dataSource = com.zaxxer.hikari.HikariDataSource(config)

        jdbcTemplate = JdbcTemplate(dataSource)
        databaseAnalyzer = DatabaseAnalyzer(jdbcTemplate, properties)
        indexOptimizer = IndexOptimizer(jdbcTemplate, properties)
        queryOptimizer = QueryOptimizer(jdbcTemplate, properties)
        partitionManager = PartitionManager(jdbcTemplate, properties)

        // Create test tables
        setupTestTables()
    }

    private fun setupTestTables() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS test_users (
                id SERIAL PRIMARY KEY,
                email VARCHAR(255) UNIQUE NOT NULL,
                name VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT NOW()
            )
        """)

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS test_coupons (
                id SERIAL PRIMARY KEY,
                user_id INTEGER REFERENCES test_users(id),
                code VARCHAR(50) UNIQUE NOT NULL,
                status VARCHAR(20) DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT NOW()
            )
        """)

        // Insert test data
        jdbcTemplate.execute("""
            INSERT INTO test_users (email, name) VALUES
            ('user1@test.com', 'User 1'),
            ('user2@test.com', 'User 2'),
            ('user3@test.com', 'User 3')
            ON CONFLICT (email) DO NOTHING
        """)

        jdbcTemplate.execute("""
            INSERT INTO test_coupons (user_id, code) VALUES
            (1, 'COUPON001'),
            (1, 'COUPON002'),
            (2, 'COUPON003')
            ON CONFLICT (code) DO NOTHING
        """)
    }

    @Test
    fun `should analyze database performance`() {
        // When
        val analysis = databaseAnalyzer.analyzeDatabase()

        // Then
        assertNotNull(analysis)
        assertTrue(analysis.tables.isNotEmpty())
        assertTrue(analysis.totalSize > 0)
    }

    @Test
    fun `should analyze table statistics`() {
        // When
        val stats = databaseAnalyzer.analyzeTableStatistics("test_users")

        // Then
        assertNotNull(stats)
        assertTrue(stats.rowCount >= 0)
        assertTrue(stats.tableSize >= 0)
    }

    @Test
    fun `should identify slow queries`() {
        // Given - Execute a potentially slow query
        jdbcTemplate.queryForList("""
            SELECT u.*, c.*
            FROM test_users u
            LEFT JOIN test_coupons c ON u.id = c.user_id
            WHERE u.name LIKE '%User%'
        """)

        // When
        val slowQueries = databaseAnalyzer.identifySlowQueries()

        // Then
        assertNotNull(slowQueries)
        // Note: In a real test environment, we might not have slow queries
        // This test mainly verifies the function doesn't throw exceptions
    }

    @Test
    fun `should suggest missing indexes`() {
        // When
        val suggestions = indexOptimizer.suggestMissingIndexes()

        // Then
        assertNotNull(suggestions)
        // The function should return suggestions or empty list without errors
    }

    @Test
    fun `should analyze existing indexes`() {
        // When
        val indexAnalysis = indexOptimizer.analyzeExistingIndexes()

        // Then
        assertNotNull(indexAnalysis)
        assertTrue(indexAnalysis.isNotEmpty())
    }

    @Test
    fun `should identify unused indexes`() {
        // When
        val unusedIndexes = indexOptimizer.identifyUnusedIndexes()

        // Then
        assertNotNull(unusedIndexes)
        // Should return list (may be empty in test environment)
    }

    @Test
    fun `should optimize query execution plan`() {
        // Given
        val testQuery = "SELECT * FROM test_users WHERE email = ?"

        // When
        val optimization = queryOptimizer.optimizeQuery(testQuery, arrayOf("user1@test.com"))

        // Then
        assertNotNull(optimization)
        assertNotNull(optimization.originalPlan)
        assertNotNull(optimization.suggestions)
    }

    @Test
    fun `should analyze query performance`() {
        // Given
        val testQuery = "SELECT COUNT(*) FROM test_users"

        // When
        val performance = queryOptimizer.analyzeQueryPerformance(testQuery)

        // Then
        assertNotNull(performance)
        assertTrue(performance.executionTime >= 0)
    }

    @Test
    fun `should create partition if not exists`() {
        // Given
        val tableName = "test_partitioned_table"
        val partitionName = "test_partition_2024_01"

        // Create partitioned table first
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS $tableName (
                id SERIAL,
                created_at TIMESTAMP DEFAULT NOW()
            ) PARTITION BY RANGE (created_at)
        """)

        // When
        val created = partitionManager.createPartitionIfNotExists(
            tableName,
            partitionName,
            "2024-01-01",
            "2024-02-01"
        )

        // Then
        assertTrue(created)

        // Verify partition exists
        val partitionExists = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM information_schema.tables
            WHERE table_name = ? AND table_schema = 'public'
        """, Int::class.java, partitionName)

        assertTrue(partitionExists > 0)
    }

    @Test
    fun `should get partition information`() {
        // When
        val partitions = partitionManager.getPartitionInfo("test_users")

        // Then
        assertNotNull(partitions)
        // test_users is not partitioned, so should return empty list
        assertTrue(partitions.isEmpty())
    }

    @Test
    fun `should manage partition maintenance`() {
        // When & Then - Should not throw exception
        partitionManager.performPartitionMaintenance()
    }

    @Test
    fun `should update table statistics`() {
        // When & Then - Should not throw exception
        databaseAnalyzer.updateTableStatistics("test_users")
    }

    @Test
    fun `should get connection pool metrics`() {
        // When
        val metrics = databaseAnalyzer.getConnectionPoolMetrics()

        // Then
        assertNotNull(metrics)
        assertTrue(metrics.activeConnections >= 0)
        assertTrue(metrics.totalConnections >= 0)
    }

    @Test
    fun `should check database health`() {
        // When
        val health = databaseAnalyzer.checkDatabaseHealth()

        // Then
        assertNotNull(health)
        assertNotNull(health.status)
        assertTrue(health.responseTime >= 0)
    }
}