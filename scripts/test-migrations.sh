#!/bin/bash

# Test script to validate database migration scripts
# This script checks the syntax of all migration files

echo "🔍 Testing Database Migration Scripts..."
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counter for results
TOTAL_FILES=0
VALID_FILES=0
INVALID_FILES=0

# Function to test SQL syntax
test_sql_file() {
    local file_path="$1"
    local service_name="$2"

    TOTAL_FILES=$((TOTAL_FILES + 1))

    echo -n "Testing $service_name migration: $(basename "$file_path")... "

    # Basic syntax checks
    if ! grep -q "CREATE SCHEMA\|CREATE TABLE" "$file_path"; then
        echo -e "${RED}FAIL${NC} - No CREATE statements found"
        INVALID_FILES=$((INVALID_FILES + 1))
        return 1
    fi

    # Check for common SQL syntax issues
    if grep -q "CREATE TABLE.*(" "$file_path" && ! grep -q ");" "$file_path"; then
        echo -e "${RED}FAIL${NC} - Unclosed CREATE TABLE statement"
        INVALID_FILES=$((INVALID_FILES + 1))
        return 1
    fi

    # Check for constraint syntax
    if grep -q "CONSTRAINT.*CHECK" "$file_path"; then
        if ! grep -q "CONSTRAINT.*CHECK.*(" "$file_path"; then
            echo -e "${RED}FAIL${NC} - Invalid CHECK constraint syntax"
            INVALID_FILES=$((INVALID_FILES + 1))
            return 1
        fi
    fi

    # Check for proper schema references
    local schema_name="${service_name}_schema"
    if ! grep -q "$schema_name" "$file_path"; then
        echo -e "${YELLOW}WARN${NC} - No schema reference found"
    fi

    echo -e "${GREEN}PASS${NC}"
    VALID_FILES=$((VALID_FILES + 1))
    return 0
}

# Test Auth Service migrations
echo -e "\n${YELLOW}Auth Service:${NC}"
for file in gasolinera-jsm-ultimate/services/auth-service/src/main/resources/db/migration/*.sql; do
    if [ -f "$file" ]; then
        test_sql_file "$file" "auth"
    fi
done

# Test Station Service migrations
echo -e "\n${YELLOW}Station Service:${NC}"
for file in gasolinera-jsm-ultimate/services/station-service/src/main/resources/db/migration/*.sql; do
    if [ -f "$file" ]; then
        test_sql_file "$file" "station"
    fi
done

# Test Coupon Service migrations
echo -e "\n${YELLOW}Coupon Service:${NC}"
for file in gasolinera-jsm-ultimate/services/coupon-service/src/main/resources/db/migration/*.sql; do
    if [ -f "$file" ]; then
        test_sql_file "$file" "coupon"
    fi
done

# Test Redemption Service migrations
echo -e "\n${YELLOW}Redemption Service:${NC}"
for file in gasolinera-jsm-ultimate/services/redemption-service/src/main/resources/db/migration/*.sql; do
    if [ -f "$file" ]; then
        test_sql_file "$file" "redemption"
    fi
done

# Test Ad Engine migrations
echo -e "\n${YELLOW}Ad Engine Service:${NC}"
for file in gasolinera-jsm-ultimate/services/ad-engine/src/main/resources/db/migration/*.sql; do
    if [ -f "$file" ]; then
        test_sql_file "$file" "ad"
    fi
done

# Test Raffle Service migrations
echo -e "\n${YELLOW}Raffle Service:${NC}"
for file in gasolinera-jsm-ultimate/services/raffle-service/src/main/resources/db/migration/*.sql; do
    if [ -f "$file" ]; then
        test_sql_file "$file" "raffle"
    fi
done

# Summary
echo -e "\n========================================"
echo -e "📊 ${YELLOW}Migration Test Summary:${NC}"
echo -e "   Total files tested: $TOTAL_FILES"
echo -e "   Valid files: ${GREEN}$VALID_FILES${NC}"
echo -e "   Invalid files: ${RED}$INVALID_FILES${NC}"

if [ $INVALID_FILES -eq 0 ]; then
    echo -e "\n✅ ${GREEN}All migration scripts passed basic validation!${NC}"
    exit 0
else
    echo -e "\n❌ ${RED}Some migration scripts have issues. Please review and fix.${NC}"
    exit 1
fi