import { apiClient } from '../../api/axiosClient';

const data = (res) => res.data;

// categories
export const getCategories = () => apiClient.get('/categories').then(data);
export const createCategory = (body) => apiClient.post('/admin/categories', body).then(data);
export const updateCategory = (id, body) =>
    apiClient.put(`/admin/categories/${id}`, body).then(data);
export const deactivateCategory = (id) => apiClient.delete(`/admin/categories/${id}`);

// vendor products
export const listMyProducts = (params) =>
    apiClient.get('/vendor/products', { params }).then(data);
export const getMyProduct = (id) => apiClient.get(`/vendor/products/${id}`).then(data);
export const createProduct = (body) => apiClient.post('/vendor/products', body).then(data);
export const updateProduct = (id, body) =>
    apiClient.put(`/vendor/products/${id}`, body).then(data);
export const changeProductStatus = (id, status) =>
    apiClient.patch(`/vendor/products/${id}/status`, { status }).then(data);

// variants
export const addVariant = (id, body) =>
    apiClient.post(`/vendor/products/${id}/variants`, body).then(data);
export const updateVariant = (id, variantId, body) =>
    apiClient.put(`/vendor/products/${id}/variants/${variantId}`, body).then(data);
export const deleteVariant = (id, variantId) =>
    apiClient.delete(`/vendor/products/${id}/variants/${variantId}`).then(data);

// images
export const addImage = (id, body) =>
    apiClient.post(`/vendor/products/${id}/images`, body).then(data);
export const deleteImage = (id, imageId) =>
    apiClient.delete(`/vendor/products/${id}/images/${imageId}`).then(data);

export const getUploadSignature = () =>
    apiClient.post('/vendor/uploads/signature').then(data);