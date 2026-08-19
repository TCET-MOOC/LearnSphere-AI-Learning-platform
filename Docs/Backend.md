# LearnSphere Backend Documentation

## Overview

The backend is a **Java Spring Boot** application serving RESTful APIs to the Angular frontend. It manages business logic, data persistence, and security.

## Core Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java
- **Database Access**: Spring Data JPA / Hibernate
- **Security**: Spring Security with JSON Web Tokens (JWT)
- **Database**: Relational Database (MySQL/PostgreSQL configuration)

## Package Structure

Located in `src/main/java/com/MOOC/OnlineLearningPlatfrom/`:

- `Controller/`: REST API endpoints exposed to the frontend.
- `Service/`: Business logic implementations.
- `Entity/`: JPA entities mapping to database tables.
- `Dto/`: Data Transfer Objects for API requests and responses.
- `Repository/`: Spring Data JPA interfaces for database interaction.
- `Security/`: JWT generation, validation, and security filters.
- `Exception/`: Global exception handling (`@RestControllerAdvice`).

## Key Functionalities

- **Authentication**: Custom JWT implementation. `JwtAuthFilter` intercepts requests to extract the `userId` and `role`.
- **File Upload**: The `UploadController` handles multipart file uploads for videos and documents, currently storing them locally.
- **Entity Relationships**: Highly relational data model linking `Users`, `Courses`, `Lectures`, `Enrollments`, `Assessments`, `Payments`, and `Royalties`.
- **Roles**: Enforced strictly via `UserRole` mapping to `STUDENT`, `TEACHER`, and `ADMIN`.
