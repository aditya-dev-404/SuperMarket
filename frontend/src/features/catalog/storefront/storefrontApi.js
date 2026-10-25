import { apiClient } from '../../../api/axiosClient';

// If axiosClient's baseURL already ends with "/api/v1", change this to ''.
const API_PREFIX = '';
const PRODUCTS_URL = `${API_PREFIX}/products`;

/** Drops empty / false values so they never reach the query string. */
function compact(params) {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== undefined && value !== null && value !== '' && value !== false,
    ),
  );
}

/** Paginated search. `page` is zero-based here (the URL/UI uses one-based). */
export async function searchProducts(params) {
  const { data } = await apiClient.get(PRODUCTS_URL, { params: compact(params) });
  return data;
}

export async function getProductBySlug(slug) {
  const { data } = await apiClient.get(`${PRODUCTS_URL}/${encodeURIComponent(slug)}`);
  return data;
}

/** Categories, sizes, colours and price range for the filter sidebar. */
export async function getStorefrontFilters(category) {
  const { data } = await apiClient.get(`${PRODUCTS_URL}/filters`, {
    params: compact({ category }),
  });
  return data;
}