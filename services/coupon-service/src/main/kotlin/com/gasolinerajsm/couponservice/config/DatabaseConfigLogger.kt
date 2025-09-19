package com.gasolinerajsm.couponservice.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

@Component
class DatabaseConfigLogger {

    private val logger = LoggerFactory.getLogger(DatabaseConfigLogger::class.java)

    @Value("\${spring.datasource.url}")
    private lateinit var datasourceUrl: String

    @Value("\${spring.datasource.username}")
    private lateinit var datasourceUsername: String

    @Value("\${POSTGRES_DB:coupon_db}")
    private lateinit var postgresDb: String

    @Value("\${POSTGRES_HOST:localhost}")
    private lateinit var postgresHost: String

    @Value("\${POSTGRES_PORT:5432}")
    private lateinit var postgresPort: String

    @PostConstruct
    fun logDatabaseConfiguration() {
        logger.info("=== CONFIGURACIÓN DE BASE DE DATOS ===")
        logger.info("Datasource URL: {}", datasourceUrl)
        logger.info("Datasource Username: {}", datasourceUsername)
        logger.info("POSTGRES_DB: {}", postgresDb)
        logger.info("POSTGRES_HOST: {}", postgresHost)
        logger.info("POSTGRES_PORT: {}", postgresPort)
        logger.info("=====================================")

        // Validar si la URL contiene el nombre correcto de la base de datos
        if (!datasourceUrl.contains(postgresDb)) {
            logger.error("¡ALERTA! La URL de datasource no coincide con POSTGRES_DB")
            logger.error("URL: {} no contiene la base de datos: {}", datasourceUrl, postgresDb)
        } else {
            logger.info("Configuración de base de datos parece correcta")
        }
    }
}