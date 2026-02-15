# Spring Boot CI/CD Platform - Architecture Overview

## Table of Contents
1. [Context](#context)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Design Decisions](#design-decisions)
5. [Pipeline Flow](#pipeline-flow)
6. [Security Architecture](#security-architecture)

## Context

Organizations building Spring Boot microservices need:
- Automated build, test, and deployment pipelines
- Security scanning integrated into the development workflow
- Multi-environment support (dev/staging/production)
- Kubernetes-native deployment strategies
- Comprehensive documentation and examples

This platform provides a complete, production-ready reference implementation.

## Architecture

### High-Level Design

```
Source Code (Git)
    ↓
[Jenkins CI Pipeline]
    ├→ Checkout
    ├→ Build
    ├→ Unit Tests
    ├→ Code Quality (SonarQube)
    ├→ Security Scan (OWASP)
    └→ Artifacts
         ↓
    [Jenkins Build Pipeline]
         ├→ Docker Build
         ├→ Container Scan (Trivy)
         ├→ Tag
         └→ Push to Registry
              ↓
         [Jenkins Deploy Pipeline]
              ├→ Deploy Dev (Auto)
              ├→ Deploy Staging (Approval)
              └→ Deploy Production (Approval + Canary)
                   ↓
              [Kubernetes - Dev/Staging/Prod]
                   ├→ Deployment (Rolling Update)
                   ├→ Service
                   ├→ Horizontal Pod Autoscaler
                   ├→ Pod Disruption Budget
                   └→ Monitoring/Alerts
```

### Multi-Module Application Architecture

```
application/
├── common/                    (DTOs, Exceptions, Utilities)
│   └── No external dependencies
│
├── domain/                    (Business Logic, Services, Entities)
│   └── Depends on: common
│   └── Defines: UserRepository interface
│
├── infrastructure/            (Persistence, JPA, Repositories)
│   └── Depends on: domain, common
│   └── Implements: UserRepository interface
│
└── api/                       (REST Controllers, Main Application)
    └── Depends on: all modules
    └── Contains: Application entry point
```

This follows **Dependency Inversion Principle**: domain layer defines interfaces, infrastructure implements them.

## Technology Stack

### Build & Application
- **Java**: 17 LTS (long-term support, modern features)
- **Build Tool**: Gradle 8.5 with Kotlin DSL
- **Framework**: Spring Boot 3.2.2
- **Database**: H2 (development), configurable for production

### CI/CD
- **Orchestration**: Jenkins (declarative pipelines)
- **Container**: Docker with multi-stage builds
- **Container Registry**: Configurable (ECR, Docker Hub, etc.)

### Security
- **SAST**: SonarQube (code quality and vulnerabilities)
- **SCA**: OWASP Dependency-Check (supply chain)
- **Container Scanning**: Trivy (OS packages, app dependencies)

### Kubernetes & Deployment
- **Deployment Tools**:
  - Kustomize (template-free overlays)
  - Helm (templating and package management)
- **Monitoring**: Prometheus metrics (built-in via Spring Actuator)
- **Minimum K8s Version**: 1.24

## Design Decisions

### 1. Multi-Stage Dockerfile

**Decision**: Use multi-stage Docker build

**Rationale**:
- Separates build environment from runtime
- Reduces final image size (only JRE needed in runtime)
- Improves security (build tools not in production image)
- Faster deployments due to smaller images

**Implementation**:
```dockerfile
Stage 1: eclipse-temurin:17-jdk (Build)
   ├→ Download dependencies
   ├→ Compile code
   ├→ Run tests
   └→ Create JAR

Stage 2: eclipse-temurin:17-jre-alpine (Runtime)
   ├→ Copy JAR from stage 1
   ├→ Run as non-root user
   └→ Health checks
```

### 2. Gradle Convention Plugins

**Decision**: Use buildSrc for shared build logic

**Rationale**:
- Single source of truth for build configuration
- Consistency across all modules
- Easier to update build process (one place)
- Reusable across projects

**Benefits**:
- Reduces boilerplate in individual module build files
- Centralized dependency management
- Enforces standards

### 3. Domain-Driven Design with Repository Pattern

**Decision**: Separate domain, infrastructure, and API layers

**Benefits**:
- Domain logic independent of persistence technology
- Easy to test (mock repositories)
- Easy to switch databases (implement interface)
- Clear separation of concerns

**Example**:
```
UserRepository (interface) ← domain/
    ↑
    ├ Defined in domain/
    └ Implemented in infrastructure/

UserRepositoryImpl (JPA) ← infrastructure/
    └ Domain layer never knows about JPA
```

### 4. Dual Deployment Tools (Kustomize AND Helm)

**Decision**: Provide both deployment approaches

**Rationale**:
- **Kustomize**: Simpler, template-free, GitOps-friendly
  - Good for: Infrastructure as Code, version control
  - Use case: Teams familiar with kubectl, minimal templates
- **Helm**: More powerful, package management
  - Good for: Package sharing, release management
  - Use case: Teams needing distribution, complex deployments

**Approach**: Reference implementation includes both

### 5. Progressive Delivery for Production

**Decision**: Implement canary deployment for production

**Rationale**:
- Reduce blast radius of bad deployments
- Monitor metrics before full rollout
- Easy automated rollback
- Builds confidence in automation

**Implementation**:
- Deploy to 10% of traffic
- Monitor error rates and latency
- If OK: gradually increase to 50%, then 100%
- If NOT OK: automatic rollback to previous version

### 6. Security-First CI/CD

**Decision**: Integrate security scanning at every stage

**Stages**:
1. **CI**: SonarQube (SAST) + OWASP Dependency-Check
2. **Build**: Trivy container scanning
3. **Deploy**: Pre-deployment validation

**Philosophy**: Fail fast, fail early, fix in development not production

## Pipeline Flow

### Complete End-to-End Flow (Jenkinsfile.full)

```
1. CI Pipeline (Automatic)
   ├→ Checkout code
   ├→ Build application
   ├→ Run tests
   ├→ SonarQube analysis
   ├→ OWASP dependency scan
   └→ Quality gate check
        ↓
2. Build Pipeline (Automatic)
   ├→ Determine version
   ├→ Build Docker image
   ├→ Trivy container scan
   ├→ Quality gate (no CRITICAL)
   └→ Push to registry
        ↓
3. Deploy Dev (Automatic)
   ├→ Deploy via Kustomize/Helm
   ├→ Wait for rollout
   ├→ Smoke tests
   └→ Validate
        ↓
4. Approve Staging (Manual)
   ├→ Engineering review
   └→ Approval timeout: 4 hours
        ↓
5. Deploy Staging (Automatic)
   ├→ Deploy via Helm
   ├→ Wait for rollout
   ├→ Smoke tests
   └→ Validate
        ↓
6. Approve Production (Manual)
   ├→ Engineering Lead OR Release Manager
   └→ Approval timeout: 8 hours
        ↓
7. Deploy Production (Automatic)
   ├→ Deploy via Helm
   ├→ Wait for rollout
   ├→ Canary deployment (10% → 50% → 100%)
   ├→ Automatic rollback on errors
   └→ Post-deployment validation
```

## Security Architecture

### Threat Model

**Threats Addressed**:
1. **Known Vulnerabilities**: Dependencies, OS packages
2. **Code Quality Issues**: Bugs that could become vulnerabilities
3. **Supply Chain**: Compromised dependencies
4. **Container**: Malicious or unpatched base images
5. **Runtime**: Privileges, access control

### Security Layers

```
Application Code
    ↓
[SonarQube SAST Analysis]
    ├→ SQL Injection
    ├→ XSS vulnerabilities
    ├→ Buffer overflows
    └→ Code smells
         ↓
    [OWASP Dependency-Check]
         ├→ CVE scanning
         ├→ Known vulnerable versions
         └→ Suppression for false positives
              ↓
         [Docker Build]
              ├→ Non-root user
              ├→ Read-only filesystem
              ├→ Security context
              └→ Network policies
                   ↓
              [Trivy Container Scan]
                   ├→ OS package vulnerabilities
                   ├→ Application dependencies
                   └→ Configuration issues
                        ↓
                   [Kubernetes Runtime]
                        ├→ Network policies
                        ├→ Pod security policies
                        ├→ RBAC
                        └→ Resource quotas
```

## Key Metrics

### Build Performance
- **CI Pipeline**: 15-20 minutes (with tests)
- **Build Pipeline**: 5-10 minutes
- **Deploy Pipeline**: 10-15 minutes
- **Total End-to-End**: 30-45 minutes (plus approval wait times)

### Application
- **Response Time**: <100ms (99th percentile)
- **Error Rate**: <0.1%
- **Availability**: 99.9% (with 3 replicas)
- **Startup Time**: <30 seconds

### Resource Efficiency
- **Dev**: 1 pod, 100m CPU, 256Mi memory
- **Staging**: 2 pods, 250m CPU, 512Mi memory each
- **Production**: 3+ pods, 500m CPU, 1Gi memory each
- **Auto-scaling**: 2-10 pods based on load

## Next Steps

1. Review [02-jenkins-setup.md](02-jenkins-setup.md) to install Jenkins
2. Study [03-pipeline-explanation.md](03-pipeline-explanation.md) for detailed pipeline walkthrough
3. Read [04-spring-boot-architecture.md](04-spring-boot-architecture.md) for application design
4. Explore [05-security-scanning.md](05-security-scanning.md) for security integration
5. Learn [06-kubernetes-deployment.md](06-kubernetes-deployment.md) for K8s concepts

---

**Key Takeaway**: This platform demonstrates enterprise-grade CI/CD with security, multi-environment support, and Kubernetes-native deployment. Every design decision prioritizes production readiness and learning value.
