import { apiClient } from '../../api/axiosClient';

/** Customer side. getMyApplication rejects with status 404 when the user has not applied yet. */
export async function getMyApplication() {
  const { data } = await apiClient.get('/vendor-applications/me');
  return data;
}

export async function applyForVendor(details) {
  const { data } = await apiClient.post('/vendor-applications', details);
  return data;
}

export async function resubmitApplication(details) {
  const { data } = await apiClient.put('/vendor-applications/me', details);
  return data;
}

/** Admin side. */
export async function listApplications({ status, page = 0, size = 10 }) {
  const { data } = await apiClient.get('/admin/vendor-applications', {
    params: { status: status || undefined, page, size },
  });
  return data;
}

export async function approveApplication(id) {
  const { data } = await apiClient.post(`/admin/vendor-applications/${id}/approve`);
  return data;
}

export async function rejectApplication(id, reason) {
  const { data } = await apiClient.post(`/admin/vendor-applications/${id}/reject`, { reason });
  return data;
}