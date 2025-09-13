# Kubernetes Deployment for Gasolinera JSM

## 🚀 Overview

This directory contains comprehensive Kubernetes manifests for deploying the Gasolinera JSM platform in a production-ready cloud environment. The deployment includes:

- **High-availability microservices architecture**
- **Scalable database cluster with read replicas**
- **Redis caching layer with persistence**
- **RabbitMQ message broker with clustering**
- **Complete monitoring stack (Prometheus + Grafana)**
- **Secure ingress with SSL termination**
- **Automated deployment and management scripts**

## 📁 Directory Structure

```
kubernetes/
├── namespace.yml              # Namespace, quotas, and limits
├── secrets.yml               # All sensitive configuration
├── configmaps.yml            # Non-sensitive configuration
├── storage.yml               # Storage classes and PVCs
├── database/
│   ├── postgres-primary.yml  # Primary PostgreSQL database
│   └── postgres-replica.yml  # Read replica PostgreSQL
├── cache/
│   └── redis-primary.yml     # Redis cache with persistence
├── messaging/
│   └── rabbitmq.yml         # RabbitMQ message broker
├── applications/
│   ├── api-gateway.yml      # API Gateway service
│   └── user-service.yml     # User management service
├── monitoring/
│   ├── prometheus.yml       # Metrics collection
│   └── grafana.yml         # Visualization dashboards
├── ingress/
│   └── ingress.yml         # External access and SSL
├── deploy.sh              # Automated deployment script
└── README.md             # This file
```

## 🛠️ Prerequisites

### Required Tools

- `kubectl` (v1.25+)
- `helm` (v3.10+)
- Access to a Kubernetes cluster (v1.25+)

### Cluster Requirements

- **Minimum 3 nodes** (for high availability)
- **8 CPU cores** and **16GB RAM** per node
- **Storage class** supporting dynamic provisioning
- **LoadBalancer** service support (cloud provider)

### Cloud Provider Setup

The manifests are configured for cloud deployment with:

- **AWS EBS** storage (can be adapted for GCP/Azure)
- **Application Load Balancer** integration
- **Let's Encrypt** SSL certificates

## 🚀 Quick Start

### 1. Clone and Navigate

```bash
cd gasolinera-jsm-ultimate/infrastructure/kubernetes
```

### 2. Update Configuration

Before deployment, update the following files with your specific values:

#### secrets.yml

```bash
# Replace base64 encoded values with your actual secrets
echo -n "your_postgres_password" | base64
echo -n "your_redis_password" | base64
echo -n "your_jwt_secret" | base64
```

#### ingress.yml

```yaml
# Update domain names
- host: your-domain.com
- host: api.your-domain.com
- host: admin.your-domain.com
```

### 3. Deploy Everything

```bash
# Automated deployment
./deploy.sh deploy

# Or manual step-by-step deployment
kubectl apply -f namespace.yml
kubectl apply -f secrets.yml
kubectl apply -f configmaps.yml
kubectl apply -f storage.yml
# ... continue with other manifests
```

### 4. Verify Deployment

```bash
# Check deployment status
./deploy.sh verify

# Monitor pods
kubectl get pods -n gasolinera-jsm -w

# Check services
kubectl get svc -n gasolinera-jsm
```

## 📊 Monitoring and Observability

### Prometheus Metrics

- **Application metrics**: Custom business metrics
- **Infrastructure metrics**: CPU, memory, disk, network
- **Database metrics**: Connection pools, query performance
- **Cache metrics**: Hit rates, memory usage
- **Message queue metrics**: Queue lengths, throughput

### Grafana Dashboards

- **Platform Overview**: High-level system health
- **Kubernetes Cluster**: Node and pod monitoring
- **Database Performance**: Query analysis and optimization
- **Application Performance**: Response times and error rates

### Access Monitoring

```bash
# Port forward to Grafana
kubectl port-forward svc/grafana 3000:3000 -n gasolinera-jsm

# Port forward to Prometheus
kubectl port-forward svc/prometheus 9090:9090 -n gasolinera-jsm
```

## 🔒 Security Features

### Network Security

- **Network policies** for pod-to-pod communication
- **Ingress controller** with rate limiting
- **TLS termination** with Let's Encrypt certificates
- **CORS configuration** for web security

### Pod Security

- **Non-root containers** for all services
- **Read-only root filesystems** where possible
- **Security contexts** with minimal privileges
- **Resource limits** to prevent resource exhaustion

### Secrets Management

- **Kubernetes secrets** for sensitive data
- **Base64 encoding** for configuration
- **Separate secrets** per service type
- **Rotation-ready** secret structure

