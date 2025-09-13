# Gasolinera JSM Platform - Deployment Guide

## Quick Start

This platform provides a comprehensive deployment solution for the Gasolinera JSM loyalty and rewards system. Follow these steps to get started quickly.

### Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- Git
- 8GB+ RAM, 4+ CPU cores
- 50GB+ available disk space

### 1. Clone and Setup

```bash
git clone <repository-url>
cd gasolinera-jsm-ultimate

# Quick development setup
./scripts/deploy.sh

# Or production setup with monitoring
./scripts/deploy.sh -e production -m -B -r
```

### 2. Access Services

After deployment, services are available at:

- **API Gateway**: http://localhost:8080
- **Swagger Documentation**: http://localhost:8080/swagger-ui.html
- **Grafana Monitoring**: http://localhost:3000 (admin/gasolinera_grafana_password)
- **RabbitMQ Management**: http://localhost:15672 (gasolinera_user/gasolinera_password)
- **Jaeger Tracing**: http://localhost:16686

## Deployment Scripts

### Main Deployment Script

```bash
# Development deployment
./scripts/deploy.sh

# Production deployment with full features
./scripts/deploy.sh -e production -m -B -r

# Options:
# -e, --environment    Target environment (development|production)
# -m, --monitoring     Enable monitoring stack
# -B, --backup         Create backup before deployment
# -r, --rollback       Rollback on failure
# -s, --skip-tests     Skip running tests
# -b, --skip-build     Skip building services
# -d, --dry-run        Show what would be done
```

### Backup and Restore

```bash
# Create full backup
./scripts/backup-restore.sh --operation backup --type full --compression

# List available backups
./scripts/backup-restore.sh --operation list

# Restore from backup
./scripts/backup-restore.sh --operation restore --restore-point full_backup_20240101_120000

# Schedule automated backups
./scripts/backup-restore.sh --operation schedule --schedule "0 2 * * *" --type full
```

### Monitoring Setup

```bash
# Setup monitoring configuration
./scripts/monitoring-setup.sh --operation setup --notification-email admin@company.com

# Start monitoring services
./scripts/monitoring-setup.sh --operation start

# Check monitoring status
./scripts/monitoring-setup.sh --operation status
```

## Architecture Overview

The platform consists of:

### Core Services

- **API Gateway** (Port 8080): Central entry point and routing
- **Auth Service** (Port 8081): User authentication and authorization
- **Station Service** (Port 8083): Gas station and employee management
- **Coupon Service** (Port 8086): Coupon creation and management
- **Redemption Service** (Port 8082): Coupon redemption processing
- **Ad Engine** (Port 8084): Advertisement targeting and tracking
- **Raffle Service** (Port 8085): Raffle management and prize distribution

### Infrastructure Services

- **PostgreSQL**: Primary database
- **Redis**: Caching and session storage
- **RabbitMQ**: Message queuing and event processing
- **Vault**: Secret management
- **Jaeger**: Distributed tracing

### Monitoring Stack

- **Prometheus**: Metrics collection
- **Grafana**: Visualization and dashboards
- **Alertmanager**: Alert routing and notifications

## Environment Configuration

### Development Environment

```bash
# Setup development environment
./scripts/setup-environment.sh dev

# Features:
# - Debug ports exposed
# - Verbose logging
# - Hot reload capabilities
# - Direct database access
# - All monitoring tools accessible
```

### Production Environment

```bash
# Setup production environment with SSL
./scripts/setup-environment.sh prod --ssl

# Features:
# - Optimized resource allocation
# - Security hardening
# - SSL/TLS encryption
# - Monitoring and alerting
# - Backup strategies
```

## Testing

### Run All Tests

```bash
# Unit tests
./gradlew test

# Integration tests
./scripts/run-integration-tests.sh

# Security tests
./scripts/run-security-tests.sh all

# Performance tests
./scripts/run-performance-tests.sh
```

### Test Categories

1. **Unit Tests**: Individual component testing
2. **Integration Tests**: End-to-end workflow testing
3. **Security Tests**: Vulnerability and penetration testing
4. **Performance Tests**: Load and stress testing

## Database Management

### Migrations

```bash
# Test migrations
./scripts/test-migrations.sh

# Generate seed data
./scripts/generate-seed-data.sh

# Validate seed data
./scripts/validate-seed-data.sh
```

### Seed Data

The platform includes comprehensive seed data for:

- Test users and authentication
- Sample gas stations and employees
- Coupon campaigns and templates
- Raffle configurations
- Advertisement campaigns

