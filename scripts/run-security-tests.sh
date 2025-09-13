#!/bin/bash

# Security Tests Runner Script
# Executes comprehensive security validation tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SECURITY_TESTS_DIR="$PROJECT_ROOT/security-tests"
REPORTS_DIR="$PROJECT_ROOT/reports/security"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Create reports directory
mkdir -p "$REPORTS_DIR"

echo -e "${BLUE}=== Gasolinera JSM Security Tests Runner ===${NC}"
echo -e "${BLUE}Timestamp: $TIMESTAMP${NC}"
echo -e "${BLUE}Project Root: $PROJECT_ROOT${NC}"
echo ""

# Function to print section headers
print_section() {
    echo -e "${YELLOW}=== $1 ===${NC}"
}

# Function to check if services are running
check_services() {
    print_section "Checking Service Availability"

    local services=(
        "http://localhost:8080/actuator/health"  # API Gateway
        "http://localhost:8081/actuator/health"  # Auth Service
        "http://localhost:8083/actuator/health"  # Coupon Service
        "http://localhost:8084/actuator/health"  # Redemption Service
    )

    local all_healthy=true

    for service in "${services[@]}"; do
        if curl -s -f "$service" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Service available: $service${NC}"
        else
            echo -e "${RED}✗ Service unavailable: $service${NC}"
            all_healthy=false
        fi
    done

    if [ "$all_healthy" = false ]; then
        echo -e "${RED}Some services are not available. Please start all services before running security tests.${NC}"
        echo -e "${YELLOW}Run: ./scripts/docker-start.sh${NC}"
        exit 1
    fi

    echo -e "${GREEN}All services are healthy and ready for security testing${NC}"
    echo ""
}

# Function to run security tests
run_security_tests() {
    print_section "Running Security Tests"

    cd "$SECURITY_TESTS_DIR"

    # Set test environment variables
    export SPRING_PROFILES_ACTIVE=security-test
    export SECURITY_TEST_TIMESTAMP="$TIMESTAMP"
    export SECURITY_REPORTS_DIR="$REPORTS_DIR"

    echo -e "${BLUE}Running JWT Security Tests...${NC}"
    ./gradlew test --tests "*JwtSecurityTest*" \
        --info \
        --continue \
        -Dspring.profiles.active=security-test \
        -Dsecurity.test.reports.dir="$REPORTS_DIR" \
        || echo -e "${YELLOW}Some JWT security tests failed${NC}"

    echo -e "${BLUE}Running Error Handling Security Tests...${NC}"
    ./gradlew test --tests "*ErrorHandlingSecurityTest*" \
        --info \
        --continue \
        -Dspring.profiles.active=security-test \
        -Dsecurity.test.reports.dir="$REPORTS_DIR" \
        || echo -e "${YELLOW}Some error handling security tests failed${NC}"

    echo -e "${BLUE}Running Penetration Tests...${NC}"
    ./gradlew test --tests "*PenetrationTest*" \
        --info \
        --continue \
        -Dspring.profiles.active=security-test \
        -Dsecurity.test.reports.dir="$REPORTS_DIR" \
        || echo -e "${YELLOW}Some penetration tests failed${NC}"

    echo -e "${BLUE}Running Security Integration Tests...${NC}"
    ./gradlew test --tests "*SecurityIntegrationTest*" \
        --info \
        --continue \
        -Dspring.profiles.active=security-test \
        -Dsecurity.test.reports.dir="$REPORTS_DIR" \
        || echo -e "${YELLOW}Some security integration tests failed${NC}"

    echo -e "${GREEN}Security tests execution completed${NC}"
    echo ""
}

# Function to run all security tests
run_all_security_tests() {
    print_section "Running All Security Tests"

    cd "$SECURITY_TESTS_DIR"

    # Set test environment variables
    export SPRING_PROFILES_ACTIVE=security-test
    export SECURITY_TEST_TIMESTAMP="$TIMESTAMP"
    export SECURITY_REPORTS_DIR="$REPORTS_DIR"

    echo -e "${BLUE}Executing comprehensive security test suite...${NC}"
    ./gradlew test \
        --info \
        --continue \
        -Dspring.profiles.active=security-test \
        -Dsecurity.test.reports.dir="$REPORTS_DIR" \
        -Dtags="security" \
        || echo -e "${YELLOW}Some security tests failed - check reports for details${NC}"

    echo -e "${GREEN}All security tests execution completed${NC}"
    echo ""
}

