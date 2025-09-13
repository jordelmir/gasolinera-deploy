#!/bin/bash

# Development Data Seeder Script
# Populates the development environment with realistic test data

setil

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
API_BASE_URL="http://api-gateway:8080"
AUTH_SERVICE_URL="http://auth-service:8080"
COUPON_SERVICE_URL="http://coupon-service:8080"
STATION_SERVICE_URL="http://station-service:8080"
RAFFLE_SERVICE_URL="http://raffle-service:8080"

# Wait times
MAX_WAIT_TIME=300
CHECK_INTERVAL=5

# Functions
print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  Development Data Seeder${NC}"
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

wait_for_service() {
    local service_name="$1"
    local service_url="$2"
    local wait_time=0

    print_status "Waiting for $service_name to be ready..."

    while [ $wait_time -lt $MAX_WAIT_TIME ]; do
        if curl -f -s "$service_url/actuator/health" > /dev/null 2>&1; then
            print_success "$service_name is ready!"
            return 0
        fi

        sleep $CHECK_INTERVAL
        wait_time=$((wait_time + CHECK_INTERVAL))

        if [ $((wait_time % 30)) -eq 0 ]; then
            print_status "Still waiting for $service_name... (${wait_time}s elapsed)"
        fi
    done

    print_error "$service_name failed to start within ${MAX_WAIT_TIME}s"
    return 1
}

wait_for_database() {
    print_status "Waiting for PostgreSQL to be ready..."

    local wait_time=0
    while [ $wait_time -lt $MAX_WAIT_TIME ]; do
        if pg_isready -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" > /dev/null 2>&1; then
            print_success "PostgreSQL is ready!"
            return 0
        fi

        sleep $CHECK_INTERVAL
        wait_time=$((wait_time + CHECK_INTERVAL))
    done

    print_error "PostgreSQL failed to start within ${MAX_WAIT_TIME}s"
    return 1
}

wait_for_redis() {
    print_status "Waiting for Redis to be ready..."

    local wait_time=0
    while [ $wait_time -lt $MAX_WAIT_TIME ]; do
        if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping > /dev/null 2>&1; then
            print_success "Redis is ready!"
            return 0
        fi

        sleep $CHECK_INTERVAL
        wait_time=$((wait_time + CHECK_INTERVAL))
    done

    print_error "Redis failed to start within ${MAX_WAIT_TIME}s"
    return 1
}

create_admin_user() {
    print_status "Creating admin user..."

    local admin_data='{
        "email": "admin@gasolinera.com",
        "password": "Admin123!",
        "firstName": "Admin",
        "lastName": "User",
        "phoneNumber": "+1234567890",
        "role": "ADMIN"
    }'

    local response
    if response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d "$admin_data" \
        "$AUTH_SERVICE_URL/api/auth/register"); then

        print_success "Admin user created successfully"
        echo "$response" | jq -r '.token' > /tmp/admin_token
        return 0
    else
        print_warning "Admin user might already exist or service not ready"
        return 1
    fi
}

create_test_users() {
    print_status "Creating test users..."

    local users=(
        '{"email":"user1@test.com","password":"Test123!","firstName":"John","lastName":"Doe","phoneNumber":"+1234567891","role":"USER"}'
        '{"email":"user2@test.com","password":"Test123!","firstName":"Jane","lastName":"Smith","phoneNumber":"+1234567892","role":"USER"}'
        '{"email":"owner1@test.com","password":"Test123!","firstName":"Mike","lastName":"Johnson","phoneNumber":"+1234567893","role":"STATION_OWNER"}'
        '{"email":"owner2@test.com","password":"Test123!","firstName":"Sarah","lastName":"Wilson","phoneNumber":"+1234567894","role":"STATION_OWNER"}'
        '{"email":"advertiser1@test.com","password":"Test123!","firstName":"David","lastName":"Brown","phoneNumber":"+1234567895","role":"ADVERTISER"}'
    )

    for user_data in "${users[@]}"; do
        local email
        email=$(echo "$user_data" | jq -r '.email')

        if curl -s -X POST \
            -H "Content-Type: application/json" \
            -d "$user_data" \
            "$AUTH_SERVICE_URL/api/auth/register" > /dev/null; then
            print_success "Created user: $email"
        else
            print_warning "Failed to create user: $email (might already exist)"
        fi
    done
}

