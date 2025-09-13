# Development Data Seeder
# Populates the development environment with realistic test data

FROM openjdk:17-jre-slim

# Install required tools
RUN apt-get update && apt-get install -y \
    curl \
    postgresql-client \
    redis-tools \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy seeder scripts and data
COPY scripts/dev-seeder/seed-data.sh /app/
COPY scripts/dev-data/ /app/data/

# Make scripts executable
RUN chmod +x /app/seed-data.sh

# Set environment variables
ENV POSTGRES_HOST=postgres
ENV POSTGRES_PORT=5432
ENV POSTGRES_DB=gasolinera_jsm_dev
ENV POSTGRES_USER=gasolinera_dev
ENV POSTGRES_PASSWORD=dev_password_2024
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Run seeder
CMD ["/app/seed-data.sh"]