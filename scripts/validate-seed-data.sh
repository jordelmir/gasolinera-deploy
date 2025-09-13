#!/bin/bash

# Seed Data Validation Script for Gasolinera JSM Platform
# This script validates the integrity and consistency of seed data

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
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

# Configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-gasolinera_db}
DB_USER=${DB_USER:-gasolinera_user}
DB_PASSWORD=${DB_PASSWORD:-gasolinera_password}

# Parse arguments
VERBOSE=false
DOCKER_MODE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --verbose|-v)
            VERBOSE=true
            shift
            ;;
        --docker)
            DOCKER_MODE=true
            shift
            ;;
        --host)
            DB_HOST="$2"
            shift 2
            ;;
        --port)
            DB_PORT="$2"
            shift 2
            ;;
        --database)
            DB_NAME="$2"
            shift 2
            ;;
        --user)
            DB_USER="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --verbose, -v          Show detailed output"
            echo "  --docker               Use Docker to connect to database"
            echo "  --host HOST            Database host (default: localhost)"
            echo "  --port PORT            Database port (default: 5432)"
            echo "  --database DB          Database name (default: gasolinera_db)"
            echo "  --user USER            Database user (default: gasolinera_user)"
            echo "  --help, -h             Show this help message"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

print_status "Validating seed data for Gasolinera JSM Platform..."

# Function to execute SQL query
execute_sql() {
    local query="$1"
    if [ "$DOCKER_MODE" = true ]; then
        docker-compose exec -T postgres psql -U "$DB_USER" -d "$DB_NAME" -t -c "$query" 2>/dev/null
    else
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "$query" 2>/dev/null
    fi
}

# Function to run validation test
run_test() {
    local test_name="$1"
    local query="$2"
    local expected="$3"

    if [ "$VERBOSE" = true ]; then
        print_status "Running test: $test_name"
    fi

    local result=$(execute_sql "$query" | xargs)

    if [ "$result" = "$expected" ]; then
        print_success "✓ $test_name"
        return 0
    else
        print_error "✗ $test_name (Expected: $expected, Got: $result)"
        return 1
    fi
}

# Function to run count test
run_count_test() {
    local test_name="$1"
    local query="$2"
    local min_count="$3"

    if [ "$VERBOSE" = true ]; then
        print_status "Running count test: $test_name"
    fi

    local result=$(execute_sql "$query" | xargs)

    if [ "$result" -ge "$min_count" ]; then
        print_success "✓ $test_name ($result records)"
        return 0
    else
        print_error "✗ $test_name (Expected: >= $min_count, Got: $result)"
        return 1
    fi
}

# Test database connection
print_status "Testing database connection..."
if ! execute_sql "SELECT 1;" > /dev/null; then
    print_error "Cannot connect to database. Please check connection parameters."
    exit 1
fi
print_success "Database connection successful"

# Initialize counters
PASSED_TESTS=0
FAILED_TESTS=0
TOTAL_TESTS=0

# Test 1: User count validation
print_status "Validating user data..."
if run_count_test "Minimum user count" "SELECT COUNT(*) FROM auth_schema.users;" 20; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 2: Phone number format validation
if run_test "Phone number format" "SELECT COUNT(*) FROM auth_schema.users WHERE phone_number !~ '^\+52[0-9]{10}$';" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 3: User role distribution
if run_count_test "System admin exists" "SELECT COUNT(*) FROM auth_schema.users WHERE role = 'SYSTEM_ADMIN';" 1; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

if run_count_test "Station admins exist" "SELECT COUNT(*) FROM auth_schema.users WHERE role = 'STATION_ADMIN';" 3; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

if run_count_test "Employees exist" "SELECT COUNT(*) FROM auth_schema.users WHERE role = 'EMPLOYEE';" 5; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

