# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw .
COPY pom.xml .
COPY services/shared-lib/pom.xml services/shared-lib/
COPY services/auth-service/pom.xml services/auth-service/

# Download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY services/shared-lib/src services/shared-lib/src
COPY services/auth-service/src services/auth-service/src

# Build the application
RUN ./mvnw clean package -pl services/auth-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built artifact
COPY --from=builder /app/services/auth-service/target/*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app
USER spring

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
