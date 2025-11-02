# TreeVault — Architecture & Design

This document guides contributors and agents to build features that fit TreeVault’s tech stack, architecture, and practices.

## Tech Stack
- Backend: Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA, Validation, Actuator, Flyway, PostgreSQL, MapStruct, Lombok
- API: REST, versioned under `/api/v1`, OpenAPI via SpringDoc (`/swagger-ui/index.html`)
- Frontend: React 18 + TypeScript + Vite, Axios, Material UI, Zustand
- Testing: JUnit 5, Testcontainers (Postgres), RestAssured; Vitest + React Testing Library
- Runtime/DevOps: Docker Compose, health checks via Actuator and HTTP

## Architecture Overview
Layered/hexagonal-inspired separation:
- Domain: business concepts and rules (entities, value objects)
- Application: use cases and orchestration (services) — pure business logic
- Infrastructure: persistence, mappers, adapters (e.g. JPA entities, repositories)
- API (Web): controllers, DTOs, request/response mapping

Key ideas:
- Domain models are persistence-agnostic. JPA entities live in infrastructure.
- Mapping between domain and persistence uses MapStruct.
- API DTOs are not domain or JPA entities; map them explicitly.
- Transactions at application service boundaries.

## Conventions & Best Practices
- IDs: use `UUID` for primary identifiers.
- Time: use `java.time.*` (e.g., `Instant`/`LocalDateTime`) in UTC. Expose ISO-8601 in API.
- Validation: annotate request DTOs with Jakarta Validation; validate in controllers.
- Errors: use Spring ProblemDetail; return meaningful HTTP status codes.
- CORS: configured via properties; default allow `http://localhost:3000` in dev.
- Logging: structured messages; avoid logging sensitive data; prefer INFO in prod, DEBUG in dev.
- Migrations: all schema changes via Flyway (`src/main/resources/db/migration`). No ad-hoc/manual schema.
- Pagination and sorting for list endpoints; avoid N+1 queries (use fetch joins or projections when needed).
- Keep controllers thin; put business rules into application/domain layers.

## Backend Design Guidelines
- Controllers
  - Endpoints under `/api/v1/...`.
  - Validate inputs (`@Valid`), return DTOs only.
  - Map exceptions to ProblemDetail; avoid leaking internal exceptions.
- Services (Application layer)
  - Encapsulate use cases; orchestrate domain operations and transactions.
  - No framework-specific annotations except `@Transactional`.
- Domain model
  - Express invariants in constructors/factory methods.
  - Prefer immutable value objects.
- Persistence (Infrastructure)
  - Separate JPA entities from domain models.
  - Use repositories that return domain types; use MapStruct mappers (e.g., `PersistenceMapper`).
  - Write Flyway migrations for new entities and indexes.
- Transactions
  - Default to service-level `@Transactional`. For read-only queries use `@Transactional(readOnly = true)`.
- Validation & Error handling
  - Use Bean Validation for inputs; domain guards for invariants.
  - Convert domain/application exceptions to ProblemDetail with clear titles and details.

## API Standards
- Versioning: prefix all endpoints with `/api/v1`.
- Content type: `application/json`.
- Problem responses: RFC 7807 via Spring ProblemDetail.
- Documentation: SpringDoc OpenAPI UI at `/swagger-ui/index.html`.
- Naming: nouns for resources (e.g., `/nodes`), verbs for sub-resources when needed (e.g., `/nodes/{id}/move`).

## Frontend Guidelines
- API client
  - Axios instance in `src/api/client.ts` uses `VITE_API_URL`.
  - IMPORTANT: Set `VITE_API_URL` to the API base without `/api/v1` (the endpoints already include it).
- State management
  - Use Zustand stores for view-level and session state; keep them minimal.
- Components
  - Keep components presentational; move data fetching to hooks or thin containers when needed.
  - Use Material UI components; prefer composition over deep inheritance.
- Testing
  - Vitest + React Testing Library; write tests for critical components and hooks.

## Configuration & Environments
- Docker Compose
  - Copy `.env.example` to `.env` at project root (required by Compose): `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
  - Backend runs with Spring profile `docker`; you may override CORS via `CORS_*` env vars.
- Local development
  - Backend: set `SPRING_DATASOURCE_*` in your terminal if running outside Docker.
  - Frontend (Vite): export `VITE_API_URL=http://localhost:8080` (no `/api/v1`). You can create `treevault-frontend/.env.local` for convenience.

## Testing Strategy
- Backend
  - Unit tests for domain and application logic.
  - Integration tests with Testcontainers (Postgres) for repositories and web layers.
  - API tests can use RestAssured.
- Frontend
  - Component tests with RTL; mock API with MSW or Axios mocks when necessary.

## Performance & Reliability
- Use indexes on frequently filtered columns; add Flyway migration for new indexes.
- Batch writes with JPA `jdbc.batch_size` when applicable (already configured).
- Avoid large payloads; consider pagination and server-side filtering.

## Security & Compliance
- CORS restricted to known origins in non-dev environments.
- Do not log sensitive information.
- Add authentication/authorization when required (not implemented yet).

## How to Add a Feature (Blueprint)
1. Define the use case and API shape (path, method, request/response). Update OpenAPI via annotations.
2. Domain: model new value objects/entities and invariants.
3. Application: implement a service method encapsulating the use case. Add transactions.
4. Infrastructure: add JPA entities/repositories and Flyway migrations; MapStruct mappings.
5. API: add controller endpoints and DTOs; validation annotations.
6. Frontend: add API function in `src/api/*`, state hook/store, and UI components.
7. Tests: unit + integration (Testcontainers) + frontend tests.
8. Update docs if behavior/architecture choices change.

## References
- README for run instructions
- `.env.example` for configuration
- SpringDoc UI: `/swagger-ui/index.html`
