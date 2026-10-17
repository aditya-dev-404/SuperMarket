import { apiClient } from './axiosClient';

export async function fetchPing() {
  const { data } = await apiClient.get('/public/ping');
  return data;
}
