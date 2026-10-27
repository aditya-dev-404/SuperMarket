import { FiImage, FiMinus, FiPlus, FiTrash2 } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { thumbnail } from '../catalog/storefront/imageUrl';
import { formatPrice } from '../catalog/storefront/priceFormat';

const THUMBNAIL_SIZE = 160;

const STATUS_MESSAGES = {
  OUT_OF_STOCK: 'Out of stock. Remove it to continue.',
  UNAVAILABLE: 'No longer available. Remove it to continue.',
};

function statusMessage(item) {
  if (item.status === 'INSUFFICIENT_STOCK') {
    return `Only ${item.availableStock} left. Reduce the quantity.`;
  }
  return STATUS_MESSAGES[item.status] ?? null;
}

const stepButton =
  'inline-flex h-8 w-8 items-center justify-center rounded-md border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40';

/** One cart line. `busy` disables the controls while any cart request is running. */
export default function CartLineItem({ item, busy, onQuantityChange, onRemove }) {
  const warning = statusMessage(item);
  const canChange = item.status === 'AVAILABLE' || item.status === 'INSUFFICIENT_STOCK';
  const variantLabel = [item.colour, item.size].filter(Boolean).join(' · ');

  return (
    <li className="flex gap-4 py-4">
      <Link
        to={`/products/${item.productSlug}`}
        className="h-20 w-20 shrink-0 overflow-hidden rounded-md bg-gray-100"
      >
        {item.imageUrl ? (
          <img
            src={thumbnail(item.imageUrl, THUMBNAIL_SIZE)}
            alt={item.productName}
            className="h-full w-full object-cover"
          />
        ) : (
          <span className="flex h-full w-full items-center justify-center text-gray-300">
            <FiImage className="h-6 w-6" aria-hidden="true" />
          </span>
        )}
      </Link>

      <div className="min-w-0 flex-1">
        <div className="flex justify-between gap-3">
          <div className="min-w-0">
            <Link
              to={`/products/${item.productSlug}`}
              className="line-clamp-2 text-sm font-medium text-gray-900 hover:underline"
            >
              {item.productName}
            </Link>
            {variantLabel && <p className="mt-0.5 text-xs text-gray-500">{variantLabel}</p>}
            <p className="mt-0.5 text-xs text-gray-500">{formatPrice(item.unitPrice)} each</p>
          </div>
          <p className="shrink-0 text-sm font-semibold text-gray-900">
            {formatPrice(item.lineTotal)}
          </p>
        </div>

        {warning && (
          <p className="mt-1 text-xs font-medium text-red-600" role="alert">
            {warning}
          </p>
        )}

        <div className="mt-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <button
              type="button"
              aria-label="Decrease quantity"
              className={stepButton}
              disabled={busy || !canChange || item.quantity <= 1}
              onClick={() => onQuantityChange(item.id, item.quantity - 1)}
            >
              <FiMinus aria-hidden="true" />
            </button>
            <span className="w-6 text-center text-sm" aria-live="polite">
              {item.quantity}
            </span>
            <button
              type="button"
              aria-label="Increase quantity"
              className={stepButton}
              disabled={busy || !canChange || item.quantity >= item.availableStock}
              onClick={() => onQuantityChange(item.id, item.quantity + 1)}
            >
              <FiPlus aria-hidden="true" />
            </button>
          </div>

          <button
            type="button"
            aria-label={`Remove ${item.productName}`}
            disabled={busy}
            onClick={() => onRemove(item.id)}
            className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-red-600 disabled:opacity-40"
          >
            <FiTrash2 aria-hidden="true" /> Remove
          </button>
        </div>
      </div>
    </li>
  );
}