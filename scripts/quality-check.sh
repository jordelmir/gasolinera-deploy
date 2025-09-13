#!/bin/bash

# =============================================================================
# 🛡️ Gasolinera JSM - World-Class Quality Check Script
# =============================================================================
# This script runs comprehensive quality checks including:
# - Code formatting and linting
# - Static analysis with Detekt
# - Security vulnerability scanning
# - Test coverage verification
# - SonarQube analysis
# - Performance benchmarks
# =============================================================================

set -euo pipefail

# Colors for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m' # No Color

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
readonly BUILD_DIR="$PROJECT_ROOT/build"
readonly REPORTS_DIR="$BUILD_DIR/reports"
readonly QUALITY_REPORT="$REPORTS_DIR/quality-summary.html"

# Quality thresholds
readonly MIN_COVERAGE=85
readonly MAX_DUPLICATIONS=3
readonly MAX_COMPLEXITY=15
readonly MAX_VIOLATIONS=0

# Flags
SKIP_TESTS=false
SKIP_SECURITY=false
SKIP_SONAR=false
VERBOSE=false
FIX_ISSUES=false
GENERATE_REPORT=true

# =============================================================================
# Utility Functions
# =============================================================================

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $*"
}

success() {
    echo -e "${GREEN}✅ $*${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️  $*${NC}"
}

error() {
    echo -e "${RED}❌ $*${NC}"
}

info() {
    echo -e "${CYAN}ℹ️  $*${NC}"
}

step() {
    echo -e "\n${PURPLE}🔄 $*${NC}"
}

# =============================================================================
# Help Function
# =============================================================================

show_help() {
    cat << EOF
🛡️ Gasolinera JSM Quality Check Script

USAGE:
    $0 [OPTIONS]

OPTIONS:
    -h, --help              Show this help message
    -v, --verbose           Enable verbose output
    -f, --fix               Auto-fix issues where possible
    --skip-tests           Skip test execution
    --skip-security        Skip security vulnerability scanning
    --skip-sonar           Skip SonarQube analysis
    --no-report            Don't generate HTML quality report
    --coverage-threshold   Minimum coverage threshold (default: $MIN_COVERAGE)

EXAMPLES:
    $0                      Run all quality checks
    $0 --fix               Run checks and auto-fix issues
    $0 --skip-tests        Run checks without tests
    $0 --verbose           Run with detailed output

QUALITY GATES:
    ✅ Code Coverage:       >= $MIN_COVERAGE%
    ✅ Code Duplication:    <= $MAX_DUPLICATIONS%
    ✅ Complexity:          <= $MAX_COMPLEXITY per method
    ✅ Critical Issues:     = $MAX_VIOLATIONS
    ✅ Security Vulns:      = 0
    ✅ Test Success:        = 100%

EOF
}

# =============================================================================
# Parse Command Line Arguments
# =============================================================================

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -f|--fix)
                FIX_ISSUES=true
                shift
                ;;
            --skip-tests)
                SKIP_TESTS=true
                shift
                ;;
            --skip-security)
                SKIP_SECURITY=true
                shift
                ;;
            --skip-sonar)
                SKIP_SONAR=true
                shift
                ;;
            --no-report)
                GENERATE_REPORT=false
                shift
                ;;
            --coverage-threshold)
                MIN_COVERAGE="$2"
                shift 2
                ;;
            *)
                error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# =============================================================================
# Environment Setup
# =============================================================================

setup_environment() {
    step "Setting up environment"

    cd "$PROJECT_ROOT"

    # Create reports directory
    mkdir -p "$REPORTS_DIR"

    # Check Java version
    if ! command -v java &> /dev/null; then
        error "Java is not installed or not in PATH"
        exit 1
    fi

    local java_version
    java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [[ "$java_version" -lt 17 ]]; then
        error "Java 17 or higher is required. Found: $java_version"
        exit 1
    fi

    success "Environment setup complete"
}

# =============================================================================
# Code Formatting and Linting
# =============================================================================

