# ☁️ DESPLIEGUE ENTERPRISE EN AWS

## 🎯 ARQUITECTURA AWS COMPLETA

Despliegue profesional usando servicios managed de AWS para máxima escalabilidad y confiabilidad.

## 🏗️ ARQUITECTURA DE SERVICIOS AWS

```
┌─────────────────────────────────────────────────────────────┐
│                        INTERNET                              │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                 CloudFront CDN                               │
│              (Global Distribution)                           │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                Application Load Balancer                     │
│                  (Multi-AZ, SSL)                            │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                    ECS Fargate                              │
│   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│   │ API Gateway │ │Auth Service │ │Station Svc  │          │
│   └─────────────┘ └─────────────┘ └─────────────┘          │
│   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│   │Coupon Svc   │ │Raffle Svc   │ │Redemp. Svc  │          │
│   └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                   RDS PostgreSQL                            │
│              (Multi-AZ, Read Replicas)                      │
└─────────────────────────────────────────────────────────────┘
```

## 📋 PREPARACIÓN DE INFRAESTRUCTURA

### 1. Terraform para Infrastructure as Code

```hcl
# infrastructure/terraform/main.tf
terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC y Networking
resource "aws_vpc" "gasolinera_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "gasolinera-vpc"
  }
}

resource "aws_subnet" "private_subnets" {
  count             = 2
  vpc_id            = aws_vpc.gasolinera_vpc.id
  cidr_block        = "10.0.${count.index + 1}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "gasolinera-private-subnet-${count.index + 1}"
  }
}

resource "aws_subnet" "public_subnets" {
  count                   = 2
  vpc_id                  = aws_vpc.gasolinera_vpc.id
  cidr_block              = "10.0.${count.index + 10}.0/24"
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "gasolinera-public-subnet-${count.index + 1}"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "gasolinera_igw" {
  vpc_id = aws_vpc.gasolinera_vpc.id

  tags = {
    Name = "gasolinera-igw"
  }
}

# RDS PostgreSQL
resource "aws_db_subnet_group" "gasolinera_db_subnet_group" {
  name       = "gasolinera-db-subnet-group"
  subnet_ids = aws_subnet.private_subnets[*].id

  tags = {
    Name = "gasolinera-db-subnet-group"
  }
}

resource "aws_db_instance" "gasolinera_postgres" {
  identifier = "gasolinera-postgres"

  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.t3.micro"

  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp2"
  storage_encrypted     = true

  db_name  = "gasolinera_db"
  username = "gasolinera_user"
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.gasolinera_db_subnet_group.name

  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"

  multi_az               = true
  publicly_accessible    = false

  skip_final_snapshot = true
  deletion_protection = false

  tags = {
    Name = "gasolinera-postgres"
  }
}

# ElastiCache Redis
resource "aws_elasticache_subnet_group" "gasolinera_redis_subnet_group" {
  name       = "gasolinera-redis-subnet-group"
  subnet_ids = aws_subnet.private_subnets[*].id
}

resource "aws_elasticache_cluster" "gasolinera_redis" {
  cluster_id           = "gasolinera-redis"
  engine               = "redis"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.gasolinera_redis_subnet_group.name
  security_group_ids   = [aws_security_group.redis_sg.id]

  tags = {
    Name = "gasolinera-redis"
  }
}

# ECS Cluster
resource "aws_ecs_cluster" "gasolinera_cluster" {
  name = "gasolinera-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name = "gasolinera-cluster"
  }
}

# Application Load Balancer
resource "aws_lb" "gasolinera_alb" {
  name               = "gasolinera-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = aws_subnet.public_subnets[*].id

  enable_deletion_protection = false

  tags = {
    Name = "gasolinera-alb"
  }
}

# Security Groups
resource "aws_security_group" "alb_sg" {
  name_prefix = "gasolinera-alb-sg"
  vpc_id      = aws_vpc.gasolinera_vpc.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "gasolinera-alb-sg"
  }
}

resource "aws_security_group" "ecs_sg" {
  name_prefix = "gasolinera-ecs-sg"
  vpc_id      = aws_vpc.gasolinera_vpc.id

  ingress {
    from_port       = 8080
    to_port         = 8097
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "gasolinera-ecs-sg"
  }
}
```

### 2. ECS Task Definitions

