package com.gasolinerajsm.common.migration

import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class MigrationGenerator {

    fun generateMigration(
        name: String,
        description: String,
        databaseType: DatabaseType = DatabaseType.POSTGRESQL
    ): GeneratedMigration {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val version = getNextVersion()
        val fileName = "V${version}__${name.replace(" ", "_")}.sql"

        val template = when (databaseType) {
            DatabaseType.POSTGRESQL -> generatePostgreSQLTemplate(version, name, description)
            DatabaseType.MYSQL -> generateMySQLTemplate(version, name, description)
        }

        return GeneratedMigration(
            fileName = fileName,
            version = version,
            content = template,
            rollbackContent = generateRollbackTemplate(version, name, databaseType)
        )
    }

    fun generateRollbackScript(migrationVersion: String, databaseType: DatabaseType): String {
        return when (databaseType) {
            DatabaseType.POSTGRESQL -> generatePostgreSQLRollback(migrationVersion)
            DatabaseType.MYSQL -> generateMySQLRollback(migrationVersion)
        }
    }

    fun validateMigrationSyntax(content: String, databaseType: DatabaseType): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validaciones básicas de sintaxis
        if (!content.trim().endsWith(";")) {
            warnings.add("Migration should end with semicolon")
        }

        // Validaciones específicas por motor de BD
        when (databaseType) {
            DatabaseType.POSTGRESQL -> validatePostgreSQLSyntax(content, errors, warnings)
            DatabaseType.MYSQL -> validateMySQLSyntax(content, errors, warnings)
        }

        return ValidationResult(
            isCompatible = errors.isEmpty(),
            incompatibleMigrations = errors,
            warnings = warnings
        )
    }

    private fun generatePostgreSQLTemplate(version: String, name: String, description: String): String {
        return """
-- Migración V$version: $name
-- Descripción: $description
-- Autor: Sistema Gasolinera JSM
-- Fecha: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}
-- Compatibilidad: PostgreSQL 12+

-- TODO: Implementar migración aquí
-- Ejemplo:
-- CREATE TABLE example_table (
--     id BIGSERIAL PRIMARY KEY,
--     name VARCHAR(255) NOT NULL,
--     created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
-- );

-- Índices
-- CREATE INDEX idx_example_name ON example_table(name);

-- Datos iniciales (si es necesario)
-- INSERT INTO example_table (name) VALUES ('Initial data');
        """.trimIndent()
    }

    private fun generateMySQLTemplate(version: String, name: String, description: String): String {
        return """
-- Migración V$version: $name
-- Descripción: $description
-- Autor: Sistema Gasolinera JSM
-- Fecha: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}
-- Compatibilidad: MySQL 8.0+

-- TODO: Implementar migración aquí
-- Ejemplo:
-- CREATE TABLE example_table (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     name VARCHAR(255) NOT NULL,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     INDEX idx_example_name (name)
-- ) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Datos iniciales (si es necesario)
-- INSERT INTO example_table (name) VALUES ('Initial data');
        """.trimIndent()
    }

    private fun generateRollbackTemplate(version: String, name: String, databaseType: DatabaseType): String {
        return when (databaseType) {
            DatabaseType.POSTGRESQL -> generatePostgreSQLRollback(version)
            DatabaseType.MYSQL -> generateMySQLRollback(version)
        }
    }

    private fun generatePostgreSQLRollback(version: String): String {
        return """
-- Rollback V$version
-- Autor: Sistema Gasolinera JSM
-- Fecha: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}

-- TODO: Implementar rollback aquí
-- IMPORTANTE: Las operaciones deben ser en orden inverso a la migración
-- Ejemplo:
-- DROP INDEX IF EXISTS idx_example_name;
-- DROP TABLE IF EXISTS example_table;
        """.trimIndent()
    }

    private fun generateMySQLRollback(version: String): String {
        return """
-- Rollback V$version
-- Autor: Sistema Gasolinera JSM
-- Fecha: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}

-- TODO: Implementar rollback aquí
-- IMPORTANTE: Las operaciones deben ser en orden inverso a la migración
-- Ejemplo:
-- DROP TABLE IF EXISTS example_table;
        """.trimIndent()
    }

    private fun validatePostgreSQLSyntax(content: String, errors: MutableList<String>, warnings: MutableList<String>) {
        val lowerContent = content.lowercase()

        // Verificar uso de características específicas de PostgreSQL
        if (lowerContent.contains("auto_increment")) {
            errors.add("Use SERIAL or BIGSERIAL instead of AUTO_INCREMENT in PostgreSQL")
        }

        if (lowerContent.contains("engine=")) {
            errors.add("ENGINE clause is MySQL specific, not supported in PostgreSQL")
        }

        if (lowerContent.contains("unsigned")) {
            errors.add("UNSIGNED is MySQL specific, not supported in PostgreSQL")
        }

        // Advertencias
        if (lowerContent.contains("varchar") && !lowerContent.contains("varchar(")) {
            warnings.add("Consider specifying VARCHAR length for better compatibility")
        }
    }

    private fun validateMySQLSyntax(content: String, errors: MutableList<String>, warnings: MutableList<String>) {
        val lowerContent = content.lowercase()

        // Verificar uso de características específicas de MySQL
        if (lowerContent.contains("serial") || lowerContent.contains("bigserial")) {
            errors.add("Use AUTO_INCREMENT instead of SERIAL in MySQL")
        }

        if (lowerContent.contains("jsonb")) {
            errors.add("Use JSON instead of JSONB in MySQL")
        }

        if (lowerContent.contains("::")) {
            errors.add("PostgreSQL-style casting (::) not supported in MySQL, use CAST() function")
        }

        // Advertencias
        if (lowerContent.contains("text") && lowerContent.contains("not null") && !lowerContent.contains("default")) {
            warnings.add("TEXT columns with NOT NULL should have a default value in MySQL")
        }
    }

    private fun getNextVersion(): String {
        // En una implementación real, esto consultaría la base de datos o el sistema de archivos
        // para determinar el siguiente número de versión
        val currentTime = System.currentTimeMillis()
        return (currentTime % 10000).toString().padStart(4, '0')
    }
}

data class GeneratedMigration(
    val fileName: String,
    val version: String,
    val content: String,
    val rollbackContent: String
)