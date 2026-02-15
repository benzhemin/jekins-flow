# Jenkins Shared Library - Complete Guide from Concepts to Implementation

## Table of Contents
1. [Conceptual Foundation](#conceptual-foundation)
2. [The Problem It Solves](#the-problem-it-solves)
3. [Shared Library Architecture](#shared-library-architecture)
4. [Directory Structure](#directory-structure)
5. [Creating Shared Functions](#creating-shared-functions)
6. [Using Shared Library in Pipelines](#using-shared-library-in-pipelines)
7. [Advanced Patterns](#advanced-patterns)
8. [Testing Shared Library](#testing-shared-library)
9. [Best Practices](#best-practices)
10. [Real Examples from Platform](#real-examples-from-platform)

---

## Conceptual Foundation

### What is a Jenkins Shared Library?

**Definition**: A reusable Groovy code library that you can share across multiple Jenkins pipelines.

**Simple Analogy**:
```
Without Shared Library:
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ Jenkinsfile.ci   │  │ Jenkinsfile.build│  │Jenkinsfile.deploy│
│                  │  │                  │  │                  │
│ def deployApp()  │  │ def deployApp()  │  │ def deployApp()  │
│   // code...     │  │   // code...     │  │   // code...     │
│   // same!       │  │   // same!       │  │   // same!       │
└──────────────────┘  └──────────────────┘  └──────────────────┘
       ↓                      ↓                      ↓
     DRY (Don't Repeat Yourself) Violation!
     Same code copied 3 times = Maintenance nightmare


With Shared Library:
┌──────────────────────────────────────────────────┐
│           Shared Library                         │
│  ┌────────────────────────────────────────────┐  │
│  │ vars/deployApp.groovy                      │  │
│  │ def call() {                               │  │
│  │   // code once                             │  │
│  │ }                                          │  │
│  └────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
       ↑                      ↑                      ↑
       │                      │                      │
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ Jenkinsfile.ci   │  │ Jenkinsfile.build│  │Jenkinsfile.deploy│
│                  │  │                  │  │                  │
│ deployApp()      │  │ deployApp()      │  │ deployApp()      │
└──────────────────┘  └──────────────────┘  └──────────────────┘
       ↑                      ↑                      ↑
     Uses same function - Single source of truth!
```

### Why Shared Library Matters

**Problems Without It**:
1. **Code Duplication**: Same logic copied across pipelines
2. **Maintenance Nightmare**: Fix bug in one place, 10 other pipelines still broken
3. **Inconsistency**: Different implementations of same logic
4. **Testing Difficulty**: Can't test shared logic in isolation
5. **Version Control Chaos**: Multiple versions of same code

**Benefits With It**:
1. **DRY (Don't Repeat Yourself)**: Code once, use everywhere
2. **Consistency**: All pipelines use same logic
3. **Maintenance**: Fix bug once, all pipelines benefit
4. **Testability**: Test functions in isolation
5. **Reusability**: New pipelines can leverage existing functions
6. **Version Control**: Shared library versioned with Git

---

## The Problem It Solves

### Scenario: Deploying to Kubernetes

**Without Shared Library**:

```groovy
// Jenkinsfile.ci
pipeline {
    stages {
        stage('Deploy') {
            steps {
                script {
                    sh '''
                        kubectl config use-context ${CLUSTER}
                        kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                        KUSTOMIZE_PATH="kubernetes/kustomize/overlays/${ENVIRONMENT}"
                        cd $KUSTOMIZE_PATH
                        kustomize edit set image springboot-app=${FULL_IMAGE}
                        cd ${WORKSPACE}

                        kustomize build $KUSTOMIZE_PATH | \
                            kubectl apply -f - \
                            -n ${NAMESPACE}

                        kubectl rollout status deployment/springboot-app \
                            -n ${NAMESPACE} \
                            --timeout=5m
                    '''
                }
            }
        }
    }
}

// Jenkinsfile.build
pipeline {
    stages {
        stage('Deploy') {
            steps {
                script {
                    sh '''
                        kubectl config use-context ${CLUSTER}
                        kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                        KUSTOMIZE_PATH="kubernetes/kustomize/overlays/${ENVIRONMENT}"
                        cd $KUSTOMIZE_PATH
                        kustomize edit set image springboot-app=${FULL_IMAGE}
                        cd ${WORKSPACE}

                        kustomize build $KUSTOMIZE_PATH | \
                            kubectl apply -f - \
                            -n ${NAMESPACE}

                        kubectl rollout status deployment/springboot-app \
                            -n ${NAMESPACE} \
                            --timeout=5m
                    '''
                }
            }
        }
    }
}

// Jenkinsfile.deploy
pipeline {
    stages {
        stage('Deploy') {
            steps {
                script {
                    sh '''
                        kubectl config use-context ${CLUSTER}
                        kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

                        KUSTOMIZE_PATH="kubernetes/kustomize/overlays/${ENVIRONMENT}"
                        cd $KUSTOMIZE_PATH
                        kustomize edit set image springboot-app=${FULL_IMAGE}
                        cd ${WORKSPACE}

                        kustomize build $KUSTOMIZE_PATH | \
                            kubectl apply -f - \
                            -n ${NAMESPACE}

                        kubectl rollout status deployment/springboot-app \
                            -n ${NAMESPACE} \
                            --timeout=5m
                    '''
                }
            }
        }
    }
}

// THREE IDENTICAL COPIES!
// If we find a bug in deployment logic:
//   - Fix in 3 places
//   - Test all 3 pipelines
//   - Risk of inconsistency
// IF WE MISS ONE, PRODUCTION BREAKS!
```

**With Shared Library**:

```groovy
// jenkins/shared-library/vars/deployToKubernetes.groovy
def call(Map config) {
    def cluster = config.cluster
    def namespace = config.namespace
    def environment = config.environment
    def image = config.image

    sh '''
        kubectl config use-context ${cluster}
        kubectl create namespace ${namespace} --dry-run=client -o yaml | kubectl apply -f -

        KUSTOMIZE_PATH="kubernetes/kustomize/overlays/${environment}"
        cd $KUSTOMIZE_PATH
        kustomize edit set image springboot-app=${image}
        cd ${WORKSPACE}

        kustomize build $KUSTOMIZE_PATH | \
            kubectl apply -f - \
            -n ${namespace}

        kubectl rollout status deployment/springboot-app \
            -n ${namespace} \
            --timeout=5m
    '''
}

// Jenkinsfile.ci
@Library('jenkins-shared-library') _

pipeline {
    stages {
        stage('Deploy') {
            steps {
                deployToKubernetes(
                    cluster: 'dev-cluster',
                    namespace: 'dev',
                    environment: 'dev',
                    image: env.FULL_IMAGE
                )
            }
        }
    }
}

// Jenkinsfile.build
@Library('jenkins-shared-library') _

pipeline {
    stages {
        stage('Deploy') {
            steps {
                deployToKubernetes(
                    cluster: 'staging-cluster',
                    namespace: 'staging',
                    environment: 'staging',
                    image: env.FULL_IMAGE
                )
            }
        }
    }
}

// Jenkinsfile.deploy
@Library('jenkins-shared-library') _

pipeline {
    stages {
        stage('Deploy') {
            steps {
                deployToKubernetes(
                    cluster: 'prod-cluster',
                    namespace: 'production',
                    environment: 'production',
                    image: env.FULL_IMAGE
                )
            }
        }
    }
}

// SINGLE SOURCE OF TRUTH!
// If we find a bug:
//   - Fix in ONE place
//   - All pipelines automatically use fixed version
//   - No risk of inconsistency
```

**Key Difference**:
- Without: 3 copies of identical code (nightmare to maintain)
- With: 1 function, 3 different calls with different parameters (clean and maintainable)

---

## Shared Library Architecture

### How Jenkins Loads and Executes Shared Libraries

```
1. JENKINS CONFIGURATION
   ├─ Manage Jenkins → Configure System
   ├─ Global Pipeline Libraries section
   ├─ Add library: jenkins-shared-library
   │  ├─ Default version: main (Git branch)
   │  ├─ Repository URL: https://github.com/company/jenkins-shared-library.git
   │  └─ Load implicitly: ☑ (auto-load for all pipelines)
   │
   └─ Result: Jenkins knows where to find library

2. JENKINSFILE DECLARES LIBRARY
   @Library('jenkins-shared-library') _

   This tells Jenkins:
   - Load 'jenkins-shared-library' library
   - Use default branch (main)
   - Make functions available in this Jenkinsfile

3. JENKINS FETCHES FROM GIT
   git clone https://github.com/company/jenkins-shared-library.git
   git checkout main

4. JENKINS LOADS GROOVY FILES
   ├─ src/com/company/PipelineUtils.groovy (Shared classes)
   ├─ vars/deployToKubernetes.groovy (Global variables)
   └─ resources/ (Static files, templates)

5. FUNCTIONS AVAILABLE IN PIPELINE
   deployToKubernetes(config)  // Can now use in Jenkinsfile

6. EXECUTION
   When you call deployToKubernetes():
   ├─ Jenkins invokes the function
   ├─ Function executes in pipeline context
   ├─ Has access to environment variables, credentials, etc.
   └─ Results printed to console log
```

### Execution Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│  Jenkinsfile                                                │
│  @Library('jenkins-shared-library') _                       │
│                                                             │
│  pipeline {                                                 │
│    stages {                                                 │
│      stage('Deploy') {                                      │
│        steps {                                              │
│          deployToKubernetes(...)  ←─── CALL               │
│        }                                                    │
│      }                                                      │
│    }                                                        │
│  }                                                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓ Jenkins finds function
┌──────────────────────────────────────────────────────────────┐
│  Shared Library                                              │
│  vars/deployToKubernetes.groovy                             │
│                                                             │
│  def call(Map config) {  ←─── FUNCTION DEFINED            │
│    // Implementation                                        │
│    sh '''kubectl ...'''                                     │
│  }                                                          │
│                                                             │
│  ↓ Parameters passed                                        │
│                                                             │
│  def call(Map config) {                                     │
│    cluster = config.cluster                                │
│    namespace = config.namespace                            │
│    environment = config.environment                        │
│    image = config.image                                    │
│                                                             │
│    sh '''                                                  │
│      kubectl config use-context ${cluster}                │
│      ...                                                   │
│    '''                                                     │
│  }                                                          │
└──────────────────────────────────────────────────────────────┘
                       ↑
                       │ Function executes
                       │
┌──────────────────────────────────────────────────────────────┐
│  Pipeline Execution                                          │
│  - Kubernetes deployment applied                            │
│  - Rollout status monitored                                 │
│  - Results logged to console                                │
└──────────────────────────────────────────────────────────────┘
```

---

## Directory Structure

### Standard Shared Library Layout

```
jenkins-shared-library/
├── README.md                          # Library documentation
├── .git/                             # Git history
├── vars/                             # Global variables (functions)
│   ├── deployToKubernetes.groovy    # Function callable as: deployToKubernetes()
│   ├── deployToHelm.groovy          # Function callable as: deployToHelm()
│   ├── runSecurityScans.groovy      # Function callable as: runSecurityScans()
│   ├── buildDocker.groovy           # Function callable as: buildDocker()
│   ├── notifySlack.groovy           # Function callable as: notifySlack()
│   └── GradleUtils.groovy           # Class callable as: GradleUtils.build()
│
├── src/                             # Shared classes (internal)
│   └── com/company/
│       ├── PipelineUtils.groovy     # Utility class: PipelineUtils.getVersion()
│       ├── DeploymentManager.groovy # Class: new DeploymentManager()
│       ├── KubernetesHelper.groovy  # Class: KubernetesHelper.validate()
│       └── SecurityManager.groovy   # Class: new SecurityManager()
│
├── resources/                       # Static files
│   ├── templates/                  # Template files
│   │   ├── deployment-template.yaml
│   │   └── service-template.yaml
│   │
│   ├── scripts/                    # Bash scripts
│   │   ├── validate-image.sh
│   │   └── check-cluster.sh
│   │
│   └── configs/                    # Configuration files
│       └── sonar-config.properties
│
└── test/                           # Unit tests (optional)
    ├── groovy/
    │   ├── com/company/PipelineUtilsTest.groovy
    │   └── com/company/DeploymentManagerTest.groovy
    │
    └── resources/
        └── test-data.json
```

### Key Difference: vars/ vs src/

```
vars/ (Global Variables)
├─ Callable directly from Jenkinsfile
├─ @Field available to all
├─ Implicit 'this' binding
├─ Example: deployToKubernetes()
│
└─ Pattern:
   def call(Map config) {
       // executes when function called
   }

src/ (Shared Classes)
├─ Need explicit import
├─ Regular Java/Groovy classes
├─ Better for complex logic
├─ Example: import com.company.DeploymentManager
│
└─ Pattern:
   class DeploymentManager {
       void deploy() {
           // method
       }
   }
```

### Why This Structure?

```
vars/ for Simple Functions:
✅ Direct callable: deployToKubernetes()
✅ Simple parameter passing
✅ Good for straightforward tasks
✅ Keep it simple

src/ for Complex Logic:
✅ Can be tested independently
✅ Object-oriented design
✅ Reusable class patterns
✅ Better for complex operations
```

---

## Creating Shared Functions

### Pattern 1: Simple Function (vars/)

**File: `jenkins/shared-library/vars/buildDocker.groovy`**

```groovy
/*
 * Build Docker Image
 *
 * Global variable function - callable directly from Jenkinsfile
 *
 * Usage:
 *   buildDocker(
 *       imageName: 'springboot-app',
 *       tag: 'v1.0.0',
 *       dockerfile: 'application/docker/Dockerfile'
 *   )
 */

def call(Map config) {
    // Extract parameters with defaults
    def imageName = config.imageName ?: 'app'
    def tag = config.tag ?: 'latest'
    def dockerfile = config.dockerfile ?: 'Dockerfile'
    def context = config.context ?: '.'
    def buildArgs = config.buildArgs ?: ''
    def registry = config.registry ?: ''

    // Build full image name
    def fullImage = registry ? "${registry}/${imageName}:${tag}" : "${imageName}:${tag}"

    echo "====== Building Docker Image ======"
    echo "Image: ${fullImage}"
    echo "Dockerfile: ${dockerfile}"
    echo "Context: ${context}"

    try {
        // Build image
        sh '''
            docker build \
                -f ${dockerfile} \
                -t ${fullImage} \
                ${buildArgs} \
                ${context}
        '''

        echo "✓ Docker build successful"

        // Verify image was created
        sh "docker images | grep ${imageName}"

        // Return image name for downstream use
        return fullImage

    } catch (Exception e) {
        echo "✗ Docker build failed: ${e.message}"
        throw e
    }
}
```

**Usage in Jenkinsfile**:

```groovy
@Library('jenkins-shared-library') _

pipeline {
    agent any

    stages {
        stage('Build Image') {
            steps {
                script {
                    // Simple call with defaults
                    buildDocker(imageName: 'springboot-app')

                    // Or with full parameters
                    def image = buildDocker(
                        imageName: 'springboot-app',
                        tag: 'v1.0.0',
                        dockerfile: 'application/docker/Dockerfile',
                        context: '.',
                        registry: 'gcr.io/my-project'
                    )

                    echo "Built image: ${image}"
                }
            }
        }
    }
}
```

**Key Concepts**:
1. **Map Parameters**: `config.imageName ?: 'app'` (default value)
2. **Echo for Logging**: Console output for debugging
3. **Try-Catch**: Proper error handling
4. **Return Value**: Can return data to caller

### Pattern 2: Function with Helper Methods

**File: `jenkins/shared-library/vars/deployToKubernetes.groovy`**

```groovy
/*
 * Deploy to Kubernetes
 *
 * Supports both Kustomize and Helm deployments
 * Handles namespace creation, image update, rollout monitoring
 */

def call(Map config) {
    def environment = config.environment ?: 'dev'
    def imageName = config.imageName ?: 'springboot-app'
    def imageTag = config.imageTag ?: 'latest'
    def deploymentTool = config.deploymentTool ?: 'kustomize'
    def namespace = config.namespace ?: environment
    def kubeContext = config.kubeContext ?: environment

    echo "====== Deploying to Kubernetes ======"
    echo "Environment: ${environment}"
    echo "Image: ${imageName}:${imageTag}"
    echo "Tool: ${deploymentTool}"
    echo "Namespace: ${namespace}"

    try {
        // Step 1: Validate prerequisites
        validatePrerequisites(kubeContext, namespace)

        // Step 2: Deploy based on tool
        if (deploymentTool == 'helm') {
            deployWithHelm(imageName, imageTag, environment, namespace)
        } else {
            deployWithKustomize(imageName, imageTag, environment, namespace)
        }

        // Step 3: Monitor rollout
        monitorRollout(imageName, namespace)

        // Step 4: Run smoke tests
        runSmokeTests(namespace)

        echo "✓ Deployment successful"

    } catch (Exception e) {
        echo "✗ Deployment failed: ${e.message}"
        throw e
    }
}

// Helper methods (private to this function)

private void validatePrerequisites(String kubeContext, String namespace) {
    echo "Validating prerequisites..."

    sh '''
        # Check kubectl
        kubectl version --client

        # Check context
        kubectl config use-context ${kubeContext}

        # Create namespace if doesn't exist
        kubectl create namespace ${namespace} --dry-run=client -o yaml | kubectl apply -f -
    '''

    echo "✓ Prerequisites validated"
}

private void deployWithHelm(String imageName, String imageTag, String env, String ns) {
    echo "Deploying with Helm..."

    sh '''
        helm upgrade --install ${imageName} \
            kubernetes/helm/springboot-app \
            --namespace ${ns} \
            --set image.repository=${imageName} \
            --set image.tag=${imageTag} \
            --set global.environment=${env} \
            -f kubernetes/helm/springboot-app/values-${env}.yaml \
            --wait \
            --timeout 5m
    '''

    echo "✓ Helm deployment complete"
}

private void deployWithKustomize(String imageName, String imageTag, String env, String ns) {
    echo "Deploying with Kustomize..."

    sh '''
        cd kubernetes/kustomize/overlays/${env}
        kustomize edit set image ${imageName}=${imageName}:${imageTag}
        cd ${WORKSPACE}

        kustomize build kubernetes/kustomize/overlays/${env} | \
            kubectl apply -f - -n ${ns}
    '''

    echo "✓ Kustomize deployment complete"
}

private void monitorRollout(String appName, String namespace) {
    echo "Monitoring rollout..."

    sh '''
        kubectl rollout status deployment/${appName} \
            -n ${namespace} \
            --timeout=5m

        # Display pod status
        kubectl get pods -n ${namespace} -l app=${appName}
    '''

    echo "✓ Rollout successful"
}

private void runSmokeTests(String namespace) {
    echo "Running smoke tests..."

    sh '''
        # Get service endpoint
        SERVICE_IP=$(kubectl get service springboot-app \
            -n ${namespace} \
            -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

        # Test health endpoint
        curl -f http://${SERVICE_IP}:8080/actuator/health || true
    '''

    echo "✓ Smoke tests complete"
}
```

**Key Concepts**:
1. **Private Helper Methods**: `private void methodName()` (helper functions)
2. **Parameter Extraction**: Extract and validate at start
3. **Echo Statements**: Clear logging of progress
4. **Error Handling**: Try-catch with meaningful messages
5. **Modular Steps**: Each step can be tested separately

### Pattern 3: Function Returning Complex Data

**File: `jenkins/shared-library/vars/runSecurityScans.groovy`**

```groovy
/*
 * Run Security Scans
 *
 * Executes multiple security scanning tools
 * Returns detailed report of all findings
 */

def call(Map config) {
    def scanType = config.scanType ?: 'all'
    def failOnCritical = config.failOnCritical ?: true
    def sonarProjectKey = config.sonarProjectKey ?: 'default'

    echo "====== Running Security Scans ======"
    echo "Scan Type: ${scanType}"

    // Map to store results
    Map results = [:]

    try {
        if (scanType in ['all', 'sast']) {
            results.sonarqube = runSonarQube(sonarProjectKey)
        }

        if (scanType in ['all', 'sca']) {
            results.dependencyCheck = runDependencyCheck()
        }

        if (scanType in ['all', 'container']) {
            results.trivy = runTrivy(config.imageName ?: 'app:latest')
        }

        // Evaluate results
        evaluateResults(results, failOnCritical)

        echo "✓ Security scans complete"
        return results  // Return detailed results

    } catch (Exception e) {
        echo "✗ Security scan failed: ${e.message}"
        throw e
    }
}

private Map runSonarQube(String projectKey) {
    echo "Running SonarQube analysis..."

    sh '''
        ./gradlew sonar \
            -Dsonar.projectKey=${projectKey}
    '''

    return [
        status: 'COMPLETE',
        tool: 'SonarQube',
        projectKey: projectKey,
        reportUrl: "http://sonarqube:9000/dashboard?id=${projectKey}"
    ]
}

private Map runDependencyCheck() {
    echo "Running OWASP Dependency-Check..."

    sh '''
        ./gradlew dependencyCheck || true
    '''

    // Parse results
    def criticalCount = sh(
        script: "grep -c '\"Severity\":\"CRITICAL\"' build/reports/dependency-check/dependency-check-report.json || echo '0'",
        returnStdout: true
    ).trim().toInteger()

    return [
        status: 'COMPLETE',
        tool: 'OWASP Dependency-Check',
        criticalCount: criticalCount,
        reportPath: 'build/reports/dependency-check/dependency-check-report.html'
    ]
}

private Map runTrivy(String imageName) {
    echo "Running Trivy container scan..."

    sh '''
        trivy image \
            --severity CRITICAL,HIGH \
            --format json \
            --output trivy-report.json \
            ${imageName}
    '''

    // Parse results
    def criticalCount = sh(
        script: "grep -c '\"Severity\":\"CRITICAL\"' trivy-report.json || echo '0'",
        returnStdout: true
    ).trim().toInteger()

    return [
        status: 'COMPLETE',
        tool: 'Trivy',
        imageName: imageName,
        criticalCount: criticalCount,
        reportPath: 'trivy-report.json'
    ]
}

private void evaluateResults(Map results, boolean failOnCritical) {
    echo "Evaluating scan results..."

    if (!failOnCritical) {
        return  // Don't fail
    }

    // Check for critical vulnerabilities
    def hasCritical = results.findAll { key, value ->
        value.criticalCount > 0
    }

    if (hasCritical) {
        def details = hasCritical.collect { key, value ->
            "${value.tool}: ${value.criticalCount} critical"
        }.join(', ')

        error "Critical vulnerabilities found: ${details}"
    }
}
```

**Usage**:

```groovy
@Library('jenkins-shared-library') _

pipeline {
    stages {
        stage('Security') {
            steps {
                script {
                    def results = runSecurityScans(
                        scanType: 'all',
                        failOnCritical: true,
                        sonarProjectKey: 'my-project',
                        imageName: 'my-app:v1.0.0'
                    )

                    // Use returned results
                    println "SonarQube: ${results.sonarqube.reportUrl}"
                    println "Dependency Check: ${results.dependencyCheck.criticalCount} critical"
                    println "Trivy: ${results.trivy.criticalCount} critical"
                }
            }
        }
    }
}
```

**Key Concepts**:
1. **Return Maps**: Return structured data from functions
2. **Parsing Output**: Extract info from tool outputs
3. **Conditional Logic**: Different behavior based on results
4. **Error Evaluation**: Check results and fail appropriately

---

## Using Shared Library in Pipelines

### Method 1: Global Library (Implicit Load)

**Jenkins Configuration** (one time):
```
Manage Jenkins → Configure System
→ Global Pipeline Libraries
  ├─ Name: jenkins-shared-library
  ├─ Default version: main (or specific version)
  ├─ Load implicitly: ☑ (auto-load for all pipelines)
  └─ Repository URL: https://github.com/company/jenkins-shared-library.git
```

**Jenkinsfile** (automatically loaded):
```groovy
@Library('jenkins-shared-library') _

pipeline {
    stages {
        stage('Deploy') {
            steps {
                // Functions available immediately
                deployToKubernetes(...)
            }
        }
    }
}
```

### Method 2: Explicit Version Specification

```groovy
// Use specific Git tag
@Library('jenkins-shared-library@v1.5.0') _

// Or specific branch
@Library('jenkins-shared-library@develop') _

// Or specific commit
@Library('jenkins-shared-library@abc123def') _

pipeline {
    // ... pipeline code
}
```

### Method 3: Multiple Libraries

```groovy
// Load multiple shared libraries
@Library(['jenkins-shared-library', 'common-library']) _

pipeline {
    stages {
        stage('Build') {
            steps {
                script {
                    // Functions from jenkins-shared-library
                    buildDocker(...)

                    // Functions from common-library
                    notifyTeam(...)
                }
            }
        }
    }
}
```

### Method 4: Selective Loading

```groovy
import com.company.PipelineUtils
import com.company.DeploymentManager

pipeline {
    stages {
        stage('Deploy') {
            steps {
                script {
                    def version = PipelineUtils.getVersion()
                    def manager = new DeploymentManager()
                    manager.deploy(version)
                }
            }
        }
    }
}
```

---

## Advanced Patterns

### Pattern 1: Shared Library Using src/ Classes

**File: `jenkins/shared-library/src/com/company/DeploymentManager.groovy`**

```groovy
package com.company

class DeploymentManager {
    // Class properties
    private String cluster
    private String namespace
    private String imageName
    private String imageTag
    private String environment

    // Constructor
    DeploymentManager(Map config) {
        this.cluster = config.cluster ?: 'default'
        this.namespace = config.namespace ?: 'default'
        this.imageName = config.imageName ?: 'app'
        this.imageTag = config.imageTag ?: 'latest'
        this.environment = config.environment ?: 'dev'
    }

    // Main public method
    void deploy() {
        try {
            validateCluster()
            ensureNamespace()
            updateImage()
            applyManifests()
            monitorRollout()
        } catch (Exception e) {
            rollback()
            throw e
        }
    }

    // Private methods
    private void validateCluster() {
        println "Validating cluster: ${cluster}"
        // Validation logic
    }

    private void ensureNamespace() {
        println "Ensuring namespace: ${namespace}"
        // Namespace creation logic
    }

    private void updateImage() {
        println "Updating image to: ${imageName}:${imageTag}"
        // Image update logic
    }

    private void applyManifests() {
        println "Applying manifests"
        // Manifest application logic
    }

    private void monitorRollout() {
        println "Monitoring rollout"
        // Rollout monitoring logic
    }

    private void rollback() {
        println "Rolling back deployment"
        // Rollback logic
    }

    // Getter methods
    String getStatus() {
        return "Deployed to ${environment}"
    }
}
```

**Global Variable Using Class**:

**File: `jenkins/shared-library/vars/deployKubernetes.groovy`**

```groovy
def call(Map config) {
    // Use shared class
    def manager = new com.company.DeploymentManager(config)
    manager.deploy()

    println "Deployment Status: ${manager.getStatus()}"
}
```

**Usage**:

```groovy
@Library('jenkins-shared-library') _

pipeline {
    stages {
        stage('Deploy') {
            steps {
                script {
                    deployKubernetes(
                        cluster: 'prod-cluster',
                        namespace: 'production',
                        imageName: 'springboot-app',
                        imageTag: 'v1.0.0',
                        environment: 'production'
                    )
                }
            }
        }
    }
}
```

### Pattern 2: Conditional Execution

```groovy
def call(Map config) {
    def environment = config.environment ?: 'dev'
    def skipIfPr = config.skipIfPr ?: false

    // Skip if this is a pull request
    if (skipIfPr && env.CHANGE_ID) {
        echo "Skipping deployment for PR"
        return
    }

    // Only deploy to prod on main branch
    if (environment == 'production' && env.BRANCH_NAME != 'main') {
        error "Production deployments only allowed from main branch"
    }

    // Continue with deployment
    echo "Deploying to ${environment}"
}
```

### Pattern 3: Timeout and Retry

```groovy
def call(Map config) {
    def maxRetries = config.maxRetries ?: 3
    def retryCount = 0

    while (retryCount < maxRetries) {
        try {
            timeout(time: 5, unit: 'MINUTES') {
                sh 'deploy-command'
            }
            echo "✓ Deployment succeeded"
            return
        } catch (Exception e) {
            retryCount++
            if (retryCount < maxRetries) {
                echo "⚠ Attempt ${retryCount} failed, retrying..."
                sleep(10)  // Wait 10 seconds before retry
            } else {
                error "Deployment failed after ${maxRetries} attempts"
            }
        }
    }
}
```

---

## Testing Shared Library

### Unit Testing Shared Functions

**File: `jenkins/shared-library/test/groovy/com/company/DeploymentManagerTest.groovy`**

```groovy
package com.company

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import static org.junit.jupiter.api.Assertions.*

class DeploymentManagerTest {

    private DeploymentManager manager

    @BeforeEach
    void setup() {
        manager = new DeploymentManager(
            cluster: 'test-cluster',
            namespace: 'test-ns',
            imageName: 'test-app',
            imageTag: 'v1.0.0',
            environment: 'test'
        )
    }

    @Test
    void testGetStatus() {
        // Arrange
        def expected = "Deployed to test"

        // Act
        def result = manager.getStatus()

        // Assert
        assertEquals(expected, result)
    }

    @Test
    void testConstructorDefaults() {
        // Arrange
        def config = [:]

        // Act
        def mgr = new DeploymentManager(config)

        // Assert
        assertEquals('default', mgr.getCluster())  // Assuming getter exists
    }
}
```

### Integration Testing

```groovy
// Test shared library function in actual pipeline
@Test
void testDeploymentFunction() {
    // Use Jenkins Test Harness
    WorkflowJob job = createPipelineJob()

    job.setDefinition(cps('''
        @Library('jenkins-shared-library') _

        pipeline {
            agent any
            stages {
                stage('Deploy') {
                    steps {
                        script {
                            deployToKubernetes(
                                environment: 'test',
                                imageName: 'test-app'
                            )
                        }
                    }
                }
            }
        }
    '''))

    WorkflowRun build = job.scheduleBuild2(0).get()
    assert build.getResult() == Result.SUCCESS
}
```

---

## Best Practices

### 1. **Clear Naming Conventions**

```groovy
// ✅ GOOD: Clear what it does
vars/deployToKubernetes.groovy
vars/runSecurityScans.groovy
vars/notifySlack.groovy
vars/buildDocker.groovy

// ❌ BAD: Unclear
vars/deploy.groovy      // Deploy to what?
vars/scan.groovy        # Scan what?
vars/notify.groovy      # Notify where?
vars/build.groovy       # Build what?
```

### 2. **Comprehensive Documentation**

```groovy
/*
 * Deploy to Kubernetes
 *
 * Deploys application to Kubernetes cluster using Kustomize or Helm
 * Handles namespace creation, image update, rollout monitoring
 *
 * Parameters:
 *   environment (String): Target environment (dev, staging, production)
 *   imageName (String): Docker image name (default: springboot-app)
 *   imageTag (String): Docker image tag (default: latest)
 *   deploymentTool (String): Tool to use (kustomize|helm, default: kustomize)
 *   namespace (String): Kubernetes namespace (default: same as environment)
 *   kubeContext (String): Kubernetes context (default: same as environment)
 *
 * Returns:
 *   Status message
 *
 * Example:
 *   deployToKubernetes(
 *       environment: 'production',
 *       imageName: 'springboot-app',
 *       imageTag: 'v1.0.0',
 *       deploymentTool: 'helm'
 *   )
 *
 * Throws:
 *   Exception if deployment fails
 */
def call(Map config) {
    // ...
}
```

### 3. **Error Handling**

```groovy
def call(Map config) {
    try {
        // Main logic
        sh 'some-command'
    } catch (Exception e) {
        // Log details
        echo "❌ Deployment failed: ${e.message}"
        echo "Stack trace: ${e.toString()}"

        // Attempt cleanup
        try {
            cleanup()
        } catch (Exception cleanupError) {
            echo "⚠ Cleanup also failed: ${cleanupError.message}"
        }

        // Re-throw for pipeline to catch
        throw e
    }
}
```

### 4. **Parameter Validation**

```groovy
def call(Map config) {
    // Validate required parameters
    if (!config.environment) {
        error "Parameter 'environment' is required"
    }

    if (!config.imageName) {
        error "Parameter 'imageName' is required"
    }

    // Validate parameter values
    def validEnvironments = ['dev', 'staging', 'production']
    if (!validEnvironments.contains(config.environment)) {
        error "environment must be one of: ${validEnvironments}"
    }

    // Continue with validated parameters
}
```

### 5. **Logging and Observability**

```groovy
def call(Map config) {
    echo "====== Starting Deployment ======"
    echo "Timestamp: ${new Date()}"
    echo "Jenkins Build: ${env.BUILD_NUMBER}"
    echo "Git Commit: ${env.GIT_COMMIT}"
    echo "Parameters: environment=${config.environment}, image=${config.imageName}:${config.imageTag}"

    try {
        sh 'deployment-command'
        echo "✓ Deployment completed successfully"
    } catch (Exception e) {
        echo "✗ Deployment failed"
        echo "Error: ${e.message}"
        throw e
    } finally {
        echo "====== Deployment Summary ======"
        echo "Status: ${currentBuild.result}"
        echo "Duration: ${currentBuild.durationString}"
    }
}
```

### 6. **Version Control**

```
Jenkins Shared Library Repository
├── main (stable, production-ready)
├── develop (development branch)
├── feature/* (feature branches)
└── tags (releases: v1.0.0, v1.1.0, etc)

Usage:
@Library('jenkins-shared-library@main') _       // Latest stable
@Library('jenkins-shared-library@develop') _    // Latest development
@Library('jenkins-shared-library@v1.0.0') _     // Specific release
```

---

## Real Examples from Platform

### Example 1: deployToKubernetes from Platform

**File: `jenkins/shared-library/vars/deployToKubernetes.groovy`**

```groovy
def call(Map config) {
    def environment = config.environment ?: 'dev'
    def imageName = config.imageName ?: 'springboot-app'
    def imageTag = config.imageTag ?: 'latest'
    def deploymentTool = config.deploymentTool ?: 'kustomize'
    def namespace = config.namespace ?: environment
    def timeout = config.timeout ?: '5m'
    def dryRun = config.dryRun ?: false

    echo "====== Deploying to Kubernetes ======"
    echo "Environment: ${environment}"
    echo "Image: ${imageName}:${imageTag}"
    echo "Tool: ${deploymentTool}"
    echo "Namespace: ${namespace}"

    try {
        // Install required tools
        sh '''
            apk add --no-cache kubectl helm kustomize curl
            kubectl version --client
        '''

        // Create namespace if it doesn't exist
        sh '''
            kubectl create namespace ${namespace} --dry-run=client -o yaml | kubectl apply -f -
        '''

        if (deploymentTool == 'helm') {
            deployWithHelm(imageName, imageTag, environment, namespace, timeout, dryRun)
        } else {
            deployWithKustomize(imageName, imageTag, environment, namespace, timeout, dryRun)
        }

        echo "✓ Deployment completed successfully"
    } catch (Exception e) {
        echo "✗ Deployment failed: ${e.message}"
        throw e
    }
}

private void deployWithHelm(String imageName, String imageTag, String env, String ns, String timeout, Boolean dryRun) {
    echo "Deploying with Helm..."

    def dryRunFlag = dryRun ? "--dry-run --debug" : "--wait --timeout ${timeout}"

    sh '''
        helm upgrade --install ${imageName} \
            kubernetes/helm/springboot-app \
            --namespace ${ns} \
            --set image.repository=${imageName} \
            --set image.tag=${imageTag} \
            --set global.environment=${env} \
            -f kubernetes/helm/springboot-app/values-${env}.yaml \
            ${dryRunFlag}
    '''

    if (!dryRun) {
        sh '''
            kubectl rollout status deployment/${imageName} \
                -n ${ns} \
                --timeout=${timeout}
        '''
    }
}

private void deployWithKustomize(String imageName, String imageTag, String env, String ns, String timeout, Boolean dryRun) {
    echo "Deploying with Kustomize..."

    def dryRunFlag = dryRun ? "--dry-run=client" : ""

    sh '''
        cd kubernetes/kustomize/overlays/${env}
        kustomize edit set image ${imageName}=${imageName}:${imageTag}
        cd ${WORKSPACE}

        kustomize build kubernetes/kustomize/overlays/${env} | \
            kubectl apply -f - \
            -n ${ns} \
            ${dryRunFlag}
    '''

    if (!dryRun) {
        sh '''
            kubectl rollout status deployment/${imageName} \
                -n ${ns} \
                --timeout=${timeout}
        '''
    }
}
```

### Example 2: runSecurityScans from Platform

**File: `jenkins/shared-library/vars/runSecurityScans.groovy`**

```groovy
def call(Map config) {
    def scanType = config.scanType ?: 'all'
    def sonarProjectKey = config.sonarProjectKey ?: 'default'
    def sonarHostUrl = config.sonarHostUrl ?: 'http://sonarqube:9000'
    def failOnCritical = config.failOnCritical ?: false
    def imageName = config.imageName ?: 'springboot-app:latest'

    echo "====== Running Security Scans ======"
    echo "Scan Type: ${scanType}"

    try {
        if (scanType in ['all', 'sast']) {
            runSonarQubeAnalysis(sonarProjectKey, sonarHostUrl)
        }

        if (scanType in ['all', 'sca']) {
            runDependencyCheck()
        }

        if (scanType in ['all', 'container']) {
            runContainerScan(imageName, failOnCritical)
        }

        echo "✓ Security scans completed"
    } catch (Exception e) {
        echo "✗ Security scan failed: ${e.message}"
        if (failOnCritical) {
            throw e
        }
    }
}

private void runSonarQubeAnalysis(String projectKey, String hostUrl) {
    echo "Running SonarQube analysis..."
    sh '''
        ./gradlew sonar \
            -Dsonar.host.url=${hostUrl} \
            -Dsonar.projectKey=${projectKey}
    '''
}

private void runDependencyCheck() {
    echo "Running OWASP Dependency-Check..."
    sh '''
        ./gradlew dependencyCheck || true
    '''
}

private void runContainerScan(String imageName, Boolean failOnCritical) {
    echo "Running Trivy container scan..."
    sh '''
        trivy image \
            --severity CRITICAL,HIGH \
            --format json \
            --output trivy-report.json \
            ${imageName}
    '''

    if (failOnCritical) {
        def criticalCount = sh(
            script: "grep -c '\"Severity\":\"CRITICAL\"' trivy-report.json || echo '0'",
            returnStdout: true
        ).trim().toInteger()

        if (criticalCount > 0) {
            error "Found ${criticalCount} CRITICAL vulnerabilities in container"
        }
    }
}
```

---

## Summary

### Key Concepts

✅ **What**: Reusable Groovy functions/classes shared across pipelines
✅ **Why**: DRY principle, consistency, maintainability, testability
✅ **Where**: `vars/` for functions, `src/` for classes, `resources/` for files
✅ **How**: `@Library('name')` in Jenkinsfile, configure in Jenkins
✅ **Benefits**: Single source of truth, easier maintenance, version control

### Shared Library Checklist

- [x] Create Git repository for library
- [x] Organize in `vars/` and `src/` directories
- [x] Write functions with clear documentation
- [x] Implement error handling and validation
- [x] Include logging and observability
- [x] Version in Git with tags
- [x] Configure in Jenkins (Global Pipeline Libraries)
- [x] Test functions before using in production
- [x] Document usage with examples
- [x] Keep functions focused and single-responsibility

### Development Workflow

```
1. Create feature branch
   git checkout -b feature/new-function

2. Write function in vars/
   vars/myNewFunction.groovy

3. Test locally
   Run in test pipeline

4. Code review
   Pull request review

5. Merge to main
   git merge feature/new-function

6. Tag release
   git tag v1.1.0
   git push --tags

7. Use in pipelines
   @Library('jenkins-shared-library@v1.1.0') _
```

---

This shared library pattern is essential for scaling Jenkins usage across large teams. It embodies DevOps best practices: DRY, testability, consistency, and maintainability.
