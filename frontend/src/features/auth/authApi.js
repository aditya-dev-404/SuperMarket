import { apiClient, PUBLIC_ENDPOINT, refreshSession } from '../../api/axiosClient';

export { refreshSession };

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
