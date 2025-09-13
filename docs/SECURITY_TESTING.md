# Security Testing Documentation

## Overview

This document describes the comprehensive security testing framework implemented for the Gasolinera JSM Platform. The security tests validate JWT token security, role-based access control enforcement, error handling security, and protection against common vulnerabilities.

## Security Test Categories

### 1. JWT Security Tests (`JwtSecurityTest`)

**Purpose:** Validate JWT token security, manipulation detection, and role-based access control.

**Test Coverage:**

- JWT token validation and signature verification
- Token manipulation detection (header, payload, signature)
- Algorithm confusion attacks (none algorithm)
- Token replay attack prevention
- Timing attack resistance
- Role-based access control enforcement
- Privilege escalation prevention

**Key Security Validations:**

- Modified JWT tokens are rejected
- Expired tokens are consistently rejected
- Role-based access is properly enforced
- Timing attacks are prevented
- Privilege escalation attempts are blocked

### 2. Error Handling Security Tests (`ErrorHandlingSecurityTest`)

**Purpose:** Ensure error handling doesn't expose sensitive information and provides graceful degradation.

**Test Coverage:**

- Information disclosure prevention in error messages
- Stack trace exposure prevention
- Internal path disclosure prevention
- Version information exposure prevention
- Graceful degradation under failure conditions
- Circuit breaker behavior validation
- Rate limiting error handling
- Error response consistency
- Security error handling (user enumeration prevention)
- Secure error logging

**Key Security Validations:**

- Database errors don't expose connection details
- Stack traces are not exposed in production
- Internal server paths are not revealed
- Authentication errors don't reveal user existence
- Validation errors don't echo malicious input
- Error responses maintain consistent format

### 3. Penetration Tests (`PenetrationTest`)

**Purpose:** Test protection against OWASP Top 10 vulnerabilities and common attack vectors.

**Test Coverage:**

- **SQL Injection Protection**
  - Authentication parameter injection
  - Search query injection
  - Union-based injection attempts
  - Blind SQL injection attempts

- **Cross-Site Scripting (XSS) Prevention**
  - Reflected XSS in user input
  - Stored XSS in API responses
  - DOM-based XSS attempts
  - Script injection in various contexts

- **Cross-Site Request Forgery (CSRF) Protection**
  - State-changing operation protection
  - SameSite cookie attributes
  - CSRF token validation

- **Insecure Direct Object Reference (IDOR) Prevention**
  - User data access control
  - Resource enumeration protection
  - Sequential ID access validation

- **Security Misconfiguration Detection**
  - Debug endpoint exposure
  - Default credential usage
  - Directory listing prevention
  - Server information disclosure

- **Sensitive Data Exposure Prevention**
  - API response data filtering
  - Log file access protection
  - Configuration file protection
  - Backup file security

- **Broken Access Control Detection**
  - Horizontal privilege escalation
  - Vertical privilege escalation
  - Method-based access control
  - Parameter pollution attacks

**Key Security Validations:**

- SQL injection attempts are blocked or sanitized
- XSS payloads are properly sanitized
- CSRF protection is active for state changes
- Users cannot access other users' data
- Debug endpoints are not publicly accessible
- Sensitive data is not exposed in responses

### 4. Security Integration Tests (`SecurityIntegrationTest`)

**Purpose:** Validate end-to-end security workflows and cross-service security interactions.

**Test Coverage:**

- **End-to-End Authentication Flow**
  - User registration security
  - OTP request rate limiting
  - JWT token generation and validation
  - Cross-service security context
  - Token expiration handling

- **Multi-Service Security Boundaries**
  - Service-to-service authentication
  - Gateway bypass prevention
  - Inter-service communication security
  - Service mesh security

- **Security Monitoring and Logging**
  - Failed authentication logging
  - Suspicious activity detection
  - Security event correlation
  - Audit trail completeness

- **Security Resilience Under Load**
  - Authentication under concurrent load
  - JWT validation performance
  - Rate limiting effectiveness
  - Circuit breaker activation

**Key Security Validations:**

- Security context is maintained across services
- Direct service access is properly blocked
- Security events are properly logged and correlated
- Security controls remain effective under load

## Test Execution

### Prerequisites

1. **Services Running:** All microservices must be running and healthy
2. **Test Environment:** Use `security-test` profile
3. **Dependencies:** Testcontainers for isolated testing environment

### Running Security Tests

```bash
# Run all security tests
./scripts/run-security-tests.sh all

# Run specific test categories
./scripts/run-security-tests.sh jwt
./scripts/run-security-tests.sh error
./scripts/run-security-tests.sh penetration
./scripts/run-security-tests.sh integration

# Generate security report only
./scripts/run-security-tests.sh report
```

### Manual Test Execution

```bash
# Navigate to security tests directory
cd security-tests

# Run specific test class
./gradlew test --tests "JwtSecurityTest" -Dspring.profiles.active=security-test

# Run all security tests with detailed output
./gradlew test --info -Dspring.profiles.active=security-test -Dtags="security"
```

## Security Test Configuration

### Test Environment Setup

The security tests use Testcontainers to create isolated test environments:

```yaml
# application-security-test.yml
spring:
  profiles:
    active: security-test
  datasource:
    url: # Provided by Testcontainers
    username: security_user
    password: security_password
  redis:
    host: # Provided by Testcontainers
    port: # Provided by Testcontainers
    password: security_password

security:
  jwt:
    secret: security_test_jwt_secret_key_for_testing_only
    expiration: 3600
  test:
    enabled: true
    mock-services: true
```

