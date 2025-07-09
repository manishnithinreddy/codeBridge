FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the JAR file from the host
COPY target/codebridge-teams-service-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
