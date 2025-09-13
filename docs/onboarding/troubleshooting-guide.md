# 🔧 Troubleshooting Guide - Gasolinera JSM

## 📋 Overview

Esta guía te ayudará a diagnosticar y resolver problemas comunes en el desarrollo y operación de Gasolinera JSM. Incluye desde problemas de configuración local hasta issues de producción.

## 🚨 Emergency Contacts

### Production Issues

- **On-Call Engineer**: +52-XXX-XXX-XXXX
- **DevOps Team**: devops@gasolinera-jsm.com
- **Slack Channel**: #gasolinera-alerts

### Development Support

- **Tech Lead**: tech-lead@gasolinera-jsm.com
- **Architecture Team**: architecture@gasolinera-jsm.com
- **Slack Channel**: #gasolinera-dev

## 🏠 Local Development Issues

### 1. Environment Setup Problems

#### Docker Services Won't Start

**Symptoms:**

```bash
ERROR: Couldn't connect to Docker daemon at unix:///var/run/docker.sock
```

**Solutions:**

```bash
# Check Docker daemon status
sudo systemctl status docker

# Start Docker daemon
sudo systemctl start docker

# Add user to docker group (requires logout/login)
sudo usermod -aG docker $USER

# Verify Docker installation
docker --version
docker-compose --version
```

#### Port Conflicts

**Symptoms:**

```bash
ERROR: for postgres  Cannot start service postgres:
Ports are not available: listen tcp 0.0.0.0:5432: bind: address already in use
```

**Solutions:**

```bash
# Find process using the port
lsof -i :5432
netstat -tulpn | grep :5432

# Kill the process (replace PID)
kill -9 <PID>

# Or use different ports in docker-compose.dev.yml
ports:
  - "5433:5432"  # Use 5433 instead of 5432
```

#### Database Connection Issues

**Symptoms:**

```bash
org.postgresql.util.PSQLException: Connection to localhost:5432 refused
```

**Diagnostic Steps:**

```bash
# Check if PostgreSQL container is running
docker-compose -f docker-compose.dev.yml ps postgres

# Check PostgreSQL logs
docker-compose -f docker-compose.dev.yml logs postgres

# Test connection manually
docker exec -it gasolinera_postgres psql -U gasolinera_user -d gasolinera_jsm_dev

# Check network connectivity
docker network ls
docker network inspect gasolinera-jsm-ultimate_default
```

**Solutions:**

```bash
# Restart PostgreSQL service
docker-compose -f docker-compose.dev.yml restart postgres

# Reset database completely
docker-compose -f docker-compose.dev.yml down -v
docker-compose -f docker-compose.dev.yml up -d postgres
./gradlew flywayMigrate

# Check environment variables
cat .env.development | grep DB_
```

### 2. Build and Compilation Issues

#### Gradle Build Failures

**Symptoms:**

```bash
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':coupon-service:compileKotlin'.
```

**Diagnostic Steps:**

```bash
# Clean build
./gradlew clean

# Build with detailed output
./gradlew build --info --stacktrace

# Check Java version
java -version
echo $JAVA_HOME

# Check Gradle version
./gradlew --version
```

**Solutions:**

```bash
# Fix Java version (use Java 17)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
# Or use SDKMAN
sdk use java 17.0.2-open

# Clear Gradle cache
rm -rf ~/.gradle/caches/
./gradlew build --refresh-dependencies

# Fix dependency conflicts
./gradlew dependencies --configuration compileClasspath
```

#### Kotlin Compilation Errors

**Symptoms:**

```bash
e: Unresolved reference: suspend
e: Modifier 'suspend' is not applicable to 'property getter'
```

**Solutions:**

```kotlin
// Ensure proper coroutine imports
import kotlinx.coroutines.*

// Check Kotlin version in build.gradle.kts
kotlin {
    jvmToolchain(17)
}

// Verify coroutines dependency
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
```

### 3. Testing Issues

#### TestContainers Failures

**Symptoms:**

```bash
org.testcontainers.containers.ContainerLaunchException:
Could not create/start container
```

**Diagnostic Steps:**

```bash
# Check Docker availability
docker info

# Check available resources
docker system df
docker system prune  # Clean up if needed

# Check TestContainers logs
export TESTCONTAINERS_RYUK_DISABLED=true  # Disable cleanup for debugging
```

**Solutions:**

