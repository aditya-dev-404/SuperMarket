# Project context (read this first in any new chat)

Project: **SuperMarket** (`com.aditya.superMarket` / package `com.aditya.supermarket`),
multi-vendor e-commerce marketplace. Monorepo: `backend/` (IntelliJ), `frontend/` (VS Code). macOS.

## Stack
Java 21, Spring Boot 4.1.1, Maven, MySQL 9.7 (Flyway), React JS + Vite, TanStack Query, Axios.
Mock payments first, JWT auth, Cloudinary images, email notifications, wishlist, size/colour variants.
No coupons for now.

## Delivery style
After Phase 0 (zip), deliver files in chat with their relative paths; user copy-pastes them.
Industry-standard clean code (see CODING_STANDARDS.md). Roadmap: see ROADMAP.md.

## Current state: Phase 0 complete
Backend (`backend/src/main/java/com/aditya/supermarket`):
- `SuperMarketApplication`
- `common/entity/BaseEntity` (id, createdAt, updatedAt; auditing enabled)
- `common/config`: `JpaAuditingConfig`, `WebConfig` + `CorsProperties` (app.cors.allowed-origins),
  `OpenApiConfig`, `TimeConfig` (Clock bean)
- `common/exception`: `ResourceNotFoundException`, `DuplicateResourceException`,
  `BusinessRuleException`, `GlobalExceptionHandler` (ProblemDetail, 404/409/422/400/500)
- `system`: `GET /api/v1/public/ping`
- Empty module packages (with `.gitkeep`): admin, auth, cart, catalog, inventory, notification,
  order, payment, payout, review, user, vendor, wishlist
- Resources: `application.yml` (+ `-dev`, `-prod`), Flyway `V1__baseline.sql` (no tables yet)
- MapStruct deliberately not added yet; add in Phase 1 when first needed. Spring Security: Phase 1.

Frontend (`frontend/src`): `api/axiosClient.js` (base `/api/v1`, error normalisation),
`api/systemApi.js`, `hooks/usePing.js`, `components/{layout,common}`, `pages/{HomePage,NotFoundPage}`,
`routes/AppRoutes.jsx`, `App.jsx` (QueryClient + Router), `features/*` placeholders.

## Next: Phase 1 - Auth & Users
Next Flyway migration number: V2.
