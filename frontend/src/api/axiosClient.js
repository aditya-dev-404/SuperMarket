import axios from 'axios';
import { env } from '../config/env';

/** Shared Axios instance. All API modules must use this, never raw axios. */
export const apiClient = axios.create({
  baseURL: `${env.apiBaseUrl}/api/v1`,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

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

apiClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(toApiError(error)),
);
