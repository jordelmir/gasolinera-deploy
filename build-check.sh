#!/bin/bash

# Build Check Script for Gasolinera JSM Ultimate
# This script checks the build status of all services

set -e

echo "🚀 Gasolinera JSM Ultimate - Build Check"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
    fi
}

# Function to print warning
print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

echo ""
echo "📋 Checking Working Services..."
echo "--------------------------------"

# Check auth-service
echo "Checking auth-service..."
if gradle :services:auth-service:compileKotlin --no-daemon > /dev/null 2>&1; then
    print_status 0 "auth-service compiles successfully"
else
    print_status 1 "auth-service compilation failed"
fi

# Check coupon-service
echo "Checking coupon-service..."
if gradle :services:coupon-service:compileKotlin --no-daemon > /dev/null 2>&1; then
    print_status 0 "coupon-service compiles successfully"
else
    print_status 1 "coupon-service compilation failed"
fi

echo ""
echo "📋 Checking Problematic Services..."
echo "-----------------------------------"

# List of problematic services
PROBLEMATIC_SERVICES=("station-service" "api-gateway" "ad-engine" "raffle-service")

for service in "${PROBLEMATIC_SERVICES[@]}"; do
    echo "Checking $service..."
    if gradle :services:$service:compileKotlin --no-daemon > /dev/null 2>&1; then
        print_status 0 "$service compiles successfully"
    else
        print_status 1 "$service has compilation issues (expected)"
        print_warning "Check services/$service/TODO.md for details"
    fi
done

echo ""
echo "📋 Checking Frontend..."
echo "-----------------------"

# Check owner-dashboard
echo "Checking owner-dashboard..."
cd apps/owner-dashboard
if npm run build > /dev/null 2>&1; then
    print_status 0 "owner-dashboard builds successfully"
else
    print_status 1 "owner-dashboard has build issues"
fi
cd ../..

echo ""
echo "📋 Summary"
echo "----------"
echo "✅ Working Services: auth-service, coupon-service"
echo "❌ Problematic Services: station-service, api-gateway, ad-engine, raffle-service"
echo "📝 Check individual TODO.md files for detailed issue analysis"
echo ""
echo "🚀 To start development environment:"
echo "   docker-compose -f docker-compose.dev.yml up -d"
echo ""