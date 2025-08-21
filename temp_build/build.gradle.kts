plugins {
    id("org.openapi.generator") version "7.0.1"
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    kotlin("plugin.jpa") version "1.9.24" apply false
    id("org.springframework.boot") version "3.3.3" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
}

// Spring Cloud BOM for all subprojects
subprojects {
    apply(plugin = "io.spring.dependency-management")

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
        }
    }
}

// 1. Centraliza la configuración común para todas las tareas de generación de clientes.
// Esto evita la repetición y facilita las actualizaciones.
val commonConfigOptions = mapOf(
    "dateLibrary" to "java8",
    "useCoroutines" to "true",
    "useBeanValidation" to "true",
    "interfaceOnly" to "true",
    "skipFormModel" to "true",
    "skipDefaultInterface" to "true",
    "useTags" to "true",
    "openApiNullable" to "false"
)

// 2. Define los detalles de cada cliente de API en una lista.
// Para añadir un nuevo cliente, simplemente añade una nueva entrada a esta lista.
val apiClients = listOf(
    mapOf(
        "name" to "Auth",
        "servicePath" to "auth-service",
        "sdkPackage" to "com.gasolinerajsm.sdk.auth"
    ),
    mapOf(
        "name" to "Redemption",
        "servicePath" to "redemption-service",
        "sdkPackage" to "com.gasolinerajsm.sdk.redemption"
    ),
    mapOf(
        "name" to "AdEngine",
        "servicePath" to "ad-engine",
        "sdkPackage" to "com.gasolinerajsm.sdk.adengine"
    ),
    mapOf(
        "name" to "Raffle",
        "servicePath" to "raffle-service",
        "sdkPackage" to "com.gasolinerajsm.sdk.raffle"
    ),
    mapOf(
        "name" to "Station",
        "servicePath" to "station-service",
        "sdkPackage" to "com.gasolinerajsm.sdk.station"
    )
)

// 3. Itera sobre la lista de clientes para registrar dinámicamente una tarea de generación para cada uno.
// Esto elimina la necesidad de tener un bloque de tareas repetido para cada cliente.
apiClients.forEach { client ->
    val taskName = "generate${client["name"]}Client"
    val servicePath = client["servicePath"]
    val sdkPackage = client["sdkPackage"]

    tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(taskName) {
        group = "openapi tools"
        description = "Generates the ${client["name"]} API client."
        generatorName.set("kotlin-spring")
        inputSpec.set("$projectDir/services/$servicePath/openapi.yaml")
        outputDir.set("${layout.buildDirectory.get()}/generated/$servicePath-client")
        apiPackage.set("$sdkPackage.api")
        modelPackage.set("$sdkPackage.model")
        configOptions.set(commonConfigOptions)
    }
}