/**
 * Keeps the access token in memory only (never localStorage), so a script injected by an
 * XSS bug cannot read it from storage. A page reload clears it; the session is then restored
 * silently using the HttpOnly refresh cookie.
 */
let accessToken = null;
let sessionExpiredHandler = null;

export const tokenStore = {
  get: () => accessToken,
  set: (token) => {
    accessToken = token;
  },
  clear: () => {
    accessToken = null;
  },
  /** The auth provider registers a callback that runs when a refresh fails mid-session. */
  onSessionExpired: (handler) => {
    sessionExpiredHandler = handler;
  },
  expireSession: () => {
    accessToken = null;
    sessionExpiredHandler?.();
  },
};
