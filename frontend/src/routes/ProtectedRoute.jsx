import { Navigate, Outlet, useLocation } from 'react-router-dom';
import Loader from '../components/common/Loader';
import { useAuth } from '../features/auth/useAuth';

/**
 * Guards a group of routes. Without `roles` any signed-in user may enter; with `roles`
 * (e.g. ['ADMIN']) only those roles may. This is a UX guard only: the backend enforces access.
 */
export default function ProtectedRoute({ roles }) {
  const { status, user } = useAuth();
  const location = useLocation();

  if (status === 'loading') {
    return <Loader label="Loading your session..." />;
  }
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/forbidden" replace />;
  }
  return <Outlet />;
}
