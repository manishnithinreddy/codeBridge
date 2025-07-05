FROM openjdk:21-jdk-slim as builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Install Maven
RUN apt-get update && apt-get install -y maven

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:21-jre-slim

WORKDIR /app

# Copy the built JAR file
COPY --from=builder /app/target/codebridge-documentation-service-0.0.1-SNAPSHOT.jar app.jar

# Create documentation directory
RUN mkdir -p /app/documentation
VOLUME /app/documentation

# Expose port
EXPOSE 8087

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8087/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
