import { apiClient } from '../../api/axiosClient';

export async function listAddresses() {
  const { data } = await apiClient.get('/addresses');
  return data;
}

export async function createAddress(address) {
  const { data } = await apiClient.post('/addresses', address);
  return data;
}

export async function updateAddress({ id, ...address }) {
  const { data } = await apiClient.put(`/addresses/${id}`, address);
  return data;
}

export async function setDefaultAddress(id) {
  const { data } = await apiClient.post(`/addresses/${id}/default`);
  return data;
}

export async function deleteAddress(id) {
  await apiClient.delete(`/addresses/${id}`);
}