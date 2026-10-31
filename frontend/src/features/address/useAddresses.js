import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useUserScope } from '../../hooks/useUserScope';
import {
  createAddress,
  deleteAddress,
  listAddresses,
  setDefaultAddress,
  updateAddress,
} from './addressApi';

export const addressesKey = (scope) => ['addresses', scope];

export function useAddresses() {
  const { isAuthenticated, scope } = useUserScope();
  return useQuery({ queryKey: addressesKey(scope), queryFn: listAddresses, enabled: isAuthenticated });
}

/** Any change can move the default flag, so every successful call refetches the list. */
function useAddressMutation(mutationFn) {
  const queryClient = useQueryClient();
  const { scope } = useUserScope();
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: addressesKey(scope) }),
  });
}

export function useCreateAddress() {
  return useAddressMutation(createAddress);
}

export function useUpdateAddress() {
  return useAddressMutation(updateAddress);
}

export function useSetDefaultAddress() {
  return useAddressMutation(setDefaultAddress);
}

export function useDeleteAddress() {
  return useAddressMutation(deleteAddress);
}