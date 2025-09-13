# 🚀 Gasolinera JSM - Development Environment

## TOP MUNDIAL Development Setup! 🔥

This guide will help you set up the complete development environment for Gasolinera JSM with all the bells and whistles!

## 🎯 Quick Start

### Prerequisites

- Docker & Docker Compose
- Git
- Make
- 8GB+ RAM recommended
- 20GB+ free disk space

### One-Command Setup

```bash
# 🚀 BOOM! Complete setup in one command
./scripts/dev-setup.sh
```

That's it! Grab a coffee ☕ and watch the magic happen!

## 🛠️ What You Get

### 🏗️ Infrastructure Services

- **PostgreSQL 15** - Optimized for development with query logging
- **Redis 7** - High-performance caching with monitoring
- **RabbitMQ 3.12** - Message queue with management UI
- **Jaeger** - Distributed tracing for debugging
- **Vault** - Secrets management (dev mode)

### 🎯 Microservices

- **API Gateway** (Port 8080) - Main entry point
- **Auth Service** (Port 8081) - Authentication & authorization
- **Coupon Service** (Port 8082) - Coupon management
- **Station Service** (Port 8083) - Gas station operations
- **Raffle Service** (Port 8084) - Raffle system
- **Redemption Service** (Port 8085) - Redemption processing
- **Ad Engine** (Port 8086) - Advertisement engine

### 🔧 Development Tools

- **PgAdmin** (Port 5050) - Database administration
- **Redis Commander** (Port 8081) - Redis management
- **RabbitMQ Management** (Port 15672) - Queue monitoring
- **Jaeger UI** (Port 16686) - Tracing visualization
- **Vault UI** (Port 8200) - Secrets management

## 🎮 Development Features

### 🔥 Hot Reload

- **Spring DevTools** enabled on all services
- **Volume mounting** for instant code changes
- **LiveReload** support for frontend development

### 🐛 Debugging

- **Remote debugging** ports 5005-5011
- **Debug profiles** pre-configured
- **Detailed logging** with correlation IDs
- **All actuator endpoints** exposed

### 📊 Monitoring

- **Health checks** on all services
- **Metrics collection** with Prometheus format
- **Distributed tracing** with Jaeger
- **Performance monitoring** built-in

### 🎭 Test Data

- **Pre-seeded** realistic test data
- **Multiple user roles** for testing
- **Sample gas stations** with locations
- **Active campaigns** and raffles
- **Mock external services**

## 🚀 Quick Commands

```bash
# Start everything
make dev

# Follow logs
make dev-logs

# Restart services
make dev-restart

# Rebuild and restart
make dev-rebuild

# Stop everything
make dev-stop

# Clean everything
make dev-clean

# Reset and rebuild
make reset-dev
```

## 👤 Test Accounts

| Role       | Email                | Password  | Description        |
| ---------- | -------------------- | --------- | ------------------ |
| Admin      | admin@gasolinera.com | Admin123! | Full system access |
| User       | user1@test.com       | Test123!  | Regular customer   |
| Owner      | owner1@test.com      | Test123!  | Station owner      |
| Advertiser | advertiser1@test.com | Test123!  | Campaign manager   |

## 🌐 Service URLs

### 🎯 Application Services

- **API Gateway**: http://localhost:8080
- **Auth Service**: http://localhost:8081
- **Coupon Service**: http://localhost:8082
- **Station Service**: http://localhost:8083
- **Raffle Service**: http://localhost:8084
- **Redemption Service**: http://localhost:8085
- **Ad Engine**: http://localhost:8086

### 🛠️ Development Tools

- **PgAdmin**: http://localhost:5050 (dev@gasolinera.com / dev_password_2024)
- **Redis Commander**: http://localhost:8081 (dev / dev_password_2024)
- **RabbitMQ Management**: http://localhost:15672 (gasolinera_dev / dev_password_2024)
- **Jaeger UI**: http://localhost:16686
- **Vault UI**: http://localhost:8200 (Token: dev_vault_token_2024)

## 🔍 Health Checks

Check if everything is running:

```bash
# Quick health check
curl http://localhost:8080/actuator/health

# All services health
for port in 8080 8081 8082 8083 8084 8085 8086; do
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health | jq -r '.status // "DOWN"')"
done
```

## 🐛 Debugging

### Remote Debugging Ports

- API Gateway: 5005
- Auth Service: 5006
- Coupon Service: 5007
- Station Service: 5008
- Raffle Service: 5009
- Redemption Service: 5010
- Ad Engine: 5011

### IntelliJ IDEA Setup

1. Run → Edit Configurations
2. Add → Remote JVM Debug
3. Host: localhost
4. Port: 5006 (for auth-service)
5. Use module classpath: auth-service

### VS Code Setup

```json
{
  "type": "java",
  "name": "Debug Auth Service",
  "request": "attach",
  "hostName": "localhost",
  "port": 5006
}
```

## 📊 Monitoring & Observability

### Logs

```bash
# Follow all logs
docker-compose -f docker-compose.dev.yml logs -f

# Service-specific logs
docker-compose -f docker-compose.dev.yml logs -f auth-service

# Structured logs with correlation IDs
grep "correlation-id" logs/auth-service.log
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8081/actuator/prometheus

# Health details
curl http://localhost:8081/actuator/health | jq
```

### Tracing

