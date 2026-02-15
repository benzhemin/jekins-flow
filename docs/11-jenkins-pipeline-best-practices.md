# Jenkins Pipeline Best Practices & Advanced Patterns
## From Declarative to Production: Comprehensive Pipeline Mastery

---

## Table of Contents

1. [Declarative vs Scripted Pipelines](#declarative-vs-scripted-pipelines)
2. [Pipeline Parameters & Conditional Logic](#pipeline-parameters--conditional-logic)
3. [Multi-Branch Pipelines](#multi-branch-pipelines)
4. [Advanced Groovy for Jenkins](#advanced-groovy-for-jenkins)
5. [Testing Jenkins Pipelines](#testing-jenkins-pipelines)
6. [Artifact Management](#artifact-management)
7. [Configuration as Code](#configuration-as-code)
8. [GitFlow Integration](#gitflow-integration)
9. [Release Management](#release-management)
10. [Performance Optimization](#performance-optimization)
11. [Debugging & Troubleshooting](#debugging--troubleshooting)
12. [Common Anti-Patterns](#common-anti-patterns)
13. [Real-World Patterns](#real-world-patterns)

---

# 1. Declarative vs Scripted Pipelines

## What You Need to Know FIRST

When you first encounter Jenkins pipelines, you'll see TWO completely different syntaxes. Understanding which to use and WHY is critical.

### **The Problem They Solve**

Before Jenkins Pipeline, every job was manually configured through the UI. Changes were hard to track, teams had inconsistent processes, and disaster recovery was painful.

**Pipeline** = Code that defines your build process, stored in version control

But there are two ways to write that code:

---

## Declarative Pipelines - The Modern Standard

### What is Declarative?

**Declarative Pipeline** is a newer, more structured way to write Jenkins pipelines. Think of it as "form-filling" - you declare what you want in a specific format.

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Building...'
            }
        }
    }
}
```

### Why Declarative? Real-World Benefits

Let's see why teams prefer Declarative:

#### Benefit 1: Better Error Handling (Built-In)

**Declarative** automatically handles failures:

```groovy
pipeline {
    agent any

    stages {
        stage('Test') {
            steps {
                sh 'gradle test'
            }
            // If this fails, Jenkins AUTOMATICALLY stops
            // This is "fail fast" behavior - no extra code needed
        }
    }

    post {
        always {
            // Runs regardless: success OR failure
            junit 'build/test-results/**/*.xml'
        }
        success {
            echo 'Build passed!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}
```

With declarative, you don't worry about exceptions or error checking - it's built in.

#### Benefit 2: Built-In Timeout Protection

Stop builds that hang:

```groovy
pipeline {
    agent any

    options {
        timeout(time: 1, unit: 'HOURS')  // Kill build if it takes > 1 hour
        timestamps()                      // Add timestamps to console
        buildDiscarder(logRotator(       // Keep only last 30 builds
            numToKeepStr: '30'
        ))
    }

    stages {
        stage('Long Running Task') {
            steps {
                sh 'some-command-that-might-hang'
            }
        }
    }
}
```

#### Benefit 3: Parameters Are First-Class

Define what users can input:

```groovy
pipeline {
    agent any

    parameters {
        string(
            name: 'ENVIRONMENT',
            defaultValue: 'dev',
            description: 'Deployment environment'
        )
        choice(
            name: 'LOG_LEVEL',
            choices: ['DEBUG', 'INFO', 'WARN', 'ERROR'],
            description: 'Application log level'
        )
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip test execution?'
        )
    }

    stages {
        stage('Build') {
            steps {
                echo "Building for: ${params.ENVIRONMENT}"
                echo "Log level: ${params.LOG_LEVEL}"
                echo "Tests skipped: ${params.SKIP_TESTS}"
            }
        }
    }
}
```

When someone clicks "Build with Parameters", they see:
```
ENVIRONMENT: [dev dropdown]
LOG_LEVEL: [DEBUG/INFO/WARN/ERROR choices]
SKIP_TESTS: [checkbox]
```

### Declarative Limitations (When to Consider Alternatives)

Declarative can't easily do:
- Complex loops (only `for` loop via Groovy)
- Deep object manipulation
- Dynamic stage generation
- Complex variable scoping

---

## Scripted Pipelines - The Flexible Alternative

### What is Scripted?

**Scripted Pipeline** is just Groovy code. Much more flexible, but you need to handle more manually.

```groovy
node {  // Instead of 'pipeline', use 'node'
    try {
        stage('Build') {
            echo 'Building...'
            sh 'gradle build'
        }
    } catch (Exception e) {
        echo "Build failed: ${e.message}"
        currentBuild.result = 'FAILURE'
    }
}
```

### Real Comparison: Declarative vs Scripted

#### Scenario: Build Multiple Services

**Declarative Approach** (simpler for most cases):

```groovy
pipeline {
    agent any

    stages {
        stage('Build All Services') {
            parallel {
                stage('Service A') {
                    steps {
                        sh 'cd serviceA && gradle build'
                    }
                }
                stage('Service B') {
                    steps {
                        sh 'cd serviceB && gradle build'
                    }
                }
                stage('Service C') {
                    steps {
                        sh 'cd serviceC && gradle build'
                    }
                }
            }
        }
    }
}
```

**Scripted Approach** (flexible but more code):

```groovy
node {
    def services = ['serviceA', 'serviceB', 'serviceC']
    def parallelBuilds = [:]

    for (service in services) {
        parallelBuilds[service] = {
            stage("Build ${service}") {
                sh "cd ${service} && gradle build"
            }
        }
    }

    parallel(parallelBuilds)
}
```

**Why Scripted here?** If you had 100 services, scripted avoids copy-paste.

### When Each Makes Sense

| Aspect | Declarative | Scripted |
|--------|-------------|----------|
| **Readability** | Excellent | Good (for experts) |
| **Learning Curve** | Easy | Steep |
| **Error Handling** | Automatic | Manual |
| **Dynamic Stages** | Hard | Easy |
| **Parameter Handling** | Built-in | Manual |
| **Timeouts/Limits** | Built-in | Manual |
| **Loops** | Limited | Full Groovy |
| **Team Adoption** | Easier | Harder |

### The Hybrid Approach (Most Common in Practice)

Real-world teams use **mostly Declarative with Scripted blocks** for complex logic:

```groovy
pipeline {
    agent any

    stages {
        stage('Prepare') {
            steps {
                script {
                    // Complex logic in script block
                    def services = sh(
                        script: 'ls -1 services/',
                        returnStdout: true
                    ).trim().split('\n')

                    env.SERVICES = services.join(',')
                }
            }
        }

        stage('Build') {
            parallel {
                // Dynamic generated stages in Declarative
                // Based on script block output
            }
        }
    }
}
```

---

## Side-by-Side Declarative Feature Showcase

### Error Handling

**Declarative (automatic)**:
```groovy
pipeline {
    agent any

    post {
        failure {
            // This ALWAYS runs on failure
            emailext(
                to: 'team@company.com',
                subject: 'Build Failed',
                body: 'Check logs'
            )
        }
        unstable {
            // Runs if tests fail but build succeeds
            slackSend(color: 'warning', message: 'Tests failing')
        }
    }

    stages {
        stage('Test') {
            steps {
                sh 'gradle test'
            }
        }
    }
}
```

**Scripted (manual)**:
```groovy
node {
    try {
        stage('Test') {
            sh 'gradle test'
        }
    } catch (Exception e) {
        // You must write this yourself
        currentBuild.result = 'FAILURE'
        emailext(
            to: 'team@company.com',
            subject: 'Build Failed',
            body: "Error: ${e.message}"
        )
    } finally {
        // Clean up code here
    }
}
```

### Options and Conditions

**Declarative has built-in options**:

```groovy
pipeline {
    agent any

    options {
        // Automatic cleanup
        buildDiscarder(logRotator(numToKeepStr: '30'))

        // Timeout protection
        timeout(time: 30, unit: 'MINUTES')

        // Timestamp every log line
        timestamps()

        // Don't run on weekends
        skipDefaultCheckout()

        // Disable concurrent builds
        disableConcurrentBuilds()
    }

    stages {
        stage('Build') {
            when {
                // Conditional execution (Declarative feature!)
                branch 'main'
            }
            steps {
                echo 'Building main branch'
            }
        }

        stage('Deploy') {
            when {
                allOf {
                    branch 'main'
                    expression { currentBuild.result == 'SUCCESS' }
                }
            }
            steps {
                echo 'Deploying to production'
            }
        }
    }
}
```

---

# 2. Pipeline Parameters & Conditional Logic

## What Parameters Are (And Why They Matter)

Parameters let you run the same pipeline with different inputs **without editing code**.

### Problem It Solves

**Without Parameters**:
```groovy
// Bad: Need to edit code to change deployment target
pipeline {
    agent any
    stages {
        stage('Deploy') {
            steps {
                sh 'kubectl apply -k kubernetes/kustomize/overlays/staging'
                // Need to manually change to 'production'
            }
        }
    }
}
```

**With Parameters**:
```groovy
// Good: User selects at build time
pipeline {
    agent any

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'production'],
            description: 'Where to deploy?'
        )
    }

    stages {
        stage('Deploy') {
            steps {
                sh "kubectl apply -k kubernetes/kustomize/overlays/${params.ENVIRONMENT}"
            }
        }
    }
}
```

Now someone can click "Build with Parameters" and choose the environment.

---

## Parameter Types: Complete Reference

### String Parameter

For text input:

```groovy
parameters {
    string(
        name: 'APP_VERSION',
        defaultValue: '1.0.0',
        description: 'What version should we build?'
    )
}

stages {
    stage('Build') {
        steps {
            echo "Building version: ${params.APP_VERSION}"
            sh "gradle build -Pversion=${params.APP_VERSION}"
        }
    }
}
```

**Use when**: User needs to enter arbitrary text (version, URL, name)

### Choice Parameter

For selecting from fixed options:

```groovy
parameters {
    choice(
        name: 'ENVIRONMENT',
        choices: ['dev', 'staging', 'production'],
        description: 'Deployment target'
    )

    choice(
        name: 'LOG_LEVEL',
        choices: ['DEBUG', 'INFO', 'WARN', 'ERROR'],
        description: 'Application log level'
    )
}

stages {
    stage('Deploy') {
        steps {
            echo "Deploying to: ${params.ENVIRONMENT}"
            echo "Log level: ${params.LOG_LEVEL}"
        }
    }
}
```

**Use when**: You want to restrict choices to prevent mistakes

### Boolean Parameter

For yes/no decisions:

```groovy
parameters {
    booleanParam(
        name: 'RUN_TESTS',
        defaultValue: true,
        description: 'Run automated tests?'
    )

    booleanParam(
        name: 'DEPLOY_NOW',
        defaultValue: false,
        description: 'Deploy immediately?'
    )
}

stages {
    stage('Test') {
        when {
            expression { params.RUN_TESTS == true }
        }
        steps {
            sh 'gradle test'
        }
    }

    stage('Deploy') {
        when {
            expression { params.DEPLOY_NOW == true }
        }
        steps {
            sh 'kubectl apply -f deployment.yaml'
        }
    }
}
```

**Use when**: User needs to make yes/no decisions

### Credentials Parameter

For secrets:

```groovy
parameters {
    credentials(
        name: 'REGISTRY_CREDENTIALS',
        description: 'Docker registry credentials',
        credentialsType: 'com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl',
        required: true
    )
}

stages {
    stage('Push Docker Image') {
        steps {
            withCredentials([
                usernamePassword(
                    credentialsId: params.REGISTRY_CREDENTIALS,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )
            ]) {
                sh 'docker login -u $DOCKER_USER -p $DOCKER_PASS'
                sh 'docker push myapp:latest'
            }
        }
    }
}
```

**Use when**: User needs to select which credentials to use

### Multi-Select Parameter

For selecting multiple options:

```groovy
parameters {
    extendedChoice(
        name: 'SERVICES_TO_BUILD',
        type: 'PT_CHECKBOX',
        value: 'serviceA,serviceB,serviceC,serviceD',
        description: 'Which services should we build?'
    )
}

stages {
    stage('Build Selected Services') {
        steps {
            script {
                def services = params.SERVICES_TO_BUILD.split(',')
                for (service in services) {
                    echo "Building: ${service}"
                    sh "cd ${service} && gradle build"
                }
            }
        }
    }
}
```

**Use when**: User needs to select multiple items

---

## Conditional Execution (The `when` Directive)

Parameters alone aren't powerful - they're powerful when combined with conditions.

### Simple Condition: Branch

```groovy
pipeline {
    agent any

    stages {
        stage('Deploy to Production') {
            when {
                branch 'main'  // Only run on main branch
            }
            steps {
                echo 'Deploying production build'
            }
        }
    }
}
```

### Multiple Conditions: All Must Match

```groovy
pipeline {
    agent any

    parameters {
        booleanParam(name: 'DEPLOY', defaultValue: false)
    }

    stages {
        stage('Deploy') {
            when {
                allOf {
                    branch 'main'                              // AND
                    expression { params.DEPLOY == true }       // AND
                    expression { currentBuild.result != 'FAILURE' }  // AND
                }
            }
            steps {
                echo 'Production deployment approved'
            }
        }
    }
}
```

### Multiple Conditions: Any Can Match

```groovy
pipeline {
    agent any

    stages {
        stage('Notify') {
            when {
                anyOf {
                    branch 'main'                              // OR
                    branch 'release/*'                         // OR
                    branch 'hotfix/*'                          // OR
                }
            }
            steps {
                echo 'Sending production notification'
            }
        }
    }
}
```

### Complex Conditions: Expression Blocks

```groovy
pipeline {
    agent any

    parameters {
        string(name: 'VERSION', defaultValue: '1.0.0')
    }

    stages {
        stage('Semantic Versioning Check') {
            when {
                expression {
                    // Complex logic in expression block
                    def version = params.VERSION

                    // Must match semantic versioning (1.2.3)
                    version.matches(/^\d+\.\d+\.\d+$/)
                }
            }
            steps {
                echo "Version ${params.VERSION} is valid"
            }
        }

        stage('Production Only') {
            when {
                expression {
                    def isMainBranch = env.BRANCH_NAME == 'main'
                    def isTagged = env.TAG_NAME != null
                    def noSkipCI = !env.COMMIT_MESSAGE.contains('[skip-ci]')

                    isMainBranch && isTagged && noSkipCI
                }
            }
            steps {
                echo 'All conditions met for production'
            }
        }
    }
}
```

### Not/Negation Conditions

```groovy
pipeline {
    agent any

    stages {
        stage('Development') {
            when {
                not {
                    branch 'main'  // Run on everything EXCEPT main
                }
            }
            steps {
                echo 'Dev environment'
            }
        }
    }
}
```

---

## Real-World Parameterized Pipeline: Complete Example

Here's a production-grade parameterized pipeline:

```groovy
pipeline {
    agent any

    parameters {
        // What to build
        choice(
            name: 'BRANCH',
            choices: ['main', 'develop', 'staging'],
            description: 'Which branch to build?'
        )

        // Where to deploy
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'production'],
            description: 'Deployment environment'
        )

        // How to deploy
        booleanParam(
            name: 'CANARY_DEPLOYMENT',
            defaultValue: false,
            description: 'Use canary deployment for production?'
        )

        // Optional configuration
        string(
            name: 'REPLICAS',
            defaultValue: '3',
            description: 'Number of replicas'
        )

        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run automated tests?'
        )

        booleanParam(
            name: 'RUN_SECURITY_SCAN',
            defaultValue: true,
            description: 'Run security scans?'
        )
    }

    options {
        timeout(time: 2, unit: 'HOURS')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        APP_NAME = 'my-spring-app'
    }

    stages {
        stage('Validate Parameters') {
            steps {
                script {
                    echo "========== Build Configuration =========="
                    echo "Branch: ${params.BRANCH}"
                    echo "Environment: ${params.ENVIRONMENT}"
                    echo "Canary: ${params.CANARY_DEPLOYMENT}"
                    echo "Replicas: ${params.REPLICAS}"
                    echo "Run Tests: ${params.RUN_TESTS}"
                    echo "Run Security: ${params.RUN_SECURITY_SCAN}"
                    echo "========================================"

                    // Validate replicas is a number
                    if (!params.REPLICAS.matches(/^\d+$/)) {
                        error("REPLICAS must be a number, got: ${params.REPLICAS}")
                    }

                    // Cannot run canary on dev
                    if (params.ENVIRONMENT == 'dev' && params.CANARY_DEPLOYMENT) {
                        error("Canary deployment not allowed on dev environment")
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    checkout(
                        $class: 'GitSCM',
                        branches: [[name: "origin/${params.BRANCH}"]],
                        userRemoteConfigs: [[url: 'https://github.com/company/app.git']]
                    )

                    env.GIT_COMMIT = sh(
                        script: 'git rev-parse HEAD',
                        returnStdout: true
                    ).trim()
                }
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }

        stage('Test') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                sh './gradlew test'
                junit 'build/test-results/**/*.xml'
            }
        }

        stage('Security Scan') {
            when {
                expression { params.RUN_SECURITY_SCAN == true }
            }
            parallel {
                stage('SonarQube') {
                    steps {
                        sh './gradlew sonarqube'
                    }
                }
                stage('Dependency Check') {
                    steps {
                        sh './gradlew dependencyCheck'
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    env.IMAGE_TAG = "${BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
                    sh """
                        docker build -t ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG} .
                        docker tag ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG} ${DOCKER_REGISTRY}/${APP_NAME}:latest
                    """
                }
            }
        }

        stage('Deploy') {
            when {
                expression { params.ENVIRONMENT != null }
            }
            steps {
                script {
                    if (params.CANARY_DEPLOYMENT && params.ENVIRONMENT == 'production') {
                        // Canary: start with 10% traffic
                        sh """
                            kubectl set image deployment/${APP_NAME}-canary \
                                ${APP_NAME}=${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG} \
                                -n production
                            sleep 60
                            # Monitor metrics...
                            # Gradually increase traffic...
                        """
                    } else {
                        // Standard deployment
                        sh """
                            kubectl apply -k kubernetes/kustomize/overlays/${params.ENVIRONMENT}
                            kubectl set image deployment/${APP_NAME} \
                                ${APP_NAME}=${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG} \
                                -n ${params.ENVIRONMENT}
                            kubectl rollout status deployment/${APP_NAME} -n ${params.ENVIRONMENT} --timeout=5m
                        """
                    }
                }
            }
        }

        stage('Smoke Tests') {
            steps {
                script {
                    sh """
                        # Wait for service
                        kubectl rollout status deployment/${APP_NAME} -n ${params.ENVIRONMENT}

                        # Get service IP
                        SERVICE_IP=\$(kubectl get svc ${APP_NAME} -n ${params.ENVIRONMENT} -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

                        # Basic health check
                        curl -f http://\$SERVICE_IP:8080/actuator/health || exit 1
                    """
                }
            }
        }
    }

    post {
        success {
            script {
                def msg = """
                Build Successful!

                Environment: ${params.ENVIRONMENT}
                Branch: ${params.BRANCH}
                Image: ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG}
                Commit: ${env.GIT_COMMIT}
                """

                slackSend(
                    color: 'good',
                    message: msg,
                    channel: '#deployments'
                )
            }
        }

        failure {
            script {
                def msg = """
                Build Failed!

                Check: ${BUILD_URL}console
                """

                slackSend(
                    color: 'danger',
                    message: msg,
                    channel: '#alerts'
                )
            }
        }
    }
}
```

**Key learning points**:
- Parameters defined upfront
- Validation in first stage
- Conditions control execution of stages
- Real-world use: branching strategy, environment selection, feature flags

---

# 3. Multi-Branch Pipelines

## What Are Multi-Branch Pipelines?

### The Problem They Solve

**Without Multi-Branch**: You need separate Jenkins jobs for each branch

```
┌─ Jenkins Server
│
├─ Job: "build-main"          ← Manually configured for main
├─ Job: "build-develop"       ← Manually configured for develop
├─ Job: "build-feature-x"     ← Need to create for each feature
└─ Job: "build-feature-y"     ← Duplicate configurations
```

**With Multi-Branch**: One Jenkins job watches your repository

```
┌─ Jenkins Server
│
└─ Job: "my-app"
    │
    ├─ Discovers main branch       → Auto-creates job
    ├─ Discovers develop branch    → Auto-creates job
    ├─ Discovers PR branches       → Auto-creates job
    └─ Auto-deletes old branches   → Keeps things clean
```

### How Multi-Branch Works (Under the Hood)

```
1. Jenkins watches your Git repository
2. Finds all branches and pull requests
3. For each branch, looks for Jenkinsfile
4. Runs pipeline automatically
5. Reports status back to GitHub (PR checks)
```

---

## Setting Up Multi-Branch Pipelines

### Step 1: Create the Job

In Jenkins, create a new "Multibranch Pipeline" job:

```
1. Click "New Item"
2. Enter name: "my-app-pipeline"
3. Select "Multibranch Pipeline"
4. Click "Create"
```

### Step 2: Configure Branch Source

Configure where Jenkins should watch:

```groovy
// In the Job Configuration UI:

Branch Sources:
  └─ GitHub
     ├─ Repository HTTPS URL: https://github.com/company/my-app.git
     ├─ Credentials: [select your GitHub credentials]
     └─ Discover Branches
         └─ Include branches: All (or use patterns like */main, */release/*)

Behaviors:
  ├─ Discover pull requests from origin
  └─ Discover tags
```

### Step 3: Add Jenkinsfile to Repository

Place `Jenkinsfile` in repository root:

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo "Building on branch: ${env.BRANCH_NAME}"
                sh './gradlew build'
            }
        }
    }
}
```

Now Jenkins automatically:
- Creates job for each branch
- Runs pipeline on code changes
- Reports status to GitHub (green check / red X)
- Cleans up completed PR branches

---

## Branch-Specific Behavior

### Different Pipeline for Different Branches

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }

        stage('Deploy to Dev') {
            when {
                branch 'develop'  // Only on develop branch
            }
            steps {
                sh 'kubectl apply -k kubernetes/kustomize/overlays/dev'
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'  // Only on main branch
            }
            steps {
                input message: 'Deploy to production?'
                sh 'kubectl apply -k kubernetes/kustomize/overlays/production'
            }
        }

        stage('Deploy Feature') {
            when {
                branch 'feature/*'  // All feature branches
            }
            steps {
                sh 'kubectl apply -k kubernetes/kustomize/overlays/feature'
            }
        }
    }
}
```

### PR-Specific Behavior

```groovy
pipeline {
    agent any

    stages {
        stage('Build & Test') {
            steps {
                sh './gradlew clean build test'
            }
        }

        stage('Report PR Status') {
            when {
                branch 'PR-*'  // Only in PR builds
            }
            steps {
                script {
                    if (currentBuild.result == 'SUCCESS') {
                        // Automatically mark PR as "Ready to merge"
                        echo 'PR checks passed'
                    } else {
                        echo 'PR checks failed'
                    }
                }
            }
        }
    }

    post {
        always {
            junit 'build/test-results/**/*.xml'
        }
    }
}
```

---

## Advanced Multi-Branch: Branch Patterns

```groovy
// In Job Configuration

Branch Sources > GitHub > Discover Branches:
  Include: refs/heads/main, refs/heads/develop, refs/heads/release/*
  Exclude: refs/heads/old-*, refs/heads/abandoned-*
```

Now Jenkins only watches specific branches, ignoring others.

---

## Real-World Git Flow with Multi-Branch

If your team uses Git Flow:

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }

        // Develop branch: Deploy to staging
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                sh 'kubectl apply -k kubernetes/kustomize/overlays/staging'
            }
        }

        // Feature branches: Deploy to ephemeral environment
        stage('Deploy Feature Env') {
            when {
                branch 'feature/*'
            }
            steps {
                script {
                    def featureName = env.BRANCH_NAME.replace('feature/', '')
                    sh """
                        kubectl create namespace feature-${featureName} || true
                        kubectl apply -k kubernetes/kustomize/overlays/dev \
                            -n feature-${featureName}
                    """
                }
            }
        }

        // Main branch: Tag and deploy to production
        stage('Tag Release') {
            when {
                branch 'main'
            }
            steps {
                script {
                    // Auto-generate semantic version
                    env.VERSION = sh(
                        script: 'git describe --tags --abbrev=0 2>/dev/null || echo "1.0.0"',
                        returnStdout: true
                    ).trim()

                    sh """
                        git tag -a v${VERSION} -m "Release ${VERSION}"
                        git push origin v${VERSION}
                    """
                }
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            input {
                message "Deploy to production?"
                ok "Deploy"
            }
            steps {
                sh 'kubectl apply -k kubernetes/kustomize/overlays/production'
            }
        }
    }

    post {
        success {
            script {
                if (env.BRANCH_NAME == 'main') {
                    slackSend(channel: '#releases', message: "Production deployment successful: v${env.VERSION}")
                }
            }
        }
    }
}
```

---

# 4. Advanced Groovy for Jenkins

## Groovy Basics for Non-Groovy Developers

### What You NEED to Know About Groovy

Groovy is a language that runs on the Java Virtual Machine. In Jenkins, it's used to script pipelines. You don't need to be an expert, but understanding basics helps.

### String Interpolation (Most Important)

```groovy
def name = "Alice"
def version = "1.2.3"

// Without interpolation (string concatenation)
echo "Hello " + name + " version " + version
// Output: Hello Alice version 1.2.3

// WITH interpolation ($ syntax) - much cleaner
echo "Hello $name version $version"
// Output: Hello Alice version 1.2.3

// Complex expressions in interpolation
def count = 5
echo "Building $count services"
echo "Next build: ${count + 1}"  // Need {} for expressions
```

### Variables and Data Types

```groovy
// Strings
def appName = "my-app"
def message = '''
    Multi-line string
    Preserves whitespace
'''

// Numbers
def buildNumber = 42
def percentage = 99.5

// Collections (lists)
def environments = ['dev', 'staging', 'production']
echo environments[0]  // dev
echo environments.size()  // 3
for (env in environments) {
    echo env
}

// Dictionaries (maps)
def config = [
    database: 'postgres',
    port: 5432,
    debug: true
]
echo config.database  // postgres
echo config['port']   // 5432
```

### Conditionals and Loops

```groovy
// If statements
def environment = 'production'

if (environment == 'production') {
    echo 'Running production build'
} else if (environment == 'staging') {
    echo 'Running staging build'
} else {
    echo 'Running dev build'
}

// For loops
def services = ['api', 'web', 'worker']
for (service in services) {
    echo "Building: $service"
}

// While loops
def counter = 0
while (counter < 3) {
    echo "Count: $counter"
    counter++
}

// Each (Groovy's functional approach)
services.each { service ->
    echo "Building: $service"
}
```

---

## Jenkins-Specific Groovy

### Environment Variables

```groovy
pipeline {
    agent any

    environment {
        APP_NAME = 'my-app'
        VERSION = '1.0.0'
        DOCKER_REGISTRY = 'docker.io'
    }

    stages {
        stage('Build') {
            steps {
                script {
                    // Access environment variables
                    echo "Building ${APP_NAME} version ${VERSION}"

                    // Jenkins built-in variables
                    echo "Build number: ${BUILD_NUMBER}"
                    echo "Build URL: ${BUILD_URL}"
                    echo "Workspace: ${WORKSPACE}"

                    // Set new variables
                    env.BUILD_VERSION = "${VERSION}-${BUILD_NUMBER}"
                    echo "Full version: ${BUILD_VERSION}"
                }
            }
        }
    }
}
```

### Running Shell Commands and Capturing Output

```groovy
pipeline {
    agent any

    stages {
        stage('Get Git Info') {
            steps {
                script {
                    // Run command, capture output
                    def gitCommit = sh(
                        script: 'git rev-parse HEAD',
                        returnStdout: true
                    ).trim()

                    echo "Current commit: $gitCommit"

                    // Run command, get return code
                    def testResult = sh(
                        script: './gradlew test',
                        returnStatus: true
                    )

                    if (testResult != 0) {
                        error 'Tests failed'
                    }

                    // List files
                    def files = sh(
                        script: 'ls -1 src/',
                        returnStdout: true
                    ).trim().split('\n')

                    echo "Files: $files"
                }
            }
        }
    }
}
```

### Conditional Logic with Groovy

```groovy
pipeline {
    agent any

    parameters {
        string(name: 'VERSION', defaultValue: '1.0.0')
    }

    stages {
        stage('Validate Version') {
            steps {
                script {
                    def version = params.VERSION

                    // Check if semantic version
                    if (!version.matches(/^\d+\.\d+\.\d+$/)) {
                        error "Invalid version format: $version (must be X.Y.Z)"
                    }

                    // Parse version parts
                    def parts = version.split('\\.')
                    def major = parts[0].toInteger()
                    def minor = parts[1].toInteger()
                    def patch = parts[2].toInteger()

                    echo "Major: $major, Minor: $minor, Patch: $patch"

                    // Business logic
                    if (major == 0) {
                        echo 'Pre-release version'
                    } else if (major > 2) {
                        echo 'Major version jump'
                    }
                }
            }
        }
    }
}
```

### Working with Closures (Functions)

```groovy
pipeline {
    agent any

    stages {
        stage('Process Services') {
            steps {
                script {
                    // Define a closure (anonymous function)
                    def buildService = { serviceName ->
                        echo "Building $serviceName"
                        sh "cd services/$serviceName && gradle build"
                    }

                    // Use it
                    def services = ['api', 'web', 'worker']
                    services.each { service ->
                        buildService(service)
                    }

                    // Or with map (functional style)
                    def results = services.collect { service ->
                        "Built: $service"
                    }
                    echo results.join(', ')
                }
            }
        }
    }
}
```

---

## Real-World Groovy Example: Dynamic Stage Generation

```groovy
pipeline {
    agent any

    stages {
        stage('Prepare') {
            steps {
                script {
                    // Get list of services from directory
                    def servicesDir = 'services'
                    env.SERVICES = sh(
                        script: "ls -1 $servicesDir",
                        returnStdout: true
                    ).trim().split('\n')

                    echo "Found services: ${env.SERVICES.join(', ')}"
                }
            }
        }

        stage('Build All Services') {
            steps {
                script {
                    // Create dynamic parallel builds
                    def parallelBuilds = [:]

                    for (service in env.SERVICES.split(',')) {
                        parallelBuilds[service] = {
                            stage("Build $service") {
                                sh "cd services/$service && gradle build"
                            }
                        }
                    }

                    // Execute all in parallel
                    parallel(parallelBuilds)
                }
            }
        }
    }
}
```

---

# 5. Testing Jenkins Pipelines

## Why Test Pipelines?

**The Problem**: Pipelines can have bugs too!

```groovy
// Bad pipeline code
stage('Deploy') {
    sh "kubectl apply -f deployment.yaml"  // What if file doesn't exist?
    sh "curl http://app:8080/health"       // What if app isn't up?
}
// No error handling = production outages
```

**Solution**: Test your pipeline code before using it.

---

## Unit Testing Shared Library Functions

Shared library functions can be tested with JUnit:

```java
// test/groovy/com/company/pipeline/DeploymentUtilsTest.groovy

import com.company.pipeline.DeploymentUtils
import org.junit.Test
import static org.junit.Assert.*
import static org.mockito.Mockito.*

class DeploymentUtilsTest {

    @Test
    void testParseImageTag() {
        // Given
        def imagePath = "docker.io/myapp:v1.2.3"

        // When
        def tag = DeploymentUtils.parseImageTag(imagePath)

        // Then
        assertEquals("v1.2.3", tag)
    }

    @Test
    void testValidateKubernetesResource() {
        // Given
        def resourceName = "my-deployment"

        // When
        def isValid = DeploymentUtils.validateResourceName(resourceName)

        // Then
        assertTrue(isValid)
    }

    @Test
    void testInvalidResourceNameThrows() {
        // Given
        def invalidName = "My-Deployment!"  // Contains invalid character

        // When & Then
        try {
            DeploymentUtils.validateResourceName(invalidName)
            fail("Should have thrown exception")
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid Kubernetes resource name", e.message)
        }
    }
}
```

---

## Integration Testing Pipelines

Test actual pipeline execution with mock steps:

```groovy
// test/groovy/com/company/pipeline/DeploymentPipelineTest.groovy

import org.jenkinsci.plugins.workflow.steps.StepContext
import com.lesfurets.jenkins.unit.BasePipelineTest
import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource
import org.junit.Before
import org.junit.Test

class DeploymentPipelineTest extends BasePipelineTest {

    @Before
    void setUp() throws Exception {
        super.setUp()

        // Load shared library
        def library = library()
            .name('pipeline-library')
            .retriever(localSource('vars'))
            .targetPath('vars')
            .build()

        helper.registerSharedLibrary(library)
    }

    @Test
    void testDeploymentSuccessful() throws Exception {
        // Given
        def script = loadScript('Jenkinsfile')

        // When
        script.execute()

        // Then
        // Verify stages ran
        assertJobStatusSuccess()
        printCallStack()
    }

    @Test
    void testDeploymentFailsOnMissingConfigMap() throws Exception {
        // Given
        def script = loadScript('Jenkinsfile')
        helper.registerAllowedMethod('sh', [String.class], {
            if (it.contains('get configmap')) {
                throw new RuntimeException('ConfigMap not found')
            }
        })

        // When/Then
        script.execute()
        assertJobStatusFailure()
    }
}
```

---

## Smoke Testing Pipelines

After deployment, run smoke tests:

```groovy
pipeline {
    agent any

    stages {
        stage('Deploy') {
            steps {
                sh 'kubectl apply -f deployment.yaml'
                sh 'kubectl rollout status deployment/myapp'
            }
        }

        stage('Smoke Tests') {
            steps {
                script {
                    // Wait for service to be ready
                    sh '''
                        for i in {1..30}; do
                            if curl -f http://myapp:8080/actuator/health; then
                                echo "Health check passed"
                                break
                            fi
                            echo "Waiting for service... ($i/30)"
                            sleep 10
                        done
                    '''

                    // Basic functionality tests
                    sh '''
                        # Test API endpoints
                        curl -f http://myapp:8080/api/users || exit 1
                        curl -f http://myapp:8080/api/health || exit 1

                        # Test database connectivity
                        curl -f -H "Content-Type: application/json" \
                            -X POST http://myapp:8080/api/users \
                            -d '{"name":"test"}' || exit 1
                    '''
                }
            }
        }
    }

    post {
        failure {
            script {
                sh 'kubectl logs -l app=myapp --tail=100'
                sh 'kubectl describe pods -l app=myapp'
            }
        }
    }
}
```

---

## Testing Error Scenarios

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }

        stage('Test Error Handling') {
            steps {
                script {
                    try {
                        // Simulate failure scenario
                        sh 'exit 1'
                    } catch (Exception e) {
                        echo "Caught expected error: ${e.message}"
                    }
                }
            }
        }

        stage('Retry on Failure') {
            steps {
                retry(3) {
                    // Try up to 3 times
                    sh 'curl -f http://flaky-service.com/health'
                }
            }
        }
    }
}
```

---

# 6. Artifact Management

## What Are Artifacts?

**Artifacts** = Files produced by your build that you want to save.

### Examples of Artifacts

```
build/libs/my-app-1.0.0.jar        ← Built JAR file
build/reports/                      ← Test reports
build/docker-image.tar              ← Docker image TAR
coverage/index.html                 ← Code coverage report
performance-results.json            ← Load test results
```

---

## Archiving Artifacts

### Basic Archiving

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }
    }

    post {
        always {
            // Archive JAR files
            archiveArtifacts artifacts: 'build/libs/*.jar'

            // Archive test results
            archiveArtifacts artifacts: 'build/test-results/**/*.xml'

            // Archive reports
            archiveArtifacts artifacts: 'build/reports/**/*'
        }
    }
}
```

### Advanced Artifact Archiving

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh '''
                    ./gradlew build
                    # Create artifact manifest
                    echo "build.jar|$(date +%s)|$(git rev-parse HEAD)" > build/manifest.txt
                '''
            }
        }
    }

    post {
        success {
            script {
                // Archive with metadata
                archiveArtifacts(
                    artifacts: 'build/libs/*.jar,build/manifest.txt',
                    allowEmptyArchive: false,
                    fingerprint: true,  // Track which builds use this JAR
                    onlyIfSuccessful: true
                )

                // Upload to artifact repository
                sh '''
                    curl -v -u admin:password \
                        --upload-file build/libs/my-app.jar \
                        http://nexus:8081/repository/releases/
                '''
            }
        }
    }
}
```

---

## Passing Artifacts Between Pipeline Stages

### Problem: Different Agents Can't Share Files

```groovy
// BAD: Assumes file exists on agent2
pipeline {
    agent any

    stages {
        stage('Build') {
            agent { label 'docker' }  // Builds on docker agent
            steps {
                sh './gradlew build'  // File is on docker agent
            }
        }

        stage('Test') {
            agent { label 'kubernetes' }  // Different agent!
            steps {
                sh 'java -jar build/libs/my-app.jar'  // FILE NOT HERE!
            }
        }
    }
}
```

### Solution 1: Archive and Restore

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            agent { label 'docker' }
            steps {
                sh './gradlew build'
                archiveArtifacts artifacts: 'build/libs/my-app.jar'
            }
        }

        stage('Test') {
            agent { label 'kubernetes' }
            steps {
                // Copy artifact from Jenkins storage
                copyArtifacts(
                    projectName: env.JOB_NAME,
                    selector: specific(env.BUILD_NUMBER)
                )

                // Now file is available
                sh 'java -jar build/libs/my-app.jar'
            }
        }
    }
}
```

### Solution 2: Shared Workspace

```groovy
pipeline {
    agent any

    options {
        // Share workspace across stages (same agent)
        // If using different agents, use archive/restore instead
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }

        stage('Test') {
            steps {
                // Artifact already here (same agent)
                sh 'java -jar build/libs/my-app.jar'
            }
        }
    }
}
```

### Solution 3: Store in External Repo

```groovy
pipeline {
    agent any

    environment {
        NEXUS_URL = 'http://nexus:8081/repository'
        APP_NAME = 'my-app'
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew build'

                sh '''
                    VERSION=$(cat version.txt)
                    JAR="build/libs/${APP_NAME}-${VERSION}.jar"

                    # Upload to Nexus
                    curl -v -u ${NEXUS_USER}:${NEXUS_PASS} \
                        --upload-file $JAR \
                        ${NEXUS_URL}/releases/${APP_NAME}/${VERSION}/
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    VERSION=$(cat version.txt)

                    # Download from Nexus
                    curl -u ${NEXUS_USER}:${NEXUS_PASS} \
                        -o my-app.jar \
                        ${NEXUS_URL}/releases/${APP_NAME}/${VERSION}/${APP_NAME}-${VERSION}.jar

                    java -jar my-app.jar
                '''
            }
        }
    }
}
```

---

## Docker Image Artifacts

```groovy
pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'docker.io'
        IMAGE_NAME = "${DOCKER_REGISTRY}/my-app"
    }

    stages {
        stage('Build Docker Image') {
            steps {
                script {
                    env.IMAGE_TAG = "${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"

                    sh """
                        docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest
                    """
                }
            }
        }

        stage('Push to Registry') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-hub',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        docker login -u ${DOCKER_USER} -p ${DOCKER_PASS}
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${IMAGE_NAME}:latest
                    '''
                }
            }
        }
    }

    post {
        success {
            // Record image information
            sh '''
                echo "Image: ${IMAGE_NAME}:${IMAGE_TAG}" > artifact-metadata.txt
                echo "Built: $(date)" >> artifact-metadata.txt
                echo "Commit: ${GIT_COMMIT}" >> artifact-metadata.txt
            '''
            archiveArtifacts artifacts: 'artifact-metadata.txt'
        }
    }
}
```

---

# 7. Configuration as Code

## What is Configuration as Code?

### The Problem It Solves

**Manual Configuration**:
- Someone logs into Jenkins UI
- Clicks through menus (Email settings, Credentials, Plugins)
- Hard to track changes
- Disaster recovery is painful
- Teams have inconsistent setups

**Configuration as Code**:
- Jenkins configuration stored in YAML files
- Version controlled in Git
- Reproducible across environments
- Easy to track changes

---

## Jenkins Configuration as Code (JCasC)

### Basic Setup

JCasC is a Jenkins plugin that loads configuration from YAML files on startup.

```yaml
# jenkins/casc/jenkins.yaml

unclassified:
  location:
    url: http://jenkins.company.com:8080/

  mailer:
    replyToAddress: jenkins@company.com
    smtpAuthenticationUsername: jenkins@company.com
    smtpAuthenticationPassword: ${JENKINS_MAIL_PASSWORD}
    smtpHost: smtp.company.com
    smtpPort: 587
    useSsl: true

jenkins:
  systemMessage: "CI/CD Pipeline Server"

  numExecutors: 5
  mode: NORMAL

  crumbIssuer:
    standard:
      excludeClientIPFromCrumb: true

  securityRealm:
    ldap:
      server: ldap://ldap.company.com
      rootDN: dc=company,dc=com
      userSearchBase: ou=users
      userSearch: uid={0}
      groupSearchBase: ou=groups
      groupSearchFilter: (memberUid={0})
      managerDN: cn=admin,dc=company,dc=com
      managerPasswordSecret: ${JENKINS_LDAP_PASSWORD}

  authorizationStrategy:
    projectMatrix:
      permissions:
        - "hudson.model.Item.Build:developers"
        - "hudson.model.Item.Cancel:developers"
        - "hudson.model.Item.Read:developers"
        - "hudson.model.View.Create:developers"
        - "hudson.model.View.Delete:developers"
        - "hudson.model.View.Read:developers"
        - "hudson.model.Run.Delete:developers"
        - "hudson.model.Run.Update:developers"
        - "hudson.model.Item.Configure:admins"
        - "hudson.model.Item.Delete:admins"
        - "hudson.model.Item.Move:admins"
        - "hudson.model.Item.Create:admins"
        - "hudson.model.View.Configure:admins"
        - "hudson.model.Computer.Create:admins"
        - "hudson.model.Computer.Delete:admins"
        - "hudson.model.Computer.Connect:admins"
        - "hudson.model.Computer.Create:admins"
        - "hudson.model.Computer.Disconnect:admins"
        - "hudson.scm.SCM.Tag:admins"
        - "com.cloudbees.plugins.credentials.CredentialsProvider.Create:admins"
        - "com.cloudbees.plugins.credentials.CredentialsProvider.Delete:admins"
        - "com.cloudbees.plugins.credentials.CredentialsProvider.ManageDomains:admins"
        - "com.cloudbees.plugins.credentials.CredentialsProvider.Update:admins"
        - "com.cloudbees.plugins.credentials.CredentialsProvider.View:admins"
```

### Configuring Plugins via JCasC

```yaml
# jenkins/casc/plugins.yaml

credentials:
  system:
    domainCredentials:
      - credentials:
          - usernamePassword:
              scope: GLOBAL
              id: "github-credentials"
              username: ${GITHUB_USERNAME}
              password: ${GITHUB_TOKEN}
              description: "GitHub access token"

          - string:
              scope: GLOBAL
              id: "sonarqube-token"
              secret: ${SONARQUBE_TOKEN}
              description: "SonarQube authentication"

          - aws:
              scope: GLOBAL
              id: "aws-credentials"
              accessKey: ${AWS_ACCESS_KEY}
              secretKey: ${AWS_SECRET_KEY}
              description: "AWS access"

          - sshUserPrivateKey:
              scope: GLOBAL
              id: "deploy-key"
              username: deploy
              privateKeySource:
                directEntry:
                  privateKey: |
                    -----BEGIN RSA PRIVATE KEY-----
                    ${DEPLOY_PRIVATE_KEY}
                    -----END RSA PRIVATE KEY-----
              description: "SSH deploy key"

unclassified:
  sonarGlobalConfiguration:
    installations:
      - name: "SonarQube Server"
        serverUrl: "http://sonarqube:9000"
        credentialsId: "sonarqube-token"
        webhookSecretId: "sonarqube-webhook"
        mdbrunner: false

  awsConfig:
    region: us-east-1
    useInstanceProfile: false
    credentialsId: "aws-credentials"

  github:
    apiUrl: "https://api.github.com"
    credentialsId: "github-credentials"
    clientCacheSize: 20

  location:
    url: "http://jenkins.company.com:8080/"
```

### Configuring Jenkins Tools

```yaml
# jenkins/casc/tools.yaml

tool:
  git:
    installations:
      - name: "Default"
        home: "git"

  maven:
    installations:
      - name: "Maven 3.8.1"
        properties:
          - installSource:
              direct:
                url: "https://archive.apache.org/dist/maven/maven-3/3.8.1/binaries/apache-maven-3.8.1-bin.zip"

  gradle:
    installations:
      - name: "Gradle 7.0"
        properties:
          - installSource:
              direct:
                url: "https://services.gradle.org/distributions/gradle-7.0-bin.zip"

  jdk:
    installations:
      - name: "JDK 17"
        properties:
          - installSource:
              adoptOpenJdkInstaller:
                id: "jdk17.0.1_12"
```

---

## Loading JCasC Configuration

### Docker Approach

```dockerfile
FROM jenkins/jenkins:latest

# Install JCasC plugin
RUN jenkins-plugin-cli --plugins configuration-as-code

# Copy configuration
COPY jenkins/casc/ /var/jenkins_home/casc-configs/

# Tell Jenkins to load from YAML
ENV CASC_JENKINS_CONFIG=/var/jenkins_home/casc-configs
```

### Environment Variables with JCasC

```yaml
# jenkins/casc/jenkins.yaml

jenkins:
  systemMessage: "Jenkins Server - ${ENVIRONMENT}"

  securityRealm:
    ldap:
      server: ${LDAP_SERVER}
      managerDN: ${LDAP_MANAGER_DN}
      managerPasswordSecret: ${LDAP_PASSWORD}
```

```bash
# Run Jenkins with JCasC variables
docker run \
  -e CASC_JENKINS_CONFIG=/var/jenkins_home/casc-configs \
  -e ENVIRONMENT=production \
  -e LDAP_SERVER=ldap://ldap.company.com \
  -e LDAP_MANAGER_DN=cn=admin,dc=company,dc=com \
  -e LDAP_PASSWORD=secret123 \
  jenkins/jenkins:latest
```

---

# 8. GitFlow Integration

## Understanding Git Flow

**Git Flow** = A branching strategy that structures how teams work with Git.

### The Problem It Solves

Without a branching strategy:
```
main branch
├─ Commit A (feature from Alice)
├─ Commit B (hotfix from Bob)
├─ Commit C (experiment from Charlie)
└─ Commit D (release from Diana)
```

Hard to track what's what. Releases mixed with experiments.

### Git Flow Solution

```
main              (Production-ready releases only)
↑
release/1.2.0     (Release preparation)
↑
develop           (Integration branch)
↑↑
├─ feature/user-login      (Feature branches)
├─ feature/payment-api
├─ feature/auth-improvements
└─ bugfix/login-timeout

(Production issues) →
hotfix/forgot-password    (Hotfix branches)
→ back to main and develop
```

---

## Jenkins Integration with Git Flow

### Jenkinsfile for Git Flow

```groovy
pipeline {
    agent any

    stages {
        stage('Build & Test') {
            steps {
                sh './gradlew clean build test'
            }
        }

        // Feature branches: Deploy to feature environment
        stage('Deploy Feature') {
            when {
                branch 'feature/*'
            }
            steps {
                script {
                    def featureName = env.BRANCH_NAME.replace('feature/', '')
                    echo "Deploying feature: $featureName"

                    sh """
                        kubectl create namespace feature-${featureName} || true
                        kubectl apply -k kubernetes/kustomize/overlays/feature \
                            -n feature-${featureName}
                    """
                }
            }
        }

        // Develop branch: Deploy to staging
        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                echo "Deploying develop to staging"
                sh 'kubectl apply -k kubernetes/kustomize/overlays/staging'
            }
        }

        // Release branch: Final testing before production
        stage('Release Testing') {
            when {
                branch 'release/*'
            }
            steps {
                script {
                    def version = env.BRANCH_NAME.replace('release/', '')
                    echo "Testing release: $version"

                    sh '''
                        ./gradlew test
                        ./gradlew intTest  // Integration tests
                        ./gradlew performanceTest
                    '''
                }
            }
        }

        // Release branch: Create tag for production
        stage('Tag Release') {
            when {
                branch 'release/*'
            }
            steps {
                script {
                    def version = env.BRANCH_NAME.replace('release/', '')

                    sh '''
                        git tag -a v${version} -m "Release ${version}"
                        git push origin v${version}
                    '''
                }
            }
        }

        // Main branch: Production deployment
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            input {
                message "Deploy to production?"
                ok "Deploy Now"
            }
            steps {
                echo "Deploying to production"
                sh 'kubectl apply -k kubernetes/kustomize/overlays/production'
            }
        }

        // Hotfix branches: Urgent production fixes
        stage('Deploy Hotfix') {
            when {
                branch 'hotfix/*'
            }
            steps {
                script {
                    def hotfixName = env.BRANCH_NAME.replace('hotfix/', '')
                    echo "Deploying hotfix: $hotfixName"

                    sh '''
                        ./gradlew build test
                        kubectl apply -k kubernetes/kustomize/overlays/production
                    '''
                }
            }
        }
    }
}
```

---

# 9. Release Management

## Release Pipeline

```groovy
pipeline {
    agent any

    parameters {
        choice(
            name: 'RELEASE_TYPE',
            choices: ['patch', 'minor', 'major'],
            description: 'Semantic versioning increment'
        )
    }

    stages {
        stage('Determine Version') {
            steps {
                script {
                    // Get current version
                    def currentVersion = sh(
                        script: "git describe --tags --abbrev=0 2>/dev/null || echo '1.0.0'",
                        returnStdout: true
                    ).trim()

                    echo "Current version: $currentVersion"

                    // Parse version parts
                    def parts = currentVersion.replace('v', '').split('\\.')
                    def major = parts[0].toInteger()
                    def minor = parts[1].toInteger()
                    def patch = parts[2].toInteger()

                    // Increment based on parameter
                    switch(params.RELEASE_TYPE) {
                        case 'major':
                            major++
                            minor = 0
                            patch = 0
                            break
                        case 'minor':
                            minor++
                            patch = 0
                            break
                        case 'patch':
                            patch++
                            break
                    }

                    env.NEW_VERSION = "${major}.${minor}.${patch}"
                    echo "New version: ${env.NEW_VERSION}"
                }
            }
        }

        stage('Create Release Branch') {
            steps {
                script {
                    sh '''
                        git checkout develop
                        git pull origin develop

                        git checkout -b release/${NEW_VERSION}

                        # Update version in build files
                        sed -i "s/version = .*/version = '${NEW_VERSION}'/" build.gradle.kts

                        # Commit version bump
                        git add build.gradle.kts
                        git commit -m "Bump version to ${NEW_VERSION}"
                        git push origin release/${NEW_VERSION}
                    '''
                }
            }
        }

        stage('Build Release') {
            steps {
                sh './gradlew clean build'
                sh 'docker build -t myapp:${NEW_VERSION} .'
            }
        }

        stage('Release Testing') {
            steps {
                sh '''
                    ./gradlew test
                    ./gradlew intTest
                    ./gradlew smokeTest
                '''
            }
        }

        stage('Merge to Main') {
            steps {
                sh '''
                    git checkout main
                    git pull origin main
                    git merge --no-ff release/${NEW_VERSION} -m "Release ${NEW_VERSION}"
                    git tag -a v${NEW_VERSION} -m "Release ${NEW_VERSION}"
                    git push origin main
                    git push origin v${NEW_VERSION}
                '''
            }
        }

        stage('Merge Back to Develop') {
            steps {
                sh '''
                    git checkout develop
                    git pull origin develop
                    git merge --no-ff release/${NEW_VERSION} -m "Merge release ${NEW_VERSION} back"
                    git push origin develop

                    # Clean up release branch
                    git push origin --delete release/${NEW_VERSION}
                '''
            }
        }

        stage('Deploy Release') {
            steps {
                sh '''
                    docker tag myapp:${NEW_VERSION} myapp:latest
                    docker push myapp:${NEW_VERSION}
                    docker push myapp:latest

                    kubectl set image deployment/myapp \
                        myapp=myapp:${NEW_VERSION} \
                        -n production

                    kubectl rollout status deployment/myapp -n production
                '''
            }
        }

        stage('Release Notes') {
            steps {
                script {
                    def previousTag = sh(
                        script: "git describe --tags --abbrev=0 $(git rev-list --tags --skip=1 -n 1) 2>/dev/null || echo 'START'",
                        returnStdout: true
                    ).trim()

                    sh """
                        git log ${previousTag}..v${NEW_VERSION} --oneline > release-notes.txt
                        cat release-notes.txt
                    """

                    archiveArtifacts artifacts: 'release-notes.txt'
                }
            }
        }
    }

    post {
        success {
            slackSend(
                channel: '#releases',
                color: 'good',
                message: "Release v${env.NEW_VERSION} deployed successfully"
            )
        }
    }
}
```

---

# 10. Performance Optimization

## Slow Pipeline Problems and Solutions

### Problem 1: Long Build Times

```groovy
// SLOW: Sequential everything
pipeline {
    agent any
    stages {
        stage('Build') { steps { sh './gradlew build' } }       // 2 min
        stage('Unit Tests') { steps { sh './gradlew test' } }   // 3 min
        stage('Integration Tests') { steps { sh './gradlew intTest' } }  // 4 min
        stage('Security Scan') { steps { sh './gradlew sonarqube' } }    // 2 min
        stage('Docker Build') { steps { sh 'docker build .' } } // 2 min
    }
    // Total: 13 minutes
}
```

**Solution**: Run independent stages in parallel

```groovy
// FAST: Parallel execution
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh './gradlew build'  // 2 min
            }
        }

        stage('Parallel Checks') {
            parallel {
                stage('Unit Tests') {
                    steps { sh './gradlew test' }  // 3 min (in parallel)
                }
                stage('Integration Tests') {
                    steps { sh './gradlew intTest' }  // 4 min (in parallel)
                }
                stage('Security Scan') {
                    steps { sh './gradlew sonarqube' }  // 2 min (in parallel)
                }
                stage('Docker Build') {
                    steps { sh 'docker build .' }  // 2 min (in parallel)
                }
            }
        }
    }
    // Total: 6 minutes (max of parallel stages)
}
```

### Problem 2: Gradle/Maven Build Cache

```groovy
// SLOW: Re-downloading dependencies
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew build'  // Downloads everything every time
            }
        }
    }
}
```

**Solution**: Cache dependencies

```groovy
pipeline {
    agent any

    options {
        // Keep workspace between builds
        disableConcurrentBuilds()
    }

    stages {
        stage('Build') {
            steps {
                sh '''
                    # Use Gradle cache
                    ./gradlew build \
                        --gradle-user-home $WORKSPACE/.gradle \
                        --build-cache
                '''
            }
        }
    }

    post {
        always {
            // Archive gradle cache for faster rebuilds
            archiveArtifacts artifacts: '.gradle/caches/**'
        }
    }
}
```

### Problem 3: Docker Layer Caching

```dockerfile
# SLOW: Rebuilds dependencies every time
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN ./gradlew build  # Full rebuild every time

# FAST: Leverage layer caching
FROM eclipse-temurin:17-jdk as builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew dependencies  # Cached unless build files change
COPY src ./src
RUN ./gradlew build  # Only rebuilds if src changes
```

### Problem 4: Test Parallelization

```groovy
pipeline {
    agent any

    stages {
        stage('Unit Tests') {
            steps {
                sh '''
                    # Run tests in parallel with 4 threads
                    ./gradlew test \
                        --max-workers=4 \
                        --parallel
                '''
            }
        }
    }
}
```

### Problem 5: Agent Resource Limits

```groovy
pipeline {
    agent {
        docker {
            image 'gradle:7.0-jdk17'
            // Limit resources to prevent slowdown
            args '--memory=2g --cpus=2'
        }
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }
    }
}
```

---

# 11. Debugging & Troubleshooting

## Common Pipeline Errors

### Error 1: Timeout

```
Timeout: The operation has timed out after 10 minutes
```

**Fix**:
```groovy
pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')  // Increase timeout
    }

    stages {
        stage('Long Running') {
            options {
                timeout(time: 20, unit: 'MINUTES')  // Stage-specific timeout
            }
            steps {
                sh 'some-long-running-command'
            }
        }
    }
}
```

### Error 2: Workspace Not Found

```
ERROR: Workspace does not exist
```

**Fix**:
```groovy
pipeline {
    agent any

    stages {
        stage('Cleanup') {
            steps {
                // Create workspace if needed
                sh 'mkdir -p $WORKSPACE'
                sh 'pwd'  // Debug: show workspace path
            }
        }
    }
}
```

### Error 3: Permission Denied

```
bash: ./gradlew: Permission denied
```

**Fix**:
```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'chmod +x ./gradlew'  // Make executable
                sh './gradlew build'
            }
        }
    }
}
```

### Error 4: File Not Found

```
docker: error: file not found: Dockerfile
```

**Fix**:
```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                script {
                    // Debug: list files
                    sh 'ls -la'
                    sh 'pwd'

                    // Verify file exists
                    if (!fileExists('Dockerfile')) {
                        error 'Dockerfile not found'
                    }

                    sh 'docker build .'
                }
            }
        }
    }
}
```

---

## Debugging Techniques

### Enable Debug Logging

```groovy
pipeline {
    agent any

    options {
        timestamps()  // Add timestamps to logs
    }

    stages {
        stage('Debug') {
            steps {
                script {
                    // Print all environment variables
                    sh 'printenv | sort'

                    // Print Jenkins variables
                    echo "Build: ${env.BUILD_NUMBER}"
                    echo "Branch: ${env.BRANCH_NAME}"
                    echo "Workspace: ${WORKSPACE}"
                }
            }
        }
    }
}
```

### Print Pipeline Execution

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                script {
                    // Print what stage we're in
                    echo "===== Stage: ${env.STAGE_NAME} ====="
                    echo "Build: ${env.BUILD_NUMBER}"
                    echo "Job: ${env.JOB_NAME}"
                }
            }
        }
    }

    post {
        always {
            // Print final status
            script {
                echo "Build completed: ${currentBuild.result}"
                echo "Build duration: ${currentBuild.durationString}"
            }
        }
    }
}
```

### Inspect Failed Logs

```groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }
    }

    post {
        failure {
            script {
                // Capture detailed error logs
                sh '''
                    echo "===== Build Output ====="
                    cat build.log || true

                    echo "===== Error Details ====="
                    tail -100 build/reports/*.log || true

                    echo "===== Disk Usage ====="
                    du -sh * || true
                '''
            }
        }
    }
}
```

---

# 12. Common Anti-Patterns

## Anti-Pattern 1: God Pipeline (Doing Everything)

**❌ BAD**: One Jenkinsfile that does everything
```groovy
pipeline {
    agent any

    stages {
        stage('Build') { steps { sh './gradlew build' } }
        stage('Test') { steps { sh './gradlew test' } }
        stage('Security Scan') { steps { sh './gradlew sonarqube' } }
        stage('Docker Build') { steps { sh 'docker build .' } }
        stage('Deploy Dev') { steps { sh 'kubectl apply...' } }
        stage('Deploy Staging') { steps { sh 'kubectl apply...' } }
        stage('Manual Approval') { steps { input 'Deploy to prod?' } }
        stage('Deploy Production') { steps { sh 'kubectl apply...' } }
        stage('Smoke Tests') { steps { sh 'curl http://app/health' } }
        stage('Notify Slack') { steps { slackSend(...) } }
    }
    // 500+ line pipeline - hard to maintain, hard to test
}
```

**✅ GOOD**: Split into focused pipelines

```groovy
// Jenkinsfile.ci - Just build and test
pipeline {
    stages {
        stage('Build') { steps { sh './gradlew build' } }
        stage('Test') { steps { sh './gradlew test' } }
        stage('Security') { steps { sh './gradlew sonarqube' } }
    }
}

// Jenkinsfile.build - Just Docker image
pipeline {
    stages {
        stage('Build Image') { steps { sh 'docker build .' } }
        stage('Push') { steps { sh 'docker push ...' } }
    }
}

// Jenkinsfile.deploy - Just deployment
pipeline {
    stages {
        stage('Deploy') { steps { sh 'kubectl apply...' } }
        stage('Verify') { steps { sh 'curl http://app/health' } }
    }
}

// Jenkinsfile (Orchestrator) - Chains them together
pipeline {
    stages {
        stage('CI') { steps { build 'ci-pipeline' } }
        stage('Build') { steps { build 'build-pipeline' } }
        stage('Deploy') { steps { build 'deploy-pipeline' } }
    }
}
```

---

## Anti-Pattern 2: Hard-Coded Secrets

**❌ BAD**: Secrets in pipeline code
```groovy
pipeline {
    stages {
        stage('Push Image') {
            steps {
                sh '''
                    docker login -u admin -p hardcoded123
                    docker push myapp:latest
                '''
            }
        }
    }
}
```

**✅ GOOD**: Use Jenkins credentials

```groovy
pipeline {
    stages {
        stage('Push Image') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'docker-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        docker login -u ${DOCKER_USER} -p ${DOCKER_PASS}
                        docker push myapp:latest
                    '''
                }
            }
        }
    }
}
```

---

## Anti-Pattern 3: No Error Handling

**❌ BAD**: No cleanup on failure
```groovy
pipeline {
    stages {
        stage('Deploy') {
            steps {
                sh 'kubectl apply -f deployment.yaml'
                sh 'curl http://app:8080/health'  // What if this fails?
                sh 'send-notification.sh'          // Runs anyway
            }
        }
    }
}
```

**✅ GOOD**: Proper error handling
```groovy
pipeline {
    stages {
        stage('Deploy') {
            steps {
                sh 'kubectl apply -f deployment.yaml'
            }
        }

        stage('Verify') {
            steps {
                script {
                    try {
                        sh 'curl -f http://app:8080/health'
                    } catch (Exception e) {
                        echo "Health check failed: ${e.message}"
                        error 'Application not healthy'
                    }
                }
            }
        }
    }

    post {
        always {
            // Runs regardless of success/failure
            sh 'cleanup-resources.sh'
        }

        failure {
            // Only on failure
            slackSend(color: 'danger', message: 'Deployment failed')
            sh 'rollback-deployment.sh'
        }
    }
}
```

---

## Anti-Pattern 4: Manual Jenkins Configuration

**❌ BAD**: Manual clicks in Jenkins UI
- Create job in UI
- Configure credentials by hand
- Install plugins manually
- Disaster recovery takes hours

**✅ GOOD**: Configuration as Code
```yaml
# jenkins.yaml - Stored in Git
jenkins:
  systemMessage: "Production Jenkins"
  numExecutors: 5

credentials:
  system:
    domainCredentials:
      - credentials:
          - usernamePassword:
              id: "github-token"
              username: ${GITHUB_USER}
              password: ${GITHUB_TOKEN}

unclassified:
  sonarGlobalConfiguration:
    installations:
      - name: "SonarQube"
        serverUrl: "http://sonarqube:9000"
```

---

# 13. Real-World Patterns

## Pattern: Multi-Environment Deployment

```groovy
pipeline {
    agent any

    parameters {
        choice(name: 'ENV', choices: ['dev', 'staging', 'prod'], description: 'Target')
    }

    environment {
        APP = 'myapp'
        REGISTRY = 'docker.io'
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build'
                sh "docker build -t ${REGISTRY}/${APP}:${BUILD_NUMBER} ."
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }

        stage('Deploy') {
            steps {
                script {
                    def imageTag = "${REGISTRY}/${APP}:${BUILD_NUMBER}"
                    def replicas = params.ENV == 'prod' ? 3 : 1

                    sh """
                        kubectl set image deployment/${APP} \
                            ${APP}=${imageTag} \
                            -n ${params.ENV} \
                            --record

                        kubectl rollout status deployment/${APP} \
                            -n ${params.ENV} \
                            --timeout=5m
                    """
                }
            }
        }
    }
}
```

---

## Pattern: Approval Gates with Timeouts

```groovy
pipeline {
    agent any

    stages {
        stage('Deploy to Staging') {
            input {
                message "Deploy to staging?"
                ok "Deploy"
                submitter "developers"
            }
            steps {
                sh 'kubectl apply -k kubernetes/kustomize/overlays/staging'
            }
        }

        stage('Wait for Testing') {
            steps {
                // Give team time to test
                sleep(time: 1, unit: 'HOURS')
            }
        }

        stage('Deploy to Production') {
            input {
                message "Ready for production?"
                ok "Deploy to Prod"
                submitter "admins"
                parameters {
                    choice(
                        name: 'DEPLOYMENT_TYPE',
                        choices: ['rolling', 'canary', 'blue-green'],
                        description: 'Deployment strategy'
                    )
                }
            }
            steps {
                script {
                    sh """
                        if [ '${DEPLOYMENT_TYPE}' = 'canary' ]; then
                            kubectl patch deployment myapp --type='json' \
                                -p='[{"op": "replace", "path": "/spec/replicas", "value":4}]'
                        fi

                        kubectl apply -k kubernetes/kustomize/overlays/production
                    """
                }
            }
        }
    }
}
```

---

# Summary: The Complete Pipeline Mastery

By now, you understand:

1. **Declarative vs Scripted** - Declarative for most cases, Scripted for complex logic
2. **Parameters** - Making pipelines reusable and user-friendly
3. **Multi-Branch** - Automating across many Git branches
4. **Groovy Fundamentals** - String interpolation, loops, conditionals
5. **Testing Pipelines** - Unit testing shared libraries
6. **Artifact Management** - Passing files between stages
7. **Configuration as Code** - Version-controlled Jenkins setup
8. **Git Flow** - Branch strategies with Jenkins
9. **Release Management** - Semantic versioning and releases
10. **Performance** - Parallel builds, caching, optimization
11. **Debugging** - Troubleshooting and error handling
12. **Anti-Patterns** - What NOT to do
13. **Real-World Patterns** - Practical implementations

---

## Next Steps

1. **Apply to Your Project**: Take these patterns and implement them in your pipelines
2. **Practice**: Build test pipelines to understand each concept
3. **Share**: Teaching others cements your knowledge
4. **Optimize**: Measure build times and apply optimizations
5. **Document**: Keep pipeline code well-commented for your team

You're now equipped to design, implement, and maintain enterprise-grade Jenkins pipelines.