if run_count_test "Customers exist" "SELECT COUNT(*) FROM auth_schema.users WHERE role = 'CUSTOMER';" 10; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 4: Station data validation
print_status "Validating station data..."
if run_count_test "Minimum station count" "SELECT COUNT(*) FROM station_schema.stations;" 10; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 5: Station coordinates validation
if run_test "Station coordinates within Mexico" "SELECT COUNT(*) FROM station_schema.stations WHERE latitude < 14.0 OR latitude > 33.0 OR longitude < -118.0 OR longitude > -86.0;" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 6: Station codes uniqueness
if run_test "Station codes are unique" "SELECT COUNT(*) - COUNT(DISTINCT code) FROM station_schema.stations;" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 7: Employee-station relationships
if run_test "Employee-station relationships" "SELECT COUNT(*) FROM station_schema.employees e LEFT JOIN station_schema.stations s ON e.station_id = s.id WHERE s.id IS NULL;" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 8: Campaign data validation
print_status "Validating campaign data..."
if run_count_test "Minimum campaign count" "SELECT COUNT(*) FROM coupon_schema.campaigns;" 8; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 9: Campaign date consistency
if run_test "Campaign date consistency" "SELECT COUNT(*) FROM coupon_schema.campaigns WHERE start_date >= end_date;" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 10: Campaign codes uniqueness
if run_test "Campaign codes are unique" "SELECT COUNT(*) - COUNT(DISTINCT campaign_code) FROM coupon_schema.campaigns;" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 11: Coupon data validation
print_status "Validating coupon data..."
if run_count_test "Minimum coupon count" "SELECT COUNT(*) FROM coupon_schema.coupons;" 20; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 12: Coupon-campaign relationships
if run_test "Coupon-campaign relationships" "SELECT COUNT(*) FROM coupon_schema.coupons c LEFT JOIN coupon_schema.campaigns cp ON c.campaign_id = cp.id WHERE cp.id IS NULL;" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 13: Coupon codes uniqueness
if run_test "Coupon codes are unique" "SELECT COUNT(*) - COUNT(DISTINCT coupon_code) FROM coupon_schema.coupons;" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 14: Redemption data validation
print_status "Validating redemption data..."
if run_count_test "Minimum redemption count" "SELECT COUNT(*) FROM redemption_schema.redemptions;" 5; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 15: Redemption transaction references uniqueness
if run_test "Redemption transaction references are unique" "SELECT COUNT(*) - COUNT(DISTINCT transaction_reference) FROM redemption_schema.redemptions;" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 16: Ad campaign data validation
print_status "Validating ad campaign data..."
if run_count_test "Minimum ad campaign count" "SELECT COUNT(*) FROM ad_schema.ad_campaigns;" 5; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 17: Advertisement data validation
if run_count_test "Minimum advertisement count" "SELECT COUNT(*) FROM ad_schema.advertisements;" 8; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 18: Ad engagement data validation
if run_count_test "Minimum ad engagement count" "SELECT COUNT(*) FROM ad_schema.ad_engagements;" 10; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 19: Raffle data validation
print_status "Validating raffle data..."
if run_count_test "Minimum raffle count" "SELECT COUNT(*) FROM raffle_schema.raffles;" 3; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 20: Raffle prize data validation
if run_count_test "Minimum raffle prize count" "SELECT COUNT(*) FROM raffle_schema.raffle_prizes;" 10; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 21: Raffle ticket data validation
if run_count_test "Minimum raffle ticket count" "SELECT COUNT(*) FROM raffle_schema.raffle_tickets;" 15; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 22: Data consistency - coupon usage history
if run_test "Coupon usage history consistency" "SELECT COUNT(*) FROM coupon_schema.coupon_usage_history cuh LEFT JOIN coupon_schema.coupons c ON cuh.coupon_id = c.id WHERE c.id IS NULL;" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Test 23: Data consistency - raffle entries
if run_test "Raffle entries consistency" "SELECT COUNT(*) FROM raffle_schema.raffle_entries re LEFT JOIN raffle_schema.raffles r ON re.raffle_id = r.id WHERE r.id IS NULL;" "0"; then
    ((PASSED_TESTS++))
else
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

# Summary
echo ""
print_status "=== SEED DATA VALIDATION SUMMARY ==="
echo "Total tests: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"

if [ $FAILED_TESTS -eq 0 ]; then
    print_success "All tests passed! Seed data is valid and consistent."
    exit 0
else
    print_error "$FAILED_TESTS test(s) failed. Please review the seed data."
    exit 1
fi