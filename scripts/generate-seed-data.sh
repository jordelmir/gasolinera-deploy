#!/bin/bash

# Seed Data Generation Script for Gasolinera JSM Platform
# This script generates additional seed data for development and testing

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
SEED_DATA_DIR="$PROJECT_ROOT/seed-data"

# Default values
USERS_COUNT=100
STATIONS_COUNT=20
CAMPAIGNS_COUNT=15
COUPONS_PER_CAMPAIGN=50
ADS_PER_CAMPAIGN=5
RAFFLES_COUNT=10

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --users)
            USERS_COUNT="$2"
            shift 2
            ;;
        --stations)
            STATIONS_COUNT="$2"
            shift 2
            ;;
        --campaigns)
            CAMPAIGNS_COUNT="$2"
            shift 2
            ;;
        --coupons-per-campaign)
            COUPONS_PER_CAMPAIGN="$2"
            shift 2
            ;;
        --ads-per-campaign)
            ADS_PER_CAMPAIGN="$2"
            shift 2
            ;;
        --raffles)
            RAFFLES_COUNT="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --users COUNT              Number of test users to generate (default: 100)"
            echo "  --stations COUNT           Number of test stations to generate (default: 20)"
            echo "  --campaigns COUNT          Number of test campaigns to generate (default: 15)"
            echo "  --coupons-per-campaign N   Number of coupons per campaign (default: 50)"
            echo "  --ads-per-campaign N       Number of ads per campaign (default: 5)"
            echo "  --raffles COUNT            Number of test raffles to generate (default: 10)"
            echo "  --help, -h                 Show this help message"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

print_status "Generating seed data for Gasolinera JSM Platform..."
print_status "Configuration:"
echo "  Users: $USERS_COUNT"
echo "  Stations: $STATIONS_COUNT"
echo "  Campaigns: $CAMPAIGNS_COUNT"
echo "  Coupons per campaign: $COUPONS_PER_CAMPAIGN"
echo "  Ads per campaign: $ADS_PER_CAMPAIGN"
echo "  Raffles: $RAFFLES_COUNT"

# Create seed data directory
mkdir -p "$SEED_DATA_DIR"

# Generate additional users
print_status "Generating additional users..."
cat > "$SEED_DATA_DIR/additional_users.sql" << EOF
-- Additional test users for development
-- Generated on $(date)

INSERT INTO auth_schema.users (
    phone_number, first_name, last_name, role, is_active, is_phone_verified
) VALUES
EOF

# Generate users with Mexican names and phone numbers
MEXICAN_FIRST_NAMES=("José" "María" "Juan" "Ana" "Luis" "Carmen" "Carlos" "Rosa" "Miguel" "Elena" "Pedro" "Luz" "Francisco" "Esperanza" "Antonio" "Dolores" "Jesús" "Guadalupe" "Alejandro" "Margarita")
MEXICAN_LAST_NAMES=("García" "Rodríguez" "Martínez" "Hernández" "López" "González" "Pérez" "Sánchez" "Ramírez" "Cruz" "Flores" "Gómez" "Díaz" "Morales" "Jiménez" "Álvarez" "Romero" "Ruiz" "Gutiérrez" "Ortiz")

