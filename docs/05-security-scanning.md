# Security Scanning - From Concepts to Implementation

## Table of Contents
1. [Security Concepts](#security-concepts)
2. [Threat Model](#threat-model)
3. [SonarQube - SAST (Static Analysis)](#sonarqube---sast-static-analysis)
4. [OWASP Dependency-Check - Supply Chain](#owasp-dependency-check---supply-chain)
5. [Trivy - Container Scanning](#trivy---container-scanning)
6. [Integration into Pipeline](#integration-into-pipeline)
7. [Interpreting Results & Fixing Issues](#interpreting-results--fixing-issues)

---

## Security Concepts

### DevSecOps: Security in Development

**Definition**: Integrating security testing into every step of the development pipeline

**Traditional Approach** (Waterfall Security):
```
Code → Build → Test → Deploy → Security Audit (6 months later!)

Problems:
- Vulnerabilities found too late
- Expensive to fix (already in production)
- Takes months to remediate
- Creates bottlenecks
```

**Modern Approach** (DevSecOps):
```
Code → [Security Scan] → Build → [Security Scan] → Test → [Security Scan] → Deploy

Benefits:
- Issues found immediately (minutes)
- Cheap to fix (just written code)
- Developers fix their own code
- Continuous security feedback
- Prevents bad code from reaching production
```

### Security Scanning Types

```
┌────────────────────────────────────────────────────────┐
│            Security Scanning Landscape                 │
├────────────────────────────────────────────────────────┤
│                                                        │
│  SAST (Static Application Security Testing)           │
│  └─ Analyzes source code without running it           │
│     Examples: SonarQube, Checkmarx, Fortify           │
│     Finds: Bugs, code smells, vulnerabilities         │
│                                                        │
│  SCA (Software Composition Analysis)                  │
│  └─ Analyzes dependencies for known CVEs              │
│     Examples: OWASP Dependency-Check, Snyk, WhiteSource
│     Finds: Vulnerable libraries, outdated packages    │
│                                                        │
│  DAST (Dynamic Application Security Testing)          │
│  └─ Tests running application like attacker           │
│     Examples: OWASP ZAP, Burp Suite                   │
│     Finds: Runtime vulnerabilities, auth issues       │
│                                                        │
│  Container Scanning                                   │
│  └─ Analyzes Docker images for vulnerabilities        │
│     Examples: Trivy, Grype, Clair                     │
│     Finds: OS package CVEs, app dependency CVEs       │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### CVE (Common Vulnerabilities and Exposures)

**What**: Identifier for known security vulnerabilities

**Example**: CVE-2024-12345

```
CVE-2024-12345
├─ CVE: Unique identifier
├─ 2024: Year discovered
├─ 12345: Sequential number
│
└─ References:
   ├─ NVD (National Vulnerability Database): nvd.nist.gov
   ├─ Details what software is affected
   ├─ CVSS score (0-10, higher = more severe)
   ├─ Recommendations for fixes
   └─ Available patches
```

**Severity Levels**:
```
CRITICAL (9.0-10.0): Fix immediately, blocks production
HIGH     (7.0-8.9):  Fix before next release
MEDIUM   (4.0-6.9):  Fix in regular maintenance
LOW      (0.1-3.9):  Monitor and fix when convenient
```

---

## Threat Model

### What We're Protecting Against

```
┌─────────────────────────────────────────────────────────┐
│           Threat Categories                            │
└─────────────────────────────────────────────────────────┘

1. CODE VULNERABILITIES
   ├─ SQL Injection
   │  └─ Attacker inputs: '  OR  '1'='1'
   │  └─ Result: Database exposed
   │  └─ Prevention: Parameterized queries
   │
   ├─ Cross-Site Scripting (XSS)
   │  └─ Attacker inputs: <script>alert('hacked')</script>
   │  └─ Result: User session stolen
   │  └─ Prevention: Input sanitization, output encoding
   │
   ├─ Authentication Bypass
   │  └─ Missing authorization checks
   │  └─ Result: Unauthorized access
   │  └─ Prevention: Proper RBAC implementation
   │
   └─ Other: Buffer overflows, race conditions, etc.

2. DEPENDENCY VULNERABILITIES
   ├─ Outdated Library with Known CVE
   │  └─ Example: log4j 2.14.1 (remote code execution)
   │  └─ Fix: Update to 2.17.0+
   │
   ├─ Transitive Dependency Issues
   │  └─ Your code uses: library-a v1.0
   │  └─ library-a uses: vulnerable-lib v2.0
   │  └─ You inherit the vulnerability!
   │
   └─ License Violations
      └─ Using GPL library in proprietary code (legal issue)

3. CONTAINER/DEPLOYMENT ISSUES
   ├─ Vulnerable Base Image
   │  └─ Ubuntu:18.04 (outdated, many CVEs)
   │  └─ Fix: Use Ubuntu:22.04 or Alpine (minimal)
   │
   ├─ Running as Root
   │  └─ Vulnerability in app = attacker is root
   │  └─ Fix: Non-root user (appuser)
   │
   └─ Exposed Secrets
      └─ AWS keys in environment variables
      └─ Fix: Use secret management
```

---

## SonarQube - SAST (Static Analysis)

### Concept: Analyzing Code Without Running It

**How It Works**:

```
Your Source Code
      ↓
[SonarQube Scanner]
      ├─ Parses syntax
      ├─ Builds abstract syntax tree (AST)
      ├─ Applies rules
      └─ Generates report
      ↓
Vulnerabilities Found (by analyzing code structure)
```

### SonarQube Rules

SonarQube has 500+ rules analyzing code for:

| Category | Examples | Severity |
|----------|----------|----------|
| **Bugs** | Null pointer, infinite loop, unreachable code | High |
| **Vulnerabilities** | SQL injection, XSS, auth bypass | Critical |
| **Code Smells** | Duplicate code, long methods, unclear naming | Medium |
| **Maintainability** | High cyclomatic complexity | Low |

### Example: SQL Injection Detection

**Bad Code SonarQube Detects**:
```java
// ❌ VULNERABILITY: SQL Injection
String query = "SELECT * FROM users WHERE id = " + userId;
ResultSet rs = stmt.executeQuery(query);

// Why it's vulnerable:
// If userId = "1 OR 1=1", query becomes: "SELECT * FROM users WHERE id = 1 OR 1=1"
// This returns ALL users, not just one!
```

**SonarQube reports**: "SQL Injection - severity: CRITICAL"

**Good Code SonarQube Approves**:
```java
// ✅ SAFE: Parameterized query
String query = "SELECT * FROM users WHERE id = ?";
PreparedStatement stmt = connection.prepareStatement(query);
stmt.setLong(1, userId);
ResultSet rs = stmt.executeQuery();

// Why it's safe:
// userId is treated as data, not SQL code
// Even if userId = "1 OR 1=1", it's treated as literal string
```

### Setting Up SonarQube

#### Installation (Docker)

```bash
# Run SonarQube container
docker run -d \
  --name sonarqube \
  -p 9000:9000 \
  sonarqube:latest

# Access at http://localhost:9000
# Default: admin / admin
```

#### Configuration in Jenkins Pipeline

**File: `application/build.gradle.kts`**

```kotlin
plugins {
    id("org.sonarqube") version "4.4.1.3373"
}

sonarqube {
    properties {
        // Unique identifier
        property("sonar.projectKey", "springboot-cicd-platform")
        property("sonar.projectName", "Spring Boot CI/CD Platform")
        property("sonar.projectVersion", project.version)

        // Source code
        property("sonar.sources", "src/main/java")
        property("sonar.tests", "src/test/java")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.java.source", "17")

        // Server
        property("sonar.host.url", "http://sonarqube:9000")
        property("sonar.login", "your-token")
    }
}
```

**File: `jenkins/Jenkinsfile.ci`**

```groovy
stage('Code Quality') {
    steps {
        script {
            sh '''
                ./gradlew sonar \
                    -Dsonar.host.url=${SONAR_HOST_URL} \
                    -Dsonar.login=${SONAR_LOGIN}
            '''
        }
    }
}
```

### Quality Gate: Enforcement

**Concept**: Rules that MUST pass for pipeline to continue

**Example Quality Gate Configuration**:

```
SonarQube → Administration → Quality Gates

Rules that fail the gate:
- Bugs > 0             ❌ FAIL
- Vulnerabilities > 0  ❌ FAIL
- Coverage < 80%       ❌ FAIL
- Duplicated lines > 3% ❌ FAIL

Example:
✅ PASS: 0 bugs, 0 vulnerabilities, 85% coverage, 2% duplication
❌ FAIL: 2 bugs, 1 CRITICAL vulnerability, 70% coverage
```

**How It's Checked in Pipeline**:

```groovy
stage('Quality Gate') {
    steps {
        script {
            // Wait for SonarQube to complete analysis
            timeout(time: 5, unit: 'MINUTES') {
                // Call SonarQube API
                def response = sh(script: '''
                    curl -u ${SONAR_LOGIN}: \
                      ${SONAR_HOST_URL}/api/qualitygates/project_status \
                      ?projectKey=springboot-cicd-platform
                ''', returnStdout: true).trim()

                // Parse result
                if (response.contains('"status":"ERROR"')) {
                    error("Quality gate failed!")
                }
            }
        }
    }
}
```

---

## OWASP Dependency-Check - Supply Chain

### Concept: Finding Vulnerabilities in Dependencies

**Problem Scenario**:

```
Your Code (secure)
    ↓
Uses Library A (seems fine)
    ↓
Library A Uses Library B v1.0
    ↓
Library B v1.0 has CVE-2024-XXXXX
    ↓
YOUR APPLICATION IS VULNERABLE!

Because you depend on Library A, which depends on vulnerable Library B
```

### How It Works

```
Your Project
    ├─ pom.xml / build.gradle (dependency declarations)
    │
    └─ [OWASP Dependency-Check Scanner]
        ├─ Extract all dependencies
        ├─ Check against NVD (National Vulnerability Database)
        ├─ Report vulnerabilities found
        └─ Generate detailed report
```

### Configuration

**File: `application/build.gradle.kts`**

```kotlin
plugins {
    id("org.owasp.dependencycheck") version "9.0.9"
}

dependencyCheck {
    // Types of analysis
    analyzers = listOf("GRADLE")

    // Output formats
    format = "ALL"  // Generates HTML, JSON, XML, CSV

    // Fail on high severity
    failOnError = false  // Log but don't stop build (checked separately)

    // Suppress false positives
    suppressionFile = "security/owasp/suppressions.xml"

    // Report generation
    reportDir = "build/reports/dependency-check"
}
```

### Suppression File (False Positives)

**File: `security/owasp/suppressions.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">

    <!-- Suppress false positives here -->
    <!-- Example: Library flagged but doesn't affect us -->

    <suppress until="2025-06-30">
        <notes>
            CVE-2024-12345: This CVE affects a code path we don't use.
            Library is used for X, CVE is in unused feature Y.
            Scheduled for update in Q2 2025.
        </notes>
        <cve>CVE-2024-12345</cve>
    </suppress>

    <!-- OR suppress by library -->
    <suppress>
        <notes>
            Suppress all CVEs for specific library version
            Reason: Internal library, audited separately
        </notes>
        <packageUrl regex="true">
            pkg:maven/com\.example/internal-lib@.*
        </packageUrl>
    </suppress>

</suppressions>
```

**Important**: Document WHY you're suppressing, with a date to reassess

### Running the Check

```bash
# In CI pipeline
./gradlew dependencyCheck

# Generates reports:
# - build/reports/dependency-check/dependency-check-report.html (human readable)
# - build/reports/dependency-check/dependency-check-report.json (machine readable)
```

### Interpreting Results

**Example Report Output**:

```
Dependency Check Report
===========================================

CRITICAL VULNERABILITIES (0)
- None found ✓

HIGH VULNERABILITIES (1)
├─ log4j-core 2.14.0
│  ├─ CVE-2021-44228 (CRITICAL)
│  ├─ Severity: 10.0 (maximum)
│  ├─ Description: RCE (Remote Code Execution)
│  ├─ Fixed in: 2.17.0
│  └─ Recommendation: Update immediately
│
└─ Jackson Databind 2.10.0
   ├─ Multiple CVEs
   └─ Recommendation: Update to 2.14.0+

MEDIUM VULNERABILITIES (3)
├─ ...

LOW VULNERABILITIES (8)
├─ ...
```

**Action Plan for HIGH CVE**:

```
1. IMMEDIATE (next commit):
   Update log4j to 2.17.0+
   gradle dependencies --update

2. TEST (before pushing):
   ./gradlew test
   Verify no dependency breaks

3. VERIFY:
   Run dependency check again
   Confirm CVE is gone

4. COMMIT:
   git commit -m "Security: Update log4j to 2.17.0 (CVE-2021-44228)"
```

---

## Trivy - Container Scanning

### Concept: Scanning Docker Images for Vulnerabilities

**Problem**: Even if your code is secure, your Docker base image might have OS vulnerabilities

**Example**:

```
Your Application Code
    ↓ (packaged in)
Docker Image built from: ubuntu:18.04
    ├─ OpenSSL 1.1.0 (vulnerable)
    ├─ curl 7.58 (vulnerable)
    ├─ glibc 2.27 (vulnerable)
    └─ 200+ other vulnerable packages!

Even though your code is perfect, the image has vulnerabilities!
```

### How Trivy Works

```
Docker Image
    ↓
[Trivy Scanner]
    ├─ Unpacks image layers
    ├─ Scans OS packages (via APT, RPM, etc databases)
    ├─ Scans application dependencies (NPM, PIP, Maven, etc)
    └─ Checks against vulnerability databases
        ├─ NVD (National Vulnerability Database)
        ├─ Security advisories
        └─ Vendor-specific databases
    ↓
Report: Vulnerabilities Found
```

### Installation

```bash
# Install Trivy
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin

# Or with package manager
apt-get install trivy

# Verify
trivy --version
```

### Running Trivy in Pipeline

**File: `jenkins/Jenkinsfile.build`**

```groovy
stage('Container Scan') {
    steps {
        script {
            sh '''
                # Scan image
                trivy image \
                    --severity CRITICAL,HIGH,MEDIUM \
                    --format json \
                    --output trivy-report.json \
                    ${IMAGE_REPO}:${IMAGE_TAG}

                # Generate human-readable report
                trivy image \
                    --severity CRITICAL,HIGH,MEDIUM \
                    --format table \
                    ${IMAGE_REPO}:${IMAGE_TAG}

                # Count vulnerabilities (exit code: 1 if vulnerabilities found)
                CRITICAL=$(grep -c '"Severity":"CRITICAL"' trivy-report.json || echo "0")
                HIGH=$(grep -c '"Severity":"HIGH"' trivy-report.json || echo "0")

                echo "Critical: $CRITICAL, High: $HIGH"

                # Fail if CRITICAL vulnerabilities found
                if [ "$CRITICAL" -gt 0 ]; then
                    echo "❌ CRITICAL vulnerabilities found!"
                    exit 1
                fi
            '''
        }
    }
}
```

### Interpreting Trivy Results

**Example Output**:

```
2024-01-15T10:30:00Z    INFO    Scanning image...
2024-01-15T10:30:05Z    INFO    Analyzing layers
2024-01-15T10:30:10Z    INFO    Scanning the file system

springboot-app:v1.0.0 (alpine 3.18.0)
=====================================
Total: 5 vulnerabilities (1 CRITICAL, 2 HIGH, 2 MEDIUM)

CRITICAL (1)
├─ libssl3
│  ├─ CVE-2023-5363
│  ├─ Severity: CRITICAL
│  ├─ Fixed in: 3.0.8 or later
│  └─ Recommendation: Update Alpine to 3.18.1+
│
└─ [in alpine-base-image]

HIGH (2)
├─ curl
│  ├─ CVE-2023-46604
│  ├─ Severity: HIGH
│  └─ Fixed in: curl 8.4.0+
│
└─ openssl
    └─ ...

MEDIUM (2)
├─ ...
```

### Fixing Container Vulnerabilities

**Option 1: Update Base Image** (Preferred)

```dockerfile
# Before (vulnerable)
FROM ubuntu:18.04  # Outdated!

# After (patched)
FROM ubuntu:22.04  # LTS, regularly patched

# Even better: Alpine (minimal surface area)
FROM alpine:3.18
```

**Why Alpine**:
```
Image size comparison:
- Ubuntu 22.04: 77 MB
- Debian 12: 124 MB
- Alpine 3.18: 7 MB (10x smaller!)

Smaller = Fewer packages = Fewer vulnerabilities
```

**Option 2: Apply Updates Inside Image**

```dockerfile
FROM ubuntu:22.04

# Update all packages
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy application...
COPY --from=builder /app/app.jar /app/

RUN chmod +x /app/app.jar
CMD ["java", "-jar", "/app/app.jar"]
```

---

## Integration into Pipeline

### Complete Security Scanning Pipeline

```groovy
pipeline {
    stages {
        // ... Build and Test ...

        stage('Security Scanning') {
            parallel {
                // SAST: Analyze source code
                stage('SonarQube') {
                    steps {
                        sh './gradlew sonar ...'
                    }
                }

                // SCA: Check dependencies
                stage('Dependency Check') {
                    steps {
                        sh './gradlew dependencyCheck ...'
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                // Check SonarQube results
                // Check dependency check results
                // FAIL if critical issues found
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build ...'
            }
        }

        // Container Scanning: Scan Docker image
        stage('Container Scan') {
            steps {
                sh 'trivy image ${IMAGE_TAG} ...'
            }
        }

        // Final gate: Ensure container is safe
        stage('Container Quality Gate') {
            steps {
                // Check Trivy results
                // FAIL if CRITICAL vulnerabilities
            }
        }

        stage('Deploy') {
            // Only reached if all security gates passed!
        }
    }
}
```

---

## Interpreting Results & Fixing Issues

### Vulnerability Lifecycle

```
1. VULNERABILITY REPORTED
   ├─ CVE published
   ├─ Severity score assigned
   └─ Patch/workaround announced

2. DETECTION
   ├─ SonarQube finds code issue
   ├─ OWASP finds vulnerable dependency
   └─ Trivy finds OS package CVE

3. ANALYSIS (Your Job)
   ├─ Is it real? (false positive?)
   ├─ Does it affect us? (unused code?)
   ├─ What's the severity?
   └─ How urgent is the fix?

4. REMEDIATION
   ├─ Update dependency
   ├─ Update base image
   ├─ Apply patch
   └─ OR suppress if not applicable

5. VERIFICATION
   ├─ Re-run security scan
   ├─ Confirm vulnerability gone
   ├─ Run tests (ensure nothing broke)
   └─ Deploy updated version

6. MONITORING
   ├─ Watch for new CVEs
   ├─ Set up alerts
   └─ Plan regular updates
```

### Fixing a SonarQube Issue

**Example: SQL Injection Detected**

```
SonarQube Report:
Location: UserRepository.java:45
Severity: CRITICAL
Issue: Potential SQL injection

Line 45: String query = "SELECT * FROM users WHERE name = '" + name + "'";
```

**Fix Steps**:

```java
// BEFORE (vulnerable)
public List<User> findByName(String name) {
    String query = "SELECT * FROM users WHERE name = '" + name + "'";
    return jdbcTemplate.query(query, new UserRowMapper());
}

// AFTER (secure)
public List<User> findByName(String name) {
    String query = "SELECT * FROM users WHERE name = ?";
    return jdbcTemplate.query(
        query,
        new UserRowMapper(),
        name  // Parameter is safe
    );
}
```

**Commit**:
```bash
git commit -m "Security: Fix SQL injection in findByName - CVE prevention"
```

**Verify**:
```bash
./gradlew sonar  # Re-run scan
# Confirm: "SQL Injection: RESOLVED"
```

### Fixing a Dependency Vulnerability

**Example: Log4j CVE**

```
OWASP Report:
Library: log4j-core 2.14.0
CVE: CVE-2021-44228
Severity: CRITICAL
Description: Remote Code Execution
```

**Fix Steps**:

```gradle
// BEFORE
dependencies {
    implementation 'org.apache.logging.log4j:log4j-core:2.14.0'  // Vulnerable!
}

// AFTER
dependencies {
    implementation 'org.apache.logging.log4j:log4j-core:2.17.0'  // Patched!
}
```

**Verify**:
```bash
./gradlew dependencyCheck  # Re-run scan
# Confirm: "log4j-core: No vulnerabilities found"
```

**Commit**:
```bash
git commit -m "Security: Update log4j-core 2.14.0→2.17.0 (CVE-2021-44228)"
```

### Suppressing a False Positive

**Scenario**: SonarQube flags issue that's not actually a problem

```
SonarQube Report:
Issue: Potential SQL injection in user search
Location: UserService.java:100
Actual: False positive - parameter is validated and parameterized
```

**Suppress in Code**:

```java
@SuppressWarnings("java:S2068")  // SQL Injection warning key
public List<User> searchUsers(String query) {
    // This uses parameterized query, not vulnerable
    return repository.search(query);
}
```

**OR Suppress in Quality Profile**:

```
SonarQube → Quality Profiles → Edit → Deactivate Rule
(Only for false positives across many files)
```

---

## Summary

**Security Scanning Checklist**:

- [x] SAST (SonarQube) - Analyzes code for vulnerabilities
- [x] SCA (OWASP) - Checks dependencies for CVEs
- [x] Container (Trivy) - Scans Docker images
- [x] Quality Gates - Enforces policies
- [x] Continuous Monitoring - Checks on every build
- [x] Remediation Process - Fix vulnerabilities quickly
- [x] Documentation - Track all suppressions and why

**Key Principles**:

1. ✅ **Fail Early** - Catch vulnerabilities in development, not production
2. ✅ **Automate** - Security checks in pipeline, not manual
3. ✅ **Be Pragmatic** - Suppress false positives, prioritize real issues
4. ✅ **Track Changes** - Document why vulnerabilities were suppressed
5. ✅ **Continuous Updates** - Regularly update dependencies and base images

**Time to Fix**:
```
CRITICAL:  This week (immediate)
HIGH:      This sprint (1-2 weeks)
MEDIUM:    Next quarter (monthly maintenance)
LOW:       Annual review (no urgency)
```
