# Makefile for Gasolinera JSM

.PHONY: help build-all build-optimized build-sequential build-and-push build-complete build-fast dev dev-frontend stop clean logs test seed mobile k8s-up k8s-down lint format check-deps quality-check auto-fix gradle-build gradle-test gradle-clean security-scan docker-clean docker-size docker-inspect build-service build-no-cache build-with-tests build-production dev-optimized dev-rebuild docker-logs docker-stats docker-health ci-build cd-deploy local-test clean-all reset-dev help-docker

help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Development:"
	@echo "  dev          Starts the full development environment using Docker Compose."
	@echo "  dev-frontend Starts only frontend apps in development mode."
	@echo "  stop         Stops the development environment."
	@echo "  clean        Stops and removes all containers, networks, and volumes."
	@echo "  logs         Follows the logs of all running services."
	@echo ""
	@echo "Build & Test:"
	@echo "  build-all         Builds all Docker images with optimization."
	@echo "  build-optimized   Builds optimized Docker images with caching."
	@echo "  build-sequential  Builds Docker images sequentially."
	@echo "  build-and-push    Builds and pushes Docker images to registry."
	@echo "  build-complete    Runs complete build pipeline (build + test + scan)."
	@echo "  build-fast        Fast build without tests or security scan."
	@echo "  test              Runs all tests for all services."
	@echo "  lint              Runs linting for all projects."
	@echo "  format            Formats code using Prettier."
	@echo ""
	@echo "Security & Docker:"
	@echo "  security-scan     Scans Docker images for vulnerabilities."
	@echo "  docker-clean      Cleans up Docker build cache and dangling images."
	@echo "  docker-size       Shows Docker image sizes."
	@echo ""
	@echo "Data & Mobile:"
	@echo "  seed         Seeds the database with initial data."
	@echo "  mobile       Starts the React Native development server."
	@echo ""
	@echo "Kubernetes:"
	@echo "  k8s-up       Deploys the application to a local Kubernetes cluster."
	@echo "  k8s-down     Removes the application from the local Kubernetes cluster."
	@echo ""
	@echo "Quality & Code Analysis:"
	@echo "  quality-check Runs comprehensive quality checks (ktlint, detekt, tests)"
	@echo "  auto-fix     Automatically fixes common code quality issues"
	@echo "  gradle-build Builds all Gradle services"
	@echo "  gradle-test  Runs all Gradle tests with coverage"
	@echo "  gradle-clean Cleans all Gradle build artifacts"
	@echo ""
	@echo "Utilities:"
	@echo "  check-deps   Checks for outdated dependencies."

build-all:
	@echo "Building all Docker images with optimization..."
	./scripts/docker-build-optimized.sh

build-optimized:
	@echo "Building optimized Docker images with caching..."
	./scripts/docker-build-optimized.sh --parallel 4

build-sequential:
	@echo "Building Docker images sequentially..."
	./scripts/docker-build-optimized.sh --sequential

build-and-push:
	@echo "Building and pushing Docker images to registry..."
	./scripts/docker-build-optimized.sh --push

build-complete:
	@echo "Running complete build pipeline (build + test + scan)..."
	./scripts/docker-complete-build.sh

build-fast:
	@echo "Fast build without tests or security scan..."
	./scripts/docker-complete-build.sh --no-tests --no-security-scan

dev:
	@echo "Starting development environment..."
	docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

dev-frontend:
	@echo "Starting frontend development servers..."
	npm run nx -- run-many --target=serve --projects=admin,advertiser --parallel

stop:
	@echo "Stopping development environment..."
	docker compose down

clean:
	@echo "Cleaning up development environment..."
	docker compose down -v --remove-orphans

logs:
	@echo "Following logs..."
	docker compose logs -f

test:
	@echo "Running tests..."
	npm run nx -- run-many --target=test --all

seed:
	@echo "Seeding database..."
	npm run nx -- run ops:seed

seed-coupon-system:
	@echo "Seeding coupon system data..."
	ts-node ops/scripts/seed-coupon-system.ts

mobile:
	@echo "Starting mobile app..."
	cd apps/mobile && npm start

k8s-up:
	@echo "Deploying to Kubernetes..."
	helm upgrade --install gasolinera-jsm ./infra/helm/gasolinera-jsm -f ./infra/helm/gasolinera-jsm/values.yaml

