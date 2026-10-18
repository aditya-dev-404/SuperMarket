# Project context (read this first in any new chat)

Project: **SuperMarket** (`com.aditya.superMarket` / package `com.aditya.supermarket`),
multi-vendor e-commerce marketplace. Monorepo: `backend/` (IntelliJ), `frontend/` (VS Code). macOS.

## Stack
Java 21 target (JDK 25 installed locally), Spring Boot 4.1.1, Maven, MySQL (Flyway), React JS + Vite,
TanStack Query, Axios. Config uses `.properties` files. Mock payments first, Cloudinary images,
email notifications, wishlist, size/colour variants. No coupons for now.

## Delivery style
Phase 0 came as a zip. From Phase 1 on, only new/changed files are delivered with their paths
(a zip of changed files plus a path list); the user copies them into the project.
Industry-standard clean code (see CODING_STANDARDS.md). Roadmap: see ROADMAP.md.

## Environment notes
- Local MySQL on :3306 (user `supermarket`, DB `supermarket`); credentials are passed through the IntelliJ
  run configuration (Program arguments / env vars), never committed.
- Dev env vars: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (dev has a fallback), `ADMIN_EMAIL`,
  `ADMIN_PASSWORD` (>= 12 chars, creates the first admin on startup).

## Phase 0 - Foundation (done)
Base entity with auditing, global exception handler (ProblemDetail), OpenAPI, CORS properties, Clock bean,
`GET /api/v1/public/ping`, Flyway `V1__baseline.sql`, React skeleton (router, Axios client, TanStack Query).

## Phase 1 - Auth & Users (done)
Backend (`com.aditya.supermarket`):
- `user`: `entity/{User,Role}` (Role: CUSTOMER, VENDOR, ADMIN; one role per user), `UserRepository`,
  `UserService` (register customer, create admin, profile, change password; emails lower-cased),
  `UserController` (`GET/PUT /api/v1/users/me`), DTOs `UserResponse`, `UpdateProfileRequest`.
- `auth`: `AuthController` (`/api/v1/auth/{register,login,refresh,logout,change-password}`), `AuthService`,
  `JwtService` (HS256 access token: sub=userId, claims email + role), `RefreshTokenService`
  (opaque token, only SHA-256 hash stored, rotation + reuse detection), `RefreshToken` entity/repository,
  `config/{SecurityConfig,JwtConfig,JwtProperties,RefreshCookieProperties,AdminProperties}`,
  `security/{RefreshCookieManager,RestAuthenticationEntryPoint,RestAccessDeniedHandler}`,
  `bootstrap/AdminSeeder`.
- `common/security/CurrentUser` (user id from `@AuthenticationPrincipal Jwt`).
- Security rules: `/api/v1/public/**` and `/api/v1/auth/**` open (except change-password), `/api/v1/admin/**`
  ADMIN, `/api/v1/vendor/**` VENDOR, everything else authenticated. Stateless, CSRF off (Bearer header).
- Flyway `V2__create_users_and_refresh_tokens.sql` (tables `users`, `refresh_tokens`). **Next migration: V3.**
- Access token 15 min, refresh token 7 days (httpOnly, SameSite=Strict cookie, path `/api/v1/auth`).
- Role changes (customer -> vendor in Phase 2) reach the access token only after the next refresh.
- MapStruct not used: DTOs have `from(entity)` factories. Revisit if mapping volume grows.

Frontend (`frontend/src`):
- `api/axiosClient.js`: `apiClient` (adds Bearer token, on 401 does one single-flight refresh then retries),
  `refreshSession`, `PUBLIC_ENDPOINT` options (`skipAuth`, `skipRefresh`), `toApiError`.
- `features/auth/`: `tokenStore` (access token in memory only), `authApi`, `AuthContext`, `AuthProvider`
  (restores session via refresh cookie on page load; status loading|authenticated|anonymous), `useAuth`.
- `routes/ProtectedRoute.jsx` (optional `roles`), `routes/AppRoutes.jsx` (`/`, `/login`, `/register`,
  `/account`, `/forbidden`), pages `LoginPage`, `RegisterPage`, `ProfilePage`, `ForbiddenPage`,
  `components/form/FormField.jsx`, header with login/logout.

## Next: Phase 2 - Vendor onboarding & admin
Customer applies to become a vendor (store profile), admin approves/rejects/suspends, role becomes VENDOR,
dashboard shells for vendor and admin. Next Flyway migration: V3.

## Phase 2: Vendor onboarding (done)
- Table `vendors` (V3): one row per applicant user, status PENDING/APPROVED/REJECTED, unique store_name.
- Customer: POST /api/v1/vendor-applications, GET and PUT /api/v1/vendor-applications/me.
- Admin: GET /api/v1/admin/vendor-applications?status=, GET /{id}, POST /{id}/approve, POST /{id}/reject {reason}.
- Approval calls User.promoteToVendor(); the frontend then refreshes the session so the new JWT carries role VENDOR.
- Rejected applicants can edit and resubmit (status returns to PENDING).
- Frontend: BecomeVendorPage, AdminVendorApplicationsPage, VendorDashboardPage (placeholder); API in features/vendor/vendorApi.js.
- Next: Phase 3 vendor catalog (categories, products, variants, Cloudinary); next migration is V4.