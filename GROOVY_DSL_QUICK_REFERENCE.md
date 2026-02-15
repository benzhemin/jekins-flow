# Gradle Groovy DSL - Quick Reference Guide

## Overview
Complete migration from Kotlin DSL (`.kts`) to Groovy DSL (`.gradle`) with new version catalog system.

---

## File Structure

```
application/
├── build.gradle                           (Root - manages all modules)
├── settings.gradle                        (Project settings)
├── gradle/
│   └── libs.versions.toml                (Version catalog - centralized dependencies)
├── buildSrc/
│   ├── build.gradle                      (Convention plugins build config)
│   └── src/main/groovy/
│       ├── spring-conventions.gradle     (Reusable Spring Boot config)
│       └── java-library-conventions.gradle (Java library config)
├── common/
│   └── build.gradle                      (Shared utilities module)
├── domain/
│   └── build.gradle                      (Business logic module)
├── infrastructure/
│   └── build.gradle                      (Persistence/data access module)
└── api/
    └── build.gradle                      (REST API & main app module)
```

---

## Syntax Quick Reference

### 1. Basic Plugin Application

```groovy
// Simple plugin
plugins {
    id 'java'
}

// Plugin with version
plugins {
    id 'org.springframework.boot' version '3.2.2'
}

// Multiple plugins
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.2'
    id 'io.spring.dependency-management'
}
```

### 2. Dependencies

```groovy
dependencies {
    // Direct implementation (from version catalog)
    implementation libs.'spring-boot-starter-web'
    
    // With configuration
    compileOnly libs.lombok
    annotationProcessor libs.lombok
    
    // Project dependencies
    api project(':application:common')
    implementation project(':application:domain')
    
    // Test dependencies
    testImplementation libs.'mockito-core'
    testRuntimeOnly libs.'junit-jupiter-engine'
}
```

### 3. Version Catalog Usage

```groovy
// In libs.versions.toml:
[versions]
spring-boot = "3.2.2"
junit = "5.9.3"

[libraries]
spring-boot-starter-web = { group = "org.springframework.boot", 
                            name = "spring-boot-starter-web", 
                            version.ref = "spring-boot" }
junit-jupiter-api = { group = "org.junit.jupiter", 
                      name = "junit-jupiter-api", 
                      version.ref = "junit" }

// In build.gradle:
dependencies {
    implementation libs.'spring-boot-starter-web'
    testImplementation libs.'junit-jupiter-api'
}
```

### 4. Configuration Blocks

```groovy
// Java configuration
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// SonarQube configuration
sonarqube {
    properties {
        property 'sonar.projectKey', 'myapp'
        property 'sonar.projectName', 'My Application'
    }
}

// OWASP configuration
dependencyCheck {
    format = org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension.Format.ALL
    analyzers = ['GRADLE']
    failOnError = false
}
```

### 5. Task Configuration

```groovy
// Configure specific task type
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
    options.compilerArgs = ['-deprecation', '-Xlint:unchecked']
}

// Configure test execution
tasks.withType(Test) {
    useJUnitPlatform()
    testLogging {
        events 'passed', 'skipped', 'failed'
        showStandardStreams = false
    }
}

// Configure JAR creation
tasks.jar {
    manifest {
        attributes 'Main-Class': 'com.example.Application'
    }
}

// Configure boot JAR
tasks.bootJar {
    archiveFileName = 'app.jar'
}
```

### 6. Plugin Application in Subprojects

```groovy
// Apply to all subprojects
subprojects {
    apply plugin: 'java'
    apply plugin: 'org.springframework.boot'
    apply plugin: 'io.spring.dependency-management'
    
    // Configuration for all subprojects
    dependencies {
        implementation libs.lombok
        testImplementation libs.'mockito-core'
    }
}
```

---

## Each Module Explained

### `build.gradle` (Root)
- Applies plugins to all subprojects
- Defines shared dependencies
- Configures SonarQube and OWASP scanning
- Manages shared properties

**Key sections**:
```groovy
plugins { ... }           # Root-level plugins
subprojects { ... }       # Applied to all modules
sonarqube { ... }         # Code quality config
dependencyCheck { ... }   # Security scanning config
```

### `common/build.gradle`
- Shared DTOs, exceptions, utilities
- No dependencies on other modules
- Provides validation utilities

**Dependencies**: Only Spring Validation and Lombok

### `domain/build.gradle`
- Business logic and domain services
- Service interfaces and implementations
- Depends on `common`

**Dependencies**: common + Spring components + Lombok

