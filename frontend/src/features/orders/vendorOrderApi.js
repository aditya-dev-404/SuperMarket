import { apiClient } from '../../api/axiosClient';

export async function listVendorSubOrders({ status, page = 0, size = 10 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  const { data } = await apiClient.get('/vendor/sub-orders', { params });
  return data;
}

export async function getVendorSubOrder(subOrderId) {
  const { data } = await apiClient.get(`/vendor/sub-orders/${subOrderId}`);
  return data;
}

export async function updateVendorSubOrderStatus({ subOrderId, status }) {
  const { data } = await apiClient.patch(`/vendor/sub-orders/${subOrderId}/status`, { status });
  return data;
}