- Open Jaeger UI: http://localhost:16686
- Search for traces by service name
- Analyze request flows across services

## 🗄️ Database

### Connection Details

- **Host**: localhost:5432
- **Database**: gasolinera_jsm_dev
- **Username**: gasolinera_dev
- **Password**: dev_password_2024

### PgAdmin Access

1. Open http://localhost:5050
2. Login: dev@gasolinera.com / dev_password_2024
3. Server is pre-configured as "Gasolinera JSM Development"

### Direct Connection

```bash
psql -h localhost -p 5432 -U gasolinera_dev -d gasolinera_jsm_dev
```

## 🔴 Redis

### Connection

```bash
redis-cli -h localhost -p 6379
```

### Redis Commander

- URL: http://localhost:8081
- Username: dev
- Password: dev_password_2024

## 🐰 RabbitMQ

### Management UI

- URL: http://localhost:15672
- Username: gasolinera_dev
- Password: dev_password_2024

### Queues

- `coupon.events` - Coupon-related events
- `redemption.events` - Redemption processing
- `raffle.events` - Raffle system events
- `user.events` - User management events
- `notification.events` - Notifications
- `audit.events` - Audit logging

## 🔒 Vault Secrets

### Access

- URL: http://localhost:8200
- Token: dev_vault_token_2024

### Useful Commands

```bash
# Set Vault environment
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=dev_vault_token_2024

# Read JWT secrets
vault kv get secret/gasolinera/jwt

# Read database config
vault kv get secret/gasolinera/database

# Generate database credentials
vault read database/creds/gasolinera-role
```

## 🧪 Testing

### API Testing

```bash
# Get auth token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@gasolinera.com","password":"Admin123!"}' | jq -r '.token')

# Use token for authenticated requests
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/stations
```

### Load Testing

```bash
# Install k6 (if not installed)
brew install k6  # macOS
# or
sudo apt install k6  # Ubuntu

# Run load tests
k6 run scripts/load-tests/auth-service.js
```

## 🔧 Customization

### Environment Variables

Edit `.env.dev` to customize:

- Database settings
- Redis configuration
- JWT secrets
- Feature flags
- Debug settings

### Service Configuration

Each service has development profiles in:

- `services/*/src/main/resources/application-dev.yml`
- `config/spring-profiles/application-dev.yml`

### Docker Compose Overrides

Create `docker-compose.override.yml` for personal customizations:

```yaml
version: '3.8'
services:
  auth-service:
    environment:
      - CUSTOM_ENV_VAR=value
    ports:
      - '9999:8080' # Custom port mapping
```

## 🚨 Troubleshooting

### Common Issues

#### Services Won't Start

```bash
# Check Docker resources
docker system df
docker system prune

# Check logs
docker-compose -f docker-compose.dev.yml logs service-name

# Restart specific service
docker-compose -f docker-compose.dev.yml restart service-name
```

#### Port Conflicts

```bash
# Check what's using a port
lsof -i :8080

# Kill process using port
kill -9 $(lsof -t -i:8080)
```

#### Database Connection Issues

```bash
# Check PostgreSQL logs
docker-compose -f docker-compose.dev.yml logs postgres

# Test connection
docker-compose -f docker-compose.dev.yml exec postgres psql -U gasolinera_dev -d gasolinera_jsm_dev -c "SELECT 1;"
```

#### Memory Issues

```bash
# Check Docker memory usage
docker stats

# Increase Docker memory limit (Docker Desktop)
# Settings → Resources → Memory → 8GB+
```

### Reset Everything

```bash
# Nuclear option - reset everything
./scripts/dev-setup.sh --clean
```

## 🎯 Development Workflow

### 1. Code Changes

- Edit code in your IDE
- Services automatically restart (hot reload)
- Check logs for any issues

### 2. Database Changes

- Add migrations to `database/migrations/`
- Restart postgres service to apply
- Use PgAdmin to verify changes

### 3. API Changes

- Update OpenAPI specs
- Test with Swagger UI (if enabled)
- Verify with integration tests

### 4. Testing

- Unit tests: `./gradlew test`
- Integration tests: `./gradlew integrationTest`
- Load tests: `k6 run scripts/load-tests/*.js`

## 🏆 Pro Tips

### 🚀 Performance

- Use SSD for Docker volumes
- Allocate 8GB+ RAM to Docker
- Enable BuildKit for faster builds
- Use parallel builds when possible

### 🐛 Debugging

- Use correlation IDs to trace requests
- Check Jaeger for distributed traces
- Monitor metrics in real-time
- Use remote debugging for complex issues

### 🔄 Productivity

- Set up IDE auto-import for faster development
- Use Docker layer caching for faster builds
- Keep services running between sessions
- Use make commands for common tasks

### 🛡️ Security

- Never commit real secrets to git
- Use Vault for all sensitive data
- Rotate development tokens regularly
- Keep development isolated from production

## 🤝 Contributing

1. Make your changes
2. Test locally with full environment
3. Run quality checks: `make quality-check`
4. Submit PR with detailed description

## 📚 Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools)
- [Jaeger Tracing](https://www.jaegertracing.io/docs/)
- [HashiCorp Vault](https://www.vaultproject.io/docs)

---

## 🎉 Happy Coding!

You now have a world-class development environment! 🌟

Need help? Check the logs, use the debugging tools, or ask the team!

**Remember**: This is a development environment - have fun and break things! 🎯
