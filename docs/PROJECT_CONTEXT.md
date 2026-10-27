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


# Phase 4: Public Storefront

Anyone (no login) can search, filter, sort, paginate and open a product page.

## Decisions
- Page size **15** (default; `limit` param, max 50). Pagination is done in SQL.
- Search: MySQL **FULLTEXT** on `products(name, description)`, boolean mode, prefix match per word.
- Out-of-stock products are **shown greyed out** (hide them with the "In stock only" filter).
- No vendor storefront page.

## Visibility rule (every public query)
Product `status = ACTIVE` AND category `active` AND vendor `status = APPROVED`; only `active` variants count.
Drafts / archived / unapproved-vendor products return 404 on the detail endpoint and never appear in lists.

## API (public, `GET /api/v1/products/**`)
| Endpoint | Purpose |
|---|---|
| `GET /api/v1/products` | Paginated list. Params: `q`, `category` (slug, includes sub-categories), `minPrice`, `maxPrice`, `size`, `colour`, `inStock`, `sort` (`newest` \| `price_asc` \| `price_desc` \| `relevance`), `page` (0-based), `limit` |
| `GET /api/v1/products/filters?category=` | Sidebar data: categories (flat, with `parentSlug`), sizes, colours, min/max price |
| `GET /api/v1/products/{slug}` | Product page: images, active variants (effective price + stock), vendor, breadcrumb |

List card price = lowest effective price across variants (`priceVaries` → UI shows "From ₹X").
Default sort: `relevance` when `q` is present, otherwise `newest`.

## Files
Backend (`com.aditya.supermarket`): `catalog/{controller/StorefrontController, service/StorefrontService, service/FullTextQuery, repository/StorefrontQueryRepository, dto/*}`, `common/dto/PagedResponse`, Flyway `V5__storefront_search_indexes.sql` (next migration: V6), test `FullTextQueryTest`.

Frontend: `features/catalog/storefront/*`, `pages/ProductListPage.jsx`, `pages/ProductDetailPage.jsx`, `components/layout/HeaderSearch.jsx`.

## Manual wiring (existing files, not replaced)
1. **SecurityConfig**: permit `GET /api/v1/products/**`
   `.requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()`
2. **AppRoutes.jsx**: add two public routes
   ```jsx
   import ProductListPage from '../pages/ProductListPage';
   import ProductDetailPage from '../pages/ProductDetailPage';
   <Route path="/products" element={<ProductListPage />} />
   <Route path="/products/:slug" element={<ProductDetailPage />} />
   ```
3. **Header.jsx**: add `<HeaderSearch className="w-full max-w-sm" />` and a "Shop" link to `/products`.
4. **HomePage.jsx**: render `<HomeCategories />` and `<LatestProducts />`.
5. If `axiosClient` baseURL already ends with `/api/v1`, set `API_PREFIX = ''` in `storefrontApi.js`.

## Assumed schema (from V4/V3 migrations, verify once)
All SQL is in `StorefrontQueryRepository`, so any mismatch is a one-file fix.
- `products`: id, vendor_id, category_id, name, slug, description, base_price, status, created_at
- `product_variants`: id, product_id, sku, size, colour, price (nullable override), stock_quantity, active
- `product_images`: product_id, url, `position`
- `categories`: id, name, slug, parent_id, active
- `vendors`: id, status (`'APPROVED'`), `business_name`
- IDs are `BIGINT` (Java `Long`). `ResourceNotFoundException` has a `(String message)` constructor.
- Needs a `JdbcClient` bean (comes with `spring-boot-starter-jdbc` / data-jpa).

