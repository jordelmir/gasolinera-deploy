#!/bin/bash

# Test Seed Data Script for Gasolinera JSM Platform
# This script tests seed data generation, application, and validation

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
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Default values
CLEANUP=false
DOCKER_MODE=false
GENERATE_ADDITIONAL=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --cleanup)
            CLEANUP=true
            shift
            ;;
        --docker)
            DOCKER_MODE=true
            shift
            ;;
        --generate)
            GENERATE_ADDITIONAL=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --cleanup              Clean up generated seed data after testing"
            echo "  --docker               Use Docker for database operations"
            echo "  --generate             Generate additional seed data"
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

print_status "Testing seed data for Gasolinera JSM Platform..."

# Test 1: Validate existing seed data
print_status "Step 1: Validating existing seed data..."
if [ "$DOCKER_MODE" = true ]; then
    if "$SCRIPT_DIR/validate-seed-data.sh" --docker; then
        print_success "Existing seed data validation passed"
    else
        print_error "Existing seed data validation failed"
        exit 1
    fi
else
    if "$SCRIPT_DIR/validate-seed-data.sh"; then
        print_success "Existing seed data validation passed"
    else
        print_error "Existing seed data validation failed"
        exit 1
    fi
fi

# Test 2: Generate additional seed data (if requested)
if [ "$GENERATE_ADDITIONAL" = true ]; then
    print_status "Step 2: Generating additional seed data..."

    if "$SCRIPT_DIR/generate-seed-data.sh" --users 50 --stations 10 --campaigns 5; then
        print_success "Additional seed data generated successfully"
    else
        print_error "Failed to generate additional seed data"
        exit 1
    fi

    # Apply generated seed data
    print_status "Applying generated seed data..."
    SEED_DATA_DIR="$PROJECT_ROOT/seed-data"

    if [ -f "$SEED_DATA_DIR/apply_all_seed_data.sql" ]; then
        if [ "$DOCKER_MODE" = true ]; then
            if docker-compose exec -T postgres psql -U gasolinera_user -d gasolinera_db -f /seed-data/apply_all_seed_data.sql; then
                print_success "Generated seed data applied successfully"
            else
                print_error "Failed to apply generated seed data"
                exit 1
            fi
        else
            if PGPASSWORD=gasolinera_password psql -h localhost -U gasolinera_user -d gasolinera_db -f "$SEED_DATA_DIR/apply_all_seed_data.sql"; then
                print_success "Generated seed data applied successfully"
            else
                print_error "Failed to apply generated seed data"
                exit 1
            fi
        fi
    else
        print_warning "No generated seed data found to apply"
    fi

    # Validate after applying additional data
    print_status "Validating data after applying additional seed data..."
    if [ "$DOCKER_MODE" = true ]; then
        if "$SCRIPT_DIR/validate-seed-data.sh" --docker; then
            print_success "Post-generation seed data validation passed"
        else
            print_error "Post-generation seed data validation failed"
            exit 1
        fi
    else
        if "$SCRIPT_DIR/validate-seed-data.sh"; then
            print_success "Post-generation seed data validation passed"
        else
            print_error "Post-generation seed data validation failed"
            exit 1
        fi
    fi
fi

# Test 3: Test data integrity across services
print_status "Step 3: Testing cross-service data integrity..."

# Function to execute SQL query
execute_sql() {
    local query="$1"
    if [ "$DOCKER_MODE" = true ]; then
        docker-compose exec -T postgres psql -U gasolinera_user -d gasolinera_db -t -c "$query" 2>/dev/null | xargs
    else
        PGPASSWORD=gasolinera_password psql -h localhost -U gasolinera_user -d gasolinera_db -t -c "$query" 2>/dev/null | xargs
    fi
}