k8s-down:
	@echo "Removing from Kubernetes..."
	helm uninstall gasolinera-jsm
lint:
	@echo "Running linting..."
	npm run lint

format:
	@echo "Formatting code..."
	npm run format

check-deps:
	@echo "Checking for outdated dependencies..."
	npm outdated

# Mobile development
client-mobile:
	@echo "Starting client mobile app..."
	cd apps/client-mobile && npm start

employee-mobile:
	@echo "Starting employee mobile app..."
	cd apps/employee-mobile && npm start

# Frontend development
owner-dashboard:
	@echo "Starting owner dashboard..."
	cd apps/owner-dashboard && npm run dev

# Full system commands
dev-mobile:
	@echo "Starting all mobile apps..."
	make client-mobile & make employee-mobile

dev-web:
	@echo "Starting all web apps..."
	make owner-dashboard & npm run nx -- serve admin --port 3000 & npm run nx -- serve advertiser --port 3001

# Production deployment
deploy-staging:
	@echo "Deploying to staging..."
	docker-compose -f docker-compose.yml -f docker-compose.staging.yml up -d

deploy-production:
	@echo "Deploying to production..."
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Database operations
db-migrate:
	@echo "Running database migrations..."
	npm run nx -- run ops:migrate

db-backup:
	@echo "Creating database backup..."
	docker exec postgres pg_dump -U puntog puntog > backup_$(shell date +%Y%m%d_%H%M%S).sql

db-restore:
	@echo "Restoring database from backup..."
	@read -p "Enter backup file path: " backup_file; \
	docker exec -i postgres psql -U puntog -d puntog < $$backup_file
# Qu
ality and Code Analysis
quality-check:
	@echo "Running comprehensive quality checks..."
	./scripts/quality-check.sh

auto-fix:
	@echo "Running auto-fix for common code issues..."
	./scripts/auto-fix.sh

gradle-build:
	@echo "Building all Gradle services..."
	./gradlew build --continue

gradle-test:
	@echo "Running all Gradle tests with coverage..."
	./gradlew test jacocoTestReport --continue

gradle-clean:
	@echo "Cleaning all Gradle build artifacts..."
	./gradlew clean

# Gradle specific commands
gradle-ktlint:
	@echo "Running ktlint check..."
	./gradlew ktlintCheck

gradle-ktlint-fix:
	@echo "Auto-fixing ktlint issues..."
	./gradlew ktlintFormat

gradle-detekt:
	@echo "Running detekt analysis..."
	./gradlew detekt

gradle-detekt-fix:
	@echo "Running detekt with auto-correct..."
	./gradlew detekt --auto-correct

# Service-specific builds
build-auth:
	@echo "Building auth service..."
	./gradlew :services:auth-service:build

build-coupon:
	@echo "Building coupon service..."
	./gradlew :services:coupon-service:build

build-station:
	@echo "Building station service..."
	./gradlew :services:station-service:build

build-redemption:
	@echo "Building redemption service..."
	./gradlew :services:redemption-service:build

build-ad-engine:
	@echo "Building ad engine..."
	./gradlew :services:ad-engine:build

build-raffle:
	@echo "Building raffle service..."
	./gradlew :services:raffle-service:build

build-gateway:
	@echo "Building API gateway..."
	./gradlew :services:api-gateway:build

# Testing commands
test-unit:
	@echo "Running unit tests..."
	./scripts/run-tests.sh unit

test-integration:
	@echo "Running integration tests..."
	./scripts/run-tests.sh integration

test-e2e:
	@echo "Running end-to-end tests..."
	./scripts/run-tests.sh e2e

test-performance:
	@echo "Running performance tests..."
	./scripts/run-tests.sh performance

test-all:
	@echo "Running all tests..."
	./scripts/run-tests.sh all

test-coverage:
	@echo "Generating test coverage report..."
	./gradlew test jacocoTestReport
	@echo "Coverage report available at: build/reports/jacoco/test/html/index.html"

# TestContainers management
containers-start:
	@echo "Starting TestContainers..."
	docker-compose -f docker-compose.test.yml up -d

containers-stop:
	@echo "Stopping TestContainers..."
	docker-compose -f docker-compose.test.yml down

