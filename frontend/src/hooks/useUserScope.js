import { useAuth } from '../features/auth/useAuth';

/**
 * Identifies the logged-in user for query keys, so cached cart/wishlist data
 * can never be shown to a different account on the same browser.
 */
export function useUserScope() {
  const { user, status } = useAuth();
  return {
    isAuthenticated: status === 'authenticated',
    scope: user?.id ?? user?.email ?? null,
  };
}