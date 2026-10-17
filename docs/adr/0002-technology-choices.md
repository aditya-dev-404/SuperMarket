# ADR 0002: Technology choices

Status: accepted

- Java 21 + Spring Boot 4.1.x (Maven): current LTS Java and latest stable Spring generation.
- MySQL 9.7 LTS with Flyway migrations and `ddl-auto=validate`.
- React with plain JavaScript (Vite), TanStack Query for server state.
- JWT for authentication; mock payments first, real gateway in Phase 10.
- Cloudinary for images; Mailpit for local email testing.
