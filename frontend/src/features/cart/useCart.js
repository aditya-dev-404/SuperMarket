import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useUserScope } from '../../hooks/useUserScope';
import { addCartItem, clearCart, getCart, removeCartItem, updateCartItem } from './cartApi';

const EMPTY_CART = { vendors: [], totalQuantity: 0, subtotal: 0, checkoutReady: false };

export const cartKey = (scope) => ['cart', scope];

export function useCart() {
  const { isAuthenticated, scope } = useUserScope();
  return useQuery({ queryKey: cartKey(scope), queryFn: getCart, enabled: isAuthenticated });
}

/** The API returns the whole updated cart, so a successful call just replaces the cached one. */
function useCartMutation(mutationFn) {
  const queryClient = useQueryClient();
  const { scope } = useUserScope();
  return useMutation({
    mutationFn,
    onSuccess: (cart) => queryClient.setQueryData(cartKey(scope), cart),
  });
}

export function useAddToCart() {
  return useCartMutation(addCartItem);
}

export function useUpdateCartItem() {
  return useCartMutation(updateCartItem);
}

export function useRemoveCartItem() {
  return useCartMutation(removeCartItem);
}

export function useClearCart() {
  const queryClient = useQueryClient();
  const { scope } = useUserScope();
  return useMutation({
    mutationFn: clearCart,
    onSuccess: () => queryClient.setQueryData(cartKey(scope), EMPTY_CART),
  });
}