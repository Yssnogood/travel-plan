# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw .
COPY pom.xml .
COPY services/shared-lib/pom.xml services/shared-lib/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/user-service/pom.xml services/user-service/
COPY services/travel-service/pom.xml services/travel-service/
COPY services/payment-service/pom.xml services/payment-service/
COPY services/notification-service/pom.xml services/notification-service/

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY services/shared-lib/src services/shared-lib/src
COPY services/notification-service/src services/notification-service/src

RUN ./mvnw clean package -pl services/notification-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/services/notification-service/target/*.jar app.jar

RUN chown -R spring:spring /app
USER spring

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
