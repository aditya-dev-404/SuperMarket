import { apiClient } from '../../api/axiosClient';

// Every call returns the full, updated cart, so the UI never has to merge responses.

export async function getCart() {
  const { data } = await apiClient.get('/cart');
  return data;
}

export async function addCartItem({ variantId, quantity = 1 }) {
  const { data } = await apiClient.post('/cart/items', { variantId, quantity });
  return data;
}

export async function updateCartItem({ itemId, quantity }) {
  const { data } = await apiClient.patch(`/cart/items/${itemId}`, { quantity });
  return data;
}

export async function removeCartItem(itemId) {
  const { data } = await apiClient.delete(`/cart/items/${itemId}`);
  return data;
}

export async function clearCart() {
  await apiClient.delete('/cart');
}