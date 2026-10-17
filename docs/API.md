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
