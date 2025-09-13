#!/bin/bash

# Kubernetes Deployment Script for Gasolinera JSM
# Automated deployment of the entire platform to Kubernetes cluster

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="gasolinera-jsm"
KUBECTL_TIMEOUT="300s"
DEPLOYMENT_ORDER=(
    "namespace.yml"
    "secrets.yml"
    "configmaps.yml"
    "storage.yml"
    "database/postgres-primary.yml"
    "database/postgres-replica.yml"
    "cache/redis-primary.yml"
    "messaging/rabbitmq.yml"
    "monitoring/prometheus.yml"
    "monitoring/grafana.yml"
    "applications/api-gateway.yml"
    "applications/user-service.yml"
    "ingress/ingress.yml"
)

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed or not in PATH"
        exit 1
    fi

    # Check if kubectl can connect to cluster
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi

    # Check if helm is installed (for cert-manager)
    if ! command -v helm &> /dev/null; then
        log_warning "helm is not installed. Some features may not work properly."
    fi

    log_success "Prerequisites check completed"
}

install_cert_manager() {
    log_info "Installing cert-manager..."

    # Add cert-manager repository
    helm repo add jetstack https://charts.jetstack.io || true
    helm repo update

    # Install cert-manager
    helm upgrade --install cert-manager jetstack/cert-manager \
        --namespace cert-manager \
        --create-namespace \
        --version v1.13.0 \
        --set installCRDs=true \
        --wait

    # Wait for cert-manager to be ready
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/instance=cert-manager -n cert-manager --timeout=300s

    log_success "cert-manager installed successfully"
}

create_cluster_issuer() {
    log_info "Creating Let's Encrypt cluster issuer..."

    cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@gasolinera-jsm.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF

    log_success "Cluster issuer created"
}

deploy_nginx_ingress() {
    log_info "Installing NGINX Ingress Controller..."

    # Add ingress-nginx repository
    helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx || true
    helm repo update

    # Install NGINX Ingress Controller
    helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
        --namespace ingress-nginx \
        --create-namespace \
        --set controller.replicaCount=2 \
        --set controller.nodeSelector."kubernetes\\.io/os"=linux \
        --set defaultBackend.nodeSelector."kubernetes\\.io/os"=linux \
        --set controller.admissionWebhooks.patch.nodeSelector."kubernetes\\.io/os"=linux \
        --set controller.service.externalTrafficPolicy=Local \
        --wait

    log_success "NGINX Ingress Controller installed"
}

wait_for_deployment() {
    local resource_type=$1
    local resource_name=$2
    local namespace=$3
    local timeout=${4:-300s}

    log_info "Waiting for $resource_type/$resource_name to be ready..."

    case $resource_type in
        "deployment")
            kubectl rollout status deployment/$resource_name -n $namespace --timeout=$timeout
            ;;
        "statefulset")
            kubectl rollout status statefulset/$resource_name -n $namespace --timeout=$timeout
            ;;
        "pod")
            kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=$resource_name -n $namespace --timeout=$timeout
            ;;
        *)
            sleep 10
            ;;
    esac
}

deploy_manifest() {
    local manifest_file=$1

    log_info "Deploying $manifest_file..."

    if [[ ! -f "$manifest_file" ]]; then
        log_error "Manifest file $manifest_file not found"
        return 1
    fi

    # Apply the manifest
    kubectl apply -f "$manifest_file"

    # Wait for specific resources based on file name
    case "$manifest_file" in
        *"postgres-primary"*)
            wait_for_deployment "statefulset" "postgres-primary" "$NAMESPACE"
            ;;
        *"postgres-replica"*)
            wait_for_deployment "statefulset" "postgres-replica" "$NAMESPACE"
            ;;
        *"redis-primary"*)
            wait_for_deployment "statefulset" "redis-primary" "$NAMESPACE"
            ;;
        *"rabbitmq"*)
            wait_for_deployment "statefulset" "rabbitmq" "$NAMESPACE"
            ;;
        *"prometheus"*)
            wait_for_deployment "statefulset" "prometheus" "$NAMESPACE"
            ;;
        *"grafana"*)
            wait_for_deployment "deployment" "grafana" "$NAMESPACE"
            ;;
        *"api-gateway"*)
            wait_for_deployment "deployment" "api-gateway" "$NAMESPACE"
            ;;
        *"user-service"*)
            wait_for_deployment "deployment" "user-service" "$NAMESPACE"
            ;;
    esac

    log_success "$manifest_file deployed successfully"
}

