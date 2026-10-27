import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import ErrorMessage from '../../components/common/ErrorMessage';
import { getProductBySlug } from '../catalog/storefront/storefrontApi';
import { useVariantSelection } from '../catalog/storefront/useVariantSelection';
import VariantSelector from '../catalog/storefront/VariantSelector';
import { useMoveToCart } from './useWishlist';

/** The size/colour picker; mounted only after the product (with its variants) has loaded. */
function VariantPanel({ detail }) {
  const selection = useVariantSelection(detail.variants);
  const move = useMoveToCart();
  const { selected } = selection;
  const canMove = Boolean(selected?.inStock) && !move.isPending;

  return (
    <div className="mt-2 space-y-3 rounded-md border border-gray-200 p-3">
      <VariantSelector selection={selection} />
      {move.isError && <ErrorMessage message={move.error.message} />}
      <button
        type="button"
        disabled={!canMove}
        onClick={() => move.mutate({ productId: detail.id, variantId: selected.id })}
        className="w-full rounded-md bg-gray-900 px-3 py-2 text-sm font-medium text-white hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-40"
      >
        {move.isPending ? 'Moving...' : 'Add to cart'}
      </button>
    </div>
  );
}

/** "Move to cart" for a wishlist card: pick a size/colour, then the product leaves the wishlist. */
export default function MoveToCart({ product }) {
  const [open, setOpen] = useState(false);
  // The full product (with variants) is fetched only when the picker is opened.
  const detail = useQuery({
    queryKey: ['product', product.slug],
    queryFn: () => getProductBySlug(product.slug),
    enabled: open && product.inStock,
  });

  if (!product.inStock) return null;

  return (
    <div className="mt-2">
      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm font-medium text-gray-800 hover:border-gray-900"
      >
        {open ? 'Cancel' : 'Move to cart'}
      </button>

      {open && detail.isPending && <p className="mt-2 text-sm text-gray-500">Loading options...</p>}
      {open && detail.isError && (
        <div className="mt-2">
          <ErrorMessage message={detail.error.message} />
        </div>
      )}
      {open && detail.data && <VariantPanel key={detail.data.id} detail={detail.data} />}
    </div>
  );
}