import { Route, Routes } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import ForbiddenPage from '../pages/ForbiddenPage';
import HomePage from '../pages/HomePage';
import LoginPage from '../pages/LoginPage';
import NotFoundPage from '../pages/NotFoundPage';
import ProfilePage from '../pages/ProfilePage';
import RegisterPage from '../pages/RegisterPage';
import ProtectedRoute from './ProtectedRoute';
import AdminVendorApplicationsPage from '../pages/AdminVendorApplicationsPage';
import BecomeVendorPage from '../pages/BecomeVendorPage';
import VendorDashboardPage from '../pages/VendorDashboardPage';

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="forbidden" element={<ForbiddenPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="account" element={<ProfilePage />} />
        </Route>
                {/* CUSTOMER applies; VENDOR is allowed in so the page can redirect them after activation */}
        <Route element={<ProtectedRoute roles={['CUSTOMER', 'VENDOR']} />}>
          <Route path="become-vendor" element={<BecomeVendorPage />} />
        </Route>

        <Route element={<ProtectedRoute roles={['VENDOR']} />}>
          <Route path="vendor/dashboard" element={<VendorDashboardPage />} />
        </Route>

        <Route element={<ProtectedRoute roles={['ADMIN']} />}>
          <Route path="admin/vendor-applications" element={<AdminVendorApplicationsPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