```bash
# Increase Docker resources (Docker Desktop)
# Memory: 4GB+, CPU: 2+, Disk: 20GB+

# Clean up containers
docker container prune -f
docker image prune -f

# Use specific container versions
@Container
static val postgres = PostgreSQLContainer("postgres:15.4")
    .withDatabaseName("test_db")
    .withUsername("test")
    .withPassword("test")
```

#### Test Database Issues

**Symptoms:**

```bash
org.springframework.dao.DataIntegrityViolationException:
could not execute statement; SQL [n/a]; constraint [uk_users_email]
```

**Solutions:**

```kotlin
// Use @Transactional with rollback
@Test
@Transactional
@Rollback
fun `test should rollback changes`() {
    // Test code
}

// Or use @DirtiesContext for integration tests
@Test
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
fun `test with clean context`() {
    // Test code
}

// Use unique test data
@Test
fun `test with unique data`() {
    val uniqueEmail = "test-${UUID.randomUUID()}@example.com"
    // Test code
}
```

## 🔧 Service-Specific Issues

### Auth Service Issues

#### JWT Token Problems

**Symptoms:**

```bash
io.jsonwebtoken.security.SignatureException:
JWT signature does not match locally computed signature
```

**Diagnostic Steps:**

```bash
# Check JWT keys exist
ls -la config/keys/
cat config/keys/jwt-public.pem

# Verify key format
openssl rsa -in config/keys/jwt-private.pem -text -noout
openssl rsa -in config/keys/jwt-private.pem -pubout
```

**Solutions:**

```bash
# Generate new JWT keys
mkdir -p config/keys
openssl genrsa -out config/keys/jwt-private.pem 2048
openssl rsa -in config/keys/jwt-private.pem -pubout -out config/keys/jwt-public.pem

# Check application.yml configuration
jwt:
  private-key-path: classpath:keys/jwt-private.pem
  public-key-path: classpath:keys/jwt-public.pem
```

#### HashiCorp Vault Connection Issues

**Symptoms:**

```bash
VaultException: Status 403, Forbidden
```

**Solutions:**

```bash
# Check Vault status
docker exec -it gasolinera_vault vault status

# Initialize Vault (development only)
docker exec -it gasolinera_vault vault operator init -key-shares=1 -key-threshold=1

# Unseal Vault
docker exec -it gasolinera_vault vault operator unseal <unseal_key>

# Authenticate
docker exec -it gasolinera_vault vault auth -method=userpass username=admin password=admin
```

### Coupon Service Issues

#### QR Code Generation Problems

**Symptoms:**

```bash
java.lang.IllegalArgumentException: Invalid QR code data
```

**Solutions:**

```kotlin
// Ensure proper QR code library configuration
@Configuration
class QRCodeConfig {

    @Bean
    fun qrCodeWriter(): QRCodeWriter = QRCodeWriter()

    @Bean
    fun qrCodeGenerator(): QRCodeGenerator {
        return QRCodeGenerator(
            errorCorrectionLevel = ErrorCorrectionLevel.M,
            size = 300,
            margin = 1
        )
    }
}

// Validate QR data before generation
fun generateQRCode(data: String): String {
    require(data.isNotBlank()) { "QR code data cannot be blank" }
    require(data.length <= 2953) { "QR code data too long" }

    // Generate QR code
}
```

#### Payment Integration Issues

**Symptoms:**

```bash
StripeException: No such payment_intent: pi_invalid
```

**Solutions:**

```kotlin
// Use test keys in development
stripe:
  api-key: sk_test_51234567890abcdef  # Test key
  webhook-secret: whsec_test_1234567890abcdef

// Implement proper error handling
@Service
class PaymentService {

    suspend fun processPayment(request: PaymentRequest): PaymentResult {
        return try {
            val paymentIntent = stripeClient.createPaymentIntent(request)
            PaymentResult.Success(paymentIntent.id)
        } catch (ex: StripeException) {
            logger.error("Payment failed", ex)
            PaymentResult.Failure(ex.message ?: "Payment processing failed")
        }
    }
}
```

### Station Service Issues

#### Geospatial Query Problems

**Symptoms:**

```bash
org.postgresql.util.PSQLException:
ERROR: function st_dwithin(geometry, geometry, double precision) does not exist
```

**Solutions:**

```sql
-- Install PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Verify PostGIS installation
SELECT PostGIS_Version();

-- Check spatial indexes
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE indexdef LIKE '%gist%';
```

#### Location Data Issues

**Symptoms:**

```bash
Invalid location coordinates: lat=null, lng=null
```

**Solutions:**

