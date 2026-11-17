# Stage 1: Build using Java 25 JDK
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM first to leverage layer caching
COPY mvnw ./
COPY .mvn/ .mvn/
COPY pom.xml ./

# Pre-download dependencies (optional, but ensure executable mvnw)
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and build application JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal Java 25 Runtime
FROM eclipse-temurin:25-jre AS runner
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create non-root system user and group
RUN groupadd -r whodis && useradd -r -g whodis -m -d /home/whodis whodis

# Create persistent storage folder and grant permissions
RUN mkdir -p /app/storage && chown -R whodis:whodis /app

# Copy the built JAR from builder
COPY --from=builder --chown=whodis:whodis /workspace/target/*.jar app.jar

USER whodis

# Default environment variables
ENV SERVER_PORT=8080 \
    STORAGE_ROOT=/app/storage

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
