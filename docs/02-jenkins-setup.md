# Jenkins Setup - Complete Guide from Concept to Implementation

## Table of Contents
1. [Conceptual Foundation](#conceptual-foundation)
2. [Jenkins Architecture](#jenkins-architecture)
3. [Installation Methods](#installation-methods)
4. [Configuration & Setup](#configuration--setup)
5. [Creating Pipeline Jobs](#creating-pipeline-jobs)
6. [Plugin Installation](#plugin-installation)
7. [Credentials Management](#credentials-management)
8. [Troubleshooting](#troubleshooting)

---

## Conceptual Foundation

### What is Jenkins?

**Definition**: Jenkins is an open-source automation server that orchestrates complex build, test, and deployment workflows.

**Key Concept - Why Jenkins?**

Traditional software development pipeline:
```
Developer commits code → Manual testing → Manual build → Manual deploy
                            ↓ (Error-prone, slow, inconsistent)
                     Takes weeks, many manual steps
```

With Jenkins (CI/CD):
```
Developer commits code → [Automatic] → Build → Test → Security Scan → Deploy
                                           ↓ (Minutes, consistent, reliable)
                     Production deployment in 30 minutes
```

### Core Concepts

#### 1. **Pipeline**
Definition: A sequence of automated steps that code goes through

```
Pipeline = Build Environment + Stages + Post-Actions

Example:
[Checkout Code] → [Compile] → [Test] → [Security Scan] → [Deploy]
```

#### 2. **Stage**
A logical section of a pipeline

```
Stage 1: Checkout
  - Clone repository from Git
  - Verify branch

Stage 2: Build
  - Compile Java code
  - Download dependencies

Stage 3: Test
  - Run unit tests
  - Collect test results
```

#### 3. **Agent**
The machine/container where pipeline runs

```
Agent options:
- any              : Run on any available executor
- none             : No default executor (specify per stage)
- docker           : Run in Docker container
- label('linux')   : Run on agent with 'linux' label
- kubernetes       : Run in Kubernetes pod
```

#### 4. **Post Actions**
Actions that run after pipeline completes (regardless of result)

```
post {
    always {
        // Always runs - cleanup
    }
    success {
        // Runs if pipeline passed
    }
    failure {
        // Runs if pipeline failed
    }
}
```

#### 5. **Declarative vs Scripted Pipelines**

**Declarative Pipeline** (Recommended - Structured):
```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'make'
            }
        }
    }
}
```
- Structured, easier to read
- Built-in validation
- Better UI integration
- Recommended for most use cases

**Scripted Pipeline** (Advanced - Flexible):
```groovy
node {
    stage('Build') {
        sh 'make'
    }
}
```
- More flexible
- More complex
- Better for complex conditional logic

---

## Jenkins Architecture

### Components Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Jenkins Master (Controller)              │
│  - Receives webhook notifications                           │
│  - Schedules build jobs                                     │
│  - Manages plugins                                          │
│  - Stores configuration                                     │
│  - Web UI for configuration and monitoring                  │
└─────────────────────────────────────────────────────────────┘
              ↓ (Delegate work)
┌─────────────────────────────────────────────────────────────┐
│                    Jenkins Agents (Executors)               │
│  - Execute actual build steps                               │
│  - Can be Docker containers                                 │
│  - Can be separate machines                                 │
│  - Communicate with controller via JNLP                     │
└─────────────────────────────────────────────────────────────┘
              ↓ (Interact with external systems)
┌─────────────────────────────────────────────────────────────┐
│              External Systems                               │
│  - Git (source control)                                     │
│  - Docker (containerization)                                │
│  - Kubernetes (orchestration)                               │
│  - SonarQube (code quality)                                 │
│  - Slack/Email (notifications)                              │
└─────────────────────────────────────────────────────────────┘
```

### Jenkins Home Directory

Location where Jenkins stores all data:

```
$JENKINS_HOME/
├── config.xml              (Global Jenkins configuration)
├── jobs/                   (Job definitions)
│   ├── springboot-cicd-platform-ci/
│   │   └── config.xml      (CI job configuration)
│   ├── springboot-cicd-platform-build/
│   │   └── config.xml      (Build job configuration)
│   └── ...
├── plugins/                (Installed plugins)
│   ├── pipeline-model-definition.jpi
│   ├── git.jpi
│   ├── docker-plugin.jpi
│   └── ...
├── users/                  (User accounts)
│   └── admin/
│       ├── config.xml
│       └── secrets/
├── secrets/                (Encrypted credentials)
│   ├── master.key
│   └── hudson.util.Secret
└── logs/                   (Build logs)
    ├── all.log
    ├── agents/
    └── jobs/
```

**Key Understanding**: Jenkins stores everything in this directory - configuration, credentials, logs. Backing this up = backing up your entire Jenkins system.

---

## Installation Methods

### Method 1: Docker (Recommended for Learning)

**Concept**: Run Jenkins in a Docker container - isolated, reproducible, easy to cleanup.

#### Step-by-Step Installation

**Prerequisites Check**:
```bash
# Verify Docker is installed and running
docker --version
docker ps  # Should show running containers (or empty list)
```

**Create Jenkins Volume** (persistent storage):
```bash
# This volume persists data even if container stops
docker volume create jenkins_home

# Why separate volume?
# - Container can be deleted/recreated without losing data
# - Allows easy backup of Jenkins configuration
# - Separates application from state
```

**Run Jenkins Container**:
```bash
docker run \
  -d \
  --name jenkins \
  --restart=always \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e JAVA_OPTS="-Xmx2g" \
  jenkins/jenkins:lts
```

**Explanation of each parameter**:

| Parameter | Purpose | Why? |
|-----------|---------|------|
| `-d` | Run in background (detached) | Frees up terminal |
| `--name jenkins` | Container name | Easy reference |
| `--restart=always` | Auto-restart if crashes | High availability |
| `-p 8080:8080` | Port mapping: host→container | Access Jenkins at http://localhost:8080 |
| `-p 50000:50000` | Agent communication port | Agents connect on this port |
| `-v jenkins_home:/var/jenkins_home` | Volume mount | Persistent storage |
| `-v /var/run/docker.sock:/var/run/docker.sock` | Docker socket mount | Container can run Docker commands |
| `-e JAVA_OPTS="-Xmx2g"` | Set Java heap size | Prevents out-of-memory errors |
| `jenkins/jenkins:lts` | Image name:tag | LTS = Long Term Support |

**Verify Installation**:
```bash
# Check if container is running
docker ps | grep jenkins

# View logs
docker logs -f jenkins

# Wait for this message:
# "*************************************************************
#  Jenkins initial setup is required. An admin user has been created and..."
```

**Access Jenkins**:
- Open browser: http://localhost:8080
- Wait 1-2 minutes for full startup

**Get Initial Admin Password**:
```bash
# Option 1: From logs
docker logs jenkins | grep -A5 "Jenkins initial setup"

# Option 2: From container
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword

# Output example: a1b2c3d4e5f6g7h8i9j0
```

---

### Method 2: System Package (Production)

**When to use**: Running Jenkins on permanent server

#### Ubuntu/Debian:

```bash
# Step 1: Add Jenkins repository
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

# Step 2: Update package list
sudo apt-get update

# Step 3: Install Jenkins
sudo apt-get install jenkins

# Step 4: Enable and start service
sudo systemctl enable jenkins
sudo systemctl start jenkins

# Step 5: Verify
sudo systemctl status jenkins

# Access at http://localhost:8080
```

#### CentOS/RHEL:

```bash
# Step 1: Add Jenkins repository
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo

# Step 2: Import GPG key
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key

# Step 3: Install Java (required)
sudo yum install java-17-amazon-corretto

# Step 4: Install Jenkins
sudo yum install jenkins

# Step 5: Enable and start
sudo systemctl enable jenkins
sudo systemctl start jenkins

# Verify
sudo systemctl status jenkins
```

#### macOS:

```bash
# Using Homebrew
brew install jenkins-lts

# Start Jenkins
brew services start jenkins-lts

# Access at http://localhost:8080

# View logs
tail -f /usr/local/var/log/jenkins/jenkins.log
```

---

### Method 3: Docker Compose (For Full Stack)

**Concept**: Define entire environment (Jenkins + PostgreSQL + etc) in one file

```yaml
version: '3.8'

services:
  jenkins:
    image: jenkins/jenkins:lts
    container_name: jenkins
    restart: always
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      JAVA_OPTS: "-Xmx2g"
    networks:
      - jenkins-network

  # Optional: PostgreSQL for plugins that need database
  postgres:
    image: postgres:15-alpine
    container_name: jenkins-db
    restart: always
    environment:
      POSTGRES_DB: jenkins
      POSTGRES_USER: jenkins
      POSTGRES_PASSWORD: jenkins-password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - jenkins-network

volumes:
  jenkins_home:
  postgres_data:

networks:
  jenkins-network:
    driver: bridge
```

**Usage**:
```bash
# Start everything
docker-compose up -d

# View logs
docker-compose logs -f jenkins

# Stop everything
docker-compose down

# Remove volumes (careful - deletes data!)
docker-compose down -v
```

---

## Configuration & Setup

### Initial Setup Wizard

When you first access http://localhost:8080:

#### Step 1: Unlock Jenkins

```
╔════════════════════════════════════════╗
║  Unlock Jenkins                        ║
║                                        ║
║  Jenkins is usually shut down by an    ║
║  administrator, or it failed to        ║
║  automatically restart.                ║
║                                        ║
║  [Paste initial admin password]       ║
║  [Continue]                            ║
╚════════════════════════════════════════╝
```

**Paste the password** you retrieved earlier.

#### Step 2: Customize Jenkins

Two options:

**Option A: Install Suggested Plugins** (Recommended for learning)
- Includes essential plugins
- Faster to get started
- Good for most teams

**Option B: Select Plugins to Install** (Advanced)
- Choose exactly what you need
- Faster startup
- Requires plugin knowledge

**For this project, use Option A**.

Plugins that will be installed:
- Pipeline (declarative and scripted)
- Git
- Credentials
- Email Extension
- JUnit
- etc.

Installation takes 5-10 minutes.

#### Step 3: Create First Admin User

```
╔════════════════════════════════════════╗
║  Create First Admin User               ║
║                                        ║
║  Username: admin                       ║
║  Password: ••••••••                    ║
║  Confirm: ••••••••                     ║
║  Full Name: Administrator              ║
║  Email: admin@example.com              ║
║                                        ║
║  [Save and Continue]                   ║
╚════════════════════════════════════════╝
```

**Important**: Save these credentials securely. This is the root account.

#### Step 4: Instance Configuration

```
╔════════════════════════════════════════╗
║  Jenkins URL                           ║
║                                        ║
║  http://localhost:8080/                ║
║                                        ║
║  [Save and Finish]                     ║
╚════════════════════════════════════════╝
```

This is the URL Jenkins uses to reference itself (important for webhooks).

### Post-Installation Configuration

#### 1. Configure System (Manage Jenkins → Configure System)

**Key Settings**:

- **System Message**: Add organizational header/notice
- **Jenkins Location**:
  - Jenkins URL: `http://your-domain:8080/` (for webhooks)
  - Admin email: `admin@company.com`

- **Git Configuration**:
  ```
  Name: Default
  Email: jenkins@company.com
  ```
  These are used when Jenkins commits/tags

- **Email Notification**:
  ```
  SMTP server: smtp.gmail.com
  SMTP port: 587
  Authentication: your-email@gmail.com / app-password
  ```

#### 2. Configure Credentials (Manage Jenkins → Manage Credentials)

**Why Credentials?**

Jenkins needs to authenticate with:
- Git repositories (SSH/HTTPS)
- Docker registries (username/password)
- Kubernetes clusters (kubeconfig)
- SonarQube servers (tokens)

**Credential Types**:

| Type | Use Case | Example |
|------|----------|---------|
| SSH Key | Git via SSH | `~/.ssh/id_rsa` |
| Username/Password | Registries, APIs | Docker Hub credentials |
| Secret text | API tokens | SonarQube token |
| Secret file | Kubeconfig | Kubernetes config file |
| Certificate | HTTPS, SSL | Client certificates |

**Example: Add Git SSH Key**

```
1. Manage Jenkins → Manage Credentials
2. Click "global" domain
3. Click "+ Add Credentials"
4. Kind: SSH Username with private key
5. Scope: Global
6. Username: git
7. Private Key: (paste content of ~/.ssh/id_rsa)
8. Passphrase: (if key is encrypted)
9. ID: github-ssh
10. Click "Create"
```

Now reference in Jenkinsfile:
```groovy
checkout([
    $class: 'GitSCM',
    branches: [[name: '*/main']],
    userRemoteConfigs: [[
        url: 'git@github.com:company/repo.git',
        credentialsId: 'github-ssh'
    ]]
])
```

---

## Creating Pipeline Jobs

### Creating Your First Job

#### Step 1: New Item

```
Jenkins Home → [+ New Item]
```

#### Step 2: Job Configuration

```
Job Name: springboot-cicd-platform-ci
Type: Pipeline
Description: "Build, test, and scan Spring Boot application"

[OK]
```

#### Step 3: Configure Pipeline

**General Tab**:
- Discard old builds:
  - Days to keep builds: 30
  - Max builds to keep: 50
  - Reason: Saves disk space, keeps build history short

**Build Triggers**:

Three options for when pipeline runs:

**Option 1: Poll SCM** (Check for changes periodically)
```
✓ Poll SCM
  Schedule: H/15 * * * *

  Meaning:
  H = Random hour minute (to distribute load)
  /15 = Every 15 minutes
  * * * * = Every day, every month, every weekday

  Result: Jenkins checks Git every 15 minutes for changes
```

**Option 2: GitHub Webhook** (Instant trigger on push)
```
1. GitHub → Settings → Webhooks
2. Add: http://your-jenkins:8080/github-webhook/
3. Events: Push events
4. Result: Push triggers build immediately (< 1 second)
```

**Option 3: Manual Trigger**
```
✓ Do not trigger automatically
(Start builds manually from Jenkins UI)
```

**Pipeline Tab**:

Choose where Jenkinsfile comes from:

**Option A: Pipeline script from SCM** (Recommended)
```
Definition: Pipeline script from SCM
SCM: Git
Repository URL: https://github.com/company/springboot-cicd-platform.git
Branch: */main
Script path: jenkins/Jenkinsfile.ci

Why this approach?
- Jenkinsfile versioned with code
- Changes tracked in Git
- Different branches can have different pipelines
- Easy to code review Jenkinsfile changes
```

**Option B: Pipeline script** (For testing)
```
Definition: Pipeline script
Script: [Paste entire Jenkinsfile content]

Why use this?
- Quick testing
- No Git integration needed
- Good for learning
```

---

## Plugin Installation

### Essential Plugins

**Why plugins matter**: Jenkins by itself is minimal. Plugins add functionality.

### Installation Methods

#### Method 1: Web UI (Easy)

```
1. Manage Jenkins → Manage Plugins
2. Available plugins tab
3. Search for plugin name
4. Check checkbox
5. [Install without restart] or [Install and restart]
```

#### Method 2: Jenkins Configuration as Code

```yaml
jenkins:
  securityRealm:
    saml:
      ...
  plugins:
    - timestamper
    - ansicolor
    - pipeline-model-definition
    - git
    - docker-plugin
    - kubernetes
```

### Core Plugins Needed

#### 1. **Pipeline Plugins**
```
- pipeline-model-definition (Declarative pipelines)
- pipeline-stage-view (Visualize stages)
- workflow-basic-steps (Basic pipeline steps)
```

**Why**: Enables Declarative Pipeline syntax

#### 2. **Source Control**
```
- git (Git integration)
- github (GitHub webhook)
- gitlab (GitLab webhook)
```

**Why**: Clone repositories, trigger on push

#### 3. **Docker**
```
- docker-plugin (Docker integration)
- docker-pipeline (Build Docker images in pipelines)
```

**Why**: Build containers, push to registry

#### 4. **Kubernetes**
```
- kubernetes (Kubernetes integration)
- kubernetes-cli (kubectl commands)
```

**Why**: Deploy to Kubernetes clusters

#### 5. **Code Quality**
```
- sonar (SonarQube integration)
- coverage (Code coverage reporting)
- junit (JUnit test result reporting)
```

**Why**: Integrate with security/quality tools

#### 6. **Notifications**
```
- email-ext (Advanced email)
- slack-plugin (Slack notifications)
- github (GitHub status updates)
```

**Why**: Notify team of build results

#### 7. **Utilities**
```
- ansicolor (Color console output)
- timestamper (Add timestamps to logs)
- ws-cleanup (Clean workspace)
```

**Why**: Better logs and workspace management

### Plugin Installation Script

Create `plugins.txt`:
```
pipeline-model-definition:1.7.3
git:5.2.0
docker-plugin:1.2.9
kubernetes:1.30.0
sonar:2.14.1
email-ext:2.97
junit:1.55
ansicolor:1.0.2
timestamper:1.17
slack-plugin:702.e5c3f5d0
```

Use with Docker:
```dockerfile
FROM jenkins/jenkins:lts

RUN jenkins-plugin-cli --plugins-file plugins.txt
```

---

## Credentials Management

### Credential Best Practices

#### 1. **Never Hardcode Credentials**

❌ **BAD**:
```groovy
sh 'docker login -u admin -p super-secret-password'
```

Problems:
- Visible in logs
- Visible in Jenkinsfile in Git
- Anyone with Git access sees password

✅ **GOOD**:
```groovy
withCredentials([
    usernamePassword(
        credentialsId: 'docker-hub',
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS'
    )
]) {
    sh 'docker login -u $DOCKER_USER -p $DOCKER_PASS'
}
```

How it works:
1. Jenkins stores password encrypted
2. Only makes available during build
3. Password NOT logged to console

#### 2. **Credential Scopes**

```
Global - Available to all jobs (usually)
System - Available only to Jenkins itself
Project - Available only to that job
```

For this project: Use **Global** scope for shared credentials

#### 3. **Credential Types for Each System**

**For Docker Registry**:
```
Type: Username with password
Username: your-docker-user
Password: docker-hub-token
ID: docker-hub-credentials

Use in pipeline:
withCredentials([
    usernamePassword(
        credentialsId: 'docker-hub-credentials',
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS'
    )
])
```

**For Git SSH**:
```
Type: SSH Username with private key
Username: git
Private Key: [paste ~/.ssh/id_rsa content]
Passphrase: [if key is encrypted]
ID: github-ssh-key

Use in pipeline:
checkout(scm) // Automatically uses credentials
```

**For Kubernetes**:
```
Type: Secret file
File: [kubeconfig contents]
ID: kubeconfig

Use in pipeline:
withCredentials([
    file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')
])
```

**For SonarQube**:
```
Type: Secret text
Secret: sonarqube-token
ID: sonar-token

Use in pipeline:
withCredentials([
    string(credentialsId: 'sonar-token', variable: 'SONAR_LOGIN')
])
```

#### 4. **Credential Rotation**

Best practice: Rotate credentials periodically

```
1. Generate new credential
2. Update in Jenkins
3. Test in non-critical job
4. Revoke old credential
5. Verify all jobs using new credential
```

---

## Troubleshooting

### Issue 1: Jenkins Won't Start

**Symptom**: http://localhost:8080 not accessible

**Diagnosis**:
```bash
# Check container status
docker ps | grep jenkins

# Check logs
docker logs jenkins | tail -50

# Look for errors like:
# - "Address already in use" → Port conflict
# - "OutOfMemoryError" → Not enough memory
# - "Java version" → Wrong Java version
```

**Solutions**:

**Port already in use**:
```bash
# Find what's using port 8080
lsof -i :8080

# Use different port
docker run -p 9090:8080 ...
```

**Out of memory**:
```bash
# Increase JAVA_OPTS
docker run -e JAVA_OPTS="-Xmx4g" ...
```

**Wrong Java version**:
```bash
# Jenkins LTS requires Java 11+
docker exec jenkins java -version
# Update image if needed: jenkins/jenkins:lts-jdk17
```

### Issue 2: Pipeline Can't Find Git Repository

**Symptom**:
```
ERROR: Error cloning remote repo 'origin'
hudson.plugins.git.GitException: Command "git fetch --tags" returned status code 128
```

**Diagnosis**:
```bash
# 1. Check Git credentials are configured
# Manage Jenkins → Manage Credentials

# 2. Verify SSH key works
docker exec jenkins ssh -T git@github.com
# Should output: Hi username! You've successfully authenticated...

# 3. Check Git plugin installed
# Manage Jenkins → Manage Plugins → search "git"
```

**Solution**:
```groovy
// Explicitly specify credentials
checkout([
    $class: 'GitSCM',
    branches: [[name: '*/main']],
    userRemoteConfigs: [[
        url: 'git@github.com:company/repo.git',
        credentialsId: 'github-ssh'  // Must match credential ID
    ]]
])
```

### Issue 3: Pipeline Can't Connect to Docker

**Symptom**:
```
docker: command not found
Cannot connect to Docker daemon
```

**Diagnosis**:
```bash
# Check if Docker socket is mounted
docker inspect jenkins | grep Mounts

# Should show: /var/run/docker.sock:/var/run/docker.sock
```

**Solution**: Remount volume when running Jenkins
```bash
docker run \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts
```

### Issue 4: Permission Denied Errors

**Symptom**:
```
docker: got permission denied while trying to connect
```

**Reason**: Jenkins runs as `jenkins` user, not `root`

**Solution - Add jenkins to docker group**:
```bash
# Inside Jenkins container
docker exec jenkins usermod -aG docker jenkins

# Or run Jenkins as root (NOT recommended)
docker run -e JENKINS_USER=root jenkins/jenkins:lts
```

### Issue 5: Out of Disk Space

**Symptom**:
```
No space left on device
Build artifacts can't be written
```

**Diagnosis**:
```bash
# Check disk usage
docker exec jenkins df -h

# Check workspace size
du -sh /var/jenkins_home/workspace/*/

# Check logs size
du -sh /var/jenkins_home/logs/
```

**Solution**: Clean up old builds
```groovy
// In job configuration
Discard old builds:
  - Days to keep builds: 7
  - Max # of builds to keep: 20
```

---

## Next Steps

1. **Install Jenkins** using Docker method
2. **Complete initial setup** wizard
3. **Add credentials** for Git and Docker
4. **Install plugins** from list above
5. **Create first pipeline job** pointing to jenkins/Jenkinsfile.ci
6. **Test with manual trigger**
7. **Configure webhook** for automatic triggering
8. **Review logs** to understand pipeline execution

---

**Key Takeaway**: Jenkins is the orchestration engine. Master these concepts:
- ✅ Pipelines are sequences of stages
- ✅ Agents run the work
- ✅ Credentials are encrypted and scoped
- ✅ Plugins add functionality
- ✅ Logs are your debugging tool
