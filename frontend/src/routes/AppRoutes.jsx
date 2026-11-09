import { Route, Routes } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import AdminCategoriesPage from '../pages/AdminCategoriesPage';
import AdminVendorApplicationsPage from '../pages/AdminVendorApplicationsPage';
import BecomeVendorPage from '../pages/BecomeVendorPage';
import AddressesPage from '../pages/AddressesPage';
import CartPage from '../pages/CartPage';
import CheckoutPage from '../pages/CheckoutPage.jsx';
import ForbiddenPage from '../pages/ForbiddenPage';
import ForgotPasswordPage from '../pages/ForgotPasswordPage';
import HomePage from '../pages/HomePage';
import LoginPage from '../pages/LoginPage';
import MockPaymentPage from '../pages/MockPaymentPage';
import MyOrdersPage from '../pages/MyOrdersPage';
import NotFoundPage from '../pages/NotFoundPage';
import OrderDetailPage from '../pages/OrderDetailPage';
import OrderSuccessPage from '../pages/OrderSuccessPage';
import ProductDetailPage from '../pages/ProductDetailPage';
import ProductListPage from '../pages/ProductListPage';
import ProfilePage from '../pages/ProfilePage';
import RegisterPage from '../pages/RegisterPage';
import ResetPasswordPage from '../pages/ResetPasswordPage';
import VendorDashboardPage from '../pages/VendorDashboardPage';
import VendorOrderDetailPage from '../pages/VendorOrderDetailPage';
import VendorOrdersPage from '../pages/VendorOrdersPage';
import VendorProductFormPage from '../pages/VendorProductFormPage';
import VendorProductsPage from '../pages/VendorProductsPage';
import VerifyEmailPage from '../pages/VerifyEmailPage';
import WishlistPage from '../pages/WishlistPage';
import ProtectedRoute from './ProtectedRoute';

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        {/* Public */}
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="verify-email" element={<VerifyEmailPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />
        <Route path="forbidden" element={<ForbiddenPage />} />
        <Route path="/products" element={<ProductListPage />} />
        <Route path="/products/:slug" element={<ProductDetailPage />} />

        {/* Any logged-in user */}
        <Route element={<ProtectedRoute />}>
          <Route path="account" element={<ProfilePage />} />
          <Route path="cart" element={<CartPage />} />
          <Route path="wishlist" element={<WishlistPage />} />
          <Route path="checkout" element={<CheckoutPage />} />
          <Route path="orders" element={<MyOrdersPage />} />
          <Route path="orders/:orderId" element={<OrderDetailPage />} />
          <Route path="orders/:orderId/pay" element={<MockPaymentPage />} />
          <Route path="orders/:orderId/success" element={<OrderSuccessPage />} />
          <Route path="account/addresses" element={<AddressesPage />} />
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
          <Route path="vendor/orders" element={<VendorOrdersPage />} />
          <Route path="vendor/orders/:subOrderId" element={<VendorOrderDetailPage />} />
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