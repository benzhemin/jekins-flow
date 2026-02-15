/*
 * Root settings file for the Spring Boot CI/CD Platform
 * This file defines the multi-module Gradle project structure and shared configurations
 */

rootProject.name = "springboot-cicd-platform"

// Define all modules in the project with their paths
include(
    "application:common",
    "application:domain",
    "application:infrastructure",
    "application:api"
)

// Configure plugin management for all builds
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// Configure dependency resolution for all builds
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }

    // Define version catalog for consistent dependency versions across all modules
    versionCatalog {
        val springBootVersion = "3.2.2"
        val springCloudVersion = "2023.0.0"
        val javaVersion = "17"
        val gradleWrapperVersion = "8.5"

        version("javaVersion", javaVersion)
        version("springBoot", springBootVersion)
        version("springCloud", springCloudVersion)

        library("spring-boot-starter-web", "org.springframework.boot:spring-boot-starter-web:$springBootVersion")
        library("spring-boot-starter-data-jpa", "org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
        library("spring-boot-starter-actuator", "org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
        library("spring-boot-starter-validation", "org.springframework.boot:spring-boot-starter-validation:$springBootVersion")
        library("spring-boot-starter-logging", "org.springframework.boot:spring-boot-starter-logging:$springBootVersion")
        library("spring-boot-starter-test", "org.springframework.boot:spring-boot-starter-test:$springBootVersion")

        library("h2-database", "com.h2database:h2:2.2.224")
        library("lombok", "org.projectlombok:lombok:1.18.30")
        library("jackson-databind", "com.fasterxml.jackson.core:jackson-databind:2.17.0")
        library("junit-jupiter-api", "org.junit.jupiter:junit-jupiter-api:5.10.1")
        library("junit-jupiter-engine", "org.junit.jupiter:junit-jupiter-engine:5.10.1")
        library("mockito-core", "org.mockito:mockito-core:5.7.1")

        bundle("spring-boot-common", listOf("spring-boot-starter-logging", "spring-boot-starter-validation"))
    }
}
