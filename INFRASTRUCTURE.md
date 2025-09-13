# Gasolinera JSM Platform - Infrastructure Guide

## Overview

This document provides comprehensive information about the infrastructure setup for the Gasolinera JSM Digital Coupon Platform. The platform uses a microservices architecture with Docker containerization and includes all necessary supporting services.

## Architecture Components

### Core Infrastructure Services

| Service | Container | Port | Purpose |
|---------|-----------|------|---------|
| PostgreSQL | `postgres` | 5432 | Primary database for all microservices |
| Redis | `redis` | 6379 | Caching and session management |
| RabbitMQ | `rabbitmq` | 5672, 15672 | Message broker for inter-service communication |
| HashiCorp Vault | `vault` | 8200, 8201 | Secret management and configuration |
| Jaeger | `jaeger` | 16686, 4317, 4318 | Distributed tracing and monitoring |

### Microservices

| Service | Container | Port | Purpose |
|---------|-----------|------|---------|
| API Gateway | `api-gateway` | 8080 | Request routing and authentication |
| Auth Service | `auth-service` | 8081 | User authentication and authorization |
| Redemption Service | `redemption-service` | 8082 | Coupon redemption processing |
| Station Service | `station-service` | 8083 | Gas station and employee management |
| Ad Engine | `ad-engine` | 8084 | Advertisement serving and engagement |
| Raffle Service | `raffle-service` | 8085 | Raffle management and prize distribution |
| Coupon Service | `coupon-service` | 8086 | Coupon generation and validation |

## Quick Start

### Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- Make (optional, for using Makefile commands)
- curl and jq (for health checks)

### Environment Setup

1. **Clone and navigate to the project:**
   ```bash
   cd gasolinera-jsm-ultimate
   ```

2. **Set up environment variables:**
   ```bash
   cp .env.development .env
   ```

3. **Start the complete platform:**
   ```bash
   # Using Makefile (recommended)
   make -f Makefile.infrastructure setup-dev

   # Or using Docker Compose directly
   docker-compose --env-file .env.development up -d --build
   ```

4. **Validate the setup:**
   ```bash
   ./scripts/validate-infrastructure.sh
   ```

### Manual Service Initialization

If automatic initialization fails, you can manually initialize services:

```bash
# Initialize Vault secrets
make -f Makefile.infrastructure init-vault

# Initialize RabbitMQ exchanges and queues
make -f Makefile.infrastructure init-rabbitmq

# Check overall health
make -f Makefile.infrastructure health
```

## Service Configuration

### Database Configuration

The platform uses PostgreSQL with separate schemas for each microservice:

- `auth_schema` - User management and authentication
- `station_schema` - Gas station and employee data
- `coupon_schema` - Coupon campaigns and generation
- `redemption_schema` - Redemption transactions and raffle tickets
- `ad_schema` - Advertisement campaigns and engagement
- `raffle_schema` - Raffle management and prizes

**Connection Details:**
- Host: `postgres`
- Port: `5432`
- Database: `gasolinera_jsm`
- Main User: `gasolinera_user`
- Password: `gasolinera_pass_2024`

### Redis Configuration

Redis is used for:
- Session management
- OTP storage and validation
- Rate limiting
- Caching frequently accessed data

**Connection Details:**
- Host: `redis`
- Port: `6379`
- Password: `redis_pass_2024`
- Database: `0`

### RabbitMQ Configuration

RabbitMQ handles asynchronous communication between services:

**Connection Details:**
- Host: `rabbitmq`
- Port: `5672` (AMQP), `15672` (Management UI)
- Username: `gasolinera_user`
- Password: `rabbitmq_pass_2024`
- Virtual Host: `gasolinera_vhost`

**Exchanges and Queues:**
- Main Exchange: `gasolinera.events` (topic)
- Dead Letter Exchange: `gasolinera.dlx` (direct)
- Service-specific queues for each event type
- Automatic dead letter handling with TTL

### HashiCorp Vault Configuration

Vault manages all sensitive configuration and secrets:

**Access Details:**
- URL: `http://localhost:8200`
- Root Token: `gasolinera_vault_token_2024`

**Secret Paths:**
- `/secret/database` - Database credentials
- `/secret/redis` - Redis configuration
- `/secret/rabbitmq` - RabbitMQ credentials
- `/secret/jwt` - JWT signing keys
- `/secret/qr` - QR code signing secrets
- `/secret/external` - External service configurations
- `/secret/{service-name}` - Service-specific configurations

### Jaeger Tracing Configuration

Jaeger provides distributed tracing across all microservices:

**Access Points:**
- UI: `http://localhost:16686`
- OTLP gRPC: `http://localhost:4317`
- OTLP HTTP: `http://localhost:4318`

## Management Commands

### Using Makefile (Recommended)

