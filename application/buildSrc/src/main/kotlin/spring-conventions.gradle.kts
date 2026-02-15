/*
 * Spring Boot Convention Plugin
 * This plugin encapsulates common Spring Boot module configuration
 * Reduces boilerplate by centralizing build logic that all modules need
 *
 * Apply with: plugins { id("spring-conventions") }
 */

import org.gradle.api.JavaVersion

plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// Define Java compatibility settings
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Configure compiler options
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-deprecation", "-Xlint:unchecked"))
}

// Configure test execution
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}

// Configure JAR manifest
tasks.jar {
    manifest {
        attributes(
            "Built-By" to System.getProperty("user.name"),
            "Build-Timestamp" to System.currentTimeMillis(),
            "Implementation-Version" to project.version
        )
    }
}
