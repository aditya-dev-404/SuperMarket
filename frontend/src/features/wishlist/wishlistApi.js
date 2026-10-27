import { apiClient } from '../../api/axiosClient';

/** `page` is zero-based, like the backend. */
export async function getWishlist({ page, size }) {
  const { data } = await apiClient.get('/wishlist', { params: { page, size } });
  return data;
}

/** Ids of every wishlisted product, used to fill the heart icons on list pages. */
export async function getWishlistIds() {
  const { data } = await apiClient.get('/wishlist/ids');
  return data;
}

export async function addWishlistItem(productId) {
  await apiClient.post('/wishlist/items', { productId });
}

export async function removeWishlistItem(productId) {
  await apiClient.delete(`/wishlist/items/${productId}`);
}

/** Adds one unit of the variant to the cart and removes the product from the wishlist. */
export async function moveWishlistItemToCart({ productId, variantId }) {
  const { data } = await apiClient.post(`/wishlist/items/${productId}/move-to-cart`, {
    variantId,
  });
  return data;
}