## Test checklist
- [ ] Flyway V5 applies cleanly on startup
- [ ] `/products` shows only ACTIVE products from APPROVED vendors; DRAFT ones are missing
- [ ] Search "shirt", then a partial word ("shir"), then characters like `+ - " (`: no errors
- [ ] Category filter on a parent shows products of its sub-categories
- [ ] Size / colour / price / in-stock filters combine; changing category resets them
- [ ] 16+ products: page 2 works, URL `?page=2` reloads on the same page
- [ ] A product with all variants at stock 0 is greyed with "Out of stock"
- [ ] Product page: variant switch updates price/stock; unknown slug shows "Product not found"
- [ ] Cloudinary thumbnails load small images on cards

## Good practices applied
Filtering/pagination in the DB, whitelisted sort (no raw input in SQL), sanitised search terms,
URL as the single source of truth for filters, thumbnails via Cloudinary transformations,
list DTO separate from detail DTO, loading skeletons and empty/error states.

## Note for docs/PROJECT_CONTEXT.md
Phase 4 (public storefront) code added; next migration is V6; next phase is Phase 5 (Cart & wishlist).

# Phase 5 - Cart & Wishlist

Logged-in users keep a server-side cart and a wishlist. Both are per user and need a JWT.

## Decisions

| Topic | Decision |
|---|---|
| Guests | Login required. Add to cart / wishlist sends the user to `/login` and back. |
| Price | Always the live price (variant override, else product base price). It is locked into the order only at checkout (Phase 6). |
| Stock | Checked on every cart change, never reserved. Final check happens at checkout. |
| Line limit | `app.cart.max-quantity-per-line` (default 10), also capped by stock. |
| Wishlist | Product level. "Move to cart" asks for size/colour. |
| Own products | A vendor cannot add their own product to the cart. |

## Database (Flyway `V6__cart_and_wishlist.sql`)

- `carts` - one row per user (`user_id` unique).
- `cart_items` - `(cart_id, variant_id)` unique, `quantity > 0`. Deleting a variant removes it from carts.
- `wishlist_items` - `(user_id, product_id)` unique.

## API

All endpoints require authentication. Cart calls return the full updated cart.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/cart` | Cart grouped by vendor with totals. |
| POST | `/api/v1/cart/items` | `{variantId, quantity}`; merges into an existing line. |
| PATCH | `/api/v1/cart/items/{itemId}` | `{quantity}` sets the new quantity. |
| DELETE | `/api/v1/cart/items/{itemId}` | Removes one line. |
| DELETE | `/api/v1/cart` | Clears the cart (204). |
| GET | `/api/v1/wishlist?page=&size=` | Paged storefront-style cards, newest first. Only ACTIVE products. |
| GET | `/api/v1/wishlist/ids` | Product ids, for the heart icons. |
| POST | `/api/v1/wishlist/items` | `{productId}`, idempotent (204). |
| DELETE | `/api/v1/wishlist/items/{productId}` | Idempotent (204). |
| POST | `/api/v1/wishlist/items/{productId}/move-to-cart` | `{variantId}`; adds 1 unit, removes from wishlist. |

### Cart line status

`AVAILABLE`, `INSUFFICIENT_STOCK`, `OUT_OF_STOCK`, `UNAVAILABLE` (variant/product/vendor no longer live).
The subtotal counts only `AVAILABLE` lines; `checkoutReady` is true only when every line is `AVAILABLE`.

## Frontend

- `features/cart` - `cartApi.js`, `useCart.js` (query + mutations), `CartLineItem`, `AddToCartButton`, `CartNavLink`.
- `features/wishlist` - `wishlistApi.js`, `useWishlist.js`, `WishlistButton`, `MoveToCart`.
- `hooks/useUserScope.js` - puts the user id in query keys so cached data never leaks between accounts.
- Pages: `CartPage` (`/cart`), `WishlistPage` (`/wishlist`). Both routes are behind `ProtectedRoute`.
- `ProductCard` has a heart overlay; `ProductDetailPage` has the real Add to cart and heart buttons.

## Out of scope (later phases)

- Checkout and orders (Phase 6).
- "Price changed since you added it" notice (needs a stored price per line).