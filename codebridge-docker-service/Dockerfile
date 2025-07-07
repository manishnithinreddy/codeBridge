FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the JAR file from the host
COPY target/codebridge-docker-service-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

