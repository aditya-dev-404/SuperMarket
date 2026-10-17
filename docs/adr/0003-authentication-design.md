# ADR 0003: Authentication design

Status: accepted (Phase 1)

## Decisions
- **Access token:** JWT (HS256), 15 minutes, signed with `app.jwt.secret`, validated on every request via
  Spring Security's resource server. Carries `sub` (user id), `email`, `role`. Kept in frontend memory only.
- **Refresh token:** random opaque value, 7 days, sent only in an HttpOnly, SameSite=Strict cookie scoped to
  `/api/v1/auth`. Stored as a SHA-256 hash, so a database leak does not expose usable tokens.
- **Rotation + reuse detection:** every refresh revokes the used token and issues a new one. Presenting a revoked
  token revokes all sessions of that user.
- **Roles:** one role per user (CUSTOMER, VENDOR, ADMIN). Everyone registers as CUSTOMER; vendors apply later
  (Phase 2) and an admin approves. The first admin is created from environment variables at startup.
- **CSRF:** disabled, because API calls authenticate with the Authorization header, not a cookie. The refresh
  cookie works only on `/api/v1/auth/*` and is SameSite=Strict.
- **Passwords:** delegating encoder (BCrypt). 8-72 characters with a letter and a digit. Login errors are generic.

## Consequences
- XSS cannot read tokens from storage, only act while the page is open.
- A page reload restores the session through the refresh cookie (one extra request).
- A role change shows up in the access token after the next refresh (at most 15 minutes).
- Rate limiting and expired-token cleanup are scheduled for Phase 11.
