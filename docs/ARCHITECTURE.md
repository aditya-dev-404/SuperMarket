# Architecture

## Style
Modular monolith: one Spring Boot application, strict module boundaries (package-by-feature).
Modules can later be extracted into services if ever needed.

```
React SPA  --JSON/HTTPS-->  Spring Boot REST API  --JPA-->  MySQL
```

## Backend modules (`com.aditya.supermarket.<module>`)
`auth`, `user`, `vendor`, `catalog`, `inventory`, `cart`, `wishlist`, `order`, `payment`,
`payout`, `review`, `notification`, `admin`, plus `common` (shared config, base entity,
exceptions) and `system` (ping).

Each module follows: `controller` -> `service` -> `repository`, with `dto`, `entity`, `mapper`.
Rules:
- Controllers handle HTTP only. Business logic lives in services.
- Entities are never returned from the API; use DTOs (Java records).
- A module talks to another module through its service, never its repository.

## API conventions
- Base path `/api/v1`. Groups: `/public/**`, `/customer/**`, `/vendor/**`, `/admin/**`.
- Errors use RFC 9457 `ProblemDetail` (see `GlobalExceptionHandler`).
- List endpoints are paginated.

## Domain concepts (multi-vendor)
- Roles: CUSTOMER, VENDOR, ADMIN. A vendor is a user with an approved store profile.
- One checkout creates a parent Order with one Sub-order per vendor.
- Platform collects payment; commission and vendor payouts are tracked in a ledger.
- Product variants (size/colour) carry their own price and stock.
- Money is `DECIMAL`; order items snapshot name and price at purchase time.
- Stock updates use optimistic locking.

## Database
MySQL 9.7 LTS, schema managed only by Flyway migrations (`db/migration/V<N>__name.sql`);
Hibernate `ddl-auto=validate`. Timestamps stored in UTC.

## Frontend
Feature-based folders under `src/features`, shared UI in `src/components`, one Axios client
(`src/api/axiosClient.js`), server state via TanStack Query, routes in `src/routes`.
In development, Vite proxies `/api` to `localhost:8080`.

## Security (from Phase 1)
JWT access tokens with refresh tokens, BCrypt password hashing, role-based authorization,
ownership checks in the service layer.