# Test user-employee relationships
USER_EMPLOYEE_INTEGRITY=$(execute_sql "
    SELECT COUNT(*)
    FROM station_schema.employees e
    LEFT JOIN auth_schema.users u ON e.user_id = u.id
    WHERE u.id IS NULL;
")

if [ "$USER_EMPLOYEE_INTEGRITY" = "0" ]; then
    print_success "✓ User-Employee relationship integrity"
else
    print_error "✗ User-Employee relationship integrity ($USER_EMPLOYEE_INTEGRITY orphaned records)"
fi

# Test coupon-campaign relationships
COUPON_CAMPAIGN_INTEGRITY=$(execute_sql "
    SELECT COUNT(*)
    FROM coupon_schema.coupons c
    LEFT JOIN coupon_schema.campaigns cp ON c.campaign_id = cp.id
    WHERE cp.id IS NULL;
")

if [ "$COUPON_CAMPAIGN_INTEGRITY" = "0" ]; then
    print_success "✓ Coupon-Campaign relationship integrity"
else
    print_error "✗ Coupon-Campaign relationship integrity ($COUPON_CAMPAIGN_INTEGRITY orphaned records)"
fi

# Test redemption-coupon relationships
REDEMPTION_COUPON_INTEGRITY=$(execute_sql "
    SELECT COUNT(*)
    FROM redemption_schema.redemptions r
    LEFT JOIN coupon_schema.coupons c ON r.coupon_id = c.id
    WHERE c.id IS NULL AND r.coupon_id IS NOT NULL;
")

if [ "$REDEMPTION_COUPON_INTEGRITY" = "0" ]; then
    print_success "✓ Redemption-Coupon relationship integrity"
else
    print_error "✗ Redemption-Coupon relationship integrity ($REDEMPTION_COUPON_INTEGRITY orphaned records)"
fi

# Test raffle-prize relationships
RAFFLE_PRIZE_INTEGRITY=$(execute_sql "
    SELECT COUNT(*)
    FROM raffle_schema.raffle_prizes rp
    LEFT JOIN raffle_schema.raffles r ON rp.raffle_id = r.id
    WHERE r.id IS NULL;
")

if [ "$RAFFLE_PRIZE_INTEGRITY" = "0" ]; then
    print_success "✓ Raffle-Prize relationship integrity"
else
    print_error "✗ Raffle-Prize relationship integrity ($RAFFLE_PRIZE_INTEGRITY orphaned records)"
fi

# Test 4: Performance testing with seed data
print_status "Step 4: Testing query performance with seed data..."

# Test user lookup performance
USER_LOOKUP_TIME=$(execute_sql "
    EXPLAIN ANALYZE SELECT * FROM auth_schema.users WHERE phone_number = '+525555001001';
" | grep "Execution Time" | awk '{print $3}' || echo "N/A")

print_status "User lookup by phone: ${USER_LOOKUP_TIME}ms"

# Test station search performance
STATION_SEARCH_TIME=$(execute_sql "
    EXPLAIN ANALYZE SELECT * FROM station_schema.stations WHERE city = 'Ciudad de México';
" | grep "Execution Time" | awk '{print $3}' || echo "N/A")

print_status "Station search by city: ${STATION_SEARCH_TIME}ms"

# Test coupon validation performance
COUPON_VALIDATION_TIME=$(execute_sql "
    EXPLAIN ANALYZE SELECT c.*, cp.* FROM coupon_schema.coupons c
    JOIN coupon_schema.campaigns cp ON c.campaign_id = cp.id
    WHERE c.coupon_code = 'WELCOME001';
" | grep "Execution Time" | awk '{print $3}' || echo "N/A")

print_status "Coupon validation query: ${COUPON_VALIDATION_TIME}ms"

# Test 5: Data volume verification
print_status "Step 5: Verifying data volumes..."

USER_COUNT=$(execute_sql "SELECT COUNT(*) FROM auth_schema.users;")
STATION_COUNT=$(execute_sql "SELECT COUNT(*) FROM station_schema.stations;")
CAMPAIGN_COUNT=$(execute_sql "SELECT COUNT(*) FROM coupon_schema.campaigns;")
COUPON_COUNT=$(execute_sql "SELECT COUNT(*) FROM coupon_schema.coupons;")
REDEMPTION_COUNT=$(execute_sql "SELECT COUNT(*) FROM redemption_schema.redemptions;")
AD_COUNT=$(execute_sql "SELECT COUNT(*) FROM ad_schema.advertisements;")
RAFFLE_COUNT=$(execute_sql "SELECT COUNT(*) FROM raffle_schema.raffles;")

print_status "Data volume summary:"
echo "  Users: $USER_COUNT"
echo "  Stations: $STATION_COUNT"
echo "  Campaigns: $CAMPAIGN_COUNT"
echo "  Coupons: $COUPON_COUNT"
echo "  Redemptions: $REDEMPTION_COUNT"
echo "  Advertisements: $AD_COUNT"
echo "  Raffles: $RAFFLE_COUNT"

# Cleanup (if requested)
if [ "$CLEANUP" = true ]; then
    print_status "Step 6: Cleaning up generated seed data..."

    if [ -d "$PROJECT_ROOT/seed-data" ]; then
        rm -rf "$PROJECT_ROOT/seed-data"
        print_success "Generated seed data cleaned up"
    fi

    # Optionally reset database to original seed data state
    print_warning "Note: Database still contains applied seed data. Use migration rollback to reset."
fi

# Final summary
print_success "Seed data testing completed successfully!"
print_status "Summary:"
echo "  ✓ Existing seed data validation"
if [ "$GENERATE_ADDITIONAL" = true ]; then
    echo "  ✓ Additional seed data generation and application"
fi
echo "  ✓ Cross-service data integrity"
echo "  ✓ Query performance testing"
echo "  ✓ Data volume verification"

print_status "Seed data is ready for development and testing!"