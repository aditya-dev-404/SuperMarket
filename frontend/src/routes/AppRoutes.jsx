import { Route, Routes } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import AdminCategoriesPage from '../pages/AdminCategoriesPage';
import AdminVendorApplicationsPage from '../pages/AdminVendorApplicationsPage';
import BecomeVendorPage from '../pages/BecomeVendorPage';
import ForbiddenPage from '../pages/ForbiddenPage';
import HomePage from '../pages/HomePage';
import LoginPage from '../pages/LoginPage';
import NotFoundPage from '../pages/NotFoundPage';
import ProfilePage from '../pages/ProfilePage';
import RegisterPage from '../pages/RegisterPage';
import VendorDashboardPage from '../pages/VendorDashboardPage';
import VendorProductFormPage from '../pages/VendorProductFormPage';
import VendorProductsPage from '../pages/VendorProductsPage';
import ProtectedRoute from './ProtectedRoute';

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        {/* Public */}
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="forbidden" element={<ForbiddenPage />} />

        {/* Any logged-in user */}
        <Route element={<ProtectedRoute />}>
          <Route path="account" element={<ProfilePage />} />
        </Route>

        {/* CUSTOMER applies; VENDOR is allowed in so the page can redirect them after activation */}
        <Route element={<ProtectedRoute roles={['CUSTOMER', 'VENDOR']} />}>
          <Route path="become-vendor" element={<BecomeVendorPage />} />
        </Route>

        {/* Vendor */}
        <Route element={<ProtectedRoute roles={['VENDOR']} />}>
          <Route path="vendor/dashboard" element={<VendorDashboardPage />} />
          <Route path="vendor/products" element={<VendorProductsPage />} />
          <Route path="vendor/products/new" element={<VendorProductFormPage />} />
          <Route path="vendor/products/:id/edit" element={<VendorProductFormPage />} />
        </Route>

        {/* Admin */}
        <Route element={<ProtectedRoute roles={['ADMIN']} />}>
          <Route path="admin/vendor-applications" element={<AdminVendorApplicationsPage />} />
          <Route path="admin/categories" element={<AdminCategoriesPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}