FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy parent pom.xml first (required for dependency resolution)
COPY pom.xml ./parent-pom.xml

# Copy service pom.xml and source
COPY codebridge-teams-service/pom.xml .
COPY codebridge-teams-service/src ./src

# Build the application
RUN mvn clean package -DskipTests

FROM openjdk:21-jre-slim

WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/target/codebridge-teams-service-3.2.0.jar app.jar

# Expose port
EXPOSE 8088

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
