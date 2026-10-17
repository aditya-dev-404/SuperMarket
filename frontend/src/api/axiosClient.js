import axios from 'axios';
import { env } from '../config/env';
import { tokenStore } from '../features/auth/tokenStore';

/** Shared Axios instance. All API modules must use this, never raw axios. */
export const apiClient = axios.create({
  baseURL: `${env.apiBaseUrl}/api/v1`,
  timeout: 15000,
  withCredentials: true, // lets the browser send the HttpOnly refresh cookie
  headers: { 'Content-Type': 'application/json' },
});

/** Request options for endpoints that must not carry a token or trigger a refresh. */
export const PUBLIC_ENDPOINT = { skipAuth: true, skipRefresh: true };

/**
 * Normalises backend RFC 9457 ProblemDetail errors into a plain Error with
 * `status` and `fieldErrors`, so UI code never has to inspect Axios internals.
 */
export function toApiError(error) {
  const problem = error.response?.data;
  const apiError = new Error(
    problem?.detail ?? (error.request ? 'Server is unreachable' : error.message),
  );
  apiError.status = error.response?.status ?? 0;
  apiError.fieldErrors = problem?.errors ?? {};
  return apiError;
}

let refreshPromise = null;

/**
 * Exchanges the refresh cookie for a new access token. Calls made while a refresh is already
 * running share it ("single flight"): refresh tokens rotate, so two parallel refreshes would
 * make the second one look like token theft and sign the user out.
 */
export function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = apiClient
      .post('/auth/refresh', null, PUBLIC_ENDPOINT)
      .then(({ data }) => {
        tokenStore.set(data.accessToken);
        return data;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

apiClient.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token && !config.skipAuth) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const expired = error.response?.status === 401;
    if (expired && original && !original.skipRefresh && !original.alreadyRetried) {
      original.alreadyRetried = true;
      try {
        await refreshSession();
        return await apiClient(original); // the request interceptor adds the new token
      } catch {
        tokenStore.expireSession();
        return Promise.reject(toApiError(error));
      }
    }
    return Promise.reject(toApiError(error));
  },
);