```kotlin
// Validate coordinates
data class Location(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Invalid latitude: $latitude" }
        require(longitude in -180.0..180.0) { "Invalid longitude: $longitude" }
    }
}

// Handle missing location data
@Service
class StationService {

    suspend fun findNearbyStations(location: Location?): List<Station> {
        return if (location != null) {
            stationRepository.findNearby(location, DEFAULT_RADIUS_KM)
        } else {
            stationRepository.findAll().take(20) // Fallback
        }
    }
}
```

## 🔍 Monitoring and Debugging

### Application Performance Issues

#### High Memory Usage

**Diagnostic Steps:**

```bash
# Check JVM memory usage
jstat -gc <pid>
jstat -gccapacity <pid>

# Generate heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.classloader_stats
jmap -dump:format=b,file=heapdump.hprof <pid>

# Analyze with Eclipse MAT or VisualVM
```

**Solutions:**

```bash
# Tune JVM parameters
JAVA_OPTS="
-Xms512m -Xmx1g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdumps/
"

# Monitor memory usage
@Component
class MemoryMonitor {

    @Scheduled(fixedRate = 60000) // Every minute
    fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        logger.info("Memory usage: ${usedMemory / 1024 / 1024}MB / ${totalMemory / 1024 / 1024}MB")
    }
}
```

#### Slow Database Queries

**Diagnostic Steps:**

```sql
-- Enable query logging
ALTER SYSTEM SET log_statement = 'all';
ALTER SYSTEM SET log_min_duration_statement = 100; -- Log queries > 100ms
SELECT pg_reload_conf();

-- Check slow queries
SELECT query, mean_time, calls, total_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Analyze specific query
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM coupons WHERE user_id = $1 AND status = 'ACTIVE';
```

**Solutions:**

```sql
-- Add missing indexes
CREATE INDEX CONCURRENTLY idx_coupons_user_status
ON coupons(user_id, status)
WHERE status IN ('ACTIVE', 'PARTIALLY_REDEEMED');

-- Optimize queries
-- BEFORE: N+1 query problem
SELECT * FROM coupons WHERE user_id IN (SELECT id FROM users);

-- AFTER: Single join query
SELECT c.*, u.email
FROM coupons c
JOIN users u ON c.user_id = u.id
WHERE c.status = 'ACTIVE';
```

### Redis Connection Issues

**Symptoms:**

```bash
redis.clients.jedis.exceptions.JedisConnectionException:
Could not get a resource from the pool
```

**Diagnostic Steps:**

```bash
# Check Redis status
docker exec -it gasolinera_redis redis-cli ping

# Check connection pool
docker exec -it gasolinera_redis redis-cli info clients

# Monitor Redis performance
docker exec -it gasolinera_redis redis-cli --latency-history -i 1
```

**Solutions:**

```yaml
# Optimize connection pool
spring:
  redis:
    jedis:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 2000ms
    timeout: 2000ms
    connect-timeout: 2000ms
```

### RabbitMQ Message Issues

#### Messages Not Being Consumed

**Diagnostic Steps:**

```bash
# Check RabbitMQ status
docker exec -it gasolinera_rabbitmq rabbitmqctl status

# Check queues
docker exec -it gasolinera_rabbitmq rabbitmqctl list_queues name messages consumers

# Check exchanges and bindings
docker exec -it gasolinera_rabbitmq rabbitmqctl list_exchanges
docker exec -it gasolinera_rabbitmq rabbitmqctl list_bindings
```

**Solutions:**

```kotlin
// Ensure proper consumer configuration
@RabbitListener(queues = ["coupon.events"])
@Retryable(value = [Exception::class], maxAttempts = 3)
fun handleCouponEvent(
    @Payload event: CouponEvent,
    @Header("amqp_redelivered") redelivered: Boolean
) {
    try {
        processCouponEvent(event)
    } catch (ex: Exception) {
        logger.error("Failed to process coupon event", ex)
        throw ex // Will trigger retry
    }
}

// Configure dead letter queue
@Bean
fun couponQueue(): Queue {
    return QueueBuilder.durable("coupon.events")
        .withArgument("x-dead-letter-exchange", "dlx")
        .withArgument("x-dead-letter-routing-key", "coupon.events.dlq")
        .build()
}
```

## 🚨 Production Issues

### High CPU Usage

**Immediate Actions:**

```bash
# Check top processes
top -p $(pgrep -f "coupon-service")

# Check thread dump
jstack <pid> > thread-dump.txt

# Scale horizontally (Kubernetes)
kubectl scale deployment coupon-service --replicas=5
```

