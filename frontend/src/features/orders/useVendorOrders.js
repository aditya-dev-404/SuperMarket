import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useUserScope } from '../../hooks/useUserScope';
import {
  getVendorSubOrder,
  listVendorSubOrders,
  updateVendorSubOrderStatus,
} from './vendorOrderApi';

const listKey = (scope) => ['vendor-sub-orders', scope];
const detailKey = (scope, subOrderId) => ['vendor-sub-order', scope, String(subOrderId)];

export function useVendorSubOrders({ status, page }) {
  const { isAuthenticated, scope } = useUserScope();
  return useQuery({
    queryKey: [...listKey(scope), status ?? 'ALL', page],
    queryFn: () => listVendorSubOrders({ status, page }),
    enabled: isAuthenticated,
    placeholderData: (previous) => previous,
  });
}

export function useVendorSubOrder(subOrderId) {
  const { isAuthenticated, scope } = useUserScope();
  return useQuery({
    queryKey: detailKey(scope, subOrderId),
    queryFn: () => getVendorSubOrder(subOrderId),
    enabled: isAuthenticated,
  });
}

export function useUpdateSubOrderStatus() {
  const queryClient = useQueryClient();
  const { scope } = useUserScope();
  return useMutation({
    mutationFn: updateVendorSubOrderStatus,
    onSuccess: (subOrder) => {
      queryClient.setQueryData(detailKey(scope, subOrder.id), subOrder);
      queryClient.invalidateQueries({ queryKey: listKey(scope) });
    },
  });
}