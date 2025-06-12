# Multi-stage build for CodeBridge DB Service
FROM maven:3.9.4-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy the POM file and download dependencies
COPY ./pom.xml ./pom.xml
COPY ./codebridge-parent/pom.xml ./codebridge-parent/pom.xml
COPY ./codebridge-db-service/pom.xml ./codebridge-db-service/pom.xml
RUN mvn dependency:go-offline -B -f ./codebridge-parent/pom.xml
RUN mvn dependency:go-offline -B -f ./codebridge-db-service/pom.xml

# Copy source code and build the application
COPY ./codebridge-db-service/src ./codebridge-db-service/src
RUN mvn clean package -DskipTests -f ./codebridge-db-service/pom.xml

# Create the runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create directories for schema snapshots and migrations
RUN mkdir -p /app/schema-snapshots /app/migrations

# Copy the built JAR file
COPY --from=build /app/codebridge-db-service/target/*.jar /app/app.jar

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Expose the application port
EXPOSE 8085

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

