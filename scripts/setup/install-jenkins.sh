#!/bin/bash

#
# Jenkins Installation Script
#
# This script sets up Jenkins in a Docker container with required plugins
# and configuration for the Spring Boot CI/CD Platform
#
# Usage:
#   chmod +x scripts/setup/install-jenkins.sh
#   ./scripts/setup/install-jenkins.sh
#

set -e

echo "====== Spring Boot CI/CD Platform - Jenkins Setup ======"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
JENKINS_PORT=${JENKINS_PORT:-8080}
JENKINS_AGENTS_PORT=${JENKINS_AGENTS_PORT:-50000}
JENKINS_VOLUME=${JENKINS_VOLUME:-jenkins_home}

echo "Configuration:"
echo "  Jenkins Port: $JENKINS_PORT"
echo "  Agents Port: $JENKINS_AGENTS_PORT"
echo "  Jenkins Volume: $JENKINS_VOLUME"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker is not installed${NC}"
    echo "Please install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

echo -e "${GREEN}✓ Docker is installed${NC}"

# Check if Docker daemon is running
if ! docker ps &> /dev/null; then
    echo -e "${RED}✗ Docker daemon is not running${NC}"
    echo "Please start Docker and try again"
    exit 1
fi

echo -e "${GREEN}✓ Docker daemon is running${NC}"
echo ""

# Create Jenkins volume
echo "Creating Jenkins volume..."
docker volume create $JENKINS_VOLUME 2>/dev/null || echo "  (Volume already exists)"

# Run Jenkins container
echo "Starting Jenkins container..."
docker run \
  -d \
  --name jenkins \
  --restart=always \
  -p $JENKINS_PORT:8080 \
  -p $JENKINS_AGENTS_PORT:50000 \
  -v $JENKINS_VOLUME:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e JAVA_OPTS="-Xmx2g" \
  jenkins/jenkins:lts

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Jenkins container started${NC}"
else
    echo -e "${RED}✗ Failed to start Jenkins container${NC}"
    exit 1
fi

echo ""
echo "====== Jenkins is starting ======"
echo ""
echo "Waiting for Jenkins to be ready..."
echo "(This may take 1-2 minutes)"
echo ""

# Wait for Jenkins to be ready
max_attempts=60
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if curl -s http://localhost:$JENKINS_PORT/api/json > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Jenkins is ready!${NC}"
        break
    fi
    attempt=$((attempt + 1))
    sleep 2
    echo -n "."
done

if [ $attempt -eq $max_attempts ]; then
    echo -e "${YELLOW}✗ Jenkins took too long to start. It may still be initializing.${NC}"
fi

echo ""
echo ""
echo "====== Initial Setup ======"
echo ""

# Get initial admin password
JENKINS_CONTAINER=$(docker ps --filter "name=jenkins" --format "{{.ID}}")

if [ -z "$JENKINS_CONTAINER" ]; then
    echo -e "${RED}✗ Jenkins container not found${NC}"
    exit 1
fi

echo "Retrieving initial admin password..."
sleep 5  # Wait a bit more to ensure logs are available

PASSWORD=$(docker logs $JENKINS_CONTAINER 2>&1 | grep -A5 "Jenkins initial setup is required" | grep -oP '(?<=password: )\w+' | head -1 || echo "")

if [ -z "$PASSWORD" ]; then
    echo -e "${YELLOW}⚠ Could not automatically retrieve password${NC}"
    echo ""
    echo "To get the initial admin password, run:"
    echo "  docker logs jenkins | grep -A5 'Jenkins initial setup'"
    echo ""
    echo "Or:"
    echo "  docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword"
else
    echo ""
    echo -e "${GREEN}Initial Admin Password:${NC}"
    echo "$PASSWORD"
    echo ""
fi

echo "====== Setup Instructions ======"
echo ""
echo "1. Open Jenkins in browser:"
echo "   http://localhost:$JENKINS_PORT"
echo ""
echo "2. Log in with username: admin"
if [ -n "$PASSWORD" ]; then
    echo "   Password: (shown above)"
else
    echo "   Password: (retrieve with: docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword)"
fi
echo ""
echo "3. Install recommended plugins"
echo ""
echo "4. Create new credentials for:"
echo "   - Git (if private repository)"
echo "   - Docker registry (ECR, Docker Hub, etc.)"
echo "   - SonarQube (if using)"
echo "   - Kubernetes (kubeconfig)"
echo ""
echo "5. Create pipeline jobs:"
echo "   - Job 1: springboot-cicd-platform-ci"
echo "     Pipeline script from SCM → github/repo → jenkins/Jenkinsfile.ci"
echo ""
echo "   - Job 2: springboot-cicd-platform-build"
echo "     Pipeline script from SCM → github/repo → jenkins/Jenkinsfile.build"
echo ""
echo "   - Job 3: springboot-cicd-platform-deploy"
echo "     Pipeline script from SCM → github/repo → jenkins/Jenkinsfile.deploy"
echo ""
echo "   - Job 4: springboot-cicd-platform-full (optional)"
echo "     Pipeline script from SCM → github/repo → jenkins/Jenkinsfile.full"
echo ""
echo "6. Configure job triggers:"
echo "   - Poll SCM: H/15 * * * * (every 15 minutes)"
echo "   - GitHub webhook (if available)"
echo ""
echo "====== Required Jenkins Plugins ======"
echo ""
echo "The following plugins should be installed:"
echo "  - Pipeline (Declarative and Scripted)"
echo "  - Git"
echo "  - Docker Pipeline"
echo "  - Kubernetes"
echo "  - SonarQube Scanner"
echo "  - Email Extension"
echo "  - JUnit"
echo "  - Slack (optional)"
echo ""
echo "Install via: Manage Jenkins → Manage Plugins"
echo ""
echo "====== Useful Docker Commands ======"
echo ""
echo "View Jenkins logs:"
echo "  docker logs -f jenkins"
echo ""
echo "Stop Jenkins:"
echo "  docker stop jenkins"
echo ""
echo "Start Jenkins:"
echo "  docker start jenkins"
echo ""
echo "Remove Jenkins container and volume:"
echo "  docker stop jenkins && docker rm jenkins && docker volume rm $JENKINS_VOLUME"
echo ""
echo "====== Documentation ======"
echo ""
echo "For detailed setup and configuration, see:"
echo "  docs/02-jenkins-setup.md"
echo ""

echo -e "${GREEN}Jenkins setup complete!${NC}"
