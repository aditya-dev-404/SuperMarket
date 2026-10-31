# API reference (Phase 1)

Interactive docs (dev only): http://localhost:8080/swagger-ui.html. Errors use RFC 9457 problem JSON
(`type`, `title`, `status`, `detail`, `timestamp`, and `errors` for field validation).

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/public/ping` | none | Connectivity check |
| POST | `/api/v1/auth/register` | none | Create CUSTOMER account, returns access token, sets refresh cookie (201) |
| POST | `/api/v1/auth/login` | none | Returns access token, sets refresh cookie |
| POST | `/api/v1/auth/refresh` | refresh cookie | Rotates refresh token, returns new access token |
| POST | `/api/v1/auth/logout` | refresh cookie | Revokes the refresh token, clears the cookie (204) |
| POST | `/api/v1/auth/change-password` | Bearer | Changes password, signs out all devices (204) |
| GET | `/api/v1/users/me` | Bearer | Current user's profile |
| PUT | `/api/v1/users/me` | Bearer | Update first name, last name, phone |

Auth response body: `{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 900, "user": { ... } }`.

Status codes: 400 validation, 401 bad credentials or token, 403 wrong role, 409 duplicate email, 422 business rule.
## Phase 6: addresses, checkout, orders

All endpoints need a login. `/api/v1/vendor/**` needs the VENDOR role.

### Addresses

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/addresses` | The user's addresses, default first. |
| POST | `/api/v1/addresses` | `{fullName, phone, line1, line2?, city, state, pincode, defaultAddress}`. 201. The first address becomes the default. |
| PUT | `/api/v1/addresses/{id}` | Same body. `defaultAddress=false` never removes an existing default. |
| POST | `/api/v1/addresses/{id}/default` | Makes it the default. |
| DELETE | `/api/v1/addresses/{id}` | 204. Deleting the default promotes the newest remaining. |

### Checkout and orders (customer)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/checkout/options` | `{codShippingFee}`. |
| POST | `/api/v1/orders` | `{addressId, paymentMethod: ONLINE\|COD}`. 201 with the order. Uses the cart, then clears it. 422 if a line is unavailable or stock is short. |
| GET | `/api/v1/orders?page=&size=` | Paged summaries, newest first (size 1-50). |
| GET | `/api/v1/orders/{id}` | Full order with sub-orders. Flags `payable` and `cancellable`. 404 for someone else's order. |
| POST | `/api/v1/orders/{id}/cancel` | Only while every sub-order is before shipping. Restores stock. |
| POST | `/api/v1/orders/{id}/pay` | Mock payment `{outcome: SUCCESS\|FAILURE}`. On FAILURE the order is cancelled and the items return to the cart. 422 if not awaiting payment. |

### Vendor orders

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/vendor/sub-orders?status=&page=&size=` | The vendor's own sub-orders (never unpaid online ones). |
| GET | `/api/v1/vendor/sub-orders/{id}` | Items and the delivery address; `nextStatus` is null when finished. |
| PATCH | `/api/v1/vendor/sub-orders/{id}/status` | `{status}`. Exactly one step forward (PLACED, PROCESSING, SHIPPED, DELIVERED). Anything else is 422. |