**Investigation:**

```bash
# Check application metrics
curl http://localhost:8080/actuator/metrics/process.cpu.usage

# Check for infinite loops or blocking operations
jstack <pid> | grep -A 5 -B 5 "BLOCKED\|WAITING"
```

### Database Connection Pool Exhausted

**Immediate Actions:**

```bash
# Check active connections
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

# Kill long-running queries
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'active'
AND query_start < now() - interval '5 minutes';
```

**Solutions:**

```yaml
# Increase connection pool
spring:
  datasource:
    hikari:
      maximum-pool-size: 30 # Increase from 20
      connection-timeout: 30000
      leak-detection-threshold: 60000
```

### Memory Leaks

**Diagnostic Steps:**

```bash
# Generate heap dump
kubectl exec -it coupon-service-pod -- jcmd 1 GC.run_finalization
kubectl exec -it coupon-service-pod -- jmap -dump:format=b,file=/tmp/heap.hprof 1

# Copy heap dump for analysis
kubectl cp coupon-service-pod:/tmp/heap.hprof ./heap.hprof
```

**Common Causes:**

- Unclosed resources (connections, streams)
- Static collections growing indefinitely
- Event listeners not being removed
- ThreadLocal variables not being cleaned

## 📊 Monitoring and Alerting

### Key Metrics to Monitor

```yaml
# Prometheus alerts
groups:
  - name: gasolinera-jsm
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.01
        for: 2m

      - alert: HighLatency
        expr: histogram_quantile(0.95, http_request_duration_seconds_bucket) > 0.5
        for: 5m

      - alert: DatabaseConnectionPoolHigh
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 1m
```

### Log Analysis

```bash
# Search for errors in logs
kubectl logs -f deployment/coupon-service | grep ERROR

# Search for specific patterns
kubectl logs deployment/coupon-service --since=1h | grep "OutOfMemoryError\|ConnectionTimeout\|DeadlockException"

# Aggregate error counts
kubectl logs deployment/coupon-service --since=1h | grep ERROR | wc -l
```

## 🔄 Recovery Procedures

### Service Recovery

```bash
# Restart service
kubectl rollout restart deployment/coupon-service

# Rollback to previous version
kubectl rollout undo deployment/coupon-service

# Scale down and up
kubectl scale deployment coupon-service --replicas=0
kubectl scale deployment coupon-service --replicas=3
```

### Database Recovery

```bash
# Restore from backup
pg_restore -h localhost -U gasolinera_user -d gasolinera_jsm_prod backup.sql

# Point-in-time recovery
pg_basebackup -h localhost -U replication -D /var/lib/postgresql/backup

# Failover to replica
# Update connection strings to point to replica
```

### Cache Recovery

```bash
# Clear Redis cache
kubectl exec -it redis-pod -- redis-cli FLUSHALL

# Warm up cache
curl -X POST http://api-gateway/admin/cache/warmup
```

## 📞 Escalation Procedures

### Severity Levels

#### P0 - Critical (Production Down)

- **Response Time**: 15 minutes
- **Actions**:
  1. Page on-call engineer
  2. Create incident in PagerDuty
  3. Start war room in Slack
  4. Notify stakeholders

#### P1 - High (Degraded Performance)

- **Response Time**: 1 hour
- **Actions**:
  1. Alert development team
  2. Create Jira ticket
  3. Monitor metrics closely

#### P2 - Medium (Non-critical Issues)

- **Response Time**: 4 hours
- **Actions**:
  1. Create Jira ticket
  2. Schedule for next sprint

#### P3 - Low (Minor Issues)

- **Response Time**: 24 hours
- **Actions**:
  1. Add to backlog
  2. Address in regular development cycle

### Contact Information

```yaml
Escalation Matrix:
  P0_Critical:
    - On-Call Engineer: +52-XXX-XXX-XXXX
    - DevOps Lead: +52-XXX-XXX-XXXX
    - CTO: +52-XXX-XXX-XXXX

  P1_High:
    - Tech Lead: tech-lead@company.com
    - DevOps Team: devops@company.com

  P2_Medium:
    - Development Team: dev-team@company.com

  P3_Low:
    - Product Owner: po@company.com
```

---

**🔧 Esta guía de troubleshooting te ayudará a resolver problemas rápidamente y mantener Gasolinera JSM funcionando sin problemas. Recuerda siempre documentar las soluciones para futuros problemas similares.**

_Última actualización: Enero 2024_