## Security Features

### Authentication & Authorization

- JWT-based authentication
- Role-based access control (RBAC)
- OTP verification for sensitive operations
- Session management with Redis

### Data Protection

- Database encryption at rest
- TLS encryption in transit
- Secret management with Vault
- Input validation and sanitization

### Security Testing

- Automated vulnerability scanning
- Penetration testing suite
- JWT security validation
- Error handling security tests

## Monitoring and Observability

### Metrics Collection

- Application metrics (request rate, response time, errors)
- Infrastructure metrics (CPU, memory, disk, network)
- Business metrics (user registrations, coupon redemptions)
- JVM metrics (heap usage, garbage collection)

### Alerting Rules

- Service availability alerts
- Performance degradation alerts
- Error rate threshold alerts
- Resource utilization alerts

### Dashboards

- **Overview Dashboard**: High-level system health
- **JVM Metrics**: Java application performance
- **Infrastructure**: Server and container metrics
- **Business Metrics**: Key performance indicators

## Troubleshooting

### Common Issues

1. **Services Not Starting**

   ```bash
   # Check logs
   ./scripts/docker-logs.sh

   # Validate deployment
   ./scripts/validate-deployment.sh

   # Check resource usage
   docker stats
   ```

2. **Database Connection Issues**

   ```bash
   # Check PostgreSQL status
   docker-compose exec postgres pg_isready

   # Test connection
   docker-compose exec postgres psql -U gasolinera_user -d gasolinera_db
   ```

3. **Memory Issues**

   ```bash
   # Check memory usage
   docker stats --no-stream

   # Adjust JVM settings in docker-compose.yml
   # Monitor GC metrics in Grafana
   ```

### Log Analysis

```bash
# View all logs
./scripts/docker-logs.sh

# View specific service logs
docker-compose logs -f auth-service

# Search for errors
docker-compose logs | grep ERROR

# Export logs for analysis
docker-compose logs > deployment.log
```

## Maintenance

### Regular Tasks

**Daily:**

- Monitor system health and alerts
- Check log files for errors
- Verify backup completion

**Weekly:**

- Update system packages
- Review performance metrics
- Clean up old Docker images
- Test disaster recovery procedures

**Monthly:**

- Security vulnerability scanning
- Performance optimization review
- Capacity planning assessment
- Documentation updates

### Updates and Upgrades

```bash
# Update application
git pull origin main
./scripts/docker-build.sh
./scripts/deploy.sh -e production

# Update infrastructure
sudo apt-get update && sudo apt-get upgrade
./scripts/deploy.sh -e production --skip-tests --skip-build
```

## Performance Optimization

### JVM Tuning

```bash
# Production JVM settings
JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Database Optimization

```sql
-- PostgreSQL performance tuning
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
SELECT pg_reload_conf();
```

### Container Resources

```yaml
# Resource limits in docker-compose.prod.yml
deploy:
  resources:
    limits:
      cpus: '1.0'
      memory: 1G
    reservations:
      cpus: '0.5'
      memory: 512M
```

## Support and Documentation

### Additional Resources

- [Deployment Guide](docs/DEPLOYMENT_GUIDE.md) - Comprehensive deployment documentation
- [Architecture Documentation](docs/ARCHITECTURE.md) - System architecture details
- [Security Testing](docs/SECURITY_TESTING.md) - Security testing procedures
- [Integration Tests](docs/INTEGRATION_TESTS.md) - Integration testing guide
- [Database Migrations](docs/DATABASE_MIGRATIONS.md) - Database schema management

### Getting Help

1. **Check Documentation**: Review guides and troubleshooting sections
2. **Search Logs**: Use log analysis to identify issues
3. **Monitor Metrics**: Check Grafana dashboards for insights
4. **Run Diagnostics**: Use validation scripts to identify problems

### Contact Information

- **Development Team**: dev@gasolinerajsm.com
- **Operations Team**: ops@gasolinerajsm.com
- **Security Team**: security@gasolinerajsm.com

---

## Quick Reference Commands

```bash
# Deployment
./scripts/deploy.sh -e production -m -B -r

# Backup
./scripts/backup-restore.sh --operation backup --type full --compression

# Monitoring
./scripts/monitoring-setup.sh --operation start

# Validation
./scripts/validate-deployment.sh

# Logs
./scripts/docker-logs.sh

# Stop all services
./scripts/docker-stop.sh

# Environment setup
./scripts/setup-environment.sh prod --ssl
```

_This deployment guide is maintained by the Gasolinera JSM Platform team._
