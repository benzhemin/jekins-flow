package com.company.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Application-wide Spring configuration
 *
 * This configuration class enables component scanning and sets up
 * application-level beans and configurations.
 */
@Configuration
@ComponentScan(basePackages = {
    "com.company.api",
    "com.company.domain",
    "com.company.infrastructure"
})
public class ApplicationConfiguration {
    // Configuration beans would be defined here
    // Currently relying on Spring Boot auto-configuration
}
