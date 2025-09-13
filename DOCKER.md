# Docker Configuration Guide

This document provides comprehensive information about the Docker setup for the Gasolinera JSM Platform.

## Overview

The platform uses Docker Compose to orchestrate multiple services including:

### Application Services

- **API Gateway** (Port 8080) - Main entry point and routing
- **Auth Service** (Port 8081) - Authentication and authorization
- **Station Service** (Port 8082) - Gas station and employee management
- **Coupon Service** (Port 8083) - Coupon campaigns and validation
- **Redemption Service** (Port 8084) - Coupon redemption processing
- **Ad Engine** (Port 8085) - Advertisement management and engagement
- **Raffle Service** (Port 8086) - Raffle management and prize distribution

### Infrastructure Services

- **PostgreSQL** (Port 5432) - Primary database
- **Redis** (Port 6379) - Caching and session storage
- **RabbitMQ** (Ports 5672, 15672) - Message broker
- **Prometheus** (Port 9090) - Metrics collection
- **Grafana** (Port 3000) - Monitoring dashboards
- **Jaeger** (Ports 16686, 14268) - Distributed tracing

## Quick Start

### Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- At least 8GB RAM available for Docker
- At least 10GB free disk space

### Build and Start

```bash
# Build all services
./scripts/docker-build.sh

# Start in development mode
./scripts/docker-start.sh --dev

# Start in production mode
./scripts/docker-start.sh --prod

# Start with build (rebuild images)
./scripts/docker-start.sh --build

# Start in detached mode
./scripts/docker-start.sh -d
```

### Stop Services

```bash
# Stop all services
./scripts/docker-stop.sh

# Stop and remove volumes (data loss!)
./scripts/docker-stop.sh --volumes

# Complete cleanup (removes images and volumes)
./scripts/docker-stop.sh --clean
```

### View Logs

```bash
# Interactive log viewer
./scripts/docker-logs.sh

# Follow logs for specific service
./scripts/docker-logs.sh --follow auth-service

# Show last 50 lines for all services
./scripts/docker-logs.sh --all --tail 50
```

## Configuration Files

### Docker Compose Files

- `docker-compose.yml` - Base configuration
- `docker-compose.override.yml` - Development overrides (debug ports, logging)
- `docker-compose.prod.yml` - Production overrides (scaling, resource limits)

### Environment Files

- `.env.docker` - Docker-specific environment variables
- `.env.development` - Development environment variables

### Configuration Directories

```
config/
├── redis/
│   └── redis.conf
├── rabbitmq/
│   ├── rabbitmq.conf
│   └── definitions.json
├── prometheus/
│   └── prometheus.yml
└── grafana/
    └── provisioning/
        ├── datasources/
        └── dashboards/
```

## Service URLs

### Application Services

- API Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- Station Service: http://localhost:8082
- Coupon Service: http://localhost:8083
- Redemption Service: http://localhost:8084
- Ad Engine: http://localhost:8085
- Raffle Service: http://localhost:8086

### Monitoring & Management

- Grafana: http://localhost:3000 (admin/gasolinera_grafana_password)
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686
- RabbitMQ Management: http://localhost:15672 (gasolinera_user/gasolinera_password)

### Health Checks

All application services expose health endpoints:

- Health: `http://localhost:808X/actuator/health`
- Metrics: `http://localhost:808X/actuator/prometheus`
- Info: `http://localhost:808X/actuator/info`

## Development Features

### Debug Ports

When running in development mode, each service exposes a debug port:

- API Gateway: 5005
- Auth Service: 5006
- Station Service: 5007
- Coupon Service: 5008
- Redemption Service: 5009
- Ad Engine: 5010
- Raffle Service: 5011

### Log Volumes

Development mode mounts log directories for easy access:

```
logs/
├── api-gateway/
├── auth-service/
├── station-service/
├── coupon-service/
├── redemption-service/
├── ad-engine/
├── raffle-service/
├── postgres/
├── redis/
└── rabbitmq/
```

## Production Considerations

### Resource Limits

Production configuration includes:

- Memory limits for each service
- CPU limits and reservations
- Restart policies
- Health checks with proper timeouts

### Security

- Removed debug ports
- Limited port exposure
- Reduced logging verbosity
- Secure default configurations

### Scaling

Services can be scaled using Docker Compose:

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up --scale auth-service=3
```

## Troubleshooting

### Common Issues

#### Services Won't Start

1. Check Docker daemon is running
2. Verify available resources (RAM/disk)
3. Check port conflicts
4. Review service logs

#### Database Connection Issues

1. Ensure PostgreSQL is healthy: `docker-compose ps postgres`
2. Check database logs: `./scripts/docker-logs.sh postgres`
3. Verify connection parameters in environment files

#### Memory Issues

1. Increase Docker memory allocation
2. Reduce service replicas
3. Check for memory leaks in logs

#### Network Issues

1. Verify Docker network: `docker network ls`
2. Check service discovery: `docker-compose exec auth-service nslookup postgres`
3. Review firewall settings

### Useful Commands

```bash
# Check service status
docker-compose ps

# Execute command in service
docker-compose exec auth-service bash

# View resource usage
docker stats

# Clean up everything
docker system prune -a --volumes

# Rebuild specific service
docker-compose build auth-service

# Scale services
docker-compose up --scale auth-service=2

# View service configuration
docker-compose config
```

## Monitoring

### Prometheus Metrics

All services expose metrics at `/actuator/prometheus`:

- JVM metrics (memory, GC, threads)
- HTTP request metrics
- Database connection pool metrics
- Custom business metrics

### Grafana Dashboards

Pre-configured dashboards for:

- Application overview
- JVM metrics
- Database performance
- Message queue metrics
- Infrastructure monitoring

### Distributed Tracing

Jaeger collects traces from all services:

- Request flow visualization
- Performance bottleneck identification
- Error tracking across services

## Backup and Recovery

### Database Backup

```bash
# Create backup
docker-compose exec postgres pg_dump -U gasolinera_user gasolinera_db > backup.sql

# Restore backup
docker-compose exec -T postgres psql -U gasolinera_user gasolinera_db < backup.sql
```

### Volume Backup

```bash
# Backup all volumes
docker run --rm -v gasolinera-jsm-ultimate_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres_backup.tar.gz -C /data .
```

## Performance Tuning

### JVM Options

Services use optimized JVM settings:

- Container-aware memory allocation
- G1 garbage collector
- String deduplication
- Optimized for containerized environments

### Database Tuning

PostgreSQL configuration optimized for:

- Connection pooling
- Memory usage
- Query performance
- Concurrent access

### Redis Configuration

Redis optimized for:

- Memory efficiency
- Persistence
- Performance
- Security

## Security

### Network Security

- Services communicate through internal Docker network
- Only necessary ports exposed to host
- Production mode limits external access

### Authentication

- JWT tokens for service-to-service communication
- Redis for session storage
- Secure password hashing

### Data Protection

- Database encryption at rest
- Secure communication between services
- Audit logging for sensitive operations
