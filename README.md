# CodeBridge

CodeBridge is a cross-platform developer tool designed to streamline development workflows and enhance team collaboration.

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.2.0
- **Database**: PostgreSQL
- **Authentication**: OAuth2 / JWT
- **API Documentation**: SpringDoc OpenAPI
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, Testcontainers

## Project Structure

The project follows a multi-module Maven structure:

- **codebridge-parent**: Parent module with dependency management
  - **codebridge-common**: Common utilities and base components
  - **codebridge-core**: Core service implementation

## Features

- **Team Management**: Create and manage teams with hierarchical structure
- **User Management**: User registration, authentication, and profile management
- **Role-Based Access Control**: Fine-grained permissions system
- **Service Integration**: Connect and configure external services
- **Audit Logging**: Comprehensive activity tracking
- **Token Management**: Secure token-based authentication

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL 14+

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/codebridge.git
   cd codebridge
   ```

2. Configure the database:
   ```bash
   # Create a PostgreSQL database
   createdb codebridge
   ```

3. Build the project:
   ```bash
   mvn clean install
   ```

4. Run the application:
   ```bash
   cd codebridge-parent/codebridge-core
   mvn spring-boot:run
   ```

5. Access the API documentation:
   ```
   http://localhost:8081/api/swagger-ui.html
   ```

## Development Guidelines

### Code Style

- Follow standard Java coding conventions
- Use meaningful variable and method names
- Write comprehensive JavaDoc comments
- Keep methods small and focused on a single responsibility

### Testing

- Write unit tests for all business logic
- Use integration tests for repository and controller layers
- Aim for high test coverage, especially for critical components

### Git Workflow

- Use feature branches for all new features and bug fixes
- Submit pull requests for review before merging to main
- Keep commits small and focused
- Write clear commit messages

## License

This project is licensed under the MIT License - see the LICENSE file for details.

