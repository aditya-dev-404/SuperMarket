import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useUserScope } from '../../hooks/useUserScope';
import { cartKey } from '../cart/useCart';
import {
  addWishlistItem,
  getWishlist,
  getWishlistIds,
  moveWishlistItemToCart,
  removeWishlistItem,
} from './wishlistApi';

// Same page size as the storefront list.
export const WISHLIST_PAGE_SIZE = 15;

const wishlistIdsKey = (scope) => ['wishlist', 'ids', scope];
const wishlistPageKey = (scope, page) => ['wishlist', 'page', scope, page];
const ALL_PAGES_KEY = ['wishlist', 'page'];

export function useWishlistIds() {
  const { isAuthenticated, scope } = useUserScope();
  return useQuery({
    queryKey: wishlistIdsKey(scope),
    queryFn: getWishlistIds,
    enabled: isAuthenticated,
  });
}

/** `page` is one-based (as in the URL); the API is zero-based. */
export function useWishlistPage(page) {
  const { isAuthenticated, scope } = useUserScope();
  return useQuery({
    queryKey: wishlistPageKey(scope, page),
    queryFn: () => getWishlist({ page: page - 1, size: WISHLIST_PAGE_SIZE }),
    enabled: isAuthenticated,
    placeholderData: keepPreviousData,
  });
}

/** Keeps the cached id list in step with the server, and refreshes the visible pages. */
function useWishlistCacheSync() {
  const queryClient = useQueryClient();
  const { scope } = useUserScope();
  return (productId, nowWished) => {
    queryClient.setQueryData(wishlistIdsKey(scope), (ids = []) =>
      nowWished ? [...ids, productId] : ids.filter((id) => id !== productId),
    );
    queryClient.invalidateQueries({ queryKey: ALL_PAGES_KEY });
  };
}

export function useToggleWishlist() {
  const sync = useWishlistCacheSync();
  return useMutation({
    mutationFn: ({ productId, wished }) =>
      wished ? removeWishlistItem(productId) : addWishlistItem(productId),
    onSuccess: (_result, { productId, wished }) => sync(productId, !wished),
  });
}

export function useMoveToCart() {
  const queryClient = useQueryClient();
  const { scope } = useUserScope();
  const sync = useWishlistCacheSync();
  return useMutation({
    mutationFn: moveWishlistItemToCart,
    onSuccess: (cart, { productId }) => {
      queryClient.setQueryData(cartKey(scope), cart);
      sync(productId, false);
    },
  });
}