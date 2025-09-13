#!/bin/bash

# Docker Security Scanning Script for Gasolinera JSM
# Scans Docker images for security vulnerabilities using multiple tools

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REGISTRY_PREFIX="${REGISTRY_PREFIX:-gasolinera-jsm}"
SERVICES=("auth-service" "api-gateway" "coupon-service" "station-service" "raffle-service" "redemption-service" "ad-engine")
SCAN_TOOLS=("trivy" "grype")
OUTPUT_DIR="security-reports"
SEVERITY_THRESHOLD="${SEVERITY_THRESHOLD:-HIGH}"

# Functions
print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  Docker Security Scanner${NC}"
    echo -e "${BLUE}================================${NC}"
}

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_dependencies() {
    print_status "Checking dependencies..."

    local missing_tools=()

    for tool in "${SCAN_TOOLS[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done

    if [ ${#missing_tools[@]} -ne 0 ]; then
        print_error "Missing security scanning tools: ${missing_tools[*]}"
        print_status "Installing missing tools..."

        for tool in "${missing_tools[@]}"; do
            case "$tool" in
                "trivy")
                    if [[ "$OSTYPE" == "darwin"* ]]; then
                        brew install aquasecurity/trivy/trivy
                    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
                        curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
                    fi
                    ;;
                "grype")
                    if [[ "$OSTYPE" == "darwin"* ]]; then
                        brew tap anchore/grype && brew install grype
                    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
                        curl -sSfL https://raw.githubusercontent.com/anchore/grype/main/install.sh | sh -s -- -b /usr/local/bin
                    fi
                    ;;
            esac
        done
    fi

    print_success "All dependencies are available"
}

create_output_directory() {
    mkdir -p "$OUTPUT_DIR"
    print_status "Created output directory: $OUTPUT_DIR"
}

scan_with_trivy() {
    local image="$1"
    local service="$2"
    local output_file="$OUTPUT_DIR/${service}-trivy-report.json"

    print_status "Scanning $image with Trivy..."

    trivy image \
        --format json \
        --output "$output_file" \
        --severity "$SEVERITY_THRESHOLD,CRITICAL" \
        --no-progress \
        "$image"

    # Generate human-readable report
    trivy image \
        --format table \
        --output "$OUTPUT_DIR/${service}-trivy-report.txt" \
        --severity "$SEVERITY_THRESHOLD,CRITICAL" \
        --no-progress \
        "$image"

    # Check for critical vulnerabilities
    local critical_count
    critical_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "CRITICAL")] | length' "$output_file" 2>/dev/null || echo "0")

    if [ "$critical_count" -gt 0 ]; then
        print_error "$service has $critical_count CRITICAL vulnerabilities"
        return 1
    else
        print_success "$service passed Trivy scan"
        return 0
    fi
}

scan_with_grype() {
    local image="$1"
    local service="$2"
    local output_file="$OUTPUT_DIR/${service}-grype-report.json"

    print_status "Scanning $image with Grype..."

    grype "$image" \
        -o json \
        --file "$output_file" \
        --fail-on critical

    # Generate human-readable report
    grype "$image" \
        -o table \
        --file "$OUTPUT_DIR/${service}-grype-report.txt"

    if [ $? -eq 0 ]; then
        print_success "$service passed Grype scan"
        return 0
    else
        print_error "$service failed Grype scan"
        return 1
    fi
}

scan_image() {
    local service="$1"
    local image="$REGISTRY_PREFIX/$service:latest"
    local scan_passed=true

    print_status "Scanning image: $image"

    # Check if image exists
    if ! docker image inspect "$image" &> /dev/null; then
        print_error "Image $image not found. Please build it first."
        return 1
    fi

    # Scan with each tool
    for tool in "${SCAN_TOOLS[@]}"; do
        case "$tool" in
            "trivy")
                if ! scan_with_trivy "$image" "$service"; then
                    scan_passed=false
                fi
                ;;
            "grype")
                if ! scan_with_grype "$image" "$service"; then
                    scan_passed=false
                fi
                ;;
        esac
    done

    if [ "$scan_passed" = true ]; then
        print_success "$service passed all security scans"
        return 0
    else
        print_error "$service failed security scans"
        return 1
    fi
}