```json
{
  "family": "gasolinera-api-gateway",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::ACCOUNT:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::ACCOUNT:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "api-gateway",
      "image": "ACCOUNT.dkr.ecr.REGION.amazonaws.com/gasolinera/api-gateway:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "aws"
        },
        {
          "name": "SERVER_PORT",
          "value": "8080"
        }
      ],
      "secrets": [
        {
          "name": "DATABASE_URL",
          "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT:secret:gasolinera/database-url"
        },
        {
          "name": "REDIS_URL",
          "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT:secret:gasolinera/redis-url"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/gasolinera-api-gateway",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "curl -f http://localhost:8080/actuator/health || exit 1"
        ],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

### 3. GitHub Actions para CI/CD

```yaml
# .github/workflows/aws-deploy.yml
name: Deploy to AWS

on:
  push:
    branches: [main]
  workflow_dispatch:

env:
  AWS_REGION: us-east-1
  ECR_REPOSITORY: gasolinera

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build services
        run: ./gradlew build -x :integration-tests:test --no-daemon

      - name: Build and push Docker images
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          services=("api-gateway" "auth-service" "station-service" "coupon-service" "raffle-service" "redemption-service")

          for service in "${services[@]}"; do
            echo "Building $service..."

            # Build Docker image
            docker build -t $ECR_REGISTRY/$ECR_REPOSITORY/$service:$IMAGE_TAG \
              -f docker/Dockerfile.service \
              --build-arg SERVICE_NAME=$service .

            # Push to ECR
            docker push $ECR_REGISTRY/$ECR_REPOSITORY/$service:$IMAGE_TAG

            # Tag as latest
            docker tag $ECR_REGISTRY/$ECR_REPOSITORY/$service:$IMAGE_TAG \
              $ECR_REGISTRY/$ECR_REPOSITORY/$service:latest
            docker push $ECR_REGISTRY/$ECR_REPOSITORY/$service:latest
          done

      - name: Deploy to ECS
        env:
          IMAGE_TAG: ${{ github.sha }}
        run: |
          services=("api-gateway" "auth-service" "station-service" "coupon-service" "raffle-service" "redemption-service")

          for service in "${services[@]}"; do
            echo "Deploying $service to ECS..."

            # Update task definition
            aws ecs update-service \
              --cluster gasolinera-cluster \
              --service gasolinera-$service \
              --force-new-deployment

            # Wait for deployment to complete
            aws ecs wait services-stable \
              --cluster gasolinera-cluster \
              --services gasolinera-$service
          done

      - name: Deploy Frontend to S3/CloudFront
        run: |
          # Build frontend
          cd apps/admin
          npm install
          npm run build

          # Upload to S3
          aws s3 sync out/ s3://gasolinera-admin-frontend --delete

          # Invalidate CloudFront
          aws cloudfront create-invalidation \
            --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} \
            --paths "/*"
```

## 🚀 COMANDOS DE DESPLIEGUE

### Infraestructura

```bash
# Inicializar Terraform
cd infrastructure/terraform
terraform init

# Planificar cambios
terraform plan -var="db_password=your-secure-password"

# Aplicar infraestructura
terraform apply -var="db_password=your-secure-password"
```

### Aplicaciones

```bash
# Build y push a ECR
./scripts/aws-build-and-push.sh

# Deploy a ECS
./scripts/aws-deploy-ecs.sh

# Deploy frontend a S3
./scripts/aws-deploy-frontend.sh
```

## 📊 URLS DE ACCESO

- **API Gateway**: `https://api.gasolinerajsm.com`
- **Admin Frontend**: `https://admin.gasolinerajsm.com`
- **Owner Dashboard**: `https://dashboard.gasolinerajsm.com`
- **Monitoring**: `https://monitoring.gasolinerajsm.com`

## 💰 COSTOS ESTIMADOS (Mensual)

| Servicio                  | Configuración                 | Costo Aprox.  |
| ------------------------- | ----------------------------- | ------------- |
| ECS Fargate               | 6 servicios, 0.25 vCPU, 0.5GB | $25           |
| RDS PostgreSQL            | db.t3.micro, Multi-AZ         | $30           |
| ElastiCache Redis         | cache.t3.micro                | $15           |
| Application Load Balancer | 1 ALB                         | $20           |
| CloudFront                | 1TB transferencia             | $10           |
| S3                        | Frontend hosting              | $5            |
| **TOTAL**                 |                               | **~$105/mes** |

## 🎯 VENTAJAS AWS

✅ **Alta disponibilidad** con Multi-AZ
✅ **Escalabilidad automática** con ECS Fargate
✅ **Seguridad enterprise** con VPC, Security Groups
✅ **Monitoreo completo** con CloudWatch
✅ **Backup automático** con RDS
✅ **CDN global** con CloudFront
✅ **SSL automático** con Certificate Manager