create_gas_stations() {
    print_status "Creating gas stations..."

    local stations=(
        '{"name":"Gasolinera Centro","address":"Av. Principal 123, Centro","latitude":19.4326,"longitude":-99.1332,"fuelTypes":["REGULAR","PREMIUM","DIESEL"],"services":["CONVENIENCE_STORE","CAR_WASH","ATM"]}'
        '{"name":"Gasolinera Norte","address":"Blvd. Norte 456, Zona Norte","latitude":19.4978,"longitude":-99.1269,"fuelTypes":["REGULAR","PREMIUM"],"services":["CONVENIENCE_STORE","TIRE_REPAIR"]}'
        '{"name":"Gasolinera Sur","address":"Av. Sur 789, Zona Sur","latitude":19.3910,"longitude":-99.1426,"fuelTypes":["REGULAR","PREMIUM","DIESEL","ELECTRIC"],"services":["CONVENIENCE_STORE","CAR_WASH","ATM","ELECTRIC_CHARGING"]}'
        '{"name":"Gasolinera Este","address":"Calle Este 321, Zona Este","latitude":19.4284,"longitude":-99.0921,"fuelTypes":["REGULAR","PREMIUM"],"services":["CONVENIENCE_STORE"]}'
        '{"name":"Gasolinera Oeste","address":"Av. Oeste 654, Zona Oeste","latitude":19.4395,"longitude":-99.2044,"fuelTypes":["REGULAR","PREMIUM","DIESEL"],"services":["CONVENIENCE_STORE","CAR_WASH","TIRE_REPAIR"]}'
    )

    local admin_token
    if [ -f /tmp/admin_token ]; then
        admin_token=$(cat /tmp/admin_token)
    else
        print_error "Admin token not found. Cannot create stations."
        return 1
    fi

    for station_data in "${stations[@]}"; do
        local station_name
        station_name=$(echo "$station_data" | jq -r '.name')

        if curl -s -X POST \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $admin_token" \
            -d "$station_data" \
            "$STATION_SERVICE_URL/api/stations" > /dev/null; then
            print_success "Created station: $station_name"
        else
            print_warning "Failed to create station: $station_name"
        fi
    done
}

create_coupon_campaigns() {
    print_status "Creating coupon campaigns..."

    local campaigns=(
        '{"name":"Welcome Campaign","description":"Welcome discount for new users","discountType":"PERCENTAGE","discountValue":10,"validFrom":"2024-01-01T00:00:00Z","validUntil":"2024-12-31T23:59:59Z","maxUsages":1000,"active":true}'
        '{"name":"Weekend Special","description":"Weekend fuel discount","discountType":"FIXED_AMOUNT","discountValue":50,"validFrom":"2024-01-01T00:00:00Z","validUntil":"2024-12-31T23:59:59Z","maxUsages":500,"active":true}'
        '{"name":"Premium Fuel Promo","description":"Premium fuel promotion","discountType":"PERCENTAGE","discountValue":15,"validFrom":"2024-01-01T00:00:00Z","validUntil":"2024-06-30T23:59:59Z","maxUsages":200,"active":true}'
    )

    local admin_token
    if [ -f /tmp/admin_token ]; then
        admin_token=$(cat /tmp/admin_token)
    else
        print_error "Admin token not found. Cannot create campaigns."
        return 1
    fi

    for campaign_data in "${campaigns[@]}"; do
        local campaign_name
        campaign_name=$(echo "$campaign_data" | jq -r '.name')

        if curl -s -X POST \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $admin_token" \
            -d "$campaign_data" \
            "$COUPON_SERVICE_URL/api/campaigns" > /dev/null; then
            print_success "Created campaign: $campaign_name"
        else
            print_warning "Failed to create campaign: $campaign_name"
        fi
    done
}

