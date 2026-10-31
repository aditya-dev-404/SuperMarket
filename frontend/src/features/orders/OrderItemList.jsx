import { Link } from 'react-router-dom';
import { formatPrice } from '../catalog/storefront/priceFormat';

/** The purchased lines of a (sub-)order, from the snapshot taken at checkout. */
export default function OrderItemList({ items }) {
  return (
    <ul className="divide-y divide-gray-100">
      {items.map((item) => (
        <li key={item.id} className="flex gap-4 py-3">
          {item.imageUrl ? (
            <img src={item.imageUrl} alt="" className="h-16 w-16 rounded-md object-cover" />
          ) : (
            <div className="h-16 w-16 rounded-md bg-gray-100" aria-hidden="true" />
          )}
          <div className="min-w-0 flex-1">
            {item.productSlug ? (
              <Link
                to={`/products/${item.productSlug}`}
                className="block truncate text-sm font-medium text-gray-900 hover:text-indigo-600"
              >
                {item.productName}
              </Link>
            ) : (
              <span className="block truncate text-sm font-medium text-gray-900">{item.productName}</span>
            )}
            <p className="text-xs text-gray-500">
              {[item.size, item.colour].filter(Boolean).join(' · ') || item.sku}
            </p>
            <p className="text-xs text-gray-500">
              {item.quantity} × {formatPrice(item.unitPrice)}
            </p>
          </div>
          <p className="text-sm font-medium text-gray-900">{formatPrice(item.lineTotal)}</p>
        </li>
      ))}
    </ul>
  );
}