### `infrastructure/build.gradle`
- Data persistence (JPA, repositories)
- Database configuration
- Depends on `domain` (but domain doesn't depend on infrastructure)

**Dependencies**: domain + Spring Data JPA + H2 Database

### `api/build.gradle`
- REST controllers and main application
- Entry point for the application
- Depends on all other modules

**Dependencies**: All modules + Spring Web + Spring Actuator

---

## Version Catalog Details

### Location
`gradle/libs.versions.toml`

### Structure

```toml
# Version definitions
[versions]
spring-boot = "3.2.2"
junit = "5.9.3"

# Library references
[libraries]
spring-boot-starter-web = { 
    group = "org.springframework.boot", 
    name = "spring-boot-starter-web", 
    version.ref = "spring-boot" 
}

# Bundles (groups of libraries)
[bundles]
spring-boot-common = ["spring-boot-starter-logging"]

# Plugin definitions
[plugins]
spring-boot = { 
    id = "org.springframework.boot", 
    version.ref = "spring-boot" 
}
```

### Benefits
- ✅ Single source of truth for all dependency versions
- ✅ IDE autocomplete support
- ✅ Easy to update all dependencies at once
- ✅ Prevents version mismatches across modules
- ✅ Better team collaboration (no "dependency conflicts")

---

## Convention Plugins

### What They Do
Reusable build logic applied across modules to reduce duplication.

### `spring-conventions.gradle`
Applied to Spring Boot modules. Provides:
- Java version compatibility (17)
- Compiler options (UTF-8, deprecation warnings)
- Test configuration (JUnit Platform)
- JAR manifest attributes

**Apply with**:
```groovy
plugins {
    id 'spring-conventions'
}
```

### `java-library-conventions.gradle`
Applied to library modules. Provides:
- Java library plugin (better API/implementation separation)
- Spring Boot conventions
- Reduced classpath bloat for consumers

**Apply with**:
```groovy
plugins {
    id 'java-library-conventions'
}
```

---

## Common Tasks

### Build Everything
```bash
./gradlew clean build
```

### Build Specific Module
```bash
./gradlew :application:api:build
```

### Run Tests
```bash
./gradlew test
```

### Run SonarQube Analysis
```bash
./gradlew sonarqube
```

### Run Security Scan
```bash
./gradlew dependencyCheck
```

### Run Application
```bash
./gradlew :application:api:bootRun
```

### View Dependencies
```bash
./gradlew dependencies
```

### Refresh Dependencies
```bash
./gradlew --refresh-dependencies
```

---

## Migration Checklist

- [x] Create gradle/libs.versions.toml with all versions
- [x] Convert settings.gradle.kts → settings.gradle
- [x] Convert build.gradle.kts → build.gradle (root)
- [x] Convert buildSrc/build.gradle.kts → buildSrc/build.gradle
- [x] Convert convention plugins to .gradle in src/main/groovy/
- [x] Convert all module build.gradle.kts → build.gradle
- [ ] Test build: `./gradlew clean build`
- [ ] Verify tests pass: `./gradlew test`
- [ ] Delete old .kts files (when verified)

---

## Troubleshooting

### Issue: "Could not find method libs"
**Solution**: Ensure `gradle/libs.versions.toml` exists and settings.gradle enables it.

### Issue: "Convention plugin not found"
**Solution**: Ensure convention plugins are in `buildSrc/src/main/groovy/` with `.gradle` extension.

### Issue: "Dependency not resolved"
**Solution**: Check spelling in `libs.versions.toml` - must match library name.

### Issue: "Module not found"
**Solution**: Ensure project paths use colon format: `project(':application:module-name')`

---

## Key Differences: Kotlin → Groovy

| Kotlin DSL | Groovy DSL | Notes |
|-----------|-----------|-------|
| `id("java")` | `id 'java'` | String style changed |
| `implementation(libs.foo)` | `implementation libs.foo` | Parentheses optional |
| `listOf(...)` | `[...]` | List syntax |
| `mapOf("a" to "b")` | `['a': 'b']` | Map syntax |
| `tasks.withType<JavaCompile>` | `tasks.withType(JavaCompile)` | Type parameter removed |
| `options.encoding = "UTF-8"` | `options.encoding = 'UTF-8'` | Single/double quotes same |
| `attribute("key" to "value")` | `attribute 'key': 'value'` | Arrow notation → colon |

---

## Resources

- **Official Gradle Docs**: https://docs.gradle.org/
- **Version Catalogs**: https://docs.gradle.org/current/userguide/platforms.html
- **Groovy Tutorial**: https://groovy-lang.org/
- **Spring Boot Gradle Plugin**: https://docs.spring.io/spring-boot/docs/current/gradle-plugin/

---

## Support

For questions about specific tasks:
1. Run with `--debug` flag: `./gradlew --debug build`
2. Check logs in `build/` directory
3. Verify settings in `gradle/libs.versions.toml`

**Happy building! 🚀**

