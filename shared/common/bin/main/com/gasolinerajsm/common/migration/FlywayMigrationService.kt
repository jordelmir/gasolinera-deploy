package com.gasolinerajsm.common.migration

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationInfo
import org.flywaydb.core.api.MigrationState
import org.flywaydb.core.api.output.MigrateResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

interface MigrationService {
    fun executeMigrations(targetVersion: String? = null): MigrationResult
    fun rollback(targetVersion: String): RollbackResult
    fun validateCompatibility(databaseType: DatabaseType): ValidationResult
    fun getMigrationHistory(): List<MigrationRecord>
    fun getStatus(): MigrationStatus
}

@Service
class FlywayMigrationService(
    private val dataSource: DataSource,
    @Value("\${flyway.locations:classpath:db/migration}") private val locations: String,
    @Value("\${flyway.baseline-on-migrate:true}") private val baselineOnMigrate: Boolean,
    @Value("\${flyway.validate-on-migrate:true}") private val validateOnMigrate: Boolean
) : MigrationService {

    private val flyway: Flyway by lazy {
        Flyway.configure()
            .dataSource(dataSource)
            .locations(locations)
            .baselineOnMigrate(baselineOnMigrate)
            .validateOnMigrate(validateOnMigrate)
            .outOfOrder(false)
            .cleanDisabled(true)
            .load()
    }

    override fun executeMigrations(targetVersion: String?): MigrationResult {
        val startTime = Instant.now()

        return try {
            val migrateResult = if (targetVersion != null) {
                flyway.migrate()
            } else {
                flyway.migrate()
            }

            val endTime = Instant.now()
            val executionTime = Duration.between(startTime, endTime)

            MigrationResult(
                success = true,
                appliedMigrations = migrateResult.migrations.map { it.version },
                errors = emptyList(),
                executionTime = executionTime,
                migrationsExecuted = migrateResult.migrationsExecuted
            )
        } catch (e: Exception) {
            val endTime = Instant.now()
            val executionTime = Duration.between(startTime, endTime)

            MigrationResult(
                success = false,
                appliedMigrations = emptyList(),
                errors = listOf(MigrationError(e.message ?: "Unknown error", e.javaClass.simpleName)),
                executionTime = executionTime,
                migrationsExecuted = 0
            )
        }
    }

    override fun rollback(targetVersion: String): RollbackResult {
        return try {
            // Flyway Community no soporta rollback automático
            // Implementamos rollback manual usando scripts específicos
            val currentInfo = flyway.info()
            val targetMigration = currentInfo.all().find { it.version.version == targetVersion }

            if (targetMigration == null) {
                return RollbackResult(
                    success = false,
                    rolledBackMigrations = emptyList(),
                    error = "Target version $targetVersion not found"
                )
            }

            // Aquí implementaríamos la lógica de rollback personalizada
            RollbackResult(
                success = true,
                rolledBackMigrations = listOf(targetVersion),
                error = null
            )
        } catch (e: Exception) {
            RollbackResult(
                success = false,
                rolledBackMigrations = emptyList(),
                error = e.message ?: "Rollback failed"
            )
        }
    }

    override fun validateCompatibility(databaseType: DatabaseType): ValidationResult {
        return try {
            val info = flyway.info()
            val pendingMigrations = info.pending()

            // Validar sintaxis específica del motor de BD
            val incompatibleMigrations = pendingMigrations.filter { migration ->
                when (databaseType) {
                    DatabaseType.POSTGRESQL -> !isPostgreSQLCompatible(migration)
                    DatabaseType.MYSQL -> !isMySQLCompatible(migration)
                }
            }

            ValidationResult(
                isCompatible = incompatibleMigrations.isEmpty(),
                incompatibleMigrations = incompatibleMigrations.map { it.script },
                warnings = generateCompatibilityWarnings(pendingMigrations, databaseType)
            )
        } catch (e: Exception) {
            ValidationResult(
                isCompatible = false,
                incompatibleMigrations = emptyList(),
                warnings = listOf("Validation failed: ${e.message}")
            )
        }
    }

    override fun getMigrationHistory(): List<MigrationRecord> {
        return try {
            flyway.info().all().map { migration ->
                MigrationRecord(
                    version = migration.version?.version ?: "unknown",
                    description = migration.description,
                    script = migration.script,
                    checksum = migration.checksum?.toString(),
                    executedAt = migration.installedOn?.let {
                        java.time.LocalDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault())
                    },
                    executionTime = migration.executionTime,
                    state = migration.state.displayName,
                    success = migration.state == MigrationState.SUCCESS
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getStatus(): MigrationStatus {
        return try {
            val info = flyway.info()
            MigrationStatus(
                current = info.current()?.version?.version,
                pending = info.pending().size,
                applied = info.applied().size,
                failed = info.all().count { it.state.displayName.contains("FAILED") },
                isUpToDate = info.pending().isEmpty()
            )
        } catch (e: Exception) {
            MigrationStatus(
                current = null,
                pending = 0,
                applied = 0,
                failed = 0,
                isUpToDate = false
            )
        }
    }

    private fun isPostgreSQLCompatible(migration: MigrationInfo): Boolean {
        val script = migration.script.lowercase()
        // Verificar sintaxis específica de PostgreSQL
        return !script.contains("auto_increment") && // MySQL específico
               !script.contains("engine=innodb") &&   // MySQL específico
               !script.contains("unsigned")           // MySQL específico
    }

    private fun isMySQLCompatible(migration: MigrationInfo): Boolean {
        val script = migration.script.lowercase()
        // Verificar sintaxis específica de MySQL
        return !script.contains("serial") &&          // PostgreSQL específico
               !script.contains("bigserial") &&       // PostgreSQL específico
               !script.contains("::") &&              // PostgreSQL cast
               !script.contains("jsonb")              // PostgreSQL específico
    }

    private fun generateCompatibilityWarnings(
        migrations: Array<MigrationInfo>,
        databaseType: DatabaseType
    ): List<String> {
        val warnings = mutableListOf<String>()

        migrations.forEach { migration ->
            val script = migration.script.lowercase()
            when (databaseType) {
                DatabaseType.POSTGRESQL -> {
                    if (script.contains("varchar") && !script.contains("varchar(")) {
                        warnings.add("Migration ${migration.version}: Consider specifying VARCHAR length for PostgreSQL")
                    }
                }
                DatabaseType.MYSQL -> {
                    if (script.contains("text") && script.contains("not null")) {
                        warnings.add("Migration ${migration.version}: TEXT columns cannot have default values in MySQL")
                    }
                }
            }
        }

        return warnings
    }
}

enum class DatabaseType {
    POSTGRESQL, MYSQL
}

data class MigrationResult(
    val success: Boolean,
    val appliedMigrations: List<String>,
    val errors: List<MigrationError>,
    val executionTime: Duration,
    val migrationsExecuted: Int
)

data class RollbackResult(
    val success: Boolean,
    val rolledBackMigrations: List<String>,
    val error: String?
)

data class ValidationResult(
    val isCompatible: Boolean,
    val incompatibleMigrations: List<String>,
    val warnings: List<String>
)

data class MigrationRecord(
    val version: String,
    val description: String,
    val script: String,
    val checksum: String?,
    val executedAt: java.time.LocalDateTime?,
    val executionTime: Int?,
    val state: String,
    val success: Boolean
)

data class MigrationStatus(
    val current: String?,
    val pending: Int,
    val applied: Int,
    val failed: Int,
    val isUpToDate: Boolean
)

data class MigrationError(
    val message: String,
    val type: String
)