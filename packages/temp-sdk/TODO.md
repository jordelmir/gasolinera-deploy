# Temp SDK - TODO

## 🚨 ERRORES CRÍTICOS DETECTADOS

### 1. **Dependencias HTTP Client Faltantes**

**Problema**: ApiClient.kt usa Retrofit y OkHttp sin dependencias declaradas.

**Errores**:

- `Unresolved reference: okhttp3` (línea 3)
- `Unresolved reference: retrofit2` (líneas 4-5)
- `Unresolved reference: Retrofit` (línea 10)
- `Unresolved reference: OkHttpClient` (línea 12)

**Solución**:

```kotlin
// build.gradle.kts - agregar dependencias:
dependencies {
    // HTTP Client
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON Processing
    implementation("com.google.code.gson:gson:2.10.1")
}
```

### 2. **Archivos API Faltantes**

**Problema**: Referencias a archivos que no existen en el directorio api/.

**Estructura Actual**:

```
packages/temp-sdk/src/main/kotlin/com/gasolinerajsm/sdk/adengine/
├── api/                 # Directorio existe pero vacío
├── model/              # Directorio existe pero vacío
└── ApiClient.kt        # Existe pero con dependencias faltantes
```

**Solución**:

```kotlin
// CREAR: api/AdApi.kt
interface AdApi {
    @POST("ad/select")
    suspend fun selectAd(@Body request: AdSelectionRequest): Response<AdCreativeResponse>

    @POST("ad/impression")
    suspend fun recordImpression(@Body request: ImpressionRequest): Response<Void>

    @GET("ad/impressions")
    suspend fun getImpressions(@Query("campaignId") campaignId: Long?): Response<List<AdImpression>>
}

// CREAR: model/AdSelectionRequest.kt
data class AdSelectionRequest(
    val userId: String,
    val stationId: String,
    val context: Map<String, Any> = emptyMap()
)

// CREAR: model/AdCreativeResponse.kt
data class AdCreativeResponse(
    val adUrl: String,
    val campaignId: Long,
    val creativeId: String,
    val duration: Int,
    val skipAfter: Int
)
```

### 3. **Plugin Kotlin Duplicado**

**Problema**: temp-sdk usa versión específica de Kotlin plugin violando la unificación.

**Solución**:

```kotlin
// build.gradle.kts - cambiar:
plugins {
    id("java-library")
    kotlin("jvm") // Sin versión específica
}
```

## 🏗️ PROBLEMAS DE ARQUITECTURA

### 1. **Propósito Unclear del SDK**

**Problema**: No está claro qué servicios debe incluir este SDK temporal.

**Análisis**:

- Solo tiene código para ad-engine
- Falta integración con otros servicios (auth, coupon, station, raffle)
- No hay documentación de uso

**Solución Recomendada**:

```kotlin
// Estructura completa del SDK:
src/main/kotlin/com/gasolinerajsm/sdk/
├── auth/
│   ├── api/AuthApi.kt
│   ├── model/TokenResponse.kt
│   └── client/AuthClient.kt
├── coupon/
│   ├── api/CouponApi.kt
│   ├── model/CouponDto.kt
│   └── client/CouponClient.kt
├── adengine/
│   ├── api/AdApi.kt
│   ├── model/AdModels.kt
│   └── client/AdClient.kt
├── station/
│   ├── api/StationApi.kt
│   ├── model/StationDto.kt
│   └── client/StationClient.kt
├── common/
│   ├── ApiClient.kt
│   ├── ApiException.kt
│   └── Configuration.kt
└── GasolineraSDK.kt  # Main SDK class
```

### 2. **Falta Configuración Centralizada**

**Problema**: No hay manera de configurar el SDK globalmente.

**Solución**:

```kotlin
// common/Configuration.kt
data class SDKConfiguration(
    val baseUrl: String = "http://localhost:8080",
    val apiKey: String? = null,
    val timeout: Long = 30,
    val enableLogging: Boolean = false,
    val retryPolicy: RetryPolicy = RetryPolicy.DEFAULT
)

// GasolineraSDK.kt
class GasolineraSDK private constructor(
    private val configuration: SDKConfiguration
) {
    companion object {
        fun initialize(configuration: SDKConfiguration): GasolineraSDK {
            return GasolineraSDK(configuration)
        }
    }

    val auth: AuthClient by lazy { AuthClient(configuration) }
    val coupons: CouponClient by lazy { CouponClient(configuration) }
    val ads: AdClient by lazy { AdClient(configuration) }
    val stations: StationClient by lazy { StationClient(configuration) }
}
```

### 3. **Falta Manejo de Errores**

**Problema**: No hay clases de excepción específicas del SDK.

**Solución**:

```kotlin
// common/ApiException.kt
sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkException(message: String, cause: Throwable? = null) : ApiException(message, cause)
    class AuthenticationException(message: String) : ApiException(message)
    class ValidationException(message: String, val errors: List<String>) : ApiException(message)
    class ServerException(val code: Int, message: String) : ApiException(message)
    class UnknownException(message: String, cause: Throwable? = null) : ApiException(message, cause)
}
```

## 🔧 REFACTORING REQUERIDO

### Implementación Completa del SDK:

1. **Auth Client**:

```kotlin
// auth/client/AuthClient.kt
class AuthClient(private val config: SDKConfiguration) {
    private val api: AuthApi by lazy {
        ApiClient(config).createService<AuthApi>()
    }

    suspend fun requestOtp(phone: String): Result<Unit> {
        return try {
            val response = api.requestOtp(OtpRequest(phone))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(ApiException.ServerException(response.code(), response.message()))
            }
        } catch (e: Exception) {
            Result.failure(ApiException.NetworkException("Failed to request OTP", e))
        }
    }

    suspend fun verifyOtp(phone: String, code: String): Result<TokenResponse> {
        // Implementation
    }
}
```

2. **Coupon Client**:

```kotlin
// coupon/client/CouponClient.kt
class CouponClient(private val config: SDKConfiguration) {
    suspend fun generateCoupon(request: GenerateCouponRequest): Result<CouponDto>
    suspend fun redeemCoupon(couponId: String): Result<RedemptionResult>
    suspend fun getCouponStatus(couponId: String): Result<CouponStatus>
}
```

3. **Common ApiClient Mejorado**:

```kotlin
// common/ApiClient.kt
class ApiClient(private val config: SDKConfiguration) {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(config.timeout, TimeUnit.SECONDS)
            .readTimeout(config.timeout, TimeUnit.SECONDS)
            .writeTimeout(config.timeout, TimeUnit.SECONDS)
            .apply {
                if (config.enableLogging) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
                if (config.apiKey != null) {
                    addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer ${config.apiKey}")
                            .build()
                        chain.proceed(request)
                    }
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    inline fun <reified T> createService(): T {
        return retrofit.create(T::class.java)
    }
}
```

## 📋 DEPENDENCIAS COMPLETAS REQUERIDAS

### build.gradle.kts:

```kotlin
dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // HTTP Client
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON Processing
    implementation("com.google.code.gson:gson:2.10.1")

    // Validation
    implementation("javax.validation:validation-api:2.0.1.Final")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.mockk:mockk:1.13.8")
}
```

## 🧪 TESTING REQUERIDO

### Tests Faltantes:

- [ ] Unit tests para cada client
- [ ] Integration tests con mock servers
- [ ] Tests de configuración del SDK
- [ ] Tests de manejo de errores
- [ ] Tests de retry policies

### Test Structure:

```kotlin
// test/auth/AuthClientTest.kt
class AuthClientTest {
    @Test
    fun `should request OTP successfully`()

    @Test
    fun `should handle network errors gracefully`()

    @Test
    fun `should verify OTP and return tokens`()
}
```

## 📚 DOCUMENTACIÓN FALTANTE

### README.md para el SDK:

````markdown
# Gasolinera JSM SDK

## Installation

```gradle
implementation("com.gasolinerajsm:temp-sdk:0.0.1-SNAPSHOT")
```
````

## Usage

```kotlin
val sdk = GasolineraSDK.initialize(
    SDKConfiguration(
        baseUrl = "https://api.gasolinerajsm.com",
        apiKey = "your-api-key",
        enableLogging = true
    )
)

// Request OTP
val result = sdk.auth.requestOtp("+50612345678")

// Generate coupon
val coupon = sdk.coupons.generateCoupon(
    GenerateCouponRequest(
        stationId = "station-123",
        amount = 50.0
    )
)
```

```

## 🚀 PRÓXIMOS PASOS

### Prioridad Alta:
1. Agregar dependencias HTTP client faltantes
2. Crear interfaces API para todos los servicios
3. Implementar modelos de datos completos
4. Corregir plugin Kotlin duplicado

### Prioridad Media:
1. Implementar clients para todos los servicios
2. Agregar manejo de errores robusto
3. Implementar configuración centralizada
4. Crear tests unitarios

### Prioridad Baja:
1. Agregar retry policies
2. Implementar cache local
3. Agregar métricas de uso
4. Crear documentación completa

## 📝 NOTAS ADICIONALES

- Este SDK debería ser temporal hasta que se genere automáticamente desde OpenAPI specs
- La estructura actual es muy básica y necesita expansión significativa
- Falta integración con todos los servicios del sistema
- No hay versionado de API considerado
- Debería incluir ejemplos de uso y documentación completa
- Considerar migrar a SDK generado automáticamente una vez que los servicios estén estables
```
