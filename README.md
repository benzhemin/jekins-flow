# Spring Boot CI/CD Platform

A complete, production-grade CI/CD pipeline platform for Java Spring Boot applications, including comprehensive security scanning, multi-environment Kubernetes deployments, and detailed educational documentation.

## Overview

This project provides a reference implementation of modern CI/CD practices using:
- **Jenkins** for orchestration
- **Spring Boot 3.2** with Java 17 for the application
- **Docker** for containerization
- **Kubernetes** with Kustomize and Helm for deployment
- **SonarQube** for static analysis (SAST)
- **OWASP Dependency-Check** for supply chain security
- **Trivy** for container scanning

## What You'll Learn

- **Multi-module Gradle architecture** with convention plugins
- **Enterprise-grade Jenkinsfiles** with detailed comments
- **Security-first CI/CD** with integrated scanning at every stage
- **Kubernetes deployment** with both Kustomize and Helm
- **Progressive delivery** with canary deployments
- **Production-ready patterns** like pod disruption budgets, affinity rules, and health checks

## Quick Start

### Prerequisites

- Docker Desktop or Docker Engine
- kubectl (Kubernetes command-line tool)
- Helm 3+
- Kustomize 4+
- Java 17 JDK (for local development)
- Jenkins with Docker plugin (for CI/CD pipelines)

### Local Development Setup

```bash
# Clone the repository
git clone https://github.com/company/springboot-cicd-platform
cd springboot-cicd-platform

# Build the application
./gradlew clean build

# Run the application locally
./gradlew bootRun
# Application runs on http://localhost:8080

# Test the API
curl http://localhost:8080/api/users
```

### Build Docker Image Locally

```bash
# Build the Docker image
docker build -t springboot-app:local -f application/docker/Dockerfile .

# Run the container
docker run -p 8080:8080 springboot-app:local

# Test health endpoint
curl http://localhost:8080/actuator/health
```

### Deploy to Local Kubernetes

```bash
# Create dev namespace
kubectl create namespace dev

# Deploy with Kustomize
kubectl apply -k kubernetes/kustomize/overlays/dev

# Deploy with Helm
helm install springboot-app kubernetes/helm/springboot-app \
  --namespace dev \
  -f kubernetes/helm/springboot-app/values-dev.yaml

# Check deployment status
kubectl get pods -n dev
kubectl logs -n dev deployment/springboot-app
```

## Project Structure

```
springboot-cicd-platform/
├── README.md                              # This file
├── docs/                                  # 8 comprehensive guides
│   ├── 01-overview.md                    # Architecture and decisions
│   ├── 02-jenkins-setup.md               # Jenkins installation and configuration
│   ├── 03-pipeline-explanation.md        # Deep dive into each pipeline
│   ├── 04-spring-boot-architecture.md    # Multi-module design patterns
│   ├── 05-security-scanning.md           # Security tooling and practices
│   ├── 06-kubernetes-deployment.md       # K8s deployments and GitOps
│   ├── 07-troubleshooting.md             # Common issues and solutions
│   └── 08-advanced-patterns.md           # Production patterns
│
├── jenkins/                               # Jenkins pipeline files
│   ├── Jenkinsfile.ci                    # CI pipeline (build, test, scan)
│   ├── Jenkinsfile.build                 # Build pipeline (Docker, push)
│   ├── Jenkinsfile.deploy                # Deploy pipeline (multi-env)
│   ├── Jenkinsfile.full                  # Orchestrator (end-to-end)
│   └── shared-library/                   # Reusable pipeline functions
│       └── vars/
│           ├── deployToKubernetes.groovy
│           └── runSecurityScans.groovy
│
├── application/                          # Multi-module Spring Boot app
│   ├── settings.gradle.kts               # Gradle module configuration
│   ├── build.gradle.kts                  # Root Gradle build file
│   ├── buildSrc/                         # Convention plugins
│   │   ├── src/main/kotlin/
│   │   │   ├── spring-conventions.gradle.kts
│   │   │   └── java-library-conventions.gradle.kts
│   ├── common/                           # Shared DTOs, exceptions
│   ├── domain/                           # Business logic, services
│   ├── infrastructure/                   # Persistence, JPA, repositories
│   ├── api/                              # REST controllers, main app
│   └── docker/                           # Dockerfile, build artifacts
│
├── kubernetes/                           # Kubernetes deployment files
│   ├── kustomize/
│   │   ├── base/                         # Base resources
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   ├── configmap.yaml
│   │   │   ├── hpa.yaml
│   │   │   └── pdb.yaml
│   │   └── overlays/
│   │       ├── dev/                      # Development (1 replica, low resources)
│   │       ├── staging/                  # Staging (2 replicas, medium)
│   │       └── production/               # Production (3+ replicas, high)
│   │
│   └── helm/                             # Helm chart
│       └── springboot-app/
│           ├── Chart.yaml
│           ├── values.yaml
│           ├── values-dev.yaml
│           ├── values-staging.yaml
│           ├── values-production.yaml
│           └── templates/
│
├── security/                             # Security configuration
│   ├── sonarqube/
│   │   └── sonar-project.properties
│   └── owasp/
│       └── suppressions.xml
│
├── scripts/                              # Helper scripts
│   ├── setup/
│   │   └── install-jenkins.sh            # Jenkins Docker setup
│   └── local-dev/
│       ├── docker-compose.yml            # Local dev environment
│       └── start-local-env.sh            # Start local services
│
└── gradle/                               # Gradle wrapper
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

## Key Features

### Security-First Design
- **SAST**: SonarQube static analysis integrated into CI
- **SCA**: OWASP Dependency-Check for supply chain security
- **Container Security**: Trivy scanning of Docker images
- **Fail on Critical**: Pipelines fail on HIGH/CRITICAL vulnerabilities

### Multi-Environment Deployment
- **Dev**: 1 replica, minimal resources, auto-deployed
- **Staging**: 2 replicas, medium resources, manual approval
- **Production**: 3+ replicas, full resources, approval + canary

### Production-Grade Kubernetes
- **Rolling updates** with surge and unavailable pod control
- **Health checks** (startup, liveness, readiness probes)
- **Autoscaling** based on CPU and memory metrics
- **Pod Disruption Budgets** for high availability
- **Resource limits** to prevent noisy neighbor problems
- **Security context** for least privilege execution
- **Pod anti-affinity** to spread across nodes

### Dual Deployment Tools
- **Kustomize**: Template-free, GitOps-friendly overlays
- **Helm**: Powerful templating, release management, package sharing

### Comprehensive Pipelines
- **Jenkinsfile.ci**: Build, test, scan code (15-20 min)
- **Jenkinsfile.build**: Create Docker image, scan container (5-10 min)
- **Jenkinsfile.deploy**: Multi-environment with approval gates
- **Jenkinsfile.full**: Orchestrator managing complete end-to-end flow

## Jenkins Pipeline Flow

```
Commit → CI Pipeline → Build Pipeline → Deploy Dev →
         Staging Review → Deploy Staging →
         Production Review → Deploy Production (Canary)