### Test Data and Fixtures

Security tests use predefined test data to ensure consistent results:

```kotlin
// Test user data
val testUsers = listOf(
    TestUser("customer1", "CUSTOMER", "+525555001001"),
    TestUser("employee1", "EMPLOYEE", "+525555000005"),
    TestUser("admin1", "SYSTEM_ADMIN", "+525555000001")
)

// Test JWT tokens
val validToken = generateValidJwtToken("test-user", "CUSTOMER", "+525555001001")
val expiredToken = generateExpiredJwtToken("test-user", "CUSTOMER", "+525555001001")
val malformedToken = "malformed.jwt.token"
```

## Security Test Results and Reporting

### Test Result Categories

Security test results are categorized by severity:

- **CRITICAL:** Immediate security vulnerabilities requiring urgent attention
- **HIGH:** Significant security issues that should be addressed promptly
- **MEDIUM:** Security improvements that should be implemented
- **LOW:** Minor security enhancements or best practices

### Automated Reporting

The security test framework generates comprehensive reports including:

1. **Executive Summary:** Overall security posture and key findings
2. **Test Results Summary:** Pass/fail statistics and severity breakdown
3. **Detailed Findings:** Specific security issues with remediation guidance
4. **Recommendations:** Prioritized action items for security improvements

### Report Locations

- **HTML Reports:** `security-tests/build/reports/tests/test/index.html`
- **XML Results:** `security-tests/build/test-results/test/`
- **Security Report:** `reports/security/security_test_report_[timestamp].md`

## Security Standards and Compliance

### Security Standards Validated

- **OWASP Top 10 2021:** Protection against the most critical web application security risks
- **JWT Best Practices:** RFC 7519 compliance and security recommendations
- **Input Validation:** OWASP Input Validation Cheat Sheet guidelines
- **Error Handling:** Secure error handling practices
- **Authentication Security:** Multi-factor authentication and session management

### Compliance Checks

The security tests validate compliance with:

1. **Authentication Security**
   - Strong password policies (where applicable)
   - Multi-factor authentication (OTP-based)
   - Session management security
   - Account lockout mechanisms

2. **Authorization Security**
   - Role-based access control (RBAC)
   - Principle of least privilege
   - Resource-level authorization
   - Cross-service authorization

3. **Data Protection**
   - Sensitive data handling
   - Data encryption in transit
   - Input validation and sanitization
   - Output encoding

4. **Infrastructure Security**
   - Secure configuration management
   - Error handling and logging
   - Rate limiting and DoS protection
   - Security monitoring

## Continuous Security Testing

### Integration with CI/CD

Security tests are integrated into the CI/CD pipeline:

```yaml
# .github/workflows/security-tests.yml
name: Security Tests
on: [push, pull_request]
jobs:
  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Start Services
        run: ./scripts/docker-start.sh
      - name: Run Security Tests
        run: ./scripts/run-security-tests.sh all
      - name: Upload Security Report
        uses: actions/upload-artifact@v3
        with:
          name: security-report
          path: reports/security/
```

### Scheduled Security Testing

Regular security testing is scheduled to ensure ongoing security:

- **Daily:** Basic security smoke tests
- **Weekly:** Comprehensive security test suite
- **Monthly:** Extended penetration testing
- **Quarterly:** Security audit and review

## Security Test Maintenance

### Adding New Security Tests

1. **Identify Security Requirement:** Define the security control to test
2. **Create Test Class:** Extend `BaseSecurityTest` for common utilities
3. **Implement Test Methods:** Follow naming convention and documentation standards
4. **Add Test Data:** Create necessary test fixtures and data
5. **Update Documentation:** Document new tests and their purpose

### Test Data Management

Security test data should be:

- **Isolated:** Each test should use independent data
- **Realistic:** Test data should represent real-world scenarios
- **Secure:** Test credentials should not be used in production
- **Maintainable:** Test data should be easy to update and manage

### Security Test Best Practices

1. **Test Isolation:** Each test should be independent and not affect others
2. **Negative Testing:** Test both valid and invalid scenarios
3. **Edge Cases:** Include boundary conditions and edge cases
4. **Performance Impact:** Consider the performance impact of security controls
5. **Documentation:** Clearly document test purpose and expected outcomes

## Troubleshooting Security Tests

### Common Issues

1. **Service Unavailability**
   - Ensure all services are running and healthy
   - Check service discovery and networking
   - Verify database and cache connectivity

2. **Authentication Failures**
   - Verify JWT secret configuration
   - Check token expiration settings
   - Validate user test data

3. **Test Environment Issues**
   - Ensure Testcontainers are properly configured
   - Check Docker availability and permissions
   - Verify test profile configuration

4. **Flaky Tests**
   - Review timing-dependent tests
   - Check for race conditions
   - Validate test data cleanup

### Debugging Security Tests

```bash
# Run tests with debug logging
./gradlew test --tests "SecurityTest" --debug -Dspring.profiles.active=security-test

# Run specific test method
./gradlew test --tests "JwtSecurityTest.should properly validate JWT tokens" -Dspring.profiles.active=security-test

# Generate detailed test reports
./gradlew test --info --continue -Dspring.profiles.active=security-test
```

## Security Contact Information

For security-related questions or issues:

- **Security Team:** security@gasolinerajsm.com
- **Development Team:** dev@gasolinerajsm.com
- **Security Incident Response:** incident@gasolinerajsm.com

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [JWT Security Best Practices](https://tools.ietf.org/html/rfc7519)
- [OWASP Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Testcontainers Documentation](https://www.testcontainers.org/)
