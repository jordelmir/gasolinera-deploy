# Deployment Guide - Gasolinera JSM Platform

## Overview

This guide provides comprehensive instructions for deploying the Gasolinera JSM Platform in different environments. The platform is designed as a cloud-native microservices architecture with full containerization support.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Development Deployment](#development-deployment)
4. [Production Deployment](#production-deployment)
5. [Monitoring Setup](#monitoring-setup)
6. [Troubleshooting](#troubleshooting)
7. [Maintenance](#maintenance)

## Prerequisites

### System Requirements

**Minimum Requirements:**

- CPU: 4 cores
- RAM: 8GB
- Storage: 50GB free space
- Network: Stable internet connection

**Recommended Requirements:**

- CPU: 8 cores
- RAM: 16GB
- Storage: 100GB SSD
- Network: High-speed internet connection

### Software Dependencies

**Required Software:**

- Docker 20.10+ ([Installation Guide](https://docs.docker.com/get-docker/))
- Docker Compose 2.0+ ([Installation Guide](https://docs.docker.com/compose/install/))
- Git 2.30+
- OpenSSL (for certificate generation)

**Optional Tools:**

- kubectl (for Kubernetes deployment)
- Helm 3.0+ (for Kubernetes package management)
- Terraform (for infrastructure as code)

### Network Requirements

**Ports Used:**

- 8080: API Gateway (main entry point)
- 8081-8086: Microservices (auth, redemption, station, ad-engine, raffle, coupon)
- 5432: PostgreSQL
- 6379: Redis
- 5672, 15672: RabbitMQ
- 8200: HashiCorp Vault
- 9090: Prometheus
- 3000: Grafana
- 16686: Jaeger UI

**Firewall Configuration:**

- Allow inbound traffic on port 8080 (API Gateway)
- Allow internal communication between services
- Restrict direct access to infrastructure services (PostgreSQL, Redis, etc.)

## Environment Setup

### 1. Clone Repository

```bash
git clone https://github.com/your-org/gasolinera-jsm-platform.git
cd gasolinera-jsm-platform
```

### 2. Environment Configuration

Choose your deployment environment and run the setup script:

```bash
# For development environment
./scripts/setup-environment.sh dev

# For production environment
./scripts/setup-environment.sh prod --ssl
```

This script will:

- Create necessary directories
- Generate environment-specific configuration files
- Set up SSL certificates (if requested)
- Validate the environment setup

### 3. Review Configuration

After running the setup script, review and customize the generated configuration files:

**Development:**

- `.env.development` - Development environment variables
- `docker-compose.override.yml` - Development overrides

**Production:**

- `.env.production` - Production environment variables
- `docker-compose.prod.yml` - Production configuration

## Development Deployment

### Quick Start

```bash
# 1. Setup development environment
./scripts/setup-environment.sh dev

# 2. Build all services
./scripts/docker-build.sh

# 3. Start the platform
./scripts/docker-start.sh --dev

# 4. Validate deployment
./scripts/validate-deployment.sh
```

### Development Features

**Debug Support:**

- Debug ports exposed for all services (5005-5011)
- Verbose logging enabled
- Hot reload capabilities
- Development database with sample data

**Access URLs:**

- API Gateway: http://localhost:8080
- Grafana: http://localhost:3000 (admin/gasolinera_grafana_password)
- RabbitMQ Management: http://localhost:15672 (gasolinera_user/gasolinera_password)
- Jaeger UI: http://localhost:16686

### Development Workflow

```bash
# View logs
./scripts/docker-logs.sh --follow [service-name]

# Restart specific service
docker-compose restart [service-name]

# Run tests
./scripts/run-integration-tests.sh
./scripts/run-security-tests.sh
./scripts/run-performance-tests.sh

# Stop all services
./scripts/docker-stop.sh
```

## Production Deployment

### Pre-Production Checklist

- [ ] Review security configuration
- [ ] Update default passwords
- [ ] Configure SSL certificates
- [ ] Set up external monitoring
- [ ] Configure backup strategies
- [ ] Review resource limits
- [ ] Test disaster recovery procedures

### Production Deployment Steps

#### 1. Environment Preparation

```bash
# Setup production environment
./scripts/setup-environment.sh prod --ssl

# Review and update production configuration
nano .env.production

# Update sensitive values (passwords, secrets, etc.)
```

#### 2. Security Configuration

**Update Default Passwords:**

```bash
# The setup script generates secure passwords, but you should review them
grep -E "(PASSWORD|SECRET|TOKEN)" .env.production
```

**SSL Certificate Setup:**

```bash
# For production, replace self-signed certificates with proper CA-signed certificates
cp /path/to/your/certificate.crt ssl/certificate.crt
cp /path/to/your/private.key ssl/private.key
chmod 600 ssl/private.key
chmod 644 ssl/certificate.crt
```

#### 3. Build and Deploy

```bash
# Build production images
./scripts/docker-build.sh

# Start in production mode
./scripts/docker-start.sh --prod -d

# Validate deployment
./scripts/validate-deployment.sh
```

#### 4. Post-Deployment Verification

```bash
# Check service health
docker-compose -f docker-compose.yml -f docker-compose.prod.yml ps

# Verify API endpoints
curl -f http://localhost:8080/actuator/health

# Check logs for errors
./scripts/docker-logs.sh --all --tail 100
```

### Production Configuration

**Resource Limits:**

- Each service has memory and CPU limits defined
- Automatic restart policies configured
- Health checks with appropriate timeouts

**Security Features:**

- Non-root containers
- Network isolation
- Secret management via environment variables
- Limited port exposure

**Scaling:**

```bash
# Scale specific services
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up --scale auth-service=3 -d

# Check scaling status
docker-compose ps
```

## Monitoring Setup

### Monitoring Stack Components

**Prometheus:** Metrics collection and alerting
**Grafana:** Visualization and dashboards
**Jaeger:** Distributed tracing
**Alertmanager:** Alert routing and notification

### Enable Monitoring

```bash
# Start with monitoring stack
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# Access monitoring interfaces
echo "Grafana: http://localhost:3000"
echo "Prometheus: http://localhost:9090"
echo "Jaeger: http://localhost:16686"
```

### Configure Alerts

**Email Notifications:**

```bash
# Update alertmanager configuration
nano config/alertmanager/alertmanager.yml

# Update SMTP settings
smtp_smarthost: 'your-smtp-server:587'
smtp_from: 'alerts@yourdomain.com'
smtp_auth_username: 'your-email@yourdomain.com'
smtp_auth_password: 'your-email-password'
```

**Webhook Notifications:**

```bash
# Configure webhook URL in alertmanager.yml
webhook_configs:
  - url: 'https://your-webhook-endpoint.com/alerts'
```

### Custom Dashboards

Grafana dashboards are automatically provisioned from `config/grafana/dashboards/`. To add custom dashboards:

1. Create dashboard in Grafana UI
2. Export dashboard JSON
3. Save to `config/grafana/dashboards/`
4. Restart Grafana service

## Troubleshooting

### Common Issues

#### Services Won't Start

**Symptoms:** Services fail to start or remain unhealthy

**Solutions:**

```bash
# Check Docker daemon
docker info

# Check available resources
docker system df
docker system prune -f

# Check service logs
./scripts/docker-logs.sh [service-name]

# Restart specific service
docker-compose restart [service-name]
```

#### Database Connection Issues

**Symptoms:** Services can't connect to PostgreSQL

**Solutions:**

```bash
# Check PostgreSQL health
docker-compose exec postgres pg_isready -U gasolinera_user

# Check database logs
./scripts/docker-logs.sh postgres

# Verify connection parameters
grep POSTGRES .env.production

# Reset database (CAUTION: Data loss!)
docker-compose down -v
docker-compose up postgres -d
```

#### Memory Issues

**Symptoms:** Services crashing with OOM errors

**Solutions:**

```bash
# Check memory usage
docker stats

# Increase Docker memory allocation
# Update resource limits in docker-compose.prod.yml

# Reduce service replicas
docker-compose up --scale auth-service=1 -d
```

#### Network Issues

**Symptoms:** Services can't communicate with each other

**Solutions:**

```bash
# Check Docker networks
docker network ls
docker network inspect gasolinera-network

# Test service connectivity
docker-compose exec auth-service nslookup postgres

# Recreate network
docker-compose down
docker network rm gasolinera-network
docker-compose up -d
```

### Performance Issues

#### High Response Times

**Diagnosis:**

```bash
# Check service metrics
curl http://localhost:8080/actuator/metrics

# View Grafana dashboards
open http://localhost:3000

# Check database performance
docker-compose exec postgres psql -U gasolinera_user -c "SELECT * FROM pg_stat_activity;"
```

**Solutions:**

- Scale services horizontally
- Optimize database queries
- Increase resource limits
- Enable caching

#### High Memory Usage

**Diagnosis:**

```bash
# Monitor memory usage
docker stats --no-stream

# Check JVM heap usage
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

**Solutions:**

- Adjust JVM heap sizes in environment variables
- Implement memory profiling
- Optimize application code

### Log Analysis

**Centralized Logging:**

```bash
# View all service logs
./scripts/docker-logs.sh --all

# Follow specific service logs
./scripts/docker-logs.sh --follow auth-service

# Search logs for errors
./scripts/docker-logs.sh --all | grep ERROR

# Export logs for analysis
./scripts/docker-logs.sh --all > system-logs-$(date +%Y%m%d).log
```

## Maintenance

### Regular Maintenance Tasks

#### Daily Tasks

- Monitor service health via Grafana dashboards
- Check error rates and response times
- Review security alerts

#### Weekly Tasks

- Update system packages
- Review and rotate logs
- Check disk space usage
- Validate backup integrity

#### Monthly Tasks

- Update Docker images
- Review security configurations
- Performance optimization
- Disaster recovery testing

### Backup Procedures

#### Database Backup

```bash
# Create database backup
docker-compose exec postgres pg_dump -U gasolinera_user gasolinera_db > backup-$(date +%Y%m%d).sql

# Automated backup script
./scripts/backup-database.sh

# Restore from backup
docker-compose exec -T postgres psql -U gasolinera_user gasolinera_db < backup-20241009.sql
```

#### Volume Backup

```bash
# Backup Docker volumes
docker run --rm -v gasolinera-jsm-ultimate_postgres_data:/data -v $(pwd)/backups:/backup alpine tar czf /backup/postgres-$(date +%Y%m%d).tar.gz -C /data .

# Restore volume
docker run --rm -v gasolinera-jsm-ultimate_postgres_data:/data -v $(pwd)/backups:/backup alpine tar xzf /backup/postgres-20241009.tar.gz -C /data
```

### Updates and Upgrades

#### Application Updates

```bash
# Pull latest code
git pull origin main

# Rebuild images
./scripts/docker-build.sh

# Rolling update (zero downtime)
docker-compose up -d --no-deps auth-service
docker-compose up -d --no-deps station-service
# ... continue for each service
```

#### Infrastructure Updates

```bash
# Update Docker images
docker-compose pull

# Update system packages (Ubuntu/Debian)
sudo apt update && sudo apt upgrade -y

# Update Docker
curl -fsSL https://get.docker.com | sh
```

### Disaster Recovery

#### Recovery Procedures

**Complete System Recovery:**

1. Restore from infrastructure backup
2. Restore application code from Git
3. Restore database from backup
4. Restore Docker volumes
5. Restart services
6. Validate system functionality

**Service-Specific Recovery:**

```bash
# Restart failed service
docker-compose restart [service-name]

# Rebuild and restart service
docker-compose up -d --build [service-name]

# Scale up replacement instances
docker-compose up -d --scale [service-name]=2
```

### Health Monitoring

#### Automated Health Checks

```bash
# Run comprehensive health check
./scripts/validate-deployment.sh

# Monitor service endpoints
curl -f http://localhost:8080/actuator/health
curl -f http://localhost:8081/actuator/health
# ... for each service
```

#### Custom Health Monitoring

Create custom monitoring scripts:

```bash
#!/bin/bash
# custom-health-check.sh

# Check critical services
services=("auth-service" "api-gateway" "postgres" "redis")

for service in "${services[@]}"; do
    if ! docker-compose ps $service | grep -q "Up"; then
        echo "ALERT: $service is down"
        # Send notification
    fi
done
```

## Security Considerations

### Security Best Practices

1. **Network Security**
   - Use internal Docker networks
   - Limit port exposure
   - Implement firewall rules

2. **Authentication & Authorization**
   - Strong password policies
   - JWT token security
   - Role-based access control

3. **Data Protection**
   - Encrypt data at rest
   - Secure data in transit
   - Regular security audits

4. **Container Security**
   - Use non-root containers
   - Scan images for vulnerabilities
   - Keep base images updated

### Security Monitoring

```bash
# Run security tests
./scripts/run-security-tests.sh

# Check for vulnerabilities
docker scan [image-name]

# Monitor security logs
grep -i "security\|auth\|fail" logs/*.log
```

## Support and Resources

### Documentation

- [API Documentation](./API_DOCUMENTATION.md)
- [Architecture Guide](./ARCHITECTURE.md)
- [Security Guide](./SECURITY_TESTING.md)
- [Docker Guide](./DOCKER.md)

### Monitoring and Alerts

- Grafana Dashboards: http://localhost:3000
- Prometheus Metrics: http://localhost:9090
- Jaeger Tracing: http://localhost:16686

### Support Contacts

- **Technical Support:** tech-support@gasolinerajsm.com
- **Security Issues:** security@gasolinerajsm.com
- **Emergency Contact:** emergency@gasolinerajsm.com

---

_This deployment guide is maintained by the Gasolinera JSM Platform team. For updates and contributions, please refer to the project repository._
