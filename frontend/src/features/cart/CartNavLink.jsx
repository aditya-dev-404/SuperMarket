import { FiShoppingCart } from 'react-icons/fi';
import { NavLink } from 'react-router-dom';
import { useCart } from './useCart';

// Keeps the badge small; a cart this big shows "99+" instead of a wide number.
const MAX_BADGE_COUNT = 99;

/** Header cart link with the number of units in the cart. Render only for logged-in users. */
export default function CartNavLink() {
  const { data } = useCart();
  const count = data?.totalQuantity ?? 0;

  return (
    <NavLink
      to="/cart"
      aria-label={`Cart, ${count} ${count === 1 ? 'item' : 'items'}`}
      className={({ isActive }) =>
        `relative inline-flex items-center transition hover:text-indigo-600 ${
          isActive ? 'text-indigo-600' : 'text-gray-600'
        }`
      }
    >
      <FiShoppingCart className="h-5 w-5" aria-hidden="true" />
      {count > 0 && (
        <span className="absolute -right-3 -top-2 min-w-5 rounded-full bg-indigo-600 px-1 text-center text-xs font-semibold leading-5 text-white">
          {count > MAX_BADGE_COUNT ? `${MAX_BADGE_COUNT}+` : count}
        </span>
      )}
    </NavLink>
  );
}