```

## Documentation

Start with these in order:

1. **[01-overview.md](docs/01-overview.md)** - Architecture, tech stack, design decisions
2. **[02-jenkins-setup.md](docs/02-jenkins-setup.md)** - Install and configure Jenkins
3. **[03-pipeline-explanation.md](docs/03-pipeline-explanation.md)** - Deep dive into each Jenkinsfile
4. **[04-spring-boot-architecture.md](docs/04-spring-boot-architecture.md)** - Multi-module design
5. **[05-security-scanning.md](docs/05-security-scanning.md)** - Security tools and practices
6. **[06-kubernetes-deployment.md](docs/06-kubernetes-deployment.md)** - K8s and GitOps patterns
7. **[07-troubleshooting.md](docs/07-troubleshooting.md)** - Common issues and fixes
8. **[08-advanced-patterns.md](docs/08-advanced-patterns.md)** - Production patterns and optimizations

## API Endpoints

The Spring Boot application exposes the following REST API:

```
GET    /api/users              - List all users
GET    /api/users/{id}         - Get user by ID
POST   /api/users              - Create new user
PUT    /api/users/{id}         - Update user
DELETE /api/users/{id}         - Delete user
POST   /api/users/{id}/deactivate - Deactivate account

GET    /actuator/health        - Application health
GET    /actuator/metrics       - Application metrics
GET    /h2-console             - H2 database console (dev only)
```

## Deployment Commands

### Kustomize Deployment

```bash
# Development
kubectl apply -k kubernetes/kustomize/overlays/dev

# Staging
kubectl apply -k kubernetes/kustomize/overlays/staging

# Production
kubectl apply -k kubernetes/kustomize/overlays/production
```

### Helm Deployment

```bash
# Development
helm install springboot-app kubernetes/helm/springboot-app \
  --namespace dev \
  -f kubernetes/helm/springboot-app/values-dev.yaml

# Staging
helm install springboot-app kubernetes/helm/springboot-app \
  --namespace staging \
  -f kubernetes/helm/springboot-app/values-staging.yaml

# Production
helm install springboot-app kubernetes/helm/springboot-app \
  --namespace production \
  -f kubernetes/helm/springboot-app/values-production.yaml
```

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew integrationTest
```

### Build Test
```bash
./gradlew bootRun
# In another terminal:
curl http://localhost:8080/api/users
```

## Troubleshooting

Common issues and solutions are documented in [docs/07-troubleshooting.md](docs/07-troubleshooting.md).

For quick help:
- **Build fails**: Check Java version (requires 17), run `./gradlew clean build`
- **Docker build fails**: Ensure Docker daemon is running, check disk space
- **K8s deployment fails**: Check namespace, image pull secrets, resource requests

## Contributing

This is a reference implementation. To customize for your organization:

1. Update Docker registry URLs in Jenkinsfiles
2. Configure SonarQube and OWASP servers
3. Update Kubernetes cluster context names
4. Modify resource limits based on your cluster capacity
5. Adjust approval gates and email recipients

## Support

- Review documentation in `/docs` directory
- Check troubleshooting guide for common issues
- Examine Jenkinsfile comments for implementation details
- Review Spring Boot application code and JavaDocs

## License

This reference platform is provided as educational material.

## Version

- **Application Version**: 1.0.0
- **Java Version**: 17 LTS
- **Spring Boot Version**: 3.2.2
- **Gradle Version**: 8.5
- **Kubernetes Version**: 1.24+

---

**Last Updated**: February 2026
**Created for**: Enterprise-grade CI/CD learning and implementation