## 📈 Scaling and Performance

### Horizontal Pod Autoscaling

```yaml
# Automatic scaling based on CPU/Memory
minReplicas: 2
maxReplicas: 10
targetCPUUtilizationPercentage: 70
```

### Database Scaling

- **Primary-replica setup** for read scaling
- **Connection pooling** with HikariCP
- **Query optimization** with pg_stat_statements
- **Automated backups** and point-in-time recovery

### Cache Optimization

- **Redis persistence** with AOF and RDB
- **Memory optimization** with LRU eviction
- **Connection pooling** with Lettuce
- **Monitoring** with Redis exporter

## 🔧 Maintenance Operations

### Backup and Recovery

```bash
# Database backup
kubectl exec -it postgres-primary-0 -n gasolinera-jsm -- pg_dump -U postgres gasolinera_jsm > backup.sql

# Redis backup
kubectl exec -it redis-primary-0 -n gasolinera-jsm -- redis-cli BGSAVE
```

### Updates and Rollbacks

```bash
# Rolling update
kubectl set image deployment/api-gateway api-gateway=gasolinera-jsm/api-gateway:v2.0.0 -n gasolinera-jsm

# Rollback
kubectl rollout undo deployment/api-gateway -n gasolinera-jsm
```

### Log Management

```bash
# View application logs
kubectl logs -f deployment/api-gateway -n gasolinera-jsm

# View all pods logs
kubectl logs -f -l app.kubernetes.io/part-of=gasolinera-jsm-platform -n gasolinera-jsm
```

## 🌐 Domain and DNS Configuration

### DNS Records Required

```
A     gasolinera-jsm.com           -> LoadBalancer IP
CNAME www.gasolinera-jsm.com       -> gasolinera-jsm.com
CNAME api.gasolinera-jsm.com       -> gasolinera-jsm.com
CNAME admin.gasolinera-jsm.com     -> gasolinera-jsm.com
CNAME internal.gasolinera-jsm.com  -> gasolinera-jsm.com
```

### SSL Certificate Management

- **Automatic certificate provisioning** with cert-manager
- **Let's Encrypt integration** for free SSL certificates
- **Certificate renewal** handled automatically
- **Multiple domain support** in single certificate

## 🚨 Troubleshooting

### Common Issues

#### Pods Stuck in Pending

```bash
# Check node resources
kubectl describe nodes

# Check PVC status
kubectl get pvc -n gasolinera-jsm

# Check events
kubectl get events -n gasolinera-jsm --sort-by='.lastTimestamp'
```

#### Database Connection Issues

```bash
# Check database pod logs
kubectl logs postgres-primary-0 -n gasolinera-jsm

# Test database connectivity
kubectl exec -it postgres-primary-0 -n gasolinera-jsm -- psql -U postgres -d gasolinera_jsm -c "SELECT 1;"
```

#### Ingress Not Working

```bash
# Check ingress controller
kubectl get pods -n ingress-nginx

# Check certificate status
kubectl get certificates -n gasolinera-jsm

# Check ingress events
kubectl describe ingress gasolinera-jsm-ingress -n gasolinera-jsm
```

### Performance Tuning

#### Database Optimization

- Adjust `shared_buffers` based on available memory
- Tune `work_mem` for complex queries
- Configure `effective_cache_size` appropriately
- Monitor slow queries with `pg_stat_statements`

#### Application Tuning

- Adjust JVM heap sizes based on pod memory limits
- Configure connection pool sizes appropriately
- Enable G1GC for better garbage collection
- Monitor application metrics in Grafana

## 📚 Additional Resources

### Kubernetes Documentation

- [Kubernetes Official Docs](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Helm Documentation](https://helm.sh/docs/)

### Monitoring and Observability

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Kubernetes Monitoring Best Practices](https://kubernetes.io/docs/concepts/cluster-administration/monitoring/)

### Security Best Practices

- [Kubernetes Security Best Practices](https://kubernetes.io/docs/concepts/security/)
- [Pod Security Standards](https://kubernetes.io/docs/concepts/security/pod-security-standards/)
- [Network Policies](https://kubernetes.io/docs/concepts/services-networking/network-policies/)

## 🤝 Support and Contribution

For issues, questions, or contributions:

1. Check the troubleshooting section above
2. Review application logs and Kubernetes events
3. Consult the monitoring dashboards in Grafana
4. Create detailed issue reports with logs and configurations

---

**¡Gasolinera JSM - Powering the Future of Fuel Management! ⛽🚀**
