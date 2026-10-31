import { useState } from 'react';
import { Link } from 'react-router-dom';
import Pagination from '../components/common/Pagination';
import PageError from '../components/common/PageError';
import PageSkeleton from '../components/common/PageSkeleton';
import { formatPrice } from '../features/catalog/storefront/priceFormat';
import OrderStatusBadge from '../features/orders/OrderStatusBadge';
import { formatDateTime, paymentStatusLabel } from '../features/orders/orderUi';
import { useOrders } from '../features/orders/useOrders';

export default function MyOrdersPage() {
  const [page, setPage] = useState(0);
  const { data, isPending, isError, error, refetch } = useOrders(page);

  if (isPending) return <PageSkeleton />;
  if (isError) return <PageError error={error} onRetry={refetch} />;

  if (data.totalElements === 0) {
    return (
      <div className="mx-auto max-w-xl px-4 py-20 text-center">
        <h1 className="text-2xl font-semibold text-gray-900">No orders yet</h1>
        <p className="mt-2 text-sm text-gray-500">When you place an order it will show up here.</p>
        <Link
          to="/products"
          className="mt-6 inline-block rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
        >
          Start shopping
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="text-2xl font-semibold text-gray-900">My orders</h1>
      <ul className="mt-6 space-y-4">
        {data.content.map((order) => (
          <li key={order.id}>
            <Link
              to={`/orders/${order.id}`}
              className="block rounded-lg border border-gray-200 p-4 transition hover:border-indigo-300 hover:shadow-sm"
            >
              <div className="flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm font-medium text-gray-900">{order.orderNumber}</p>
                <OrderStatusBadge status={order.status} />
              </div>
              <p className="mt-1 text-xs text-gray-500">
                {formatDateTime(order.createdAt)} · {order.itemCount} item{order.itemCount === 1 ? '' : 's'}{' '}
                · {paymentStatusLabel(order.paymentStatus)}
              </p>
              <div className="mt-3 flex items-center justify-between">
                <div className="flex gap-2">
                  {order.thumbnails.slice(0, 4).map((url) => (
                    <img key={url} src={url} alt="" className="h-12 w-12 rounded-md object-cover" />
                  ))}
                </div>
                <p className="text-sm font-semibold text-gray-900">{formatPrice(order.total)}</p>
              </div>
            </Link>
          </li>
        ))}
      </ul>
      <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  );
}