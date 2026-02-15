#!/bin/bash

#
# Start Local Development Environment
#
# Sets up local PostgreSQL, builds application, and starts services
#
# Usage:
#   chmod +x scripts/local-dev/start-local-env.sh
#   ./scripts/local-dev/start-local-env.sh
#

set -e

echo "====== Spring Boot CI/CD Platform - Local Dev Environment ======"
echo ""

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed"
    exit 1
fi

# Change to script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
cd "$SCRIPT_ROOT"

echo "Project root: $PROJECT_ROOT"
echo ""

# Check if docker-compose.yml exists
if [ ! -f "$SCRIPT_DIR/docker-compose.yml" ]; then
    echo "Error: docker-compose.yml not found in $SCRIPT_DIR"
    exit 1
fi

echo "====== Building Application ======"
echo ""

# Build application with Gradle
if ! command -v ./gradlew &> /dev/null; then
    echo "Installing Gradle wrapper..."
    chmod +x ./gradlew
fi

echo "Building Spring Boot application..."
./gradlew clean build -x test

if [ $? -ne 0 ]; then
    echo "Error: Build failed"
    exit 1
fi

echo ""
echo "====== Building Docker Image ======"
echo ""

echo "Building Docker image..."
docker build \
    -t springboot-app:local \
    -f application/docker/Dockerfile \
    .

if [ $? -ne 0 ]; then
    echo "Error: Docker build failed"
    exit 1
fi

echo ""
echo "====== Starting Services ======"
echo ""

# Start Docker Compose
echo "Starting Docker Compose services..."
docker-compose -f "$SCRIPT_DIR/docker-compose.yml" up -d

if [ $? -ne 0 ]; then
    echo "Error: Docker Compose failed to start"
    exit 1
fi

echo ""
echo "====== Waiting for Services ======"
echo ""

# Wait for application to be ready
echo "Waiting for application to be ready..."
MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✓ Application is ready!"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    sleep 2
    echo -n "."
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo ""
    echo "⚠ Application took too long to start. It may still be initializing."
    echo "Check logs with: docker logs springboot-app"
fi

echo ""
echo "====== Local Environment Ready ======"
echo ""
echo "Services running:"
echo "  Application: http://localhost:8080"
echo "  H2 Console:  http://localhost:8080/h2-console"
echo "  Health:      http://localhost:8080/actuator/health"
echo "  Metrics:     http://localhost:8080/actuator/metrics"
echo ""
echo "API Endpoints:"
echo "  GET    /api/users              - List all users"
echo "  POST   /api/users              - Create user"
echo "  GET    /api/users/{id}         - Get user by ID"
echo "  PUT    /api/users/{id}         - Update user"
echo "  DELETE /api/users/{id}         - Delete user"
echo ""
echo "Database:"
echo "  Host:     localhost:5432"
echo "  Database: springboot_db"
echo "  User:     springboot_user"
echo "  Password: springboot_password"
echo ""
echo "Useful commands:"
echo "  View logs:  docker logs -f springboot-app"
echo "  Stop:       docker-compose -f scripts/local-dev/docker-compose.yml down"
echo "  Restart:    docker-compose -f scripts/local-dev/docker-compose.yml restart"
echo ""
echo "====== Test the Application ======"
echo ""

# Test the API
echo "Testing API endpoints..."
echo ""

echo "GET /api/users:"
curl -s http://localhost:8080/api/users | head -20
echo ""
echo ""

echo "Testing health endpoint:"
curl -s http://localhost:8080/actuator/health
echo ""
echo ""

echo "✓ Local development environment is ready!"
echo ""
