import { FiShoppingCart } from 'react-icons/fi';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { useAddToCart } from './useCart';

/** Adds one unit of the chosen variant. Logged-out users are sent to log in and come back here. */
export default function AddToCartButton({ variantId, disabled = false }) {
  const { status } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const addToCart = useAddToCart();

  const handleClick = () => {
    if (status !== 'authenticated') {
      navigate('/login', { state: { from: location } });
      return;
    }
    addToCart.mutate({ variantId, quantity: 1 });
  };

  return (
    <div className="flex-1">
      <button
        type="button"
        onClick={handleClick}
        disabled={disabled || !variantId || addToCart.isPending || status === 'loading'}
        className="inline-flex w-full items-center justify-center gap-2 rounded-md bg-gray-900 px-5 py-3 text-sm font-medium text-white hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-40"
      >
        <FiShoppingCart aria-hidden="true" />
        {addToCart.isPending ? 'Adding...' : 'Add to cart'}
      </button>

      {addToCart.isSuccess && (
        <p className="mt-2 text-sm text-green-700" role="status">
          Added to your cart.{' '}
          <Link to="/cart" className="font-medium underline">
            View cart
          </Link>
        </p>
      )}
      {addToCart.isError && (
        <p className="mt-2 text-sm text-red-600" role="alert">
          {addToCart.error.message}
        </p>
      )}
    </div>
  );
}