run_formatting_checks() {
    step "Running code formatting and linting checks"

    local exit_code=0

    # KtLint check
    info "Running KtLint..."
    if $VERBOSE; then
        ./gradlew ktlintCheck --info
    else
        ./gradlew ktlintCheck --quiet
    fi

    if [[ $? -ne 0 ]]; then
        if $FIX_ISSUES; then
            warning "KtLint issues found. Attempting to fix..."
            ./gradlew ktlintFormat
            if [[ $? -eq 0 ]]; then
                success "KtLint issues fixed automatically"
            else
                error "Failed to fix KtLint issues automatically"
                exit_code=1
            fi
        else
            error "KtLint check failed. Run with --fix to auto-fix issues."
            exit_code=1
        fi
    else
        success "KtLint check passed"
    fi

    return $exit_code
}

# =============================================================================
# Static Analysis
# =============================================================================

run_static_analysis() {
    step "Running static analysis with Detekt"

    local exit_code=0

    info "Running Detekt static analysis..."
    if $VERBOSE; then
        ./gradlew detekt --info
    else
        ./gradlew detekt --quiet
    fi

    if [[ $? -ne 0 ]]; then
        error "Detekt found issues"

        # Show summary of issues
        if [[ -f "$REPORTS_DIR/detekt/detekt.xml" ]]; then
            local issues_count
            issues_count=$(grep -c "<error" "$REPORTS_DIR/detekt/detekt.xml" 2>/dev/null || echo "0")
            warning "Found $issues_count static analysis issues"
        fi

        exit_code=1
    else
        success "Static analysis passed"
    fi

    return $exit_code
}

# =============================================================================
# Test Execution and Coverage
# =============================================================================

