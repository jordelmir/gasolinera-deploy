# Integration Tests Documentation

This document provides comprehensive information about the end-to-end integration tests for the Gasolinera JSM Platform.

## Overview

The integration tests validate the complete functionality of the platform by testing real workflows across multiple microservices. These tests ensure that all services work together correctly and that the entire system meets the specified requirements.

## Test Architecture

### Test Structure

```
integration-tests/
├── src/test/kotlin/com/gasolinerajsm/integration/
│   ├── base/                    # Base test classes and configuration
│   ├── config/                  # Test-specific Spring configuration
│   ├── e2e/                     # End-to-end test scenarios
│   ├── model/                   # Test data models and DTOs
│   ├── suite/                   # Test suites and runners
│   └── util/                    # Test utilities and helpers
├── src/test/resources/          # Test configuration files
└── build.gradle.kts            # Test module build configuration
```

### Technology Stack

- **Spring Boot Test**: Test framework and dependency injection
- **TestContainers**: Containerized infrastructure for tests
- **REST Assured**: HTTP client for API testing
- **Kotest**: Kotlin-native testing framework
- **Awaitility**: Asynchronous condition waiting
- **JUnit 5**: Test execution platform

## Test Scenarios

### 1. User Authentication Flow (`UserAuthenticationFlowTest`)

**Purpose**: Validates the complete user registration and authentication workflow.

**Test Cases**:

- Complete user registration and authentication flow
- Authentication flow with different user roles
- Authentication error handling scenarios
- Account security features (lockout, rate limiting)

**Services Involved**: Auth Service

**Key Validations**:

- OTP generation and validation
- User registration with phone verification
- JWT token generation and validation
- Role-based access control
- Session management and logout
- Token refresh functionality
- Account lockout after failed attempts
- Rate limiting for OTP requests

### 2. Coupon Redemption Flow (`CouponRedemptionFlowTest`)

**Purpose**: Tests the complete coupon redemption workflow across multiple services.

**Test Cases**:

- Complete coupon redemption workflow
- Coupon redemption with fraud detection
- Coupon redemption error scenarios
- Concurrent coupon redemption handling

**Services Involved**: Auth Service, Station Service, Coupon Service, Redemption Service, Raffle Service

**Key Validations**:

- User authentication and authorization
- Station and employee verification
- Campaign and coupon validation
- Redemption processing and calculation
- Raffle ticket generation
- Fraud detection and prevention
- Audit trail creation
- Concurrent access handling
- Error handling for various failure scenarios

### 3. Raffle Participation Flow (`RaffleParticipationFlowTest`)

**Purpose**: Validates the complete raffle participation workflow from ticket earning to winner selection.

**Test Cases**:

- Complete raffle participation workflow from coupon redemption to entry
- Raffle winner selection process
- Raffle participation edge cases and error handling

**Services Involved**: All services (Auth, Station, Coupon, Redemption, Ad Engine, Raffle)

**Key Validations**:

- Raffle ticket earning through coupon redemption
- Raffle ticket multiplication through ad engagement
- Raffle entry processing
- Winner selection algorithms
- Prize distribution and claiming
- Notification system
- Edge case handling (insufficient tickets, closed raffles, etc.)

## Test Infrastructure

### TestContainers Setup

The tests use TestContainers to provide isolated, containerized infrastructure:

```kotlin
@Container
val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine")

@Container
val rabbitMQContainer = RabbitMQContainer("rabbitmq:3-management-alpine")

@Container
val redisContainer = GenericContainer<Nothing>("redis:7-alpine")
```

**Benefits**:

- Isolated test environment
- Consistent infrastructure across different machines
- Automatic cleanup after tests
- Real database and messaging behavior

### Test Data Management

**Seed Data**: Tests use the existing seed data from migration scripts
**Test Users**: Predefined test users with different roles and states
**Dynamic Data**: Tests generate additional data as needed for specific scenarios

### Configuration

**Test Profiles**: `integration-test` profile with specific configurations
**Environment Variables**: Automatically configured by TestContainers
**Timeouts**: Configurable timeouts for different operations
**Parallel Execution**: Support for parallel test execution

## Running Integration Tests

### Prerequisites

- Docker and Docker Compose installed
- Java 21 or higher
- At least 8GB RAM available
- At least 5GB free disk space

### Command Line Execution

```bash
# Run all end-to-end tests
./scripts/run-integration-tests.sh

# Run specific test type
./scripts/run-integration-tests.sh --type e2e

# Run with parallel execution
./scripts/run-integration-tests.sh --parallel

# Run without cleanup (for debugging)
./scripts/run-integration-tests.sh --no-cleanup

# Run with custom timeout
./scripts/run-integration-tests.sh --timeout 45
```

### Gradle Tasks

```bash
# Run integration tests
./gradlew integration-tests:test

# Run only E2E tests
./gradlew integration-tests:e2eTest

# Run with test report generation
./gradlew integration-tests:test integration-tests:testReport
```

### IDE Execution

Tests can be run directly from IDEs like IntelliJ IDEA:

1. Navigate to the test class
2. Right-click and select "Run"
3. TestContainers will automatically start required infrastructure

## Test Configuration

### Application Configuration (`application-integration-test.yml`)

```yaml
spring:
  profiles:
    active: integration-test
  datasource:
    # Configured by TestContainers
  rabbitmq:
    # Configured by TestContainers
  redis:
    # Configured by TestContainers

test:
  timeout:
    default: 30s
    long: 120s
  services:
    auth-service:
      base-url: http://localhost:8081
    # ... other services
```

