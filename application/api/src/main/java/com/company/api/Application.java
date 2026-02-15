package com.company.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point
 *
 * This class bootstraps the entire Spring Boot application.
 * It enables component scanning for all modules in the application package hierarchy.
 *
 * The application follows a multi-module architecture:
 * - common: Shared utilities and DTOs
 * - domain: Business logic and entities
 * - infrastructure: Persistence and external integrations
 * - api: REST controllers and main application
 */
@SpringBootApplication(scanBasePackages = {
    "com.company.api",
    "com.company.domain",
    "com.company.infrastructure",
    "com.company.common"
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
