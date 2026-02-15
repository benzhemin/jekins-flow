/*
 * API Module Build Configuration
 * This is the main application module containing REST controllers, configuration, and entry point
 * It depends on all other modules
 */

plugins {
    id("spring-conventions")
    id("org.springframework.boot") version "3.2.2"
}

description = "REST API and application entry point"

dependencies {
    // Depend on all other modules
    implementation(project(":application:common"))
    implementation(project(":application:domain"))
    implementation(project(":application:infrastructure"))

    // Spring Boot starters
    implementation(libs.`spring-boot-starter-web`)
    implementation(libs.`spring-boot-starter-actuator`)
    implementation(libs.`spring-boot-starter-data-jpa`)
    implementation(libs.`spring-boot-starter-validation`)
    implementation(libs.`spring-boot-starter-logging`)

    // Lombok
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    // Database
    runtimeOnly(libs.`h2-database`)

    // Testing
    testImplementation(libs.`spring-boot-starter-test`)
    testImplementation(libs.`mockito-core`)
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}
