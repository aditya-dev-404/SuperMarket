import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useUserScope } from '../../hooks/useUserScope';
import { cartKey } from '../cart/useCart';
import {
  cancelOrder,
  getCheckoutOptions,
  getOrder,
  listOrders,
  payOrder,
  placeOrder,
} from './orderApi';

export const ordersKey = (scope) => ['orders', scope];
export const orderKey = (scope, orderId) => ['order', scope, String(orderId)];

export function useCheckoutOptions() {
  const { isAuthenticated } = useUserScope();
  return useQuery({
    queryKey: ['checkout-options'],
    queryFn: getCheckoutOptions,
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
  });
}

export function useOrders(page) {
  const { isAuthenticated, scope } = useUserScope();
  return useQuery({
    queryKey: [...ordersKey(scope), page],
    queryFn: () => listOrders({ page }),
    enabled: isAuthenticated,
    placeholderData: (previous) => previous,
  });
}

export function useOrder(orderId) {
  const { isAuthenticated, scope } = useUserScope();
  return useQuery({
    queryKey: orderKey(scope, orderId),
    queryFn: () => getOrder(orderId),
    enabled: isAuthenticated,
  });
}

/** Writes the returned order into the cache and refreshes the lists that depend on it. */
function useOrderMutation(mutationFn, { refreshCart = false } = {}) {
  const queryClient = useQueryClient();
  const { scope } = useUserScope();
  return useMutation({
    mutationFn,
    onSuccess: (order) => {
      queryClient.setQueryData(orderKey(scope, order.id), order);
      queryClient.invalidateQueries({ queryKey: ordersKey(scope) });
      if (refreshCart) queryClient.invalidateQueries({ queryKey: cartKey(scope) });
    },
  });
}

/** Placing an order empties the cart on the server, so the cached cart is refetched. */
export function usePlaceOrder() {
  return useOrderMutation(placeOrder, { refreshCart: true });
}

export function useCancelOrder() {
  return useOrderMutation(cancelOrder);
}

/** A failed payment puts the items back into the cart on the server, so the cart is refetched too. */
export function usePayOrder() {
  return useOrderMutation(payOrder, { refreshCart: true });
}