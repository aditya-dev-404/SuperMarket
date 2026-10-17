import { AxiosError } from 'axios';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { tokenStore } from '../features/auth/tokenStore';
import { apiClient } from './axiosClient';

const originalAdapter = apiClient.defaults.adapter;

function ok(config, data) {
  return { data, status: 200, statusText: 'OK', headers: {}, config };
}

function unauthorized(config) {
  const response = {
    data: { detail: 'expired' },
    status: 401,
    statusText: 'Unauthorized',
    headers: {},
    config,
  };
  return new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, {}, response);
}

describe('apiClient token handling', () => {
  let calls;

  beforeEach(() => {
    calls = [];
    tokenStore.clear();
  });

  afterEach(() => {
    apiClient.defaults.adapter = originalAdapter;
    tokenStore.clear();
  });

  it('refreshes once and retries the request when the access token has expired', async () => {
    tokenStore.set('old-token');
    apiClient.defaults.adapter = async (config) => {
      const auth = config.headers.get('Authorization');
      calls.push(`${config.url} ${auth ?? 'no-auth'}`);
      if (config.url === '/auth/refresh') {
        return ok(config, { accessToken: 'new-token', user: { id: 1 } });
      }
      if (auth === 'Bearer new-token') {
        return ok(config, { orders: [] });
      }
      throw unauthorized(config);
    };

    const response = await apiClient.get('/orders');

    expect(response.data).toEqual({ orders: [] });
    expect(calls).toEqual([
      '/orders Bearer old-token',
      '/auth/refresh no-auth',
      '/orders Bearer new-token',
    ]);
    expect(tokenStore.get()).toBe('new-token');
  });

  it('shares one refresh between parallel requests', async () => {
    tokenStore.set('old-token');
    let refreshCalls = 0;
    apiClient.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        refreshCalls += 1;
        return ok(config, { accessToken: 'new-token', user: { id: 1 } });
      }
      if (config.headers.get('Authorization') === 'Bearer new-token') {
        return ok(config, {});
      }
      throw unauthorized(config);
    };

    await Promise.all([apiClient.get('/a'), apiClient.get('/b'), apiClient.get('/c')]);

    expect(refreshCalls).toBe(1);
  });

  it('expires the session when the refresh fails', async () => {
    tokenStore.set('old-token');
    let expired = false;
    tokenStore.onSessionExpired(() => {
      expired = true;
    });
    apiClient.defaults.adapter = async (config) => {
      throw unauthorized(config);
    };

    await expect(apiClient.get('/orders')).rejects.toMatchObject({ status: 401 });

    expect(expired).toBe(true);
    expect(tokenStore.get()).toBeNull();
    tokenStore.onSessionExpired(null);
  });

  it('does not try to refresh after a failed login', async () => {
    apiClient.defaults.adapter = async (config) => {
      calls.push(config.url);
      throw unauthorized(config);
    };

    await expect(
      apiClient.post('/auth/login', {}, { skipAuth: true, skipRefresh: true }),
    ).rejects.toMatchObject({ status: 401 });

    expect(calls).toEqual(['/auth/login']);
  });
});
