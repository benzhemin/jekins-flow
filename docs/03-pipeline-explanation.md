# Pipeline Explanation - Detailed Code Walkthrough

This guide explains each Jenkins pipeline stage by stage with code examples and explanations.

## Table of Contents

1. [Jenkinsfile.ci - CI Pipeline](#jenkinsfileci---ci-pipeline)
2. [Jenkinsfile.build - Build Pipeline](#jenkinsfilebuild---build-pipeline)
3. [Jenkinsfile.deploy - Deploy Pipeline](#jenkinsfiledeploy---deploy-pipeline)
4. [Jenkinsfile.full - Orchestrator](#jenkinsfilefull---orchestrator)
5. [Shared Library Functions](#shared-library-functions)

## Jenkinsfile.ci - CI Pipeline

### Purpose
Build, test, and scan code quality. Runs on every commit.

### Stage Breakdown

#### Stage 1: Checkout

```groovy
stage('Checkout') {
    steps {
        script {
            echo "====== Checking out source code ======"
            checkout scm  // Checkout from SCM (configured in Jenkins job)

            // Display git information for traceability
            sh '''
                echo "Repository: $(git config --get remote.origin.url)"
                echo "Branch: $(git rev-parse --abbrev-ref HEAD)"
                echo "Commit: $(git rev-parse HEAD)"
                echo "Author: $(git log -1 --pretty=format:'%an')"
                echo "Message: $(git log -1 --pretty=format:'%s')"
            '''
        }
    }
}
```

**What happens**:
- `checkout scm` clones the repository
- Git commands display information for build logs
- Useful for debugging and traceability

**Output example**:
```
Repository: https://github.com/company/springboot-cicd-platform.git
Branch: main
Commit: a1b2c3d4e5f6
Author: John Doe
Message: Add new feature: user deactivation
```

#### Stage 2: Build

```groovy
stage('Build') {
    steps {
        script {
            echo "====== Building application ======"
            try {
                sh '''
                    chmod +x ./gradlew
                    ./gradlew clean build -x test \
                        --stacktrace \
                        --info
                '''
            } catch (Exception e) {
                echo "Build failed: ${e.message}"
                error("Build stage failed")
            }
        }
    }
}
```

**What happens**:
- `chmod +x ./gradlew` makes the gradle wrapper executable
- `clean` removes previous builds
- `build` compiles all modules
- `-x test` skips tests (run separately in next stage)
- `--stacktrace` shows full error traces
- `--info` provides detailed build information

**Output example**:
```
> Task :application:api:compileJava
> Task :application:api:processResources
> Task :application:api:bootJar
BUILD SUCCESSFUL in 2m 34s
```

#### Stage 3: Test

```groovy
stage('Test') {
    steps {
        script {
            echo "====== Running tests ======"
            try {
                sh '''
                    ./gradlew test \
                        --stacktrace \
                        --info \
                        -x bootJar
                '''
            } catch (Exception e) {
                echo "Tests failed: ${e.message}"
                // Don't error out - show reports anyway
            }
        }
    }
    post {
        always {
            // JUnit plugin processes test results
            junit testResults: '**/build/test-results/**/*.xml',
                  allowEmptyResults: true

            // Archive test reports
            archiveArtifacts artifacts: '**/build/reports/tests/**',
                             allowEmptyArchive: true
        }
    }
}
```

**What happens**:
- Runs all JUnit tests
- Collects test results XML
- Publishes results to Jenkins dashboard
- Generates HTML reports
- Archives artifacts for later review

**Why separate from build**:
- Build can succeed even if tests fail (for inspection)
- Tests run in separate `post` block for guaranteed execution
- Reports generated regardless of test success

**Key insight**: JUnit plugin automatically parses `TEST-*.xml` files and shows pass/fail statistics

#### Stage 4: Code Quality (SonarQube)

```groovy
stage('Code Quality') {
    steps {
        script {
            echo "====== Running SonarQube analysis ======"
            try {
                sh '''
                    ./gradlew sonar \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=${SONAR_LOGIN} \
                        -Dsonar.projectKey=springboot-cicd-platform \
                        -Dsonar.projectName="Spring Boot CI/CD Platform" \
                        -Dsonar.sourceEncoding=UTF-8 \
                        -Dsonar.java.source=17 \
                        --stacktrace
                '''
            } catch (Exception e) {
                echo "SonarQube analysis failed: ${e.message}"
                // Non-blocking - quality gate checks in next stage
            }
        }
    }
}
```

**What happens**:
- Gradle sonar plugin sends code to SonarQube server
- SonarQube analyzes:
  - Code smells
  - Bugs
  - Vulnerabilities
  - Code coverage
  - Duplication

**SonarQube Properties**:
- `sonar.projectKey`: Unique identifier
- `sonar.host.url`: SonarQube server URL
- `sonar.login`: Authentication token
- `sonar.java.source`: Java version (17 for this project)

**After analysis**:
- Results available at: `${SONAR_HOST_URL}/dashboard?id=springboot-cicd-platform`
- Quality gate runs automatically (pass/fail)
- Feeds into next stage for gate check

#### Stage 5: Security Scan (OWASP Dependency-Check)

```groovy
stage('Security Scan') {
    steps {
        script {
            echo "====== Running OWASP Dependency-Check ======"
            try {
                sh '''
                    ./gradlew dependencyCheck \
                        --stacktrace \
                        || true  # Continue even if scan fails
                '''
            } catch (Exception e) {
                echo "OWASP scan failed: ${e.message}"
            }
        }
    }
    post {
        always {
            // Archive the report
            archiveArtifacts artifacts: '**/build/reports/dependency-check-report.*',
                             allowEmptyArchive: true
        }
    }
}
```

**What happens**:
- Gradle dependencyCheck plugin scans all dependencies
- Checks against NVD (National Vulnerability Database)
- Identifies CVEs (Common Vulnerabilities and Exposures)
- Generates HTML report with details

**Report includes**:
- Vulnerable dependency name and version
- CVE identifiers and scores
- Recommended fixes
- Suppression options for false positives

**Why `|| true`**: Don't fail the build immediately - let quality gate decide

#### Stage 6: Quality Gate

```groovy
stage('Quality Gate') {
    steps {
        script {
            echo "====== Checking SonarQube Quality Gate ======"
            try {
                timeout(time: 5, unit: 'MINUTES') {
                    sh '''
                        echo "Waiting for SonarQube quality gate..."
                        sleep 10

                        # In production, use SonarQube API:
                        # GATE_STATUS=$(curl -s \
                        #   -u "${SONAR_LOGIN}:" \
                        #   "${SONAR_HOST_URL}/api/qualitygates/project_status?projectKey=springboot-cicd-platform" \
                        #   | jq -r '.projectStatus.status')
                        #
                        # if [ "$GATE_STATUS" != "OK" ]; then
                        #   echo "Quality gate FAILED!"
                        #   exit 1
                        # fi
                    '''
                }
            } catch (Exception e) {
                echo "Quality gate check failed - ${e.message}"
            }
        }
    }
}
```

**What happens**:
- Waits for SonarQube to finish analysis
- Checks if quality gate passed
- Fails build if gate conditions not met

**Quality Gate Rules** (configurable in SonarQube):
- Coverage < 80%: FAIL
- Duplicated lines > 3%: FAIL
- Bugs > 0: FAIL
- High/Critical vulnerabilities: FAIL

**Key point**: Gate acts as policy enforcement - prevents bad code from progressing

#### Stage 7: Archive

```groovy
stage('Archive') {
    steps {
        script {
            echo "====== Archiving artifacts ======"
            sh '''
                mkdir -p artifacts
                find application/*/build/libs -name "*.jar" \
                    -exec cp {} artifacts/ \; 2>/dev/null || true

                mkdir -p artifacts/reports
                find application -path "*/build/reports" -type d \
                    -exec cp -r {} artifacts/reports/ \; 2>/dev/null || true
            '''

            // Jenkins archival (for download and access)
            archiveArtifacts artifacts: 'artifacts/**',
                             fingerprint: true
        }
    }
}
```

**What happens**:
- Collects JAR files and reports
- Stores in Jenkins for future access
- Creates fingerprints for tracking

**Why archive**:
- Available for download
- Used by downstream Build pipeline
- Retention policy enforced (configurable)

### Post Actions

```groovy
post {
    always {
        script {
            echo "====== Pipeline Cleanup ======"
            cleanWs(
                deleteDirs: true,
                patterns: [
                    [pattern: '.gradle/**', type: 'INCLUDE'],
                    [pattern: 'build/**', type: 'INCLUDE']
                ]
            )
        }
    }

    success {
        script {
            echo "====== CI Pipeline SUCCESS ======"
            emailext(
                subject: "CI Build Success: ${env.JOB_NAME}",
                body: """Build succeeded! ...""",
                to: '${DEFAULT_RECIPIENTS}',
                recipientProviders: [developers(), requestor()]
            )
        }
    }

    failure {
        script {
            echo "====== CI Pipeline FAILURE ======"
            // Notify developers of failure
        }
    }
}
```

**Post blocks execute**:
- `always`: Regardless of result (cleanup)
- `success`: Only if pipeline passed
- `failure`: Only if pipeline failed

---

## Jenkinsfile.build - Build Pipeline

### Purpose
Create Docker image, scan with Trivy, push to registry.

### Key Stages

#### Stage: Docker Build

```groovy
stage('Docker Build') {
    steps {
        script {
            sh '''
                docker build \
                    --file application/docker/Dockerfile \
                    --tag ${IMAGE_REPO}:${IMAGE_TAG} \
                    --tag ${IMAGE_REPO}:${BUILD_TIMESTAMP} \
                    --tag ${IMAGE_REPO}:latest \
                    --label "git.commit=${GIT_COMMIT_SHORT}" \
                    --label "build.number=${BUILD_NUMBER}" \
                    .
            '''
        }
    }
}
```

**Key Points**:
- Multi-tag image for different use cases
- Latest tag for CI/CD references
- Version tag for release tracking
- Labels for traceability

#### Stage: Container Scan (Trivy)

```groovy
stage('Container Scan') {
    steps {
        script {
            sh '''
                trivy image \
                    --severity CRITICAL,HIGH,MEDIUM \
                    --format json \
                    --output trivy-report.json \
                    ${IMAGE_REPO}:${IMAGE_TAG}

                # Count vulnerabilities
                CRITICAL=$(grep -c '"Severity": "CRITICAL"' trivy-report.json || echo "0")
                echo "Critical vulnerabilities: $CRITICAL"
            '''
        }
    }
}
```

**Trivy checks**:
- OS package vulnerabilities (from base image)
- Application dependency vulnerabilities
- Configuration issues
- License compliance

**Severity levels**:
- CRITICAL: Immediate action required
- HIGH: Should be fixed soon
- MEDIUM: Address in regular maintenance
- LOW: Monitor and plan

---

## Jenkinsfile.deploy - Deploy Pipeline

### Approval Gate

```groovy
stage('Approval') {
    when {
        expression {
            return params.ENVIRONMENT in ['staging', 'production']
        }
    }
    steps {
        script {
            try {
                timeout(time: 1, unit: 'HOURS') {
                    def message = params.ENVIRONMENT == 'production' ?
                        "⚠️  PRODUCTION DEPLOYMENT ⚠️" :
                        "Deploy to ${params.ENVIRONMENT}?"

                    input(message: message, ok: 'Deploy')
                }
            } catch (err) {
                error("Deployment approval failed")
            }
        }
    }
}
```

**Why approval**:
- Human verification before critical deployments
- Prevents accidents
- Audit trail for compliance
- Timeout prevents hanging pipelines

### Kustomize Deployment

```groovy
stage('Deploy - Kustomize') {
    steps {
        script {
            sh '''
                # Update image reference
                cd kubernetes/kustomize/overlays/${ENVIRONMENT}
                kustomize edit set image springboot-app=${FULL_IMAGE}
                cd ${WORKSPACE}

                # Apply manifests
                kustomize build kubernetes/kustomize/overlays/${ENVIRONMENT} | \
                    kubectl apply -f - \
                    -n ${NAMESPACE}
            '''
        }
    }
}
```

**Why Kustomize**:
- Template-free - pure YAML
- Patch-based customization
- GitOps-friendly
- Simpler than Helm for basic use cases

---

## Jenkinsfile.full - Orchestrator

This pipeline chains all others:

```groovy
stage('CI') {
    steps {
        build job: 'springboot-cicd-platform-ci',
              wait: true,
              propagate: true
    }
}

stage('Build') {
    steps {
        build job: 'springboot-cicd-platform-build',
              parameters: [...],
              wait: true
    }
}

stage('Deploy - Development') {
    steps {
        build job: 'springboot-cicd-platform-deploy',
              parameters: [
                  choice(name: 'ENVIRONMENT', value: 'dev'),
                  ...
              ],
              wait: true
    }
}

// Approval gates before staging and production...
```

**Key pattern**: Each stage calls a separate Jenkins job, waits for completion, and passes parameters

---

## Shared Library Functions

### deployToKubernetes.groovy

```groovy
def call(Map config) {
    def environment = config.environment ?: 'dev'
    def imageName = config.imageName ?: 'springboot-app'
    def imageTag = config.imageTag ?: 'latest'
    def deploymentTool = config.deploymentTool ?: 'kustomize'

    if (deploymentTool == 'helm') {
        deployWithHelm(imageName, imageTag, environment, namespace, timeout)
    } else {
        deployWithKustomize(imageName, imageTag, environment, namespace)
    }
}
```

**Benefits of shared library**:
- Reusable across multiple pipelines
- Single source of truth for deployment logic
- Easy to update deployment strategy
- Reduces code duplication

---

## Performance Optimization Tips

### 1. Parallel Test Execution

```groovy
./gradlew test --parallel
```

Runs independent test classes in parallel, reducing total test time.

### 2. Gradle Build Cache

```bash
# Enable build cache
export GRADLE_USER_HOME=~/.gradle
gradle build --build-cache
```

Reuses previous build outputs for unchanged modules.

### 3. Docker Layer Caching

Multi-stage Dockerfile leverages Docker layer caching:
- Dependencies layer: Cached until `build.gradle.kts` changes
- Source code layer: Changes frequently, invalidates previous

### 4. Sonar Skip for Non-Code Changes

```bash
# Skip SonarQube if only docs changed
git diff HEAD~1 --quiet -- application/ || \
    ./gradlew sonar
```

---

## Debugging Pipelines

### 1. View Console Output

Each pipeline stage logs to console - scroll up to find errors.

### 2. Check Workspace

Jenkins saves workspace - examine files if build artifact issues.

### 3. Replay Pipeline

Jenkins allows replaying failed pipelines with different parameters for testing.

### 4. Enable Debug Logging

```groovy
sh 'set -x'  // Enable bash trace mode
```

Shows every command executed.

---

## Next Steps

- Review actual Jenkinsfile code in repository
- Set up Jenkins and create pipeline jobs
- Run pipelines and observe behavior
- Customize for your environment

---

This guide provides the conceptual understanding. Next, implement based on your infrastructure and requirements.
