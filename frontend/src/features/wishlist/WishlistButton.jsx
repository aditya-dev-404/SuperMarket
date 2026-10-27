import { FiHeart } from 'react-icons/fi';
import { useLocation, useNavigate } from 'react-router-dom';
import { useUserScope } from '../../hooks/useUserScope';
import { useToggleWishlist, useWishlistIds } from './useWishlist';

const VARIANT_CLASSES = {
  overlay: 'absolute right-2 top-2 h-9 w-9 rounded-full bg-white/90 shadow hover:bg-white',
  bordered: 'h-12 w-12 rounded-md border border-gray-300 hover:border-gray-900',
};

/**
 * Heart toggle. `overlay` sits on top of a product card image (parent must be `relative`);
 * `bordered` is the standalone button on the product page. Logged-out users are sent to log in.
 */
export default function WishlistButton({ productId, variant = 'bordered' }) {
  const { isAuthenticated } = useUserScope();
  const navigate = useNavigate();
  const location = useLocation();
  const { data: wishlistIds } = useWishlistIds();
  const toggle = useToggleWishlist();

  const wished = wishlistIds?.includes(productId) ?? false;

  const handleClick = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } });
      return;
    }
    toggle.mutate({ productId, wished });
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={toggle.isPending}
      aria-pressed={wished}
      aria-label={wished ? 'Remove from wishlist' : 'Add to wishlist'}
      className={`inline-flex items-center justify-center text-gray-700 transition disabled:opacity-60 ${VARIANT_CLASSES[variant]}`}
    >
      <FiHeart
        aria-hidden="true"
        fill={wished ? 'currentColor' : 'none'}
        className={wished ? 'text-red-500' : ''}
      />
    </button>
  );
}