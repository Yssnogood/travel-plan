# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw .
COPY pom.xml .
COPY services/shared-lib/pom.xml services/shared-lib/
COPY services/travel-service/pom.xml services/travel-service/

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY services/shared-lib/src services/shared-lib/src
COPY services/travel-service/src services/travel-service/src

RUN ./mvnw clean package -pl services/travel-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/services/travel-service/target/*.jar app.jar

RUN chown -R spring:spring /app
USER spring

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
