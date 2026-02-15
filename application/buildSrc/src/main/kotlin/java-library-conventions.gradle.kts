/*
 * Java Library Convention Plugin
 * Applied to library modules (common, domain, infrastructure)
 * Adds common dependencies and configuration for non-application modules
 */

plugins {
    id("spring-conventions")
    `java-library`
}

// Java Library plugin provides better API/implementation separation
// than just Java plugin, reducing classpath bloat for consumers
