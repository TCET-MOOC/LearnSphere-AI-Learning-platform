# LearnSphere Architecture Documentation

## High-Level Architecture

LearnSphere follows a standard modern web application architecture, separating the client-side presentation from the server-side business logic and data persistence.

### Components

1. **Client (Frontend)**: Angular application.
2. **Server (Backend)**: Java Spring Boot application.
3. **Database**: Relational database (MySQL or PostgreSQL).
4. **AI Microservices (Planned)**: Python-based microservices for NLP and Recommendation (Currently stubbed).
5. **Payment Gateway**: Razorpay (Currently simulated).

---

## Data Flow

1. **Client Request**: The Angular app sends HTTP REST requests to the Spring Boot backend. All secured endpoints require a valid JWT token in the `Authorization` header.
2. **Security Filter**: Spring Security intercepts the request (`JwtAuthFilter`), validates the token, and sets the Security Context.
3. **Controller**: The appropriate `@RestController` handles the request.
4. **Service**: Controllers delegate business logic to `@Service` classes.
5. **Repository**: Services interact with the database via Spring Data JPA interfaces (`@Repository`).
6. **Response**: Data is returned as DTOs (Data Transfer Objects) to the client.

---

## Infrastructure (Planned)

- **Cloud Provider**: AWS or Azure.
- **Video Storage**: AWS S3 or Azure Blob Storage (Currently stored locally in `uploadDir`).
- **Video Delivery**: CloudFront or CDN for HLS adaptive bitrate streaming.
- **Deployment**: Docker containers for both backend and frontend.

## Security

- **Authentication**: JWT (JSON Web Tokens).
- **Authorization**: Role-Based Access Control (RBAC). Three strict roles: `STUDENT`, `TEACHER`, `ADMIN`.
- **Passwords**: Hashed via BCrypt.
