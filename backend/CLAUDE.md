# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Dethroned** is a Spring Boot 4.0.4 backend application written in Java 21. The project is currently in early development with a minimal structure (Spring MVC web starter with DevTools and testing support).

**Key Stack:**
- **Framework:** Spring Boot 4.0.4
- **Language:** Java 21
- **Build Tool:** Maven 3.9.14 (via wrapper: `./mvnw`)
- **Testing:** JUnit 5 + Spring Boot Test

## Development Commands

All Maven commands should be run from the `/backend` directory using the Maven wrapper:

### Building & Compiling
```bash
./mvnw clean compile          # Compile the project
./mvnw clean package          # Build a JAR package
./mvnw clean package -DskipTests  # Package without running tests
```

### Running
```bash
./mvnw spring-boot:run        # Run the application locally
```

### Testing
```bash
./mvnw clean test             # Run all tests
./mvnw test -Dtest=TestClassName  # Run a specific test class
./mvnw test -Dtest=TestClassName#testMethodName  # Run a specific test method
```

### Development
```bash
./mvnw clean compile          # Fast compile during development (DevTools watches changes)
./mvnw -DskipTests            # Skip tests during builds (when developing features)
```

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/toni/dethroned/backend/
│   │   │   └── BackendApplication.java    # Spring Boot entry point
│   │   └── resources/
│   │       ├── application.yaml           # Application configuration
│   │       ├── static/                    # Static assets (CSS, JS, images)
│   │       └── templates/                 # HTML templates (if using Thymeleaf)
│   └── test/
│       └── java/com/toni/dethroned/backend/
│           └── BackendApplicationTests.java  # Integration/context tests
├── pom.xml                    # Maven dependency and build configuration
└── mvnw / mvnw.cmd           # Maven wrapper scripts (use these instead of system Maven)
```

## Architecture Notes

- **Entry Point:** `BackendApplication.java` - Minimal Spring Boot application
- **Configuration:** `application.yaml` sets the application name to "backend"
- **Package Structure:** Follows Maven convention `com.toni.dethroned.backend`
- **Dev Tooling:** Spring Boot DevTools is configured for hot reload during development
- **Testing:** Uses `@SpringBootTest` for full application context testing

The project is in early-stage development with only a basic application shell in place. No domain models, services, controllers, or database integration are currently implemented.

## Development Notes

- Java 17 is currently installed locally but the project targets Java 21. This may need to be addressed if building fails with Java version errors.
- The project uses standard Spring Boot conventions; new features should follow the typical Spring patterns (Controllers, Services, Repositories).
- DevTools is enabled in the runtime scope, providing automatic restart capabilities during development.
