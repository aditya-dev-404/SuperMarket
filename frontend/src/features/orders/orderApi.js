import { apiClient } from '../../api/axiosClient';

export async function getCheckoutOptions() {
  const { data } = await apiClient.get('/checkout/options');
  return data;
}

export async function placeOrder({ addressId, paymentMethod }) {
  const { data } = await apiClient.post('/orders', { addressId, paymentMethod });
  return data;
}

export async function listOrders({ page = 0, size = 10 } = {}) {
  const { data } = await apiClient.get('/orders', { params: { page, size } });
  return data;
}

export async function getOrder(orderId) {
  const { data } = await apiClient.get(`/orders/${orderId}`);
  return data;
}

export async function cancelOrder(orderId) {
  const { data } = await apiClient.post(`/orders/${orderId}/cancel`);
  return data;
}

/** Mock payment: `outcome` is 'SUCCESS' or 'FAILURE' so both paths can be tried without a real gateway. */
export async function payOrder({ orderId, outcome }) {
  const { data } = await apiClient.post(`/orders/${orderId}/pay`, { outcome });
  return data;
}