generate_summary_report() {
    local summary_file="$OUTPUT_DIR/security-summary.md"

    print_status "Generating summary report..."

    cat > "$summary_file" << EOF
# Security Scan Summary Report

**Generated:** $(date)
**Severity Threshold:** $SEVERITY_THRESHOLD

## Scan Results

| Service | Trivy | Grype | Status |
|---------|-------|-------|--------|
EOF

    for service in "${SERVICES[@]}"; do
        local trivy_status="❌"
        local grype_status="❌"
        local overall_status="❌"

        # Check Trivy results
        if [ -f "$OUTPUT_DIR/${service}-trivy-report.json" ]; then
            local critical_count
            critical_count=$(jq '[.Results[]?.Vulnerabilities[]? | select(.Severity == "CRITICAL")] | length' "$OUTPUT_DIR/${service}-trivy-report.json" 2>/dev/null || echo "0")
            if [ "$critical_count" -eq 0 ]; then
                trivy_status="✅"
            fi
        fi

        # Check Grype results (simplified check)
        if [ -f "$OUTPUT_DIR/${service}-grype-report.txt" ] && ! grep -q "CRITICAL" "$OUTPUT_DIR/${service}-grype-report.txt"; then
            grype_status="✅"
        fi

        # Overall status
        if [ "$trivy_status" = "✅" ] && [ "$grype_status" = "✅" ]; then
            overall_status="✅ PASS"
        else
            overall_status="❌ FAIL"
        fi

        echo "| $service | $trivy_status | $grype_status | $overall_status |" >> "$summary_file"
    done

    cat >> "$summary_file" << EOF

## Recommendations

1. **Critical Vulnerabilities:** Address all CRITICAL severity vulnerabilities immediately
2. **Base Image Updates:** Consider updating to newer base images with security patches
3. **Dependency Updates:** Update application dependencies to latest secure versions
4. **Regular Scanning:** Integrate security scanning into CI/CD pipeline

## Detailed Reports

- Individual service reports are available in the \`$OUTPUT_DIR\` directory
- JSON reports can be integrated with security dashboards
- Text reports provide human-readable vulnerability details

EOF

    print_success "Summary report generated: $summary_file"
}

main() {
    print_header

    # Parse command line arguments
    local services_to_scan=()
    local scan_all=true

    while [[ $# -gt 0 ]]; do
        case $1 in
            --service)
                services_to_scan+=("$2")
                scan_all=false
                shift 2
                ;;
            --severity)
                SEVERITY_THRESHOLD="$2"
                shift 2
                ;;
            --help)
                echo "Usage: $0 [--service SERVICE_NAME] [--severity LEVEL]"
                echo "  --service: Scan specific service (can be used multiple times)"
                echo "  --severity: Set severity threshold (LOW, MEDIUM, HIGH, CRITICAL)"
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done

    # Use all services if none specified
    if [ "$scan_all" = true ]; then
        services_to_scan=("${SERVICES[@]}")
    fi

    check_dependencies
    create_output_directory

    local failed_scans=()

    # Scan each service
    for service in "${services_to_scan[@]}"; do
        if ! scan_image "$service"; then
            failed_scans+=("$service")
        fi
    done

    generate_summary_report

    # Final status
    if [ ${#failed_scans[@]} -eq 0 ]; then
        print_success "All security scans passed!"
        exit 0
    else
        print_error "Security scans failed for: ${failed_scans[*]}"
        print_warning "Please review the reports in $OUTPUT_DIR and address the vulnerabilities"
        exit 1
    fi
}

# Run main function with all arguments
main "$@"