### Test Properties

- **Timeouts**: Configurable timeouts for different operations
- **Service URLs**: Base URLs for all microservices
- **Test Data**: Predefined test users, stations, and campaigns
- **Retry Policies**: Automatic retry for transient failures

## Test Utilities

### TestUtils Class

Provides common functionality for integration tests:

```kotlin
// Authentication
fun authenticateUser(phoneNumber: String, otpCode: String, baseUrl: String): String

// Service interactions
fun validateCoupon(couponCode: String, stationId: Long, userId: Long, token: String, baseUrl: String)
fun processRedemption(request: RedemptionRequest, token: String, baseUrl: String)
fun enterRaffle(request: RaffleEntryRequest, token: String, baseUrl: String)

// Async operations
fun waitForCondition(timeout: Duration, condition: () -> Boolean)
fun waitForEndpoint(url: String, timeout: Duration)
```

### Response Validation

```kotlin
// Success response validation
response.verifySuccessResponse()

// Error response validation
response.verifyErrorResponse(expectedStatus)
```

## Test Data

### Test Users

- **Admin**: `+525555000001` (System administrator)
- **Station Admin**: `+525555000002` (Station manager)
- **Employee**: `+525555000005` (Station employee)
- **Customer**: `+525555001001` (Regular customer)

### Test Stations

- **JSM001**: Gasolinera JSM Centro (Primary test station)
- **JSM002**: Gasolinera JSM Polanco (Secondary test station)

### Test Campaigns

- **WELCOME2024**: Welcome campaign for new customers
- **SUMMER2024**: Summer promotional campaign

## Monitoring and Reporting

### Test Reports

Tests generate comprehensive reports:

- **HTML Report**: Detailed test execution results
- **Coverage Report**: Code coverage analysis (if configured)
- **Performance Metrics**: Test execution times and resource usage

### Logging

Tests use structured logging:

- **Test Progress**: Step-by-step test execution logging
- **Service Interactions**: HTTP requests and responses
- **Error Details**: Detailed error information for failures

### Metrics Collection

- **Test Duration**: Individual test and suite execution times
- **Resource Usage**: Memory and CPU usage during tests
- **Service Response Times**: API response time measurements

## Troubleshooting

### Common Issues

#### TestContainers Startup Failures

```bash
# Check Docker daemon
docker info

# Check available resources
docker system df

# Clean up containers
docker system prune -a
```

#### Test Timeouts

- Increase timeout values in configuration
- Check system resources (CPU, memory)
- Verify network connectivity

#### Database Connection Issues

- Ensure PostgreSQL container is healthy
- Check database migration execution
- Verify connection parameters

#### Service Communication Failures

- Check service startup order
- Verify network configuration
- Review service health endpoints

### Debugging Tips

1. **Use `--no-cleanup` flag** to keep containers running after test failure
2. **Check container logs**: `docker-compose logs [service-name]`
3. **Verify service health**: Access health endpoints directly
4. **Review test logs**: Check detailed test execution logs
5. **Use IDE debugging**: Set breakpoints in test code

### Performance Optimization

1. **Enable TestContainer reuse**: `TESTCONTAINERS_REUSE_ENABLE=true`
2. **Use parallel execution**: `--parallel` flag
3. **Optimize test data**: Minimize data setup and teardown
4. **Resource allocation**: Ensure adequate system resources

## Best Practices

### Test Design

- **Test Independence**: Each test should be independent and idempotent
- **Clear Assertions**: Use descriptive assertion messages
- **Proper Cleanup**: Ensure resources are cleaned up after tests
- **Realistic Scenarios**: Test real-world usage patterns

### Data Management

- **Use Seed Data**: Leverage existing seed data when possible
- **Generate Unique Data**: Create unique test data to avoid conflicts
- **Clean State**: Ensure tests start with a clean state
- **Isolation**: Prevent tests from interfering with each other

### Error Handling

- **Expected Failures**: Test both success and failure scenarios
- **Timeout Handling**: Use appropriate timeouts for async operations
- **Retry Logic**: Implement retry logic for transient failures
- **Detailed Logging**: Provide detailed error information

## Continuous Integration

### CI/CD Integration

```yaml
# Example GitHub Actions workflow
- name: Run Integration Tests
  run: |
    ./scripts/run-integration-tests.sh --type e2e --parallel
  timeout-minutes: 45
  env:
    TESTCONTAINERS_REUSE_ENABLE: true
```

### Test Execution Strategy

- **Pull Request**: Run critical E2E tests
- **Main Branch**: Run full integration test suite
- **Nightly**: Run extended test scenarios with performance testing

### Reporting Integration

- **Test Results**: Publish test results to CI/CD platform
- **Coverage Reports**: Generate and publish coverage reports
- **Performance Metrics**: Track test execution performance over time

## Future Enhancements

### Planned Improvements

1. **Performance Tests**: Add load and stress testing scenarios
2. **Security Tests**: Implement security-focused test scenarios
3. **Mobile API Tests**: Add mobile-specific API testing
4. **Multi-Region Tests**: Test cross-region functionality
5. **Chaos Engineering**: Add failure injection testing

### Extension Points

- **Custom Test Scenarios**: Framework supports adding new test scenarios
- **Additional Services**: Easy integration of new microservices
- **External Integrations**: Support for testing external service integrations
- **Advanced Monitoring**: Enhanced metrics and monitoring capabilities

---

For more information about specific test scenarios or troubleshooting, refer to the individual test class documentation or contact the development team.