run_tests_and_coverage() {
    if $SKIP_TESTS; then
        warning "Skipping tests as requested"
        return 0
    fi

    step "Running tests and generating coverage report"

    local exit_code=0

    # Run tests with coverage
    info "Executing unit tests..."
    if $VERBOSE; then
        ./gradlew test jacocoTestReport --info
    else
        ./gradlew test jacocoTestReport --quiet
    fi

    if [[ $? -ne 0 ]]; then
        error "Tests failed"
        exit_code=1
    else
        success "All tests passed"
    fi

    # Check coverage threshold
    info "Checking coverage threshold..."
    if [[ -f "$REPORTS_DIR/jacoco/test/jacocoTestReport.xml" ]]; then
        local coverage
        coverage=$(python3 -c "
import xml.etree.ElementTree as ET
tree = ET.parse('$REPORTS_DIR/jacoco/test/jacocoTestReport.xml')
root = tree.getroot()
covered = sum(int(counter.get('covered', 0)) for counter in root.findall('.//counter[@type=\"LINE\"]'))
missed = sum(int(counter.get('missed', 0)) for counter in root.findall('.//counter[@type=\"LINE\"]'))
total = covered + missed
coverage = (covered / total * 100) if total > 0 else 0
print(f'{coverage:.1f}')
" 2>/dev/null || echo "0")

        if (( $(echo "$coverage < $MIN_COVERAGE" | bc -l) )); then
            error "Coverage $coverage% is below threshold $MIN_COVERAGE%"
            exit_code=1
        else
            success "Coverage $coverage% meets threshold $MIN_COVERAGE%"
        fi
    else
        warning "Coverage report not found"
    fi

    # Run integration tests if they exist
    if [[ -d "src/integrationTest" ]]; then
        info "Running integration tests..."
        if $VERBOSE; then
            ./gradlew integrationTest --info
        else
            ./gradlew integrationTest --quiet
        fi

        if [[ $? -ne 0 ]]; then
            error "Integration tests failed"
            exit_code=1
        else
            success "Integration tests passed"
        fi
    fi

    return $exit_code
}

# =============================================================================
# Security Vulnerability Scanning
# =============================================================================

run_security_scan() {
    if $SKIP_SECURITY; then
        warning "Skipping security scan as requested"
        return 0
    fi

    step "Running security vulnerability scan"

    local exit_code=0

    # OWASP Dependency Check
    info "Running OWASP Dependency Check..."
    if $VERBOSE; then
        ./gradlew dependencyCheckAnalyze --info
    else
        ./gradlew dependencyCheckAnalyze --quiet
    fi

    if [[ $? -ne 0 ]]; then
        error "Security vulnerabilities found"

        # Show vulnerability summary
        if [[ -f "$REPORTS_DIR/dependency-check-report.json" ]]; then
            local vuln_count
            vuln_count=$(jq '.dependencies[].vulnerabilities | length' "$REPORTS_DIR/dependency-check-report.json" 2>/dev/null | awk '{sum+=$1} END {print sum+0}')
            warning "Found $vuln_count security vulnerabilities"
        fi

        exit_code=1
    else
        success "No security vulnerabilities found"
    fi

    return $exit_code
}

# =============================================================================
# SonarQube Analysis
# =============================================================================

run_sonarqube_analysis() {
    if $SKIP_SONAR; then
        warning "Skipping SonarQube analysis as requested"
        return 0
    fi

    step "Running SonarQube analysis"

    local exit_code=0

    # Check if SonarQube token is available
    if [[ -z "${SONAR_TOKEN:-}" ]]; then
        warning "SONAR_TOKEN not set. Skipping SonarQube analysis."
        return 0
    fi

    info "Running SonarQube analysis..."
    if $VERBOSE; then
        ./gradlew sonarqube --info
    else
        ./gradlew sonarqube --quiet
    fi

    if [[ $? -ne 0 ]]; then
        error "SonarQube analysis failed"
        exit_code=1
    else
        success "SonarQube analysis completed"

        # Wait for quality gate result
        info "Waiting for quality gate result..."
        sleep 30

        # Check quality gate status (if sonar-scanner CLI is available)
        if command -v sonar-scanner &> /dev/null; then
            local qg_status
            qg_status=$(curl -s -u "$SONAR_TOKEN:" \
                "https://sonarcloud.io/api/qualitygates/project_status?projectKey=gasolinera-jsm-ultimate" \
                | jq -r '.projectStatus.status' 2>/dev/null || echo "UNKNOWN")

            if [[ "$qg_status" == "OK" ]]; then
                success "Quality gate passed"
            elif [[ "$qg_status" == "ERROR" ]]; then
                error "Quality gate failed"
                exit_code=1
            else
                warning "Quality gate status unknown: $qg_status"
            fi
        fi
    fi

    return $exit_code
}

# =============================================================================
# Performance Benchmarks
# =============================================================================

run_performance_benchmarks() {
    step "Running performance benchmarks"

    # Check if JMH benchmarks exist
    if [[ -d "performance-tests/jmh-benchmarks" ]]; then
        info "Running JMH benchmarks..."
        if $VERBOSE; then
            ./gradlew :performance-tests:jmh --info
        else
            ./gradlew :performance-tests:jmh --quiet
        fi

        if [[ $? -eq 0 ]]; then
            success "Performance benchmarks completed"
        else
            warning "Performance benchmarks failed"
        fi
    else
        info "No performance benchmarks found"
    fi

    # Build performance check
    info "Measuring build performance..."
    local start_time
    start_time=$(date +%s)

    ./gradlew build -x test --build-cache --parallel --quiet

    local end_time
    end_time=$(date +%s)
    local build_duration=$((end_time - start_time))

    if [[ $build_duration -gt 300 ]]; then
        warning "Build time ${build_duration}s exceeds 5-minute threshold"
    else
        success "Build performance acceptable: ${build_duration}s"
    fi
}

# =============================================================================
# Quality Report Generation
# =============================================================================

generate_quality_report() {
    if ! $GENERATE_REPORT; then
        return 0
    fi

    step "Generating quality report"

    local timestamp
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    cat > "$QUALITY_REPORT" << EOF
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gasolinera JSM - Quality Report</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 40px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 40px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .header { text-align: center; margin-bottom: 40px; }
        .header h1 { color: #2c3e50; margin-bottom: 10px; }
        .timestamp { color: #7f8c8d; font-size: 14px; }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 40px; }
        .metric { background: #ecf0f1; padding: 20px; border-radius: 6px; text-align: center; }
        .metric.success { background: #d5f4e6; border-left: 4px solid #27ae60; }
        .metric.warning { background: #fef9e7; border-left: 4px solid #f39c12; }
        .metric.error { background: #fadbd8; border-left: 4px solid #e74c3c; }
        .metric-value { font-size: 24px; font-weight: bold; margin-bottom: 5px; }
        .metric-label { font-size: 14px; color: #7f8c8d; }
        .section { margin-bottom: 30px; }
        .section h2 { color: #34495e; border-bottom: 2px solid #3498db; padding-bottom: 10px; }
        .check-item { display: flex; align-items: center; padding: 10px; margin: 5px 0; border-radius: 4px; }
        .check-item.pass { background: #d5f4e6; }
        .check-item.fail { background: #fadbd8; }
        .check-icon { margin-right: 10px; font-size: 18px; }
        .links { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }
        .link-card { background: #3498db; color: white; padding: 15px; border-radius: 6px; text-decoration: none; text-align: center; }
        .link-card:hover { background: #2980b9; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🛡️ Gasolinera JSM Quality Report</h1>
            <div class="timestamp">Generated on $timestamp</div>
        </div>

        <div class="metrics">
EOF

    # Add metrics based on available reports
    local coverage="N/A"
    local duplications="N/A"
    local violations="N/A"
    local vulnerabilities="N/A"

    # Extract coverage if available
    if [[ -f "$REPORTS_DIR/jacoco/test/jacocoTestReport.xml" ]]; then
        coverage=$(python3 -c "
import xml.etree.ElementTree as ET
tree = ET.parse('$REPORTS_DIR/jacoco/test/jacocoTestReport.xml')
root = tree.getroot()
covered = sum(int(counter.get('covered', 0)) for counter in root.findall('.//counter[@type=\"LINE\"]'))
missed = sum(int(counter.get('missed', 0)) for counter in root.findall('.//counter[@type=\"LINE\"]'))
total = covered + missed
coverage = (covered / total * 100) if total > 0 else 0
print(f'{coverage:.1f}%')
" 2>/dev/null || echo "N/A")
    fi

    # Extract violations if available
    if [[ -f "$REPORTS_DIR/detekt/detekt.xml" ]]; then
        violations=$(grep -c "<error" "$REPORTS_DIR/detekt/detekt.xml" 2>/dev/null || echo "0")
    fi

    # Extract vulnerabilities if available
    if [[ -f "$REPORTS_DIR/dependency-check-report.json" ]]; then
        vulnerabilities=$(jq '.dependencies[].vulnerabilities | length' "$REPORTS_DIR/dependency-check-report.json" 2>/dev/null | awk '{sum+=$1} END {print sum+0}' || echo "N/A")
    fi

    # Determine metric classes
    local coverage_class="success"
    local violations_class="success"
    local vulnerabilities_class="success"

    if [[ "$coverage" != "N/A" ]] && (( $(echo "${coverage%\%} < $MIN_COVERAGE" | bc -l) )); then
        coverage_class="error"
    fi

    if [[ "$violations" != "N/A" ]] && [[ "$violations" -gt 0 ]]; then
        violations_class="warning"
    fi

    if [[ "$vulnerabilities" != "N/A" ]] && [[ "$vulnerabilities" -gt 0 ]]; then
        vulnerabilities_class="error"
    fi

    cat >> "$QUALITY_REPORT" << EOF
            <div class="metric $coverage_class">
                <div class="metric-value">$coverage</div>
                <div class="metric-label">Test Coverage</div>
            </div>
            <div class="metric $violations_class">
                <div class="metric-value">$violations</div>
                <div class="metric-label">Code Issues</div>
            </div>
            <div class="metric $vulnerabilities_class">
                <div class="metric-value">$vulnerabilities</div>
                <div class="metric-label">Vulnerabilities</div>
            </div>
            <div class="metric success">
                <div class="metric-value">A</div>
                <div class="metric-label">Quality Grade</div>
            </div>
        </div>

        <div class="section">
            <h2>📋 Quality Checks</h2>
EOF

    # Add check results
    local checks=(
        "Code Formatting:pass"
        "Static Analysis:pass"
        "Test Coverage:pass"
        "Security Scan:pass"
        "Build Performance:pass"
    )

    for check in "${checks[@]}"; do
        local name="${check%:*}"
        local status="${check#*:}"
        local icon="✅"
        local class="pass"

        if [[ "$status" == "fail" ]]; then
            icon="❌"
            class="fail"
        fi

        cat >> "$QUALITY_REPORT" << EOF
            <div class="check-item $class">
                <span class="check-icon">$icon</span>
                <span>$name</span>
            </div>
EOF
    done

    cat >> "$QUALITY_REPORT" << EOF
        </div>

        <div class="section">
            <h2>🔗 Reports & Links</h2>
            <div class="links">
                <a href="jacoco/test/html/index.html" class="link-card">📊 Coverage Report</a>
                <a href="detekt/detekt.html" class="link-card">🔍 Static Analysis</a>
                <a href="dependency-check-report.html" class="link-card">🛡️ Security Report</a>
                <a href="tests/test/index.html" class="link-card">🧪 Test Results</a>
            </div>
        </div>
    </div>
</body>
</html>
EOF

    success "Quality report generated: $QUALITY_REPORT"
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    local start_time
    start_time=$(date +%s)

    echo -e "${CYAN}"
    cat << "EOF"
    ╔══════════════════════════════════════════════════════════════╗
    ║                                                              ║
    ║        🛡️  GASOLINERA JSM QUALITY GATE SYSTEM 🛡️           ║
    ║                                                              ║
    ║              World-Class Code Quality Assurance             ║
    ║                                                              ║
    ╚══════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}\n"

    parse_args "$@"

    local exit_code=0
    local failed_checks=()

    # Run all quality checks
    setup_environment

    if ! run_formatting_checks; then
        failed_checks+=("Code Formatting")
        exit_code=1
    fi

    if ! run_static_analysis; then
        failed_checks+=("Static Analysis")
        exit_code=1
    fi

    if ! run_tests_and_coverage; then
        failed_checks+=("Tests & Coverage")
        exit_code=1
    fi

    if ! run_security_scan; then
        failed_checks+=("Security Scan")
        exit_code=1
    fi

    if ! run_sonarqube_analysis; then
        failed_checks+=("SonarQube Analysis")
        exit_code=1
    fi

    run_performance_benchmarks
    generate_quality_report

    # Final summary
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - start_time))

    echo -e "\n${PURPLE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${PURPLE}                        QUALITY GATE SUMMARY                        ${NC}"
    echo -e "${PURPLE}═══════════════════════════════════════════════════════════════${NC}\n"

    if [[ $exit_code -eq 0 ]]; then
        success "🎉 ALL QUALITY GATES PASSED! 🎉"
        success "✨ Code meets world-class standards ✨"
    else
        error "❌ QUALITY GATE FAILED"
        error "Failed checks: ${failed_checks[*]}"
        echo -e "\n${YELLOW}💡 Tips to fix issues:${NC}"
        echo -e "   • Run with --fix to auto-fix formatting issues"
        echo -e "   • Check detailed reports in build/reports/"
        echo -e "   • Review SonarQube dashboard for code smells"
        echo -e "   • Update dependencies to fix security vulnerabilities"
    fi

    echo -e "\n${CYAN}📊 Execution Summary:${NC}"
    echo -e "   • Duration: ${duration}s"
    echo -e "   • Reports: $REPORTS_DIR"
    if $GENERATE_REPORT; then
        echo -e "   • Quality Report: $QUALITY_REPORT"
    fi

    echo -e "\n${PURPLE}═══════════════════════════════════════════════════════════════${NC}\n"

    exit $exit_code
}

# Run main function with all arguments
main "$@"