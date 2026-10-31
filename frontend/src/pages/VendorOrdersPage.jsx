import { useState } from 'react';
import { Link } from 'react-router-dom';
import Pagination from '../components/common/Pagination';
import PageError from '../components/common/PageError';
import PageSkeleton from '../components/common/PageSkeleton';
import { formatPrice } from '../features/catalog/storefront/priceFormat';
import OrderStatusBadge from '../features/orders/OrderStatusBadge';
import { VENDOR_STATUS_FILTERS, formatDateTime, statusLabel } from '../features/orders/orderUi';
import { useVendorSubOrders } from '../features/orders/useVendorOrders';

export default function VendorOrdersPage() {
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const { data, isPending, isError, error, refetch } = useVendorSubOrders({
    status: status || undefined,
    page,
  });

  const handleFilter = (event) => {
    setStatus(event.target.value);
    setPage(0);
  };

  if (isPending) return <PageSkeleton />;
  if (isError) return <PageError error={error} onRetry={refetch} />;

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold text-gray-900">Orders to fulfil</h1>
        <label className="flex items-center gap-2 text-sm text-gray-600">
          Status
          <select
            value={status}
            onChange={handleFilter}
            className="rounded-md border border-gray-300 px-2 py-1.5 text-sm"
          >
            <option value="">All</option>
            {VENDOR_STATUS_FILTERS.map((value) => (
              <option key={value} value={value}>
                {statusLabel(value)}
              </option>
            ))}
          </select>
        </label>
      </div>

      {data.content.length === 0 ? (
        <p className="mt-16 text-center text-sm text-gray-500">No orders here yet.</p>
      ) : (
        <ul className="mt-6 space-y-3">
          {data.content.map((subOrder) => (
            <li key={subOrder.id}>
              <Link
                to={`/vendor/orders/${subOrder.id}`}
                className="block rounded-lg border border-gray-200 p-4 transition hover:border-indigo-300 hover:shadow-sm"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-sm font-medium text-gray-900">{subOrder.orderNumber}</p>
                  <OrderStatusBadge status={subOrder.status} />
                </div>
                <p className="mt-1 text-xs text-gray-500">
                  {formatDateTime(subOrder.createdAt)} · {subOrder.itemCount} item
                  {subOrder.itemCount === 1 ? '' : 's'} · {subOrder.recipientName}, {subOrder.city} ·{' '}
                  {subOrder.paymentMethod === 'COD' ? 'Cash on delivery' : 'Paid online'}
                </p>
                <p className="mt-2 text-right text-sm font-semibold text-gray-900">
                  {formatPrice(subOrder.subtotal)}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      )}
      <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  );
}