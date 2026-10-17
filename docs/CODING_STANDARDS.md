# Coding standards

## Java / Spring Boot
- Constructor injection only. No field `@Autowired`.
- DTOs are records. Validate with Bean Validation (`@Valid` on request bodies).
- No business logic in controllers; no entities in responses.
- `@Transactional` on service methods; `readOnly = true` for reads.
- Throw domain exceptions (`ResourceNotFoundException`, `BusinessRuleException`, ...);
  never return null for "not found".
- Never log secrets, tokens, passwords or card data.
- Config via `application.properties`; secrets via environment variables only.
- Schema changes only via new Flyway migrations. Never edit an applied migration.
- Avoid N+1 queries (fetch joins / entity graphs); paginate lists.
- Tests: JUnit 5 + Mockito for services, standalone MockMvc for controllers,
  Testcontainers (MySQL) for repository/integration tests (added with the first entities).
- Naming: classes `PascalCase`, methods `camelCase`, tables/columns `snake_case`, plural table names.

## React / JavaScript
- Function components and hooks only. One component per file, `PascalCase.jsx`.
- API calls only through `src/api/*` using `apiClient`; server state via TanStack Query hooks.
- No business secrets in the frontend; env via `VITE_*` variables.
- Keep components small; extract logic into custom hooks.
- Every async view handles loading, error and empty states.
- `npm run lint` and `npm test` must pass before committing.

## General
- Small, focused commits using Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`).
- No commented-out code, no dead code, no unexplained magic values.
- Comments explain *why*, not *what*.
