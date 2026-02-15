/*
 * Build configuration for buildSrc
 * buildSrc is a special Gradle directory for writing plugins and custom build logic
 * This allows us to define convention plugins that are applied across all modules
 */

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    // Plugin dependencies for use in our convention plugins
    implementation("org.springframework.boot:org.springframework.boot.gradle.plugin:3.2.2")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.4")
}