containers-clean:
	@echo "Cleaning TestContainers..."
	docker-compose -f docker-compose.test.yml down -v --remove-orphans

# Pre-commit hook
pre-commit: auto-fix quality-check test-unit
	@echo "Pre-commit checks completed successfully!"
#
 Docker Security and Optimization
security-scan:
	@echo "Scanning Docker images for security vulnerabilities..."
	./scripts/docker-security-scan.sh

security-scan-critical:
	@echo "Scanning for CRITICAL vulnerabilities only..."
	./scripts/docker-security-scan.sh --severity CRITICAL

docker-clean:
	@echo "Cleaning up Docker build cache and dangling images..."
	docker buildx prune --filter until=24h --force
	docker image prune --force
	docker system prune --force

docker-size:
	@echo "Showing Docker image sizes..."
	@echo "Current images:"
	docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep gasolinera-jsm || echo "No gasolinera-jsm images found"

docker-inspect:
	@echo "Inspecting Docker images for optimization opportunities..."
	@for service in auth-service api-gateway coupon-service station-service raffle-service redemption-service ad-engine; do \
		echo "=== $$service ==="; \
		docker history gasolinera-jsm/$$service:latest --format "table {{.CreatedBy}}\t{{.Size}}" 2>/dev/null | head -10 || echo "Image not found"; \
		echo ""; \
	done

# Docker Build Variants
build-service:
	@echo "Building specific service..."
	@read -p "Enter service name (auth-service, coupon-service, etc.): " service; \
	./scripts/docker-build-optimized.sh --service $$service

build-no-cache:
	@echo "Building without cache..."
	./scripts/docker-build-optimized.sh --no-cache

build-with-tests:
	@echo "Building with full test suite..."
	./scripts/docker-complete-build.sh

build-production:
	@echo "Building production-ready images..."
	./scripts/docker-complete-build.sh --push --clean

# Development helpers
dev-optimized:
	@echo "Starting development environment with optimized images..."
	docker-compose -f docker-compose.yml -f docker-compose.build.yml up -d

dev-rebuild:
	@echo "Rebuilding and starting development environment..."
	docker-compose -f docker-compose.yml -f docker-compose.build.yml up --build -d

# Monitoring and debugging
docker-logs:
	@echo "Following Docker container logs..."
	docker-compose logs -f

docker-stats:
	@echo "Showing Docker container resource usage..."
	docker stats --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"

docker-health:
	@echo "Checking container health status..."
	@for container in $$(docker ps --format "{{.Names}}" | grep gasolinera); do \
		echo "=== $$container ==="; \
		docker inspect $$container --format "{{.State.Health.Status}}" 2>/dev/null || echo "No health check configured"; \
	done

# Complete workflows
ci-build:
	@echo "Running CI build pipeline..."
	./scripts/docker-complete-build.sh --no-security-scan

cd-deploy:
	@echo "Running CD deployment pipeline..."
	./scripts/docker-complete-build.sh --push --clean

local-test:
	@echo "Running local test pipeline..."
	./scripts/docker-complete-build.sh --no-tests

# Cleanup commands
clean-all: containers-clean docker-clean
	@echo "Cleaning all build artifacts and containers..."
	./gradlew clean
	rm -rf build-report.md security-reports/

reset-dev: clean-all
	@echo "Resetting development environment..."
	docker-compose down -v --remove-orphans
	docker system prune -a --force
	make build-optimized
	make dev-optimized

# Help for new targets
help-docker:
	@echo "Docker & Security Commands:"
	@echo "  build-all         Build all services with optimization"
	@echo "  build-optimized   Build with caching and parallel execution"
	@echo "  build-complete    Full pipeline: build + test + security scan"
	@echo "  build-fast        Quick build without tests/scans"
	@echo "  build-production  Production build with push and cleanup"
	@echo "  security-scan     Scan images for vulnerabilities"
	@echo "  docker-clean      Clean build cache and dangling images"
	@echo "  docker-size       Show image sizes"
	@echo "  dev-optimized     Start dev environment with optimized images"
	@echo "  ci-build          CI pipeline build"
	@echo "  cd-deploy         CD pipeline deployment"
	@echo "  clean-all         Clean everything"
	@echo "  reset-dev         Reset and rebuild development environment"