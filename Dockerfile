# ============================================================================
# Multi-Stage Dockerfile for Payment Processing Application
# ============================================================================
# Stage 1: Build Stage - Maven build with dependency caching
# Stage 2: Runtime Stage - Distroless Java 11 for minimal attack surface
# ============================================================================

# ============================================================================
# Stage 1: Build Stage
# ============================================================================
FROM maven:3.8-openjdk-11 AS builder

# Set metadata labels
LABEL maintainer="payment-app-team"
LABEL description="Payment Processing Application - Build Stage"
LABEL version="1.0.0"

# Set working directory for build
WORKDIR /build

# Copy pom.xml first for better layer caching
# This allows Docker to cache dependencies if pom.xml hasn't changed
COPY pom.xml .

# Download dependencies offline to cache them in a separate layer
# This step will be cached unless pom.xml changes
RUN mvn dependency:go-offline -B

# Copy the entire source code
COPY src ./src

# Build the application
# -DskipTests: Skip tests for faster builds (tests should run in CI/CD)
# clean: Remove previous build artifacts
# package: Create the JAR file
RUN mvn clean package -DskipTests -B

# Verify the JAR file was created
RUN ls -lh /build/target/payment-app-1.0.0.jar

# ============================================================================
# Stage 2: Runtime Stage
# ============================================================================
FROM gcr.io/distroless/java11-debian11

# Set metadata labels for runtime image
LABEL maintainer="payment-app-team"
LABEL description="Payment Processing Application - Production Runtime"
LABEL version="1.0.0"
LABEL java.version="11"
LABEL base.image="gcr.io/distroless/java11-debian11"

# Set working directory
WORKDIR /app

# Copy the JAR file from builder stage with proper ownership
# Using --chown to set ownership to non-root user (UID 1001)
COPY --from=builder --chown=1001:1001 /build/target/payment-app-1.0.0.jar /app/payment-app.jar

# Switch to non-root user for security
# Distroless images don't have useradd, so we use numeric UID
USER 1001

# Expose application port
EXPOSE 8080

# Set the entrypoint to run the application
# Using exec form for proper signal handling
ENTRYPOINT ["java", "-jar", "/app/payment-app.jar"]

# ============================================================================
# Build Instructions:
# docker build -t payment-app:1.0.0 .
# 
# Run Instructions:
# docker run -p 8080:8080 payment-app:1.0.0
# 
# Security Features:
# - Distroless base image (no shell, no package manager)
# - Non-root user (UID 1001)
# - Minimal attack surface
# - No unnecessary tools or utilities
# 
# Performance Features:
# - Multi-stage build (smaller final image)
# - Layer caching for dependencies
# - Optimized build order
# ============================================================================