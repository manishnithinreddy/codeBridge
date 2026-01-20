FROM maven:3.9-eclipse-temurin-21 as builder

WORKDIR /app

# Copy parent pom.xml first (required for dependency resolution)
COPY pom.xml ./parent-pom.xml

# Copy service pom.xml and source
COPY codebridge-docker-service/pom.xml .
COPY codebridge-docker-service/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jre-slim

WORKDIR /app

# Copy the built JAR file
COPY --from=builder /app/target/codebridge-docker-service-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
