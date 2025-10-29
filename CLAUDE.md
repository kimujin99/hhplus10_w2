# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.7 e-commerce application built with Java 21 and Gradle. The project uses Lombok for boilerplate reduction and Spring Boot DevTools for development productivity.

**Package Structure**: The base package is `com.example.hhplus_ecommerce` (note the underscore, not hyphen - the original package name 'com.example.hhplus-ecommerce' is invalid in Java).

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests com.example.hhplus_ecommerce.SpecificTestClass

# Run tests with specific pattern
./gradlew test --tests '*SpecificTest'

# Run tests continuously (watch mode)
./gradlew test --continuous
```

### Development
```bash
# Run with Spring Boot DevTools (auto-reload)
./gradlew bootRun

# Check for dependency updates
./gradlew dependencyUpdates
```

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Build Tool**: Gradle with Kotlin DSL
- **Web**: Spring Web (RESTful services)
- **Development Tools**: Spring Boot DevTools (hot reload)
- **Code Generation**: Lombok
- **Testing**: JUnit 5 (Jupiter) with Spring Boot Test

### Project Structure
```
src/
├── main/
│   ├── java/com/example/hhplus_ecommerce/
│   │   └── HhplusEcommerceApplication.java (Main entry point)
│   └── resources/
│       ├── application.properties (Configuration)
│       ├── static/ (Static web resources)
│       └── templates/ (Template files)
└── test/
    └── java/com/example/hhplus_ecommerce/
        └── HhplusEcommerceApplicationTests.java
```

### Configuration
- Application configuration is in `src/main/resources/application.properties`
- Spring application name: `hhplus-ecommerce`
- Default Spring Boot port: 8080 (unless overridden in application.properties)

## Development Guidelines

### Java Version
This project requires Java 21. The toolchain is configured in build.gradle to ensure consistency.

### Lombok Usage
Lombok is configured for annotation processing. Common annotations used:
- `@Data`, `@Getter`, `@Setter` for POJOs
- `@Builder` for builder pattern
- `@AllArgsConstructor`, `@NoArgsConstructor` for constructors
- `@Slf4j` for logging

### Testing
- Use JUnit 5 (Jupiter) for unit tests
- Use `@SpringBootTest` for integration tests
- Test files should mirror the structure of main source files
