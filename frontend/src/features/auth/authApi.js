import { apiClient, PUBLIC_ENDPOINT, refreshSession } from '../../api/axiosClient';

export { refreshSession };

/** Creates an unverified account. The user is NOT logged in: they must click the emailed link first. */
export async function register(payload) {
  const { data } = await apiClient.post('/auth/register', payload, PUBLIC_ENDPOINT);
  return data;
}

export async function login(payload) {
  const { data } = await apiClient.post('/auth/login', payload, PUBLIC_ENDPOINT);
  return data;
}

export async function logout() {
  await apiClient.post('/auth/logout', null, PUBLIC_ENDPOINT);
}

export async function changePassword(payload) {
  await apiClient.post('/auth/change-password', payload);
}

export async function fetchProfile() {
  const { data } = await apiClient.get('/users/me');
  return data;
}

export async function updateProfile(payload) {
  const { data } = await apiClient.put('/users/me', payload);
  return data;
}

export async function verifyEmail(token) {
  await apiClient.post('/auth/verify-email', { token }, PUBLIC_ENDPOINT);
}

export async function resendVerification(email) {
  const { data } = await apiClient.post('/auth/resend-verification', { email }, PUBLIC_ENDPOINT);
  return data;
}

export async function forgotPassword(email) {
  const { data } = await apiClient.post('/auth/forgot-password', { email }, PUBLIC_ENDPOINT);
  return data;
}

export async function resetPassword({ token, newPassword }) {
  await apiClient.post('/auth/reset-password', { token, newPassword }, PUBLIC_ENDPOINT);
}