#!/usr/bin/env bash
# Build all Docker images for local Kubernetes deployment.
# Run from the project root: bash k8s/build-images.sh
#
# For minikube: run `eval $(minikube docker-env)` first so images land in minikube's daemon.
# For Docker Desktop: images land in the local daemon and are available to k8s automatically.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "Building Spring Boot service images..."
docker build -t ecommerce/auth-service:latest         -f auth-service/Dockerfile         .
docker build -t ecommerce/product-service:latest      -f product-service/Dockerfile      .
docker build -t ecommerce/api-gateway:latest          -f api-gateway/Dockerfile          .
docker build -t ecommerce/cart-service:latest         -f cart-service/Dockerfile         .
docker build -t ecommerce/order-service:latest        -f order-service/Dockerfile        .
docker build -t ecommerce/payment-service:latest      -f payment-service/Dockerfile      .
docker build -t ecommerce/notification-service:latest -f notification-service/Dockerfile .

echo "Building Angular frontend image..."
docker build -t ecommerce/frontend:latest ecommerce-frontend/

echo ""
echo "All images built successfully."
echo "  auth-service, product-service, api-gateway, cart-service,"
echo "  order-service, payment-service, notification-service, frontend"