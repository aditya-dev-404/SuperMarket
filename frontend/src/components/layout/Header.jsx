import { FiHeart } from 'react-icons/fi';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { env } from '../../config/env';
import { useAuth } from '../../features/auth/useAuth';
import CartNavLink from '../../features/cart/CartNavLink';
import HeaderSearch from './HeaderSearch';

const navLink = ({ isActive }) =>
  `text-sm font-medium transition hover:text-indigo-600 ${isActive ? 'text-indigo-600' : 'text-gray-600'}`;

const iconLink = ({ isActive }) =>
  `inline-flex items-center transition hover:text-indigo-600 ${isActive ? 'text-indigo-600' : 'text-gray-600'}`;

export default function Header() {
  const { user, status, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/', { replace: true });
  };

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3">
        <Link to="/" className="text-lg font-bold text-indigo-600">
          {env.appName}
        </Link>

        {/* full-width row under the logo/nav on mobile, centred between them on desktop */}
        <HeaderSearch className="order-last w-full md:order-none md:w-auto md:max-w-sm md:flex-1" />

        <nav aria-label="Main navigation" className="flex flex-wrap items-center gap-5">
          <NavLink to="/" end className={navLink}>
            Home
          </NavLink>
          <NavLink to="/products" className={navLink}>
            Shop
          </NavLink>

          {status === 'authenticated' && (
            <>
              {user.role === 'CUSTOMER' && (
                <NavLink to="/become-vendor" className={navLink}>
                  Become a vendor
                </NavLink>
              )}
              {user.role === 'VENDOR' && (
                <>
                  <NavLink to="/vendor/dashboard" className={navLink}>
                    Vendor dashboard
                  </NavLink>
                  <NavLink to="/vendor/products" className={navLink}>
                    My products
                  </NavLink>
                </>
              )}
              {user.role === 'ADMIN' && (
                <>
                  <NavLink to="/admin/vendor-applications" className={navLink}>
                    Vendor applications
                  </NavLink>
                  <NavLink to="/admin/categories" className={navLink}>
                    Categories
                  </NavLink>
                </>
              )}
              <NavLink to="/wishlist" aria-label="Wishlist" className={iconLink}>
                <FiHeart className="h-5 w-5" aria-hidden="true" />
              </NavLink>
              <CartNavLink />
              <NavLink to="/account" className={navLink}>
                {user.firstName}
              </NavLink>
              <button
                type="button"
                onClick={handleLogout}
                className="text-sm font-medium text-gray-600 hover:text-red-600"
              >
                Log out
              </button>
            </>
          )}

          {status === 'anonymous' && (
            <>
              <NavLink to="/login" className={navLink}>
                Log in
              </NavLink>
              <Link
                to="/register"
                className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-700"
              >
                Sign up
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}