create_raffles() {
    print_status "Creating raffles..."

    local raffles=(
        '{"name":"Monthly Car Raffle","description":"Win a brand new car!","prizeDescription":"2024 Toyota Corolla","startDate":"2024-01-01T00:00:00Z","endDate":"2024-01-31T23:59:59Z","maxParticipants":1000,"ticketPrice":100,"active":true}'
        '{"name":"Fuel Credit Raffle","description":"Win free fuel for a year","prizeDescription":"$5000 fuel credit","startDate":"2024-02-01T00:00:00Z","endDate":"2024-02-28T23:59:59Z","maxParticipants":500,"ticketPrice":50,"active":true}'
        '{"name":"Electronics Raffle","description":"Win the latest smartphone","prizeDescription":"iPhone 15 Pro Max","startDate":"2024-03-01T00:00:00Z","endDate":"2024-03-31T23:59:59Z","maxParticipants":300,"ticketPrice":25,"active":true}'
    )

    local admin_token
    if [ -f /tmp/admin_token ]; then
        admin_token=$(cat /tmp/admin_token)
    else
        print_error "Admin token not found. Cannot create raffles."
        return 1
    fi

    for raffle_data in "${raffles[@]}"; do
        local raffle_name
        raffle_name=$(echo "$raffle_data" | jq -r '.name')

        if curl -s -X POST \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $admin_token" \
            -d "$raffle_data" \
            "$RAFFLE_SERVICE_URL/api/raffles" > /dev/null; then
            print_success "Created raffle: $raffle_name"
        else
            print_warning "Failed to create raffle: $raffle_name"
        fi
    done
}

populate_redis_cache() {
    print_status "Populating Redis cache with sample data..."

    # Sample cache entries for development
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" SET "fuel_prices:station_1:regular" "20.50" EX 3600
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" SET "fuel_prices:station_1:premium" "22.30" EX 3600
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" SET "fuel_prices:station_1:diesel" "21.80" EX 3600

    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" SET "station_status:station_1" "OPEN" EX 1800
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" SET "station_status:station_2" "OPEN" EX 1800
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" SET "station_status:station_3" "MAINTENANCE" EX 1800

    # Sample session data
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" SET "session:dev_user_1" '{"userId":"1","email":"admin@gasolinera.com","role":"ADMIN"}' EX 7200

    print_success "Redis cache populated with sample data"
}

run_database_migrations() {
    print_status "Running database migrations..."

    # Check if migrations directory exists
    if [ -d "/app/data/migrations" ]; then
        for migration_file in /app/data/migrations/*.sql; do
            if [ -f "$migration_file" ]; then
                local filename
                filename=$(basename "$migration_file")
                print_status "Running migration: $filename"

                if psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f "$migration_file" > /dev/null 2>&1; then
                    print_success "Migration completed: $filename"
                else
                    print_warning "Migration failed or already applied: $filename"
                fi
            fi
        done
    else
        print_warning "No migrations directory found"
    fi
}

main() {
    print_header

    # Wait for infrastructure services
    wait_for_database
    wait_for_redis

    # Wait for application services
    wait_for_service "Auth Service" "$AUTH_SERVICE_URL"
    wait_for_service "Coupon Service" "$COUPON_SERVICE_URL"
    wait_for_service "Station Service" "$STATION_SERVICE_URL"
    wait_for_service "Raffle Service" "$RAFFLE_SERVICE_URL"

    # Run database migrations
    run_database_migrations

    # Seed data based on environment variables
    if [ "${SEED_USERS:-true}" = "true" ]; then
        create_admin_user
        create_test_users
    fi

    if [ "${SEED_STATIONS:-true}" = "true" ]; then
        create_gas_stations
    fi

    if [ "${SEED_CAMPAIGNS:-true}" = "true" ]; then
        create_coupon_campaigns
    fi

    if [ "${SEED_RAFFLES:-true}" = "true" ]; then
        create_raffles
    fi

    # Populate cache
    populate_redis_cache

    print_success "Development data seeding completed!"
    print_status "Available test accounts:"
    echo "  Admin: admin@gasolinera.com / Admin123!"
    echo "  User: user1@test.com / Test123!"
    echo "  Owner: owner1@test.com / Test123!"
    echo "  Advertiser: advertiser1@test.com / Test123!"
    echo ""
    print_status "Services available at:"
    echo "  API Gateway: http://localhost:8080"
    echo "  PgAdmin: http://localhost:5050"
    echo "  Redis Commander: http://localhost:8081"
    echo "  RabbitMQ Management: http://localhost:15672"
    echo "  Jaeger UI: http://localhost:16686"
    echo "  Vault UI: http://localhost:8200"
}

# Run main function
main "$@"