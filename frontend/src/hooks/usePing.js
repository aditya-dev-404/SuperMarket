import { useQuery } from '@tanstack/react-query';
import { fetchPing } from '../api/systemApi';

/** Checks that the backend is reachable. */
export function usePing() {
  return useQuery({ queryKey: ['system', 'ping'], queryFn: fetchPing, retry: false });
}