```bash
# Complete development setup
make -f Makefile.infrastructure setup-dev

# Start services
make -f Makefile.infrastructure up

# Stop services
make -f Makefile.infrastructure down

# View logs
make -f Makefile.infrastructure logs
make -f Makefile.infrastructure logs-service SERVICE=auth-service

# Health checks
make -f Makefile.infrastructure health

# Clean up
make -f Makefile.infrastructure clean

# Restart specific service
make -f Makefile.infrastructure restart-service SERVICE=auth-service

# Open monitoring dashboards
make -f Makefile.infrastructure monitor
```

### Using Docker Compose Directly

```bash
# Start all services
docker-compose --env-file .env.development up -d

# Stop all services
docker-compose --env-file .env.development down

# View logs
docker-compose --env-file .env.development logs -f

# Restart specific service
docker-compose --env-file .env.development restart auth-service

# Scale specific service
docker-compose --env-file .env.development up -d --scale auth-service=2
```

## Health Monitoring

### Service Health Endpoints

All microservices expose Spring Boot Actuator health endpoints:

```bash
# API Gateway
curl http://localhost:8080/actuator/health

# Individual services
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Redemption Service
curl http://localhost:8083/actuator/health  # Station Service
curl http://localhost:8084/actuator/health  # Ad Engine
curl http://localhost:8085/actuator/health  # Raffle Service
curl http://localhost:8086/actuator/health  # Coupon Service
```

### Infrastructure Health Checks

```bash
# PostgreSQL
docker exec postgres pg_isready -U gasolinera_user -d gasolinera_jsm

# Redis
docker exec redis redis-cli ping

# RabbitMQ
docker exec rabbitmq rabbitmqctl status

# Vault
docker exec vault vault status
```

### Automated Validation

Run the comprehensive infrastructure validation script:

```bash
./scripts/validate-infrastructure.sh
```

## Troubleshooting

### Common Issues

1. **Services not starting:**
   ```bash
   # Check Docker daemon
   docker info

   # Check service logs
   docker-compose logs service-name

   # Restart problematic service
   docker-compose restart service-name
   ```

2. **Database connection issues:**
   ```bash
   # Check PostgreSQL logs
   docker-compose logs postgres

   # Verify database is ready
   docker exec postgres pg_isready -U gasolinera_user

   # Check network connectivity
   docker exec auth-service nc -z postgres 5432
   ```

3. **Vault initialization issues:**
   ```bash
   # Check Vault status
   docker exec vault vault status

   # Re-initialize Vault
   make -f Makefile.infrastructure init-vault
   ```

4. **RabbitMQ message issues:**
   ```bash
   # Check RabbitMQ status
   docker exec rabbitmq rabbitmqctl status

   # List exchanges and queues
   docker exec rabbitmq rabbitmqctl list_exchanges
   docker exec rabbitmq rabbitmqctl list_queues

   # Re-initialize RabbitMQ
   make -f Makefile.infrastructure init-rabbitmq
   ```

### Performance Tuning

1. **Memory allocation:**
   - Adjust `JAVA_OPTS` in docker-compose.yml
   - Monitor memory usage: `docker stats`

2. **Database performance:**
   - Monitor connection pools
   - Check slow query logs
   - Optimize indexes

3. **Network performance:**
   - Use Docker networks for service communication
   - Monitor network latency between services

### Backup and Recovery

1. **Database backup:**
   ```bash
   make -f Makefile.infrastructure backup-db
   ```

2. **Database restore:**
   ```bash
   make -f Makefile.infrastructure restore-db BACKUP_FILE=backup.sql
   ```

3. **Volume backup:**
   ```bash
   docker run --rm -v gasolinera-jsm-ultimate_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres_backup.tar.gz /data
   ```

## Security Considerations

### Network Security
- Services communicate over internal Docker networks
- Only necessary ports are exposed to the host
- Use environment-specific credentials

### Secret Management
- All secrets stored in HashiCorp Vault
- Service-specific Vault policies
- Automatic secret rotation capabilities

### Database Security
- Service-specific database users
- Schema-level access control
- Connection encryption enabled

### Monitoring Security
- Audit logs for all service interactions
- Distributed tracing for request flow analysis
- Health check endpoints for service monitoring

## Development vs Production

### Development Environment
- Uses `.env.development` configuration
- Exposes all service ports for debugging
- Includes development tools and debugging endpoints
- Uses development-grade passwords and tokens

### Production Considerations
- Use `.env.production` with secure credentials
- Implement proper TLS/SSL certificates
- Configure firewall rules and network policies
- Set up external monitoring and alerting
- Implement backup and disaster recovery procedures
- Use production-grade resource limits and scaling policies

## Support and Maintenance

### Log Management
- Centralized logging with structured JSON format
- Log rotation and retention policies
- Correlation IDs for request tracing

### Monitoring and Alerting
- Jaeger for distributed tracing
- Prometheus metrics collection (when configured)
- Custom health check endpoints
- Service dependency monitoring

### Updates and Maintenance
- Rolling updates for zero-downtime deployments
- Database migration scripts with Flyway
- Configuration updates through Vault
- Automated testing and validation scripts