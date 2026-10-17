import { useCallback, useEffect, useMemo, useState } from 'react';
import * as authApi from './authApi';
import { AuthContext } from './AuthContext';
import { tokenStore } from './tokenStore';

/**
 * status: 'loading' (restoring the session on page load) | 'authenticated' | 'anonymous'.
 */
export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let active = true;

    tokenStore.onSessionExpired(() => {
      setUser(null);
      setStatus('anonymous');
    });

    async function restoreSession() {
      try {
        const data = await authApi.refreshSession();
        if (active) {
          setUser(data.user);
          setStatus('authenticated');
        }
      } catch {
        if (active) {
          setStatus('anonymous');
        }
      }
    }
    restoreSession();

    return () => {
      active = false;
      tokenStore.onSessionExpired(null);
    };
  }, []);

  const startSession = useCallback((data) => {
    tokenStore.set(data.accessToken);
    setUser(data.user);
    setStatus('authenticated');
    return data.user;
  }, []);

  const login = useCallback(
    async (credentials) => startSession(await authApi.login(credentials)),
    [startSession],
  );

  const register = useCallback(
    async (details) => startSession(await authApi.register(details)),
    [startSession],
  );

  const endSession = useCallback(() => {
    tokenStore.clear();
    setUser(null);
    setStatus('anonymous');
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      endSession();
    }
  }, [endSession]);

  const value = useMemo(
    () => ({ user, status, login, register, logout, endSession, setUser }),
    [user, status, login, register, logout, endSession],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