for i in $(seq 1 $USERS_COUNT); do
    PHONE_BASE=$((5555100000 + i))
    FIRST_NAME=${MEXICAN_FIRST_NAMES[$((RANDOM % ${#MEXICAN_FIRST_NAMES[@]}))]}
    LAST_NAME=${MEXICAN_LAST_NAMES[$((RANDOM % ${#MEXICAN_LAST_NAMES[@]}))]}

    # Determine role based on user number
    if [ $i -le 5 ]; then
        ROLE="STATION_ADMIN"
    elif [ $i -le 20 ]; then
        ROLE="EMPLOYEE"
    else
        ROLE="CUSTOMER"
    fi

    echo "('+52$PHONE_BASE', '$FIRST_NAME', '$LAST_NAME', '$ROLE', true, true)," >> "$SEED_DATA_DIR/additional_users.sql"
done

# Remove last comma and add conflict handling
sed -i '$ s/,$//' "$SEED_DATA_DIR/additional_users.sql"
echo "" >> "$SEED_DATA_DIR/additional_users.sql"
echo "ON CONFLICT (phone_number) DO NOTHING;" >> "$SEED_DATA_DIR/additional_users.sql"

print_success "Generated $USERS_COUNT additional users"

# Generate additional stations
print_status "Generating additional stations..."
cat > "$SEED_DATA_DIR/additional_stations.sql" << EOF
-- Additional test stations for development
-- Generated on $(date)

INSERT INTO station_schema.stations (
    name, code, address, city, state, postal_code, country,
    latitude, longitude, phone_number, email, manager_name,
    status, station_type, operating_hours, services_offered, fuel_types, payment_methods,
    is_24_hours, has_convenience_store, has_car_wash, has_atm, has_restrooms,
    pump_count, capacity_vehicles_per_hour, average_service_time_minutes, created_by
) VALUES
EOF

MEXICAN_CITIES=("Guadalajara" "Monterrey" "Puebla" "Tijuana" "León" "Juárez" "Torreón" "Querétaro" "San Luis Potosí" "Mérida" "Mexicali" "Aguascalientes" "Cuernavaca" "Saltillo" "Hermosillo" "Culiacán" "Morelia" "Villahermosa" "Veracruz" "Xalapa")
MEXICAN_STATES=("Jalisco" "Nuevo León" "Puebla" "Baja California" "Guanajuato" "Chihuahua" "Coahuila" "Querétaro" "San Luis Potosí" "Yucatán" "Baja California" "Aguascalientes" "Morelos" "Coahuila" "Sonora" "Sinaloa" "Michoacán" "Tabasco" "Veracruz" "Veracruz")

for i in $(seq 1 $STATIONS_COUNT); do
    STATION_NUM=$((100 + i))
    CITY_INDEX=$((RANDOM % ${#MEXICAN_CITIES[@]}))
    CITY=${MEXICAN_CITIES[$CITY_INDEX]}
    STATE=${MEXICAN_STATES[$CITY_INDEX]}

    # Generate random coordinates within Mexico
    LAT=$(echo "scale=4; 14.5 + ($RANDOM % 1000) / 100" | bc)
    LON=$(echo "scale=4; -118.0 + ($RANDOM % 1500) / 100" | bc)

    PHONE_BASE=$((5555200000 + i))
    POSTAL_CODE=$((10000 + RANDOM % 90000))

    # Random station type
    STATION_TYPES=("FULL_SERVICE" "SELF_SERVICE" "HYBRID" "PREMIUM")
    STATION_TYPE=${STATION_TYPES[$((RANDOM % ${#STATION_TYPES[@]}))]}

    # Random pump count
    PUMP_COUNT=$((4 + RANDOM % 12))

    echo "('Gasolinera JSM $CITY $i', 'JSM$STATION_NUM', 'Av. Principal $i, $CITY', '$CITY', '$STATE', '$POSTAL_CODE', 'Mexico', $LAT, $LON, '+52$PHONE_BASE', 'jsm$STATION_NUM@gasolinerajsm.com', 'Manager $i', 'ACTIVE', '$STATION_TYPE', '06:00-22:00', 'Combustible,Tienda,Baños', 'Magna,Premium,Diesel', 'Efectivo,Tarjeta,App', false, true, false, true, true, $PUMP_COUNT, $((PUMP_COUNT * 15)), 5, 'system')," >> "$SEED_DATA_DIR/additional_stations.sql"
done

# Remove last comma and add conflict handling
sed -i '$ s/,$//' "$SEED_DATA_DIR/additional_stations.sql"
echo "" >> "$SEED_DATA_DIR/additional_stations.sql"
echo "ON CONFLICT (code) DO NOTHING;" >> "$SEED_DATA_DIR/additional_stations.sql"

print_success "Generated $STATIONS_COUNT additional stations"

# Generate additional campaigns
print_status "Generating additional campaigns..."
cat > "$SEED_DATA_DIR/additional_campaigns.sql" << EOF
-- Additional test campaigns for development
-- Generated on $(date)

INSERT INTO coupon_schema.campaigns (
    name, description, campaign_code, discount_type, discount_value,
    minimum_purchase, maximum_discount, usage_limit_per_user, total_usage_limit,
    raffle_tickets_per_coupon, start_date, end_date, is_active,
    target_audience, applicable_stations, terms_and_conditions, created_by
) VALUES
EOF

CAMPAIGN_NAMES=("Descuento Matutino" "Promoción Nocturna" "Oferta Especial" "Ahorro Familiar" "Descuento Corporativo" "Promoción Estudiante" "Oferta Weekend" "Descuento Senior" "Promoción Flash" "Ahorro Máximo" "Descuento VIP" "Oferta Limitada" "Promoción Mensual" "Descuento Anual" "Oferta Exclusiva")

for i in $(seq 1 $CAMPAIGNS_COUNT); do
    CAMPAIGN_NAME=${CAMPAIGN_NAMES[$((i % ${#CAMPAIGN_NAMES[@]}))]}
    CAMPAIGN_CODE="GEN$(printf "%03d" $i)2024"

    # Random discount type and value
    if [ $((RANDOM % 2)) -eq 0 ]; then
        DISCOUNT_TYPE="PERCENTAGE"
        DISCOUNT_VALUE=$((5 + RANDOM % 25))
    else
        DISCOUNT_TYPE="FIXED_AMOUNT"
        DISCOUNT_VALUE=$((10 + RANDOM % 40))
    fi

    MIN_PURCHASE=$((50 + RANDOM % 200))
    MAX_DISCOUNT=$((DISCOUNT_VALUE + RANDOM % 50))
    USAGE_LIMIT=$((1 + RANDOM % 5))
    TOTAL_LIMIT=$((500 + RANDOM % 2000))
    TICKETS=$((1 + RANDOM % 5))

    echo "('$CAMPAIGN_NAME $i', 'Campaña generada automáticamente para pruebas', '$CAMPAIGN_CODE', '$DISCOUNT_TYPE', $DISCOUNT_VALUE.00, $MIN_PURCHASE.00, $MAX_DISCOUNT.00, $USAGE_LIMIT, $TOTAL_LIMIT, $TICKETS, CURRENT_TIMESTAMP - INTERVAL '$((RANDOM % 30)) days', CURRENT_TIMESTAMP + INTERVAL '$((30 + RANDOM % 60)) days', true, 'ALL_CUSTOMERS', 'ALL', 'Términos y condiciones estándar', 'system')," >> "$SEED_DATA_DIR/additional_campaigns.sql"
done

# Remove last comma and add conflict handling
sed -i '$ s/,$//' "$SEED_DATA_DIR/additional_campaigns.sql"
echo "" >> "$SEED_DATA_DIR/additional_campaigns.sql"
echo "ON CONFLICT (campaign_code) DO NOTHING;" >> "$SEED_DATA_DIR/additional_campaigns.sql"

print_success "Generated $CAMPAIGNS_COUNT additional campaigns"

# Generate validation tests
print_status "Generating seed data validation tests..."
cat > "$SEED_DATA_DIR/validate_seed_data.sql" << EOF
-- Seed Data Validation Tests
-- Generated on $(date)

-- Test 1: Verify user count
SELECT 'User Count Test' as test_name,
       COUNT(*) as actual_count,
       CASE WHEN COUNT(*) >= 50 THEN 'PASS' ELSE 'FAIL' END as result
FROM auth_schema.users;

-- Test 2: Verify station count
SELECT 'Station Count Test' as test_name,
       COUNT(*) as actual_count,
       CASE WHEN COUNT(*) >= 10 THEN 'PASS' ELSE 'FAIL' END as result
FROM station_schema.stations;

-- Test 3: Verify campaign count
SELECT 'Campaign Count Test' as test_name,
       COUNT(*) as actual_count,
       CASE WHEN COUNT(*) >= 10 THEN 'PASS' ELSE 'FAIL' END as result
FROM coupon_schema.campaigns;

-- Test 4: Verify coupon count
SELECT 'Coupon Count Test' as test_name,
       COUNT(*) as actual_count,
       CASE WHEN COUNT(*) >= 20 THEN 'PASS' ELSE 'FAIL' END as result
FROM coupon_schema.coupons;

-- Test 5: Verify data integrity - users with valid phone numbers
SELECT 'Phone Number Format Test' as test_name,
       COUNT(*) as invalid_count,
       CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END as result
FROM auth_schema.users
WHERE phone_number !~ '^\+52[0-9]{10}$';

-- Test 6: Verify data integrity - stations with valid coordinates
SELECT 'Station Coordinates Test' as test_name,
       COUNT(*) as invalid_count,
       CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END as result
FROM station_schema.stations
WHERE latitude < 14.0 OR latitude > 33.0 OR longitude < -118.0 OR longitude > -86.0;

-- Test 7: Verify campaign date consistency
SELECT 'Campaign Date Consistency Test' as test_name,
       COUNT(*) as invalid_count,
       CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END as result
FROM coupon_schema.campaigns
WHERE start_date >= end_date;

-- Test 8: Verify employee-station relationships
SELECT 'Employee Station Relationship Test' as test_name,
       COUNT(*) as orphaned_employees,
       CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END as result
FROM station_schema.employees e
LEFT JOIN station_schema.stations s ON e.station_id = s.id
WHERE s.id IS NULL;

-- Summary report
SELECT 'SEED DATA VALIDATION SUMMARY' as summary,
       COUNT(CASE WHEN result = 'PASS' THEN 1 END) as passed_tests,
       COUNT(CASE WHEN result = 'FAIL' THEN 1 END) as failed_tests,
       COUNT(*) as total_tests
FROM (
    SELECT CASE WHEN COUNT(*) >= 50 THEN 'PASS' ELSE 'FAIL' END as result FROM auth_schema.users
    UNION ALL
    SELECT CASE WHEN COUNT(*) >= 10 THEN 'PASS' ELSE 'FAIL' END FROM station_schema.stations
    UNION ALL
    SELECT CASE WHEN COUNT(*) >= 10 THEN 'PASS' ELSE 'FAIL' END FROM coupon_schema.campaigns
    UNION ALL
    SELECT CASE WHEN COUNT(*) >= 20 THEN 'PASS' ELSE 'FAIL' END FROM coupon_schema.coupons
    UNION ALL
    SELECT CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END FROM auth_schema.users WHERE phone_number !~ '^\+52[0-9]{10}$'
    UNION ALL
    SELECT CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END FROM station_schema.stations WHERE latitude < 14.0 OR latitude > 33.0 OR longitude < -118.0 OR longitude > -86.0
    UNION ALL
    SELECT CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END FROM coupon_schema.campaigns WHERE start_date >= end_date
    UNION ALL
    SELECT CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END FROM station_schema.employees e LEFT JOIN station_schema.stations s ON e.station_id = s.id WHERE s.id IS NULL
) validation_results;
EOF

print_success "Generated seed data validation tests"

# Create a master script to apply all seed data
cat > "$SEED_DATA_DIR/apply_all_seed_data.sql" << EOF
-- Master script to apply all seed data
-- Generated on $(date)

\\echo 'Applying additional users...'
\\i additional_users.sql

\\echo 'Applying additional stations...'
\\i additional_stations.sql

\\echo 'Applying additional campaigns...'
\\i additional_campaigns.sql

\\echo 'Running validation tests...'
\\i validate_seed_data.sql

\\echo 'Seed data application completed!'
EOF

# Create README for seed data
cat > "$SEED_DATA_DIR/README.md" << EOF
# Seed Data for Gasolinera JSM Platform

This directory contains additional seed data generated for development and testing purposes.

## Generated Files

- \`additional_users.sql\` - $USERS_COUNT additional test users
- \`additional_stations.sql\` - $STATIONS_COUNT additional test stations
- \`additional_campaigns.sql\` - $CAMPAIGNS_COUNT additional test campaigns
- \`validate_seed_data.sql\` - Validation tests for seed data integrity
- \`apply_all_seed_data.sql\` - Master script to apply all seed data

## Usage

### Apply All Seed Data
\`\`\`bash
# Using psql
psql -h localhost -U gasolinera_user -d gasolinera_db -f seed-data/apply_all_seed_data.sql

# Using Docker
docker-compose exec postgres psql -U gasolinera_user -d gasolinera_db -f /seed-data/apply_all_seed_data.sql
\`\`\`

### Apply Individual Files
\`\`\`bash
psql -h localhost -U gasolinera_user -d gasolinera_db -f seed-data/additional_users.sql
psql -h localhost -U gasolinera_user -d gasolinera_db -f seed-data/additional_stations.sql
psql -h localhost -U gasolinera_user -d gasolinera_db -f seed-data/additional_campaigns.sql
\`\`\`

### Run Validation Tests
\`\`\`bash
psql -h localhost -U gasolinera_user -d gasolinera_db -f seed-data/validate_seed_data.sql
\`\`\`

## Data Overview

### Users
- System admins: 1
- Station admins: 5
- Employees: 15
- Customers: $(($USERS_COUNT - 20))
- Total: $USERS_COUNT additional users

### Stations
- Distributed across major Mexican cities
- Various station types (Full Service, Self Service, Hybrid, Premium)
- Random but realistic coordinates within Mexico
- Total: $STATIONS_COUNT additional stations

### Campaigns
- Mix of percentage and fixed amount discounts
- Various target audiences and usage limits
- Active campaigns with realistic date ranges
- Total: $CAMPAIGNS_COUNT additional campaigns

## Validation

The validation script checks:
- Minimum data counts
- Phone number format compliance
- Geographic coordinate validity
- Date consistency in campaigns
- Referential integrity between tables

## Regeneration

To regenerate seed data with different parameters:

\`\`\`bash
./scripts/generate-seed-data.sh --users 200 --stations 50 --campaigns 25
\`\`\`

Generated on: $(date)
EOF

print_success "Seed data generation completed!"
print_status "Files created in: $SEED_DATA_DIR"
print_status "To apply seed data, run: psql -h localhost -U gasolinera_user -d gasolinera_db -f $SEED_DATA_DIR/apply_all_seed_data.sql"