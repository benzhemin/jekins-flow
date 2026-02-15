# Multi-stage Dockerfile for Spring Boot Application
# This Dockerfile demonstrates best practices for containerizing Java applications
#
# Stage 1: Build Stage
# - Compiles the application
# - Runs tests
# - Creates a fat JAR
#
# Stage 2: Runtime Stage
# - Uses a minimal JRE image
# - Copies only the compiled JAR
# - Sets up health checks
# - Runs as non-root user for security

# ============================================================================
# STAGE 1: Build Stage
# ============================================================================
FROM eclipse-temurin:17-jdk AS builder

LABEL stage=builder
LABEL description="Build stage for Spring Boot application"

# Set working directory in container
WORKDIR /workspace

# Copy Gradle wrapper and configuration files
# Doing this first allows Docker to cache the layer if these files haven't changed
COPY gradle gradle/
COPY gradlew gradlew.bat ./
COPY settings.gradle.kts build.gradle.kts ./
COPY application/buildSrc application/buildSrc/

# Copy source code
# Place after gradle files so changes to source don't invalidate gradle cache
COPY application application/

# Make gradle wrapper executable (needed in some environments)
RUN chmod +x gradlew

# Build the application
# This will:
# - Download dependencies
# - Compile source code
# - Run unit tests
# - Create the JAR file
RUN ./gradlew :application:api:bootJar -x test \
    && ls -la application/api/build/libs/

# Verify the JAR was created
RUN test -f application/api/build/libs/app.jar || exit 1

# ============================================================================
# STAGE 2: Runtime Stage
# ============================================================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="DevOps Team"
LABEL description="Production runtime image for Spring Boot application"
LABEL version="1.0.0"

# Install dumb-init to properly handle signals
# This ensures the JVM process receives SIGTERM for graceful shutdown
RUN apk add --no-cache dumb-init curl

# Create non-root user for security
# Running as root in containers is a security risk
RUN addgroup -g 1000 appuser && \
    adduser -u 1000 -G appuser -h /home/appuser -s /sbin/nologin -D appuser

# Set working directory
WORKDIR /home/appuser/app

# Copy JAR from build stage
# Extract the JAR's layers for better performance
# Spring Boot FAT JARs have a specific structure that can be optimized
COPY --from=builder --chown=appuser:appuser \
    /workspace/application/api/build/libs/app.jar app.jar

# Extract JAR into layers for improved Docker layer caching
# This allows Docker to cache application layers separately from dependency layers
RUN jar -xf app.jar && \
    rm app.jar && \
    chown -R appuser:appuser /home/appuser/app

# Switch to non-root user
USER appuser

# Container configuration
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE="production"

# Expose port (for documentation, doesn't actually publish the port)
EXPOSE 8080

# Health check configuration
# Docker uses this to determine if the container is healthy
# The container is considered healthy if this command exits with code 0
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Entrypoint with dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Default command
# This can be overridden when running the container
CMD ["sh", "-c", "java ${JAVA_OPTS} -cp app:app/lib/* com.company.api.Application"]

# Build-time metadata
LABEL org.opencontainers.image.source="https://github.com/company/springboot-cicd-platform"
LABEL org.opencontainers.image.documentation="https://github.com/company/springboot-cicd-platform/docs"
LABEL org.opencontainers.image.vendor="Company"