# Function to generate security report
generate_security_report() {
    print_section "Generating Security Test Report"

    local report_file="$REPORTS_DIR/security_test_report_$TIMESTAMP.md"
    local test_results_dir="$SECURITY_TESTS_DIR/build/test-results/test"

    cat > "$report_file" << EOF
# Security Test Report

**Generated:** $(date)
**Timestamp:** $TIMESTAMP
**Project:** Gasolinera JSM Platform

## Executive Summary

This report contains the results of comprehensive security testing performed on the Gasolinera JSM Platform.

### Test Categories Executed

1. **JWT Security Tests** - Token validation, manipulation detection, timing attacks
2. **Error Handling Security Tests** - Information disclosure, graceful degradation
3. **Penetration Tests** - OWASP Top 10 vulnerabilities, common attack vectors
4. **Security Integration Tests** - End-to-end security workflows, cross-service validation

## Test Results Summary

EOF

    # Parse test results if available
    if [ -d "$test_results_dir" ]; then
        local total_tests=0
        local failed_tests=0
        local passed_tests=0

        for xml_file in "$test_results_dir"/*.xml; do
            if [ -f "$xml_file" ]; then
                local file_tests=$(grep -o 'tests="[0-9]*"' "$xml_file" | grep -o '[0-9]*' || echo "0")
                local file_failures=$(grep -o 'failures="[0-9]*"' "$xml_file" | grep -o '[0-9]*' || echo "0")
                local file_errors=$(grep -o 'errors="[0-9]*"' "$xml_file" | grep -o '[0-9]*' || echo "0")

                total_tests=$((total_tests + file_tests))
                failed_tests=$((failed_tests + file_failures + file_errors))
            fi
        done

        passed_tests=$((total_tests - failed_tests))
        local pass_rate=0

        if [ $total_tests -gt 0 ]; then
            pass_rate=$((passed_tests * 100 / total_tests))
        fi

        cat >> "$report_file" << EOF
- **Total Tests:** $total_tests
- **Passed:** $passed_tests
- **Failed:** $failed_tests
- **Pass Rate:** $pass_rate%

EOF

        if [ $failed_tests -gt 0 ]; then
            echo "⚠️  **WARNING:** $failed_tests security tests failed" >> "$report_file"
            echo "" >> "$report_file"
        fi

        if [ $pass_rate -lt 80 ]; then
            echo "🚨 **CRITICAL:** Security test pass rate is below 80%" >> "$report_file"
            echo "" >> "$report_file"
        fi
    fi

    cat >> "$report_file" << EOF
## Security Test Categories

### 1. JWT Security Tests
- Token validation and signature verification
- JWT manipulation detection
- Timing attack resistance
- Role-based access control enforcement

### 2. Error Handling Security Tests
- Information disclosure prevention
- Graceful degradation under failures
- Consistent error response formats
- Secure error logging

### 3. Penetration Tests
- SQL injection protection
- Cross-Site Scripting (XSS) prevention
- Cross-Site Request Forgery (CSRF) protection
- Insecure Direct Object Reference (IDOR) prevention
- Security misconfiguration detection
- Sensitive data exposure prevention
- Broken access control detection

### 4. Security Integration Tests
- End-to-end authentication flows
- Multi-service security boundaries
- Security monitoring and logging
- Security resilience under load

## Recommendations

### Critical Issues
- Review any failed CRITICAL severity tests immediately
- Ensure JWT token validation is working correctly
- Verify input validation is preventing injection attacks

### High Priority Issues
- Address any HIGH severity test failures
- Review error handling to prevent information disclosure
- Validate access control mechanisms

### Medium Priority Issues
- Improve security headers implementation
- Enhance rate limiting mechanisms
- Strengthen CSRF protection

### Low Priority Issues
- Optimize security monitoring
- Improve audit logging
- Enhance security documentation

## Next Steps

1. **Immediate Actions**
   - Fix any CRITICAL severity security issues
   - Review and address HIGH severity findings
   - Validate security controls are working as expected

2. **Short Term (1-2 weeks)**
   - Address MEDIUM severity issues
   - Implement additional security monitoring
   - Update security documentation

3. **Long Term (1 month)**
   - Address LOW severity issues
   - Implement security automation
   - Schedule regular security testing

## Test Environment

- **Services Tested:** Auth Service, Coupon Service, Redemption Service, API Gateway
- **Test Framework:** JUnit 5, RestAssured, Testcontainers
- **Security Standards:** OWASP Top 10, JWT Best Practices
- **Test Coverage:** Authentication, Authorization, Input Validation, Error Handling

---

*This report was automatically generated by the Gasolinera JSM security testing framework.*
EOF

    echo -e "${GREEN}Security report generated: $report_file${NC}"
    echo ""
}

# Function to validate security test results
validate_security_results() {
    print_section "Validating Security Test Results"

    local test_results_dir="$SECURITY_TESTS_DIR/build/test-results/test"
    local critical_failures=0
    local high_failures=0
    local total_failures=0

    if [ -d "$test_results_dir" ]; then
        # Count failures from test results
        for xml_file in "$test_results_dir"/*.xml; do
            if [ -f "$xml_file" ]; then
                local file_failures=$(grep -o 'failures="[0-9]*"' "$xml_file" | grep -o '[0-9]*' || echo "0")
                local file_errors=$(grep -o 'errors="[0-9]*"' "$xml_file" | grep -o '[0-9]*' || echo "0")
                total_failures=$((total_failures + file_failures + file_errors))
            fi
        done

        # Check for critical security test failures in logs
        local log_files="$SECURITY_TESTS_DIR/build/reports/tests/test/classes/*.html"
        if ls $log_files 1> /dev/null 2>&1; then
            critical_failures=$(grep -r "CRITICAL.*failed" $log_files | wc -l || echo "0")
            high_failures=$(grep -r "HIGH.*failed" $log_files | wc -l || echo "0")
        fi
    fi

    echo "Security Test Validation Results:"
    echo "- Total Failures: $total_failures"
    echo "- Critical Failures: $critical_failures"
    echo "- High Failures: $high_failures"
    echo ""

    # Determine overall security status
    if [ $critical_failures -gt 0 ]; then
        echo -e "${RED}🚨 SECURITY STATUS: CRITICAL ISSUES FOUND${NC}"
        echo -e "${RED}Critical security vulnerabilities detected. Immediate action required.${NC}"
        return 2
    elif [ $high_failures -gt 5 ]; then
        echo -e "${YELLOW}⚠️  SECURITY STATUS: HIGH RISK${NC}"
        echo -e "${YELLOW}Multiple high-severity security issues found. Review required.${NC}"
        return 1
    elif [ $total_failures -gt 10 ]; then
        echo -e "${YELLOW}⚠️  SECURITY STATUS: MEDIUM RISK${NC}"
        echo -e "${YELLOW}Several security issues found. Review recommended.${NC}"
        return 1
    else
        echo -e "${GREEN}✅ SECURITY STATUS: ACCEPTABLE${NC}"
        echo -e "${GREEN}Security tests passed with acceptable risk level.${NC}"
        return 0
    fi
}

# Main execution
main() {
    local test_type="${1:-all}"

    case "$test_type" in
        "jwt")
            check_services
            cd "$SECURITY_TESTS_DIR"
            ./gradlew test --tests "*JwtSecurityTest*" -Dspring.profiles.active=security-test
            ;;
        "error")
            check_services
            cd "$SECURITY_TESTS_DIR"
            ./gradlew test --tests "*ErrorHandlingSecurityTest*" -Dspring.profiles.active=security-test
            ;;
        "penetration")
            check_services
            cd "$SECURITY_TESTS_DIR"
            ./gradlew test --tests "*PenetrationTest*" -Dspring.profiles.active=security-test
            ;;
        "integration")
            check_services
            cd "$SECURITY_TESTS_DIR"
            ./gradlew test --tests "*SecurityIntegrationTest*" -Dspring.profiles.active=security-test
            ;;
        "all")
            check_services
            run_all_security_tests
            generate_security_report
            validate_security_results
            ;;
        "report")
            generate_security_report
            ;;
        *)
            echo "Usage: $0 [jwt|error|penetration|integration|all|report]"
            echo ""
            echo "Test Types:"
            echo "  jwt          - Run JWT security tests only"
            echo "  error        - Run error handling security tests only"
            echo "  penetration  - Run penetration tests only"
            echo "  integration  - Run security integration tests only"
            echo "  all          - Run all security tests (default)"
            echo "  report       - Generate security report only"
            exit 1
            ;;
    esac
}

# Execute main function with all arguments
main "$@"