verify_deployment() {
    log_info "Verifying deployment..."

    # Check namespace
    if kubectl get namespace $NAMESPACE &> /dev/null; then
        log_success "Namespace $NAMESPACE exists"
    else
        log_error "Namespace $NAMESPACE not found"
        return 1
    fi

    # Check pods
    log_info "Pod status:"
    kubectl get pods -n $NAMESPACE -o wide

    # Check services
    log_info "Service status:"
    kubectl get services -n $NAMESPACE

    # Check ingress
    log_info "Ingress status:"
    kubectl get ingress -n $NAMESPACE

    # Check persistent volumes
    log_info "Persistent Volume Claims:"
    kubectl get pvc -n $NAMESPACE

    # Check for failed pods
    failed_pods=$(kubectl get pods -n $NAMESPACE --field-selector=status.phase=Failed -o jsonpath='{.items[*].metadata.name}')
    if [[ -n "$failed_pods" ]]; then
        log_warning "Found failed pods: $failed_pods"
    fi

    # Check for pending pods
    pending_pods=$(kubectl get pods -n $NAMESPACE --field-selector=status.phase=Pending -o jsonpath='{.items[*].metadata.name}')
    if [[ -n "$pending_pods" ]]; then
        log_warning "Found pending pods: $pending_pods"
    fi

    log_success "Deployment verification completed"
}

cleanup_deployment() {
    log_warning "Cleaning up deployment..."

    # Delete all resources in namespace
    kubectl delete namespace $NAMESPACE --ignore-not-found=true

    # Wait for namespace deletion
    while kubectl get namespace $NAMESPACE &> /dev/null; do
        log_info "Waiting for namespace deletion..."
        sleep 5
    done

    log_success "Cleanup completed"
}

show_access_info() {
    log_info "Deployment completed! Access information:"
    echo ""
    echo "🌐 Application URLs:"
    echo "   Main App: https://gasolinera-jsm.com"
    echo "   API Gateway: https://api.gasolinera-jsm.com"
    echo "   Admin Panel: https://admin.gasolinera-jsm.com"
    echo ""
    echo "📊 Monitoring:"
    echo "   Grafana: https://admin.gasolinera-jsm.com/grafana"
    echo "   Prometheus: https://admin.gasolinera-jsm.com/prometheus"
    echo "   RabbitMQ: https://admin.gasolinera-jsm.com/rabbitmq"
    echo ""
    echo "🔧 Internal Services (requires authentication):"
    echo "   Internal API: https://internal.gasolinera-jsm.com"
    echo ""
    echo "📝 Useful commands:"
    echo "   kubectl get pods -n $NAMESPACE"
    echo "   kubectl logs -f deployment/api-gateway -n $NAMESPACE"
    echo "   kubectl port-forward svc/grafana 3000:3000 -n $NAMESPACE"
    echo ""
}

main() {
    local action=${1:-"deploy"}

    case $action in
        "deploy")
            log_info "Starting Gasolinera JSM deployment to Kubernetes..."

            check_prerequisites

            # Install infrastructure components
            install_cert_manager
            deploy_nginx_ingress
            create_cluster_issuer

            # Deploy application manifests in order
            for manifest in "${DEPLOYMENT_ORDER[@]}"; do
                deploy_manifest "$manifest"
                sleep 5  # Brief pause between deployments
            done

            verify_deployment
            show_access_info
            ;;

        "cleanup")
            cleanup_deployment
            ;;

        "verify")
            verify_deployment
            ;;

        "status")
            kubectl get all -n $NAMESPACE
            ;;

        *)
            echo "Usage: $0 {deploy|cleanup|verify|status}"
            echo ""
            echo "Commands:"
            echo "  deploy  - Deploy the entire platform"
            echo "  cleanup - Remove all deployed resources"
            echo "  verify  - Verify deployment status"
            echo "  status  - Show current status"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"