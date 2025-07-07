FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy source code
COPY src ./src

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
