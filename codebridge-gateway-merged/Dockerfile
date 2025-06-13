FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="CodeBridge Team"
LABEL description="CodeBridge Unified Gateway Service"

# Set working directory
WORKDIR /app

# Add a non-root user to run the application
RUN addgroup -S codebridge && adduser -S codebridge -G codebridge
USER codebridge:codebridge

# Copy the JAR file
COPY --chown=codebridge:codebridge target/*.jar app.jar

# Set environment variables
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV SPRING_PROFILES_ACTIVE="prod"

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

