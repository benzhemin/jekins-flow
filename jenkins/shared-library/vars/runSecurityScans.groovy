/*
 * Shared Library: Run Security Scans
 *
 * Centralized security scanning logic for reuse across pipelines
 * Performs:
 * - SonarQube static analysis
 * - OWASP Dependency-Check
 * - Container image scanning with Trivy
 *
 * Usage:
 * @Library('jenkins-shared-library') _
 * runSecurityScans(
 *     scanType: 'all',
 *     sonarProjectKey: 'myapp',
 *     failOnCritical: true
 * )
 */

def call(Map config) {
    def scanType = config.scanType ?: 'all'
    def sonarProjectKey = config.sonarProjectKey ?: 'default'
    def failOnCritical = config.failOnCritical ?: false
    def sonarHostUrl = config.sonarHostUrl ?: 'http://sonarqube:9000'

    echo "====== Running Security Scans ======"
    echo "Scan Type: ${scanType}"

    try {
        if (scanType in ['all', 'sast']) {
            runSonarQubeAnalysis(sonarProjectKey, sonarHostUrl)
        }

        if (scanType in ['all', 'dependencies']) {
            runDependencyCheck()
        }

        if (scanType in ['all', 'container']) {
            def imageName = config.imageName ?: 'springboot-app:latest'
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

private def runSonarQubeAnalysis(String projectKey, String sonarHostUrl) {
    echo "Running SonarQube analysis..."
    sh '''
        ./gradlew sonar \
            -Dsonar.host.url=${sonarHostUrl} \
            -Dsonar.projectKey=${projectKey}
    '''
}

private def runDependencyCheck() {
    echo "Running OWASP Dependency-Check..."
    sh '''
        ./gradlew dependencyCheck || true

        if [ -f build/reports/dependency-check-report.html ]; then
            echo "✓ Dependency-Check report generated"
        fi
    '''
}

private def runContainerScan(String imageName, Boolean failOnCritical) {
    echo "Running container scan with Trivy..."
    sh '''
        apk add --no-cache curl

        # Install Trivy
        if ! command -v trivy &> /dev/null; then
            echo "Installing Trivy..."
            wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | apt-key add -
            echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | tee -a /etc/apt/sources.list.d/trivy.list
            apt-get update && apt-get install -y trivy
        fi

        # Run scan
        trivy image \
            --severity CRITICAL,HIGH \
            --format json \
            --output trivy-report.json \
            ${imageName}

        # Check results
        CRITICAL=$(grep -c '"Severity": "CRITICAL"' trivy-report.json || echo "0")
        echo "Critical vulnerabilities: $CRITICAL"

        if [ "$CRITICAL" -gt 0 ] && [ "${failOnCritical}" == "true" ]; then
            exit 1
        fi
    '''
}
