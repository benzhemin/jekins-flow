/*
 * Root build configuration for the Spring Boot CI/CD Platform
 * This file defines common build logic that applies to all modules
 */

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    id("org.springframework.boot") version "3.2.2" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

// Define project properties
val projectGroup = "com.company"
val projectVersion = "1.0.0-SNAPSHOT"
val javaSourceCompatibility = JavaVersion.VERSION_17

// Common configuration for all subprojects
allprojects {
    group = projectGroup
    version = projectVersion

    repositories {
        mavenCentral()
        google()
    }
}

// Apply common configuration to all Java projects
subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = javaSourceCompatibility
        targetCompatibility = javaSourceCompatibility
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // Enable additional compiler warnings
        options.compilerArgs.addAll(listOf(
            "-deprecation",
            "-Xlint:unchecked"
        ))
    }

    // Configure test tasks
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }

    // Custom task to display build information
    tasks.register("buildInfo") {
        doLast {
            println("Project: ${project.name}")
            println("Version: ${project.version}")
            println("Java Version: $javaSourceCompatibility")
            println("Build Time: ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)}")
        }
    }
}

// Wrapper configuration - defines Gradle version for all developers
tasks.wrapper {
    gradleVersion = "8.5"
    distributionType = Wrapper.DistributionType.BIN
}

// Task to display project structure
tasks.register("projectStructure") {
    doLast {
        println("\nProject Structure:")
        println("==================")
        subprojects.forEach { subproject ->
            println("  - ${subproject.name}")
        }
    }
}

// Root-level task to run all tests
tasks.register("allTests") {
    dependsOn(subprojects.map { it.tasks.named("test") })
}

// Root-level task to build all modules
tasks.register("buildAll") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}
