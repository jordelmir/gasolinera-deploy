package com.gasolinerajsm.common.migration

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class FlywayConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(name = ["flyway.enabled"], havingValue = "true", matchIfMissing = true)
    fun flyway(
        dataSource: DataSource,
        @Value("\${flyway.locations:classpath:db/migration}") locations: String,
        @Value("\${flyway.baseline-on-migrate:true}") baselineOnMigrate: Boolean,
        @Value("\${flyway.validate-on-migrate:true}") validateOnMigrate: Boolean,
        @Value("\${flyway.clean-disabled:true}") cleanDisabled: Boolean,
        @Value("\${flyway.out-of-order:false}") outOfOrder: Boolean,
        @Value("\${flyway.baseline-version:1}") baselineVersion: String,
        @Value("\${flyway.baseline-description:Initial version}") baselineDescription: String,
        @Value("\${flyway.table:flyway_schema_history}") table: String,
        @Value("\${flyway.schemas:}") schemas: String
    ): Flyway {
        val flywayBuilder = Flyway.configure()
            .dataSource(dataSource)
            .locations(locations)
            .baselineOnMigrate(baselineOnMigrate)
            .validateOnMigrate(validateOnMigrate)
            .cleanDisabled(cleanDisabled)
            .outOfOrder(outOfOrder)
            .baselineVersion(baselineVersion)
            .baselineDescription(baselineDescription)
            .table(table)

        // Configurar esquemas si están especificados
        if (schemas.isNotBlank()) {
            flywayBuilder.schemas(*schemas.split(",").map { it.trim() }.toTypedArray())
        }

        return flywayBuilder.load()
    }

    @Bean
    @ConditionalOnProperty(name = ["flyway.postgresql.enabled"], havingValue = "true")
    fun postgreSQLFlyway(
        dataSource: DataSource,
        @Value("\${flyway.postgresql.locations:classpath:db/migration/postgresql}") locations: String
    ): Flyway {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations(locations)
            .baselineOnMigrate(true)
            .validateOnMigrate(true)
            .cleanDisabled(true)
            .outOfOrder(false)
            .table("flyway_schema_history_postgresql")
            .load()
    }

    @Bean
    @ConditionalOnProperty(name = ["flyway.mysql.enabled"], havingValue = "true")
    fun mySQLFlyway(
        dataSource: DataSource,
        @Value("\${flyway.mysql.locations:classpath:db/migration/mysql}") locations: String
    ): Flyway {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations(locations)
            .baselineOnMigrate(true)
            .validateOnMigrate(true)
            .cleanDisabled(true)
            .outOfOrder(false)
            .table("flyway_schema_history_mysql")
